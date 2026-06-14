package com.aihot.integration.aibase;

import com.aihot.common.exception.AibaseFetchException;
import com.aihot.config.properties.AibaseProperties;
import com.aihot.domain.content.AibaseArticle;
import com.aihot.domain.content.AibaseDailyBatch;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultAibaseDailyClient implements AibaseDailyClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultAibaseDailyClient.class);

    private final HttpClient httpClient;
    private final AibaseProperties properties;
    private final AibaseDailyHtmlParser htmlParser;

    public DefaultAibaseDailyClient(
            HttpClient httpClient, AibaseProperties properties, AibaseDailyHtmlParser htmlParser) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.htmlParser = htmlParser;
    }

    @Override
    public AibaseDailyBatch fetchToday() {
        return fetchByDate(LocalDate.now());
    }

    @Override
    public AibaseDailyBatch fetchByDate(LocalDate reportDate) {
        LocalDate targetDate = reportDate != null ? reportDate : LocalDate.now();
        String html = fetchHtml(properties.getDailyUrl());
        List<AibaseArticle> articles =
                htmlParser.parseDailyListForDate(html, properties.getNewsBaseUrl(), targetDate);
        if (articles.isEmpty()) {
            throw new AibaseFetchException(
                    "aibase 日报页未解析到日期 " + targetDate + " 的新闻列表: " + properties.getDailyUrl());
        }
        List<AibaseArticle> enriched = enrichArticles(articles);
        log.info("aibase 热点拉取完成: date={}, count={}", targetDate, enriched.size());
        return new AibaseDailyBatch(targetDate, properties.getDailyUrl(), enriched);
    }

    @Override
    public String fetchArticleDetail(String sourceUrl) {
        return htmlParser.parseArticleSummary(fetchHtml(sourceUrl));
    }

    private List<AibaseArticle> enrichArticles(List<AibaseArticle> articles) {
        if (!properties.isFetchDetail()) {
            return articles;
        }
        List<AibaseArticle> result = new ArrayList<>(articles.size());
        for (AibaseArticle article : articles) {
            String summary = article.summary();
            if (!StringUtils.hasText(summary)) {
                try {
                    summary = fetchArticleDetail(article.sourceUrl());
                } catch (AibaseFetchException ex) {
                    log.warn("抓取 aibase 详情失败，保留列表标题: url={}, reason={}", article.sourceUrl(), ex.getMessage());
                }
            }
            result.add(new AibaseArticle(article.title(), article.sourceUrl(), summary));
        }
        return result;
    }

    private String fetchHtml(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(properties.getReadTimeout())
                .header("User-Agent", "ai-hot/0.1 (+https://github.com/ai-hot)")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AibaseFetchException("aibase HTTP 错误: status=%d url=%s"
                        .formatted(response.statusCode(), url));
            }
            return response.body();
        } catch (AibaseFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new AibaseFetchException("请求 aibase 失败: " + url, e);
        }
    }
}
