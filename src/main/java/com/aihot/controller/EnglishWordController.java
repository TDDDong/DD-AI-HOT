package com.aihot.controller;

import com.aihot.domain.storage.SaveResult;
import com.aihot.dto.english.EnglishWordDto;
import com.aihot.dto.english.PersistResultDto;
import com.aihot.service.english.EnglishWordFetchService;
import com.aihot.service.english.EnglishWordPersistenceService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/english-words")
public class EnglishWordController {

    private final EnglishWordFetchService fetchService;
    private final EnglishWordPersistenceService persistenceService;

    public EnglishWordController(
            EnglishWordFetchService fetchService, EnglishWordPersistenceService persistenceService) {
        this.fetchService = fetchService;
        this.persistenceService = persistenceService;
    }

    /** 从 xxapi 拉取一个随机英语单词（不落库）。 */
    @GetMapping("/random")
    public EnglishWordDto fetchRandom() {
        return EnglishWordDto.from(fetchService.fetchRandom());
    }

    /** 拉取随机单词并写入 MySQL（按 word 去重）。 */
    @PostMapping("/persist")
    public PersistResultDto fetchAndPersist() {
        SaveResult result = fetchService.fetchAndPersist();
        return PersistResultDto.from(result);
    }

    /** 列出最近入库的单词。 */
    @GetMapping
    public List<EnglishWordDto> listRecent(@RequestParam(defaultValue = "20") int limit) {
        return persistenceService.listRecent(limit).stream().map(EnglishWordDto::from).toList();
    }
}
