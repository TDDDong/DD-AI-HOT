package com.aihot.controller;

import com.aihot.domain.storage.SaveResult;
import com.aihot.dto.english.EnglishWordDetailDto;
import com.aihot.dto.english.EnglishWordDto;
import com.aihot.dto.english.PersistResultDto;
import com.aihot.service.english.EnglishWordFetchService;
import com.aihot.service.english.EnglishWordQueryService;
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
    private final EnglishWordQueryService queryService;

    public EnglishWordController(EnglishWordFetchService fetchService, EnglishWordQueryService queryService) {
        this.fetchService = fetchService;
        this.queryService = queryService;
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

    /** 查询最近入库的单词列表（JSON 列解析为结构化字段）。 */
    @GetMapping("/listAll")
    public List<EnglishWordDetailDto> listAll(@RequestParam(defaultValue = "20") int limit) {
        return queryService.listRecent(limit);
    }

    /** 按主键查询单词详情。 */
    @GetMapping("/id/{id}")
    public ResponseEntity<EnglishWordDetailDto> getById(@PathVariable Long id) {
        EnglishWordDetailDto detail = queryService.findById(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /** 按单词查询详情（忽略大小写）。 */
    @GetMapping("/{word}")
    public ResponseEntity<EnglishWordDetailDto> getByWord(@PathVariable String word) {
        EnglishWordDetailDto detail = queryService.findByWord(word);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }
}
