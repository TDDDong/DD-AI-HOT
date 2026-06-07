package com.aihot.domain.english;

import java.util.List;

/** xxapi 随机英语单词 API 返回的领域模型。 */
public record EnglishWordRecord(
        String word,
        String bookId,
        String ukPhone,
        String usPhone,
        String ukSpeech,
        String usSpeech,
        List<WordTranslation> translations,
        List<WordPhrase> phrases,
        List<WordSentence> sentences,
        List<WordSynonym> synonyms,
        List<WordRelWord> relWords) {

    public record WordTranslation(String pos, String tranCn) {}

    public record WordPhrase(String content, String cn) {}

    public record WordSentence(String content, String cn) {}

    public record WordSynonym(String pos, String tran, List<String> words) {}

    public record WordRelWord(String pos, List<RelatedHwd> hwds) {}

    public record RelatedHwd(String hwd, String tran) {}
}
