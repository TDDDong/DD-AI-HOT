package com.aihot.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twitter")
public class TwitterProperties {

    public static final String SOURCE_TYPE_PREFIX = "twitter_";

    private boolean enabled = false;
    private String cliPath = "twitter";
    private int maxPosts = 50;
    private int initialFetchCount = 5;
    private int maxFollowing = 200;
    private Duration processTimeout = Duration.ofSeconds(120);
    private String authToken = "";
    private String ct0 = "";
    private String proxy = "";

    public static String sourceTypeForHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new IllegalArgumentException("Twitter handle 不能为空");
        }
        return SOURCE_TYPE_PREFIX + handle.trim().toLowerCase().replace("@", "");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCliPath() {
        return cliPath;
    }

    public void setCliPath(String cliPath) {
        this.cliPath = cliPath;
    }

    public int getMaxPosts() {
        return maxPosts;
    }

    public void setMaxPosts(int maxPosts) {
        this.maxPosts = maxPosts;
    }

    public int getInitialFetchCount() {
        return initialFetchCount;
    }

    public void setInitialFetchCount(int initialFetchCount) {
        this.initialFetchCount = initialFetchCount;
    }

    public int getMaxFollowing() {
        return maxFollowing;
    }

    public void setMaxFollowing(int maxFollowing) {
        this.maxFollowing = maxFollowing;
    }

    public Duration getProcessTimeout() {
        return processTimeout;
    }

    public void setProcessTimeout(Duration processTimeout) {
        this.processTimeout = processTimeout;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getCt0() {
        return ct0;
    }

    public void setCt0(String ct0) {
        this.ct0 = ct0;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public boolean hasCredentials() {
        return authToken != null && !authToken.isBlank() && ct0 != null && !ct0.isBlank();
    }
}
