package com.aihot.dto.english;

import java.time.LocalDate;
import java.util.List;

/** 指定日期入库的热点英语单词及其例句。 */
public record DailyHotSentencesDto(LocalDate date, List<WordSentencesDto> words) {

    public record WordSentencesDto(String word, List<SentenceDto> sentences) {}

    public record SentenceDto(String content, String cn) {}
}
