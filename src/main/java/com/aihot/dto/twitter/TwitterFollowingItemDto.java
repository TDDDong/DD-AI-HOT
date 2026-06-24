package com.aihot.dto.twitter;

import java.time.LocalDateTime;

/** 关注博主 + 本地同步状态（供列表页展示）。 */
public record TwitterFollowingItemDto(
        String id,
        String name,
        String screenName,
        String bio,
        int followers,
        int following,
        int tweets,
        boolean verified,
        boolean synced,
        LocalDateTime lastFetchedAt,
        long postCount) {}
