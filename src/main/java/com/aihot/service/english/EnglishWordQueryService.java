package com.aihot.service.english;

import com.aihot.dto.english.DailyHotSentencesDto;
import com.aihot.dto.english.DailyHotSentencesDto.SentenceDto;
import com.aihot.dto.english.DailyHotSentencesDto.WordSentencesDto;
import com.aihot.dto.english.EnglishWordDetailDto;
import com.aihot.entity.english.EnglishWord;
import com.aihot.entity.english.WordSentenceItem;
import com.aihot.mapper.english.EnglishWordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnglishWordQueryService {

    private final EnglishWordMapper wordMapper;
    private final EnglishWordEntityMapper entityMapper;

    public EnglishWordQueryService(EnglishWordMapper wordMapper, EnglishWordEntityMapper entityMapper) {
        this.wordMapper = wordMapper;
        this.entityMapper = entityMapper;
    }

    /** 查询指定日期（UTC）入库的热点英语例句，date 为空时默认当天。 */
    public DailyHotSentencesDto findDailySentencesByDate(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now(ZoneOffset.UTC);
        LocalDateTime start = queryDate.atStartOfDay();
        LocalDateTime end = queryDate.plusDays(1).atStartOfDay();

        List<WordSentencesDto> words = wordMapper.selectList(new LambdaQueryWrapper<EnglishWord>()
                        .ge(EnglishWord::getImportedAt, start)
                        .lt(EnglishWord::getImportedAt, end)
                        .orderByAsc(EnglishWord::getImportedAt))
                .stream()
                .map(this::toWordSentencesDto)
                .toList();
        return new DailyHotSentencesDto(queryDate, words);
    }

    /** 列出最近入库的单词。 */
    public List<EnglishWordDetailDto> listRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return wordMapper.selectList(new LambdaQueryWrapper<EnglishWord>()
                        .orderByDesc(EnglishWord::getImportedAt)
                        .last("LIMIT " + size))
                .stream()
                .map(entityMapper::toDetailDto)
                .toList();
    }

    private WordSentencesDto toWordSentencesDto(EnglishWord entity) {
        List<SentenceDto> sentences = entity.getSentences() == null
                ? List.of()
                : entity.getSentences().stream().map(this::toSentenceDto).toList();
        return new WordSentencesDto(entity.getWord(), sentences);
    }

    private SentenceDto toSentenceDto(WordSentenceItem item) {
        return new SentenceDto(item.getContent(), item.getCn());
    }
}
