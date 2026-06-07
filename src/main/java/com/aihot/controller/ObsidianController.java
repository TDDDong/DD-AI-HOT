package com.aihot.controller;

import com.aihot.dto.obsidian.SaveDailySentencesRequest;
import com.aihot.dto.obsidian.SaveDailySentencesResponse;
import com.aihot.service.obsidian.ObsidianDailySentenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/obsidian")
public class ObsidianController {

    private final ObsidianDailySentenceService dailySentenceService;

    public ObsidianController(ObsidianDailySentenceService dailySentenceService) {
        this.dailySentenceService = dailySentenceService;
    }

    /** 接收每日例句并写入 Obsidian Vault（按日期追加到 Markdown 文件）。 */
    @PostMapping("/daily-sentences")
    public SaveDailySentencesResponse saveDailySentences(@Valid @RequestBody SaveDailySentencesRequest request) {
        return SaveDailySentencesResponse.from(dailySentenceService.saveDailySentences(request));
    }
}
