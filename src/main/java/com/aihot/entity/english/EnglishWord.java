package com.aihot.entity.english;

import com.aihot.domain.english.EnglishWordRecord;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;

@TableName(value = "english_word", autoResultMap = true)
public class EnglishWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private String ukPhone;

    private String usPhone;

    private String ukSpeech;

    private String usSpeech;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private EnglishWordRecord detailJson;

    private Boolean marked;

    private Boolean exported;

    private LocalDateTime importedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
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

    public EnglishWordRecord getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(EnglishWordRecord detailJson) {
        this.detailJson = detailJson;
    }

    public Boolean getMarked() {
        return marked;
    }

    public void setMarked(Boolean marked) {
        this.marked = marked;
    }

    public Boolean getExported() {
        return exported;
    }

    public void setExported(Boolean exported) {
        this.exported = exported;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
