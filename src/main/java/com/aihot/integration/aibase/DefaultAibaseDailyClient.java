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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        String listHtml = fetchHtml(properties.getDailyUrl());
        String detailUrl = htmlParser
                .resolveDailyDetailUrl(listHtml, properties.getBaseUrl(), targetDate)
                .orElseThrow(() -> new AibaseFetchException(
                        "aibase 日报列表页未找到日期 " + targetDate + " 的详情链接: " + properties.getDailyUrl()));

        String detailHtml = fetchHtml(detailUrl);
        Map<String, String> titleToNewsUrl =
                htmlParser.buildTitleToNewsUrlMap(listHtml, properties.getNewsBaseUrl(), targetDate);
        List<AibaseArticle> articles =
                htmlParser.parseDailyDetailArticles(detailHtml, titleToNewsUrl, properties.getNewsBaseUrl());
        if (articles.isEmpty()) {
            throw new AibaseFetchException("aibase 日报详情页未解析到文章: " + detailUrl);
        }

        log.info("aibase 热点拉取完成: date={}, detailUrl={}, count={}", targetDate, detailUrl, articles.size());
        return new AibaseDailyBatch(targetDate, detailUrl, articles);
    }

    @Override
    public String fetchArticleDetail(String sourceUrl) {
        throw new UnsupportedOperationException("已改为从日报详情页解析 summary，不再单独抓取新闻详情页");
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
