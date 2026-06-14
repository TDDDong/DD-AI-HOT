package com.aihot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "obsidian")
public class ObsidianProperties {

    private String vaultPath = "";
    private String vocabSubdir = "英语生词";
    private String sentenceSubdir = "每日例句";
    private String newsSubdir = "ai-daily-hot";

    public String getVaultPath() {
        return vaultPath;
    }

    public void setVaultPath(String vaultPath) {
        this.vaultPath = vaultPath;
    }

    public String getVocabSubdir() {
        return vocabSubdir;
    }

    public void setVocabSubdir(String vocabSubdir) {
        this.vocabSubdir = vocabSubdir;
    }

    public String getSentenceSubdir() {
        return sentenceSubdir;
    }

    public void setSentenceSubdir(String sentenceSubdir) {
        this.sentenceSubdir = sentenceSubdir;
    }

    public String getNewsSubdir() {
        return newsSubdir;
    }

    public void setNewsSubdir(String newsSubdir) {
        this.newsSubdir = newsSubdir;
    }
}
