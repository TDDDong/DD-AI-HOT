package com.aihot.integration.english;

import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.integration.english.response.RandomEnglishWordResponse;
import com.aihot.integration.english.response.RandomEnglishWordResponse.HwdItem;
import com.aihot.integration.english.response.RandomEnglishWordResponse.PhraseItem;
import com.aihot.integration.english.response.RandomEnglishWordResponse.RelWordItem;
import com.aihot.integration.english.response.RandomEnglishWordResponse.SentenceItem;
import com.aihot.integration.english.response.RandomEnglishWordResponse.SynonymItem;
import com.aihot.integration.english.response.RandomEnglishWordResponse.TranslationItem;
import com.aihot.integration.english.response.RandomEnglishWordResponse.WordData;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnglishWordResponseMapper {

    public EnglishWordRecord toRecord(WordData data) {
        if (data == null || data.word() == null || data.word().isBlank()) {
            throw new IllegalArgumentException("API 返回缺少 word 字段");
        }
        return new EnglishWordRecord(
                data.word(),
                data.bookId() != null ? data.bookId() : "",
                data.ukPhone(),
                data.usPhone(),
                data.ukSpeech(),
                data.usSpeech(),
                mapTranslations(data.translations()),
                mapPhrases(data.phrases()),
                mapSentences(data.sentences()),
                mapSynonyms(data.synonyms()),
                mapRelWords(data.relWords()));
    }

    private static List<EnglishWordRecord.WordTranslation> mapTranslations(List<TranslationItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordTranslation(item.pos(), item.tranCn()))
                .toList();
    }

    private static List<EnglishWordRecord.WordPhrase> mapPhrases(List<PhraseItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordPhrase(item.content(), item.cn()))
                .toList();
    }

    private static List<EnglishWordRecord.WordSentence> mapSentences(List<SentenceItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordSentence(item.content(), item.cn()))
                .toList();
    }

    private static List<EnglishWordRecord.WordSynonym> mapSynonyms(List<SynonymItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordSynonym(
                        item.pos(),
                        item.tran(),
                        item.Hwds() == null
                                ? List.of()
                                : item.Hwds().stream()
                                        .map(HwdItem::word)
                                        .filter(word -> word != null && !word.isBlank())
                                        .toList()))
                .toList();
    }

    private static List<EnglishWordRecord.WordRelWord> mapRelWords(List<RelWordItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordRelWord(
                        item.Pos(),
                        item.Hwds() == null
                                ? List.of()
                                : item.Hwds().stream()
                                        .map(hwd -> new EnglishWordRecord.RelatedHwd(hwd.hwd(), hwd.tran()))
                                        .toList()))
                .toList();
    }
}
