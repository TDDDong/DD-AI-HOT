package com.aihot.integration.english.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RandomEnglishWordResponse(int code, String msg, WordData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WordData(
            String bookId,
            List<PhraseItem> phrases,
            List<RelWordItem> relWords,
            List<SentenceItem> sentences,
            List<SynonymItem> synonyms,
            List<TranslationItem> translations,
            @JsonProperty("ukphone") String ukPhone,
            @JsonProperty("ukspeech") String ukSpeech,
            @JsonProperty("usphone") String usPhone,
            @JsonProperty("usspeech") String usSpeech,
            String word) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhraseItem(
            @JsonProperty("p_cn") String cn,
            @JsonProperty("p_content") String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SentenceItem(
            @JsonProperty("s_cn") String cn,
            @JsonProperty("s_content") String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TranslationItem(String pos, @JsonProperty("tran_cn") String tranCn) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SynonymItem(String pos, String tran, List<HwdItem> Hwds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelWordItem(String Pos, List<HwdItem> Hwds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HwdItem(String hwd, String tran, String word) {}
}
