package com.aihot.dto.english;

import java.time.LocalDateTime;
import java.util.List;

/** 从数据库查询返回的单词详情（含结构化 JSON 字段）。 */
public record EnglishWordDetailDto(
        Long id,
        String word,
        String bookId,
        String ukPhone,
        String usPhone,
        String ukSpeech,
        String usSpeech,
        List<TranslationDto> translations,
        List<PhraseDto> phrases,
        List<RelWordDto> relWords,
        List<SentenceDto> sentences,
        List<SynonymDto> synonyms,
        Boolean marked,
        Boolean exported,
        LocalDateTime importedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public record TranslationDto(String pos, String tranCn) {}

    public record PhraseDto(String content, String cn) {}

    public record SentenceDto(String content, String cn) {}

    public record SynonymDto(String pos, String tran, List<SynonymWordDto> words) {}

    public record SynonymWordDto(String word) {}

    public record RelWordDto(String pos, List<RelatedHwdDto> hwds) {}

    public record RelatedHwdDto(String hwd, String tran) {}
}
