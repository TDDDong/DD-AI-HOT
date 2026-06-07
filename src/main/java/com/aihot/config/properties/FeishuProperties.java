package com.aihot.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {

    private String appId = "";
    private String appSecret = "";
    private FeishuBaseProperties base = new FeishuBaseProperties();
    private List<FeishuChatProperties> chats = new ArrayList<>();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public FeishuBaseProperties getBase() {
        return base;
    }

    public void setBase(FeishuBaseProperties base) {
        this.base = base;
    }

    public List<FeishuChatProperties> getChats() {
        return chats;
    }

    public void setChats(List<FeishuChatProperties> chats) {
        this.chats = chats;
    }

    public List<String> getEnabledChatIds() {
        return chats.stream()
                .filter(FeishuChatProperties::isEnabled)
                .map(FeishuChatProperties::getChatId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }
}
