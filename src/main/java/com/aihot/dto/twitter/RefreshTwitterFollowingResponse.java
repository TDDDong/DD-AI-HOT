package com.aihot.dto.twitter;

import com.aihot.domain.twitter.TwitterFollowingRefreshResult;
import java.time.LocalDateTime;

public record RefreshTwitterFollowingResponse(
        String ownerScreenName,
        LocalDateTime listFetchedAt,
        int inserted,
        int updated,
        int removed,
        int total) {

    public static RefreshTwitterFollowingResponse from(TwitterFollowingRefreshResult result) {
        return new RefreshTwitterFollowingResponse(
                result.ownerScreenName(),
                result.listFetchedAt(),
                result.inserted(),
                result.updated(),
                result.removed(),
                result.total());
    }
}
