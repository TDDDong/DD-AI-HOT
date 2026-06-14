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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AibaseDailyHtmlParser {

    private static final Pattern NEWS_LINK_PATTERN = Pattern.compile(
            "<a[^>]+href=[\"'](?:https?://(?:www\\.)?aibase\\.com)?(/news/\\d+)[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile(
            "\"(?:content|description|summary|desc|detail)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PARAGRAPH_PATTERN =
            Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final DateTimeFormatter DAILY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/M/d[ HH:mm:ss][ HH:mm:ss.SSSSSSS]");

    private final ObjectMapper objectMapper;

    public AibaseDailyHtmlParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 优先解析 Next.js 内嵌的 initialDailyList JSON，失败时回退到 HTML 链接解析。 */
    public List<AibaseArticle> parseDailyList(String html, String baseUrl) {
        if (!StringUtils.hasText(html)) {
            return List.of();
        }
        List<AibaseArticle> fromJson = parseDailyListFromEmbeddedJson(html, baseUrl, null);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        return parseDailyListFromLinks(html, baseUrl);
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

    public String parseArticleSummary(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String fromArticleNode = parseArticleSummaryFromArticleNode(html);
        if (StringUtils.hasText(fromArticleNode)) {
            return fromArticleNode;
        }
        String fromJson = parseArticleSummaryFromEmbeddedJson(html);
        if (StringUtils.hasText(fromJson)) {
            return fromJson;
        }
        return parseArticleSummaryFromParagraphs(html);
    }

    private String parseArticleSummaryFromArticleNode(String html) {
        for (String marker : List.of("initialArticle", "articleDetail", "articleData")) {
            JsonNode node = extractEmbeddedJsonObject(html, marker);
            if (node == null || node.isNull()) {
                continue;
            }
            String summary = extractSummaryTextFromJsonNode(node);
            if (StringUtils.hasText(summary)) {
                return summary;
            }
        }
        return "";
    }

    private String extractSummaryTextFromJsonNode(JsonNode node) {
        for (String field : List.of("content", "description", "summary", "desc", "detail", "subtitle")) {
            String text = normalizeArticleText(textValue(node, field));
            if (isUsefulChineseSummary(text)) {
                return text;
            }
        }
        return "";
    }

    private String normalizeArticleText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return stripTags(decodeJsonString(raw)).trim();
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

    private String parseArticleSummaryFromEmbeddedJson(String html) {
        String best = "";
        long bestScore = 0;
        Matcher matcher = JSON_STRING_FIELD_PATTERN.matcher(html);
        while (matcher.find()) {
            String text = normalizeArticleText(matcher.group(1));
            if (!isUsefulChineseSummary(text)) {
                continue;
            }
            long score = summaryScore(text);
            if (score > bestScore) {
                bestScore = score;
                best = text;
            }
        }
        return best;
    }

    private String parseArticleSummaryFromParagraphs(String html) {
        StringBuilder summary = new StringBuilder();
        Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(html);
        while (paragraphMatcher.find()) {
            String text = stripTags(paragraphMatcher.group(1)).trim();
            if (!isUsefulChineseSummary(text)) {
                continue;
            }
            if (!summary.isEmpty()) {
                summary.append('\n');
            }
            summary.append(text);
            if (summary.length() > 2000) {
                break;
            }
        }
        return summary.toString().trim();
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

    private JsonNode extractEmbeddedJsonObject(String html, String marker) {
        int markerIndex = html.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        int objectStart = html.indexOf('{', markerIndex);
        if (objectStart < 0) {
            return null;
        }
        int objectEnd = findMatchingBrace(html, objectStart);
        if (objectEnd < 0) {
            return null;
        }
        return parseEmbeddedJsonFragment(html.substring(objectStart, objectEnd + 1));
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

    private static int findMatchingBrace(String text, int start) {
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
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static long summaryScore(String text) {
        long chineseCount = text.chars().filter(AibaseDailyHtmlParser::isChineseChar).count();
        return chineseCount * 1000L + text.length();
    }

    private static LocalDate parseDailyDate(String addtime) {
        if (!StringUtils.hasText(addtime)) {
            return null;
        }
        try {
            return LocalDate.parse(addtime.trim(), DAILY_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText("") : "";
    }

    private static String decodeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private static boolean isUsefulChineseSummary(String text) {
        if (!StringUtils.hasText(text) || text.length() < 20) {
            return false;
        }
        long chineseCount = text.chars().filter(AibaseDailyHtmlParser::isChineseChar).count();
        return chineseCount >= 10;
    }

    private static boolean isChineseChar(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN;
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
}
