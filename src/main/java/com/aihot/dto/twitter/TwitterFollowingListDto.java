package com.aihot.dto.twitter;

import com.aihot.domain.twitter.TwitterFollowingRefreshResult;
import java.time.LocalDateTime;
import java.util.List;

/** 已落库关注列表（列表页读库）。 */
public record TwitterFollowingListDto(
        String ownerScreenName,
        LocalDateTime listFetchedAt,
        int total,
        List<TwitterFollowingItemDto> items) {}
