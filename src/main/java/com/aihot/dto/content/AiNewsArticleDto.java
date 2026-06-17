package com.aihot.dto.content;

import java.time.LocalDate;
import java.util.List;

/** 单条 AI 热点新闻（列表项）。 */
public record AiNewsArticleDto(
        String articleId,
        Long id,
        LocalDate date,
        int rankNo,
        String title,
        String summary,
        String url,
        List<String> hotwords,
        String anchor,
        String filePath) {}
