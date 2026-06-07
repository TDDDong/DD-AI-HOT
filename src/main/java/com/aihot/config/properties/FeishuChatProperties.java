package com.aihot.config.properties;

/** 飞书群聊推送目标，对应 feishu.yaml 中 feishu.chats 列表项。 */
public class FeishuChatProperties {

    private String chatId = "";
    private String name = "";
    private boolean enabled = true;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
