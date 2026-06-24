package com.aihot.dto.twitter;

import com.aihot.domain.twitter.TwitterAuthorSyncState;
import java.time.LocalDateTime;

/** 某博主本地同步状态。 */
public record TwitterAuthorSyncStateDto(
        String handle, boolean synced, LocalDateTime lastFetchedAt, long postCount) {

    public static TwitterAuthorSyncStateDto from(TwitterAuthorSyncState state) {
        return new TwitterAuthorSyncStateDto(
                state.handle(), state.synced(), state.lastFetchedAt(), state.postCount());
    }
}
