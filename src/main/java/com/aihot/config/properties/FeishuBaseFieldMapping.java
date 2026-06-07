package com.aihot.config.properties;

/** 飞书多维表格列名映射，需与已有 Base 表字段名一致。 */
public class FeishuBaseFieldMapping {

    private String word = "单词";
    private String translation = "中文释义";
    private String ukPhone = "英式音标";
    private String usPhone = "美式音标";
    private String ukSpeech = "英式发音";
    private String usSpeech = "美式发音";
    private String detail = "详情 JSON";
    private String marked = "已标记";
    private String importedAt = "入库时间";

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getUkPhone() {
        return ukPhone;
    }

    public void setUkPhone(String ukPhone) {
        this.ukPhone = ukPhone;
    }

    public String getUsPhone() {
        return usPhone;
    }

    public void setUsPhone(String usPhone) {
        this.usPhone = usPhone;
    }

    public String getUkSpeech() {
        return ukSpeech;
    }

    public void setUkSpeech(String ukSpeech) {
        this.ukSpeech = ukSpeech;
    }

    public String getUsSpeech() {
        return usSpeech;
    }

    public void setUsSpeech(String usSpeech) {
        this.usSpeech = usSpeech;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getMarked() {
        return marked;
    }

    public void setMarked(String marked) {
        this.marked = marked;
    }

    public String getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(String importedAt) {
        this.importedAt = importedAt;
    }
}
