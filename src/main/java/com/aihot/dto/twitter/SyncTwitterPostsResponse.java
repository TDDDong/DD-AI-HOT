package com.aihot.dto.twitter;

import com.aihot.domain.twitter.TwitterSyncResult;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SyncTwitterPostsResponse(
        String handle,
        String syncMode,
        LocalDateTime cursorUsed,
        LocalDate fetchDate,
        Long digestId,
        int insertedArticles,
        int updatedArticles,
        int skippedArticles,
        int fetchedCount,
        int persistedCount) {

    public static SyncTwitterPostsResponse from(TwitterSyncResult result) {
        return new SyncTwitterPostsResponse(
                result.handle(),
                result.syncMode().name(),
                result.cursorUsed(),
                result.fetchDate(),
                result.digestId(),
                result.insertedArticles(),
                result.updatedArticles(),
                result.skippedArticles(),
                result.fetchedCount(),
                result.persistedCount());
    }
}
