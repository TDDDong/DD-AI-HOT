package com.aihot.service.english;

import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.dto.english.EnglishWordDetailDto;
import com.aihot.dto.english.EnglishWordDetailDto.PhraseDto;
import com.aihot.dto.english.EnglishWordDetailDto.RelWordDto;
import com.aihot.dto.english.EnglishWordDetailDto.RelatedHwdDto;
import com.aihot.dto.english.EnglishWordDetailDto.SentenceDto;
import com.aihot.dto.english.EnglishWordDetailDto.SynonymDto;
import com.aihot.dto.english.EnglishWordDetailDto.SynonymWordDto;
import com.aihot.dto.english.EnglishWordDetailDto.TranslationDto;
import com.aihot.entity.english.EnglishWord;
import com.aihot.entity.english.RelatedHwdItem;
import com.aihot.entity.english.SynonymWordItem;
import com.aihot.entity.english.WordPhraseItem;
import com.aihot.entity.english.WordRelWordItem;
import com.aihot.entity.english.WordSentenceItem;
import com.aihot.entity.english.WordSynonymItem;
import com.aihot.entity.english.WordTranslationItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnglishWordEntityMapper {

    public EnglishWord toEntity(EnglishWordRecord record) {
        EnglishWord entity = new EnglishWord();
        entity.setWord(record.word());
        entity.setBookId(record.bookId());
        entity.setUkPhone(record.ukPhone());
        entity.setUsPhone(record.usPhone());
        entity.setUkSpeech(record.ukSpeech());
        entity.setUsSpeech(record.usSpeech());
        entity.setTranslations(record.translations().stream()
                .map(item -> new WordTranslationItem(item.pos(), item.tranCn()))
                .toList());
        entity.setPhrases(record.phrases().stream()
                .map(item -> new WordPhraseItem(item.content(), item.cn()))
                .toList());
        entity.setRelWords(record.relWords().stream()
                .map(item -> new WordRelWordItem(
                        item.pos(),
                        item.hwds().stream()
                                .map(hwd -> new RelatedHwdItem(hwd.hwd(), hwd.tran()))
                                .toList()))
                .toList());
        entity.setSentences(record.sentences().stream()
                .map(item -> new WordSentenceItem(item.content(), item.cn()))
                .toList());
        entity.setSynonyms(record.synonyms().stream()
                .map(item -> new WordSynonymItem(
                        item.pos(),
                        item.tran(),
                        item.words().stream().map(SynonymWordItem::new).toList()))
                .toList());
        return entity;
    }

    public EnglishWordRecord toRecord(EnglishWord entity) {
        return new EnglishWordRecord(
                entity.getWord(),
                entity.getBookId() != null ? entity.getBookId() : "",
                entity.getUkPhone(),
                entity.getUsPhone(),
                entity.getUkSpeech(),
                entity.getUsSpeech(),
                mapTranslations(entity.getTranslations()),
                mapPhrases(entity.getPhrases()),
                mapSentences(entity.getSentences()),
                mapSynonyms(entity.getSynonyms()),
                mapRelWords(entity.getRelWords()));
    }

    public EnglishWordDetailDto toDetailDto(EnglishWord entity) {
        if (entity == null) {
            return null;
        }
        return new EnglishWordDetailDto(
                entity.getId(),
                entity.getWord(),
                entity.getBookId(),
                entity.getUkPhone(),
                entity.getUsPhone(),
                entity.getUkSpeech(),
                entity.getUsSpeech(),
                mapTranslationDtos(entity.getTranslations()),
                mapPhraseDtos(entity.getPhrases()),
                mapRelWordDtos(entity.getRelWords()),
                mapSentenceDtos(entity.getSentences()),
                mapSynonymDtos(entity.getSynonyms()),
                entity.getMarked(),
                entity.getExported(),
                entity.getImportedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private static List<TranslationDto> mapTranslationDtos(List<WordTranslationItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new TranslationDto(item.getPos(), item.getTranCn()))
                .toList();
    }

    private static List<PhraseDto> mapPhraseDtos(List<WordPhraseItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new PhraseDto(item.getContent(), item.getCn()))
                .toList();
    }

    private static List<SentenceDto> mapSentenceDtos(List<WordSentenceItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new SentenceDto(item.getContent(), item.getCn()))
                .toList();
    }

    private static List<SynonymDto> mapSynonymDtos(List<WordSynonymItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new SynonymDto(
                        item.getPos(),
                        item.getTran(),
                        item.getWords() == null
                                ? List.of()
                                : item.getWords().stream()
                                        .map(word -> new SynonymWordDto(word.getWord()))
                                        .toList()))
                .toList();
    }

    private static List<RelWordDto> mapRelWordDtos(List<WordRelWordItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new RelWordDto(
                        item.getPos(),
                        item.getHwds() == null
                                ? List.of()
                                : item.getHwds().stream()
                                        .map(hwd -> new RelatedHwdDto(hwd.getHwd(), hwd.getTran()))
                                        .toList()))
                .toList();
    }

    private static List<EnglishWordRecord.WordTranslation> mapTranslations(List<WordTranslationItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordTranslation(item.getPos(), item.getTranCn()))
                .toList();
    }

    private static List<EnglishWordRecord.WordPhrase> mapPhrases(List<WordPhraseItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordPhrase(item.getContent(), item.getCn()))
                .toList();
    }

    private static List<EnglishWordRecord.WordSentence> mapSentences(List<WordSentenceItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordSentence(item.getContent(), item.getCn()))
                .toList();
    }

    private static List<EnglishWordRecord.WordSynonym> mapSynonyms(List<WordSynonymItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordSynonym(
                        item.getPos(),
                        item.getTran(),
                        item.getWords() == null
                                ? List.of()
                                : item.getWords().stream()
                                        .map(SynonymWordItem::getWord)
                                        .toList()))
                .toList();
    }

    private static List<EnglishWordRecord.WordRelWord> mapRelWords(List<WordRelWordItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new EnglishWordRecord.WordRelWord(
                        item.getPos(),
                        item.getHwds() == null
                                ? List.of()
                                : item.getHwds().stream()
                                        .map(hwd -> new EnglishWordRecord.RelatedHwd(hwd.getHwd(), hwd.getTran()))
                                        .toList()))
                .toList();
    }
}
