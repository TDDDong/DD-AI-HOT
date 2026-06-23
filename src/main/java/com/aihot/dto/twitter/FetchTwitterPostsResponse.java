package com.aihot.dto.twitter;

import com.aihot.domain.twitter.TwitterPersistResult;
import java.time.LocalDate;

public record FetchTwitterPostsResponse(
        String handle,
        LocalDate fetchDate,
        Long digestId,
        int insertedArticles,
        int updatedArticles,
        int skippedArticles,
        int totalArticles) {

    public static FetchTwitterPostsResponse from(TwitterPersistResult result) {
        return new FetchTwitterPostsResponse(
                result.handle(),
                result.fetchDate(),
                result.digestId(),
                result.insertedArticles(),
                result.updatedArticles(),
                result.skippedArticles(),
                result.totalArticles());
    }
}
