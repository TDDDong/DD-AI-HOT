package com.aihot.entity.english;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@TableName(value = "english_word", autoResultMap = true)
public class EnglishWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private String bookId;

    private String ukPhone;

    private String usPhone;

    private String ukSpeech;

    private String usSpeech;

    @TableField(value = "translations_json", typeHandler = JacksonTypeHandler.class)
    private List<WordTranslationItem> translations = new ArrayList<>();

    @TableField(value = "phrases_json", typeHandler = JacksonTypeHandler.class)
    private List<WordPhraseItem> phrases = new ArrayList<>();

    @TableField(value = "rel_words_json", typeHandler = JacksonTypeHandler.class)
    private List<WordRelWordItem> relWords = new ArrayList<>();

    @TableField(value = "sentences_json", typeHandler = JacksonTypeHandler.class)
    private List<WordSentenceItem> sentences = new ArrayList<>();

    @TableField(value = "synonyms_json", typeHandler = JacksonTypeHandler.class)
    private List<WordSynonymItem> synonyms = new ArrayList<>();

    private Boolean marked;

    private Boolean exported;

    private LocalDateTime importedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
