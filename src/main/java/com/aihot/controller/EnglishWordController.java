package com.aihot.controller;

import com.aihot.domain.storage.SaveResult;
import com.aihot.dto.english.DailyHotSentencesDto;
import com.aihot.dto.english.EnglishWordDetailDto;
import com.aihot.dto.english.PersistResultDto;
import com.aihot.service.english.EnglishWordFetchService;
import com.aihot.service.english.EnglishWordQueryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/english-words")
public class EnglishWordController {

    private final EnglishWordFetchService fetchService;
    private final EnglishWordQueryService queryService;

    public EnglishWordController(EnglishWordFetchService fetchService, EnglishWordQueryService queryService) {
        this.fetchService = fetchService;
        this.queryService = queryService;
    }

    /** 拉取随机单词并写入 MySQL（按 word 去重）。 */
    @PostMapping("/persist")
    public PersistResultDto fetchAndPersist() {
        SaveResult result = fetchService.fetchAndPersist();
        return PersistResultDto.from(result);
    }

    /** 查询最近入库的单词列表（JSON 列解析为结构化字段）。 */
    @GetMapping("/listAll")
    public List<EnglishWordDetailDto> listAll(@RequestParam(defaultValue = "20") int limit) {
        return queryService.listRecent(limit);
    }

    /** 按日期查询当天入库的热点英语例句（UTC，date 省略时默认当天）。 */
    @GetMapping("/daily-sentences")
    public DailyHotSentencesDto getDailySentences(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queryService.findDailySentencesByDate(date);
    }
}
