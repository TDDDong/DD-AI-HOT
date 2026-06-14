package com.aihot.domain.content;

import java.time.LocalDate;

/** 热点新闻落库结果。 */
public record AiNewsPersistResult(
        LocalDate reportDate,
        Long digestId,
        int insertedArticles,
        int updatedArticles,
        int skippedArticles,
        int totalArticles) {}
