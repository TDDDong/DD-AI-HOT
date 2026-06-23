package com.aihot.dto.twitter;

import java.time.LocalDate;
import java.util.List;

/** 单条 Twitter 推文。 */
public record TwitterPostDto(
        String articleId,
        Long id,
        String handle,
        String tweetId,
        LocalDate date,
        int rankNo,
        String title,
        String text,
        String url,
        long likeCount,
        long retweetCount,
        long replyCount,
        long viewCount,
        boolean retweet,
        List<String> mediaUrls,
        List<String> urls,
        String createdAt,
        String sourceType) {}
