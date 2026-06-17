package com.aihot.dto.content;

import java.time.LocalDate;
import java.util.List;

/** 指定日期的 AI 日报批次及文章列表。 */
public record AiNewsDailyDto(
        LocalDate date,
        Long digestId,
        String title,
        String dailyPageUrl,
        String filePath,
        List<AiNewsArticleDto> articles) {}
