package com.aihot.integration.aibase;

import com.aihot.domain.content.AibaseArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AibaseDailyHtmlParser {

    private static final Pattern NEWS_LINK_PATTERN = Pattern.compile(
            "<a[^>]+href=[\"'](?:https?://(?:www\\.)?aibase\\.com)?(/news/\\d+)[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern DAILY_LINK_PATTERN =
            Pattern.compile("href=[\"'](/(?:zh/)?daily/(\\d+))[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern DAILY_SECTION_TITLE_PATTERN = Pattern.compile(
            "<strong>\\s*(\\d+)\\s*、\\s*(.*?)</strong>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern PARAGRAPH_PATTERN =
            Pattern.compile("<p[^>]*>((?:[^<]|<(?!/p>))*)</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern POST_CONTENT_PATTERN = Pattern.compile(
            "class=\"[^\"]*\\bpost-content\\b[^\"]*\"[^>]*>(.*?)</div>\\s*<div\\s+class=\"my-8",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SCRIPT_OR_STYLE_PATTERN =
            Pattern.compile("<(script|style)[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    private static final DateTimeFormatter DAILY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/M/d[ HH:mm:ss][ HH:mm:ss.SSSSSSS]");

    private final ObjectMapper objectMapper;

    public AibaseDailyHtmlParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 从日报列表页解析指定日期对应的详情页 URL。 */
    public Optional<String> resolveDailyDetailUrl(String listHtml, String baseUrl, LocalDate reportDate) {
        if (!StringUtils.hasText(listHtml)) {
            return Optional.empty();
        }
        JsonNode dailyList = extractInitialDailyList(listHtml);
        if (dailyList != null && dailyList.isArray()) {
            JsonNode targetDaily = selectDailyNode(dailyList, reportDate);
            if (targetDaily != null) {
                String dailyId = textValue(targetDaily, "Id");
                if (StringUtils.hasText(dailyId)) {
                    return Optional.of(buildZhDailyDetailUrl(baseUrl, dailyId));
                }
            }
        }
        return resolveDailyDetailUrlFromLinks(listHtml, baseUrl, reportDate);
    }

    /** 从列表页 ailoglist 构建标题到新闻 URL 的映射，供详情页条目匹配原文链接。 */
    public Map<String, String> buildTitleToNewsUrlMap(String listHtml, String newsBaseUrl, LocalDate reportDate) {
        Map<String, String> titleToUrl = new LinkedHashMap<>();
        JsonNode dailyList = extractInitialDailyList(listHtml);
        if (dailyList == null || !dailyList.isArray()) {
            return titleToUrl;
        }
        JsonNode targetDaily = selectDailyNode(dailyList, reportDate);
        if (targetDaily == null) {
            return titleToUrl;
        }
        JsonNode articleList = targetDaily.get("ailoglist");
        if (articleList == null || !articleList.isArray()) {
            return titleToUrl;
        }
        for (JsonNode item : articleList) {
            String newsId = textValue(item, "Id");
            String title = cleanNewsTitle(textValue(item, "title"));
            if (StringUtils.hasText(newsId) && StringUtils.hasText(title)) {
                titleToUrl.put(normalizeTitleKey(title), normalizeUrl(newsBaseUrl, "/zh/news/" + newsId));
            }
        }
        return titleToUrl;
    }

    /** 解析日报详情页中每条热点的标题与 summary。 */
    public List<AibaseArticle> parseDailyDetailArticles(
            String detailHtml, Map<String, String> titleToNewsUrl, String newsBaseUrl) {
        if (!StringUtils.hasText(detailHtml)) {
            return List.of();
        }
        String decodedHtml = decodeUnicodeEscapes(prepareHtmlForSectionParsing(detailHtml));
        List<SectionMarker> sections = findDailySections(decodedHtml);
        if (sections.isEmpty()) {
            return List.of();
        }
        List<AibaseArticle> articles = new ArrayList<>(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            SectionMarker section = sections.get(i);
            int nextStart = i + 1 < sections.size() ? sections.get(i + 1).start() : decodedHtml.length();
            String summary = extractSectionSummary(decodedHtml, section.end(), nextStart);
            String sourceUrl = resolveNewsUrl(section.title(), titleToNewsUrl, newsBaseUrl);
            articles.add(new AibaseArticle(section.title(), sourceUrl, summary));
        }
        return articles;
    }

    /** 兼容旧调用：列表页直接解析（无详情页时使用）。 */
    public List<AibaseArticle> parseDailyList(String html, String baseUrl) {
        return parseDailyListForDate(html, baseUrl, null);
    }

    public List<AibaseArticle> parseDailyListForDate(String html, String baseUrl, LocalDate reportDate) {
        if (!StringUtils.hasText(html)) {
            return List.of();
        }
        List<AibaseArticle> fromJson = parseDailyListFromEmbeddedJson(html, baseUrl, reportDate);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        return parseDailyListFromLinks(html, baseUrl);
    }

    private Optional<String> resolveDailyDetailUrlFromLinks(String listHtml, String baseUrl, LocalDate reportDate) {
        Matcher matcher = DAILY_LINK_PATTERN.matcher(listHtml);
        while (matcher.find()) {
            String dailyId = matcher.group(2);
            if (!StringUtils.hasText(dailyId)) {
                continue;
            }
            if (reportDate == null) {
                return Optional.of(buildZhDailyDetailUrl(baseUrl, dailyId));
            }
            String context = listHtml.substring(
                    Math.max(0, matcher.start() - 80), Math.min(listHtml.length(), matcher.end() + 80));
            if (context.contains(formatDailyDateLabel(reportDate))) {
                return Optional.of(buildZhDailyDetailUrl(baseUrl, dailyId));
            }
        }
        if (matcher.reset().find()) {
            return Optional.of(buildZhDailyDetailUrl(baseUrl, matcher.group(2)));
        }
        return Optional.empty();
    }

    /** 中文日报详情页必须带 /zh/ 前缀，否则默认返回英文内容。 */
    static String buildZhDailyDetailUrl(String baseUrl, String dailyId) {
        return normalizeUrl(baseUrl, "/zh/daily/" + dailyId);
    }

    private static String formatDailyDateLabel(LocalDate date) {
        return date.getYear() + "年" + date.getMonthValue() + "月" + date.getDayOfMonth();
    }

    private List<SectionMarker> findDailySections(String decodedHtml) {
        List<SectionMarker> sections = new ArrayList<>();
        Map<String, SectionMarker> dedupedByTitle = new LinkedHashMap<>();
        Matcher matcher = DAILY_SECTION_TITLE_PATTERN.matcher(decodedHtml);
        while (matcher.find()) {
            String title = cleanNewsTitle(stripTags(matcher.group(2)));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            String titleKey = normalizeTitleKey(title);
            if (!dedupedByTitle.containsKey(titleKey)) {
                dedupedByTitle.put(titleKey, new SectionMarker(matcher.start(), matcher.end(), title));
            }
        }
        sections.addAll(dedupedByTitle.values());
        return sections;
    }

    /** 优先取 SSR 正文区域，避免 script 中重复的 summary 被二次解析。 */
    private String prepareHtmlForSectionParsing(String html) {
        String postContent = extractPostContent(html);
        if (StringUtils.hasText(postContent)) {
            return postContent;
        }
        String withoutScripts = stripScriptAndStyleTags(html);
        if (StringUtils.hasText(withoutScripts)) {
            return withoutScripts;
        }
        return html;
    }

    private String extractPostContent(String html) {
        Matcher matcher = POST_CONTENT_PATTERN.matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String stripScriptAndStyleTags(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        return SCRIPT_OR_STYLE_PATTERN.matcher(html).replaceAll("");
    }

    private String extractSectionSummary(String decodedHtml, int sectionEnd, int nextSectionStart) {
        String chunk = decodedHtml.substring(sectionEnd, nextSectionStart);
        int blockquoteStart = indexOfIgnoreCase(chunk, "<blockquote");
        if (blockquoteStart >= 0) {
            chunk = chunk.substring(0, blockquoteStart);
        }
        Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(chunk);
        while (paragraphMatcher.find()) {
            String rawParagraph = paragraphMatcher.group(1);
            if (containsTag(rawParagraph, "strong") || containsTag(rawParagraph, "img")) {
                continue;
            }
            String text = stripTags(rawParagraph).trim();
            if (text.startsWith("【AiBase提要") || text.startsWith("划重点")) {
                continue;
            }
            if (isUsefulSummary(text)) {
                return text;
            }
        }
        return "";
    }

    private static int indexOfIgnoreCase(String text, String needle) {
        return text.toLowerCase().indexOf(needle.toLowerCase());
    }

    private static boolean containsTag(String html, String tag) {
        return indexOfIgnoreCase(html, "<" + tag) >= 0;
    }

    private String resolveNewsUrl(String title, Map<String, String> titleToNewsUrl, String newsBaseUrl) {
        if (titleToNewsUrl == null || titleToNewsUrl.isEmpty()) {
            return "";
        }
        String key = normalizeTitleKey(title);
        String exact = titleToNewsUrl.get(key);
        if (StringUtils.hasText(exact)) {
            return exact;
        }
        for (Map.Entry<String, String> entry : titleToNewsUrl.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private List<AibaseArticle> parseDailyListFromEmbeddedJson(String html, String baseUrl, LocalDate reportDate) {
        JsonNode dailyList = extractInitialDailyList(html);
        if (dailyList == null || !dailyList.isArray() || dailyList.isEmpty()) {
            return List.of();
        }
        JsonNode targetDaily = selectDailyNode(dailyList, reportDate);
        if (targetDaily == null) {
            return List.of();
        }
        JsonNode articleList = targetDaily.get("ailoglist");
        if (articleList == null || !articleList.isArray()) {
            return List.of();
        }
        List<AibaseArticle> articles = new ArrayList<>();
        for (JsonNode item : articleList) {
            String newsId = textValue(item, "Id");
            String title = cleanNewsTitle(textValue(item, "title"));
            if (!StringUtils.hasText(newsId) || !StringUtils.hasText(title)) {
                continue;
            }
            String url = normalizeUrl(baseUrl, "/news/" + newsId);
            articles.add(new AibaseArticle(title, url, ""));
        }
        return articles;
    }

    private JsonNode selectDailyNode(JsonNode dailyList, LocalDate reportDate) {
        if (reportDate == null) {
            return dailyList.get(0);
        }
        for (JsonNode daily : dailyList) {
            LocalDate dailyDate = parseDailyDate(textValue(daily, "addtime"));
            if (reportDate.equals(dailyDate)) {
                return daily;
            }
        }
        return null;
    }

    private List<AibaseArticle> parseDailyListFromLinks(String html, String baseUrl) {
        Map<String, AibaseArticle> deduped = new LinkedHashMap<>();
        Matcher matcher = NEWS_LINK_PATTERN.matcher(html);
        while (matcher.find()) {
            String path = matcher.group(1);
            String title = cleanNewsTitle(stripTags(matcher.group(2)));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            String url = normalizeUrl(baseUrl, path);
            deduped.putIfAbsent(url, new AibaseArticle(title, url, ""));
        }
        return new ArrayList<>(deduped.values());
    }

    private JsonNode extractInitialDailyList(String html) {
        return extractEmbeddedJsonArray(html, "initialDailyList");
    }

    private JsonNode extractEmbeddedJsonArray(String html, String marker) {
        int markerIndex = html.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        int arrayStart = html.indexOf('[', markerIndex);
        if (arrayStart < 0) {
            return null;
        }
        int arrayEnd = findMatchingBracket(html, arrayStart);
        if (arrayEnd < 0) {
            return null;
        }
        return parseEmbeddedJsonFragment(html.substring(arrayStart, arrayEnd + 1));
    }

    private JsonNode parseEmbeddedJsonFragment(String rawJson) {
        String normalizedJson = rawJson.replace("\\\"", "\"").replace("\\\\", "\\");
        try {
            return objectMapper.readTree(normalizedJson);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int findMatchingBracket(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static LocalDate parseDailyDate(String addtime) {
        if (!StringUtils.hasText(addtime)) {
            return null;
        }
        String normalized = addtime.trim();
        if (normalized.contains("T")) {
            normalized = normalized.substring(0, normalized.indexOf('T')).replace('-', '/');
        }
        try {
            return LocalDate.parse(normalized, DAILY_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText("") : "";
    }

    static String decodeUnicodeEscapes(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        Matcher matcher = UNICODE_ESCAPE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, String.valueOf((char) Integer.parseInt(matcher.group(1), 16)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static boolean isUsefulSummary(String text) {
        if (!StringUtils.hasText(text) || text.length() < 15) {
            return false;
        }
        long chineseCount = text.chars().filter(AibaseDailyHtmlParser::isChineseChar).count();
        return chineseCount >= 5;
    }

    private static boolean isChineseChar(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    static String normalizeTitleKey(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceAll("\\s+", "").trim();
    }

    static String cleanNewsTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceFirst("^\\d+\\s*、\\s*", "").trim();
    }

    private static String normalizeUrl(String baseUrl, String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + path;
    }

    static String stripTags(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record SectionMarker(int start, int end, String title) {}
}
