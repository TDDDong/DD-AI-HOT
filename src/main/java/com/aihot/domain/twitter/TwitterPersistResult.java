package com.aihot.domain.twitter;

import java.time.LocalDate;

/** Twitter 推文落库结果。 */
public record TwitterPersistResult(
        String handle,
        LocalDate fetchDate,
        Long digestId,
        int insertedArticles,
        int updatedArticles,
        int skippedArticles,
        int totalArticles) {}
