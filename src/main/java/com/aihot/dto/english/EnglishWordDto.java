package com.aihot.dto.english;

import com.aihot.domain.english.EnglishWordRecord;
import java.util.List;

public record EnglishWordDto(
        String word,
        String bookId,
        String ukPhone,
        String usPhone,
        String ukSpeech,
        String usSpeech,
        List<TranslationDto> translations,
        List<PhraseDto> phrases,
        List<SentenceDto> sentences,
        List<SynonymDto> synonyms,
        List<RelWordDto> relWords) {

    public record TranslationDto(String pos, String tranCn) {}

    public record PhraseDto(String content, String cn) {}

    public record SentenceDto(String content, String cn) {}

    public record SynonymDto(String pos, String tran, List<String> words) {}

    public record RelWordDto(String pos, List<RelatedHwdDto> hwds) {}

    public record RelatedHwdDto(String hwd, String tran) {}

    public static EnglishWordDto from(EnglishWordRecord record) {
        return new EnglishWordDto(
                record.word(),
                record.bookId(),
                record.ukPhone(),
                record.usPhone(),
                record.ukSpeech(),
                record.usSpeech(),
                record.translations().stream()
                        .map(t -> new TranslationDto(t.pos(), t.tranCn()))
                        .toList(),
                record.phrases().stream()
                        .map(p -> new PhraseDto(p.content(), p.cn()))
                        .toList(),
                record.sentences().stream()
                        .map(s -> new SentenceDto(s.content(), s.cn()))
                        .toList(),
                record.synonyms().stream()
                        .map(s -> new SynonymDto(s.pos(), s.tran(), s.words()))
                        .toList(),
                record.relWords().stream()
                        .map(r -> new RelWordDto(
                                r.pos(),
                                r.hwds().stream()
                                        .map(h -> new RelatedHwdDto(h.hwd(), h.tran()))
                                        .toList()))
                        .toList());
    }
}
