package com.aihot.domain.twitter;

import java.time.LocalDateTime;

/** 某博主的同步状态（从已落库推文聚合）。 */
public record TwitterAuthorSyncState(
        String handle, boolean synced, LocalDateTime lastFetchedAt, long postCount) {}
