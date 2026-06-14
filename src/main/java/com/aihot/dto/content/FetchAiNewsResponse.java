package com.aihot.dto.content;

import com.aihot.domain.content.AiNewsPersistResult;
import java.time.LocalDate;

public record FetchAiNewsResponse(
        LocalDate reportDate,
        Long digestId,
        int insertedArticles,
        int updatedArticles,
        int skippedArticles,
        int totalArticles) {

    public static FetchAiNewsResponse from(AiNewsPersistResult result) {
        return new FetchAiNewsResponse(
                result.reportDate(),
                result.digestId(),
                result.insertedArticles(),
                result.updatedArticles(),
                result.skippedArticles(),
                result.totalArticles());
    }
}
