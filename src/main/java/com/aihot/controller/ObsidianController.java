package com.aihot.controller;

import com.aihot.dto.content.FetchAiNewsResponse;
import com.aihot.dto.obsidian.SaveDailySentencesRequest;
import com.aihot.dto.obsidian.SaveDailySentencesResponse;
import com.aihot.service.content.AiNewsFetchService;
import com.aihot.service.obsidian.ObsidianDailySentenceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/obsidian")
public class ObsidianController {

    private final ObsidianDailySentenceService dailySentenceService;
    private final AiNewsFetchService aiNewsFetchService;

    public ObsidianController(
            ObsidianDailySentenceService dailySentenceService, AiNewsFetchService aiNewsFetchService) {
        this.dailySentenceService = dailySentenceService;
        this.aiNewsFetchService = aiNewsFetchService;
    }

    /** 拉取当天 aibase AI 热点新闻并幂等落库 MySQL。 */
    @PostMapping("/ai-news/fetch-and-persist")
    public FetchAiNewsResponse fetchAndPersistAiNews(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            return FetchAiNewsResponse.from(aiNewsFetchService.fetchTodayAndPersist());
        }
        return FetchAiNewsResponse.from(aiNewsFetchService.fetchByDateAndPersist(date));
    }

    /** 接收每日例句并写入 Obsidian Vault（按日期追加到 Markdown 文件）。 */
    @PostMapping("/daily-sentences")
    public SaveDailySentencesResponse saveDailySentences(@Valid @RequestBody SaveDailySentencesRequest request) {
        return SaveDailySentencesResponse.from(dailySentenceService.saveDailySentences(request));
    }
}
