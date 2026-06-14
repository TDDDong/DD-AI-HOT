package com.aihot.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aibase")
public class AibaseProperties {

    public static final String SOURCE_TYPE_DAILY = "aibase_daily";

    private String dailyUrl = "https://www.aibase.com/zh/daily";
    private String baseUrl = "https://www.aibase.com";
    /** 新闻详情页域名，与日报页上的 /news/{id} 链接一致 */
    private String newsBaseUrl = "https://news.aibase.com";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private boolean fetchDetail = true;

    public String getDailyUrl() {
        return dailyUrl;
    }

    public void setDailyUrl(String dailyUrl) {
        this.dailyUrl = dailyUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getNewsBaseUrl() {
        return newsBaseUrl;
    }

    public void setNewsBaseUrl(String newsBaseUrl) {
        this.newsBaseUrl = newsBaseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isFetchDetail() {
        return fetchDetail;
    }

    public void setFetchDetail(boolean fetchDetail) {
        this.fetchDetail = fetchDetail;
    }
}
