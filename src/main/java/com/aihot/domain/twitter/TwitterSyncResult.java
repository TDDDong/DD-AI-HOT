package com.aihot.domain.twitter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 智能同步结果。 */
public record TwitterSyncResult(
        String handle,
        SyncMode syncMode,
        LocalDateTime cursorUsed,
        LocalDate fetchDate,
        Long digestId,
        int insertedArticles,
        int updatedArticles,
        int skippedArticles,
        int fetchedCount,
        int persistedCount) {

    public enum SyncMode {
        INITIAL,
        INCREMENTAL
    }
}
