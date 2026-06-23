package com.aihot.domain.twitter;

import java.time.Instant;
import java.util.List;

/** twitter-cli 返回的单条推文。 */
public record TwitterPost(
        String tweetId,
        String handle,
        String text,
        Instant createdAt,
        String sourceUrl,
        long likeCount,
        long retweetCount,
        long replyCount,
        long viewCount,
        boolean retweet,
        List<String> mediaUrls,
        List<String> urls) {}
