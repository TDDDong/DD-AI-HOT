package com.aihot.domain.twitter;

import java.time.LocalDateTime;

/** 关注列表从 X 刷新落库结果。 */
public record TwitterFollowingRefreshResult(
        String ownerScreenName,
        LocalDateTime listFetchedAt,
        int inserted,
        int updated,
        int removed,
        int total) {}
