package com.aihot.service.twitter;

import com.aihot.config.properties.TwitterProperties;
import com.aihot.dto.twitter.TwitterFollowingItemDto;
import com.aihot.dto.twitter.TwitterPostDto;
import com.aihot.dto.twitter.TwitterUserDto;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUser;
import com.aihot.entity.content.ContentArticle;
import com.aihot.entity.content.ContentDigest;
import com.aihot.entity.twitter.TwitterFollowing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TwitterEntityMapper {

    public TwitterUserDto toUserDto(TwitterUser user) {
        if (user == null) {
            return null;
        }
        return new TwitterUserDto(
                user.id(),
                user.name(),
                user.screenName(),
                user.bio(),
                user.followers(),
                user.following(),
                user.tweets(),
                user.verified());
    }

    public TwitterUserDto toUserDto(TwitterFollowing entity) {
        if (entity == null) {
            return null;
        }
        return new TwitterUserDto(
                entity.getUserId(),
                entity.getName() != null ? entity.getName() : "",
                entity.getScreenName(),
                entity.getBio() != null ? entity.getBio() : "",
                entity.getFollowersCount() != null ? entity.getFollowersCount() : 0,
                entity.getFollowingCount() != null ? entity.getFollowingCount() : 0,
                entity.getTweetsCount() != null ? entity.getTweetsCount() : 0,
                Boolean.TRUE.equals(entity.getVerified()));
    }

    public TwitterFollowingItemDto toFollowingItemDto(
            TwitterFollowing entity, boolean postsSynced, LocalDateTime postsLastFetchedAt, long postCount) {
        return new TwitterFollowingItemDto(
                entity.getUserId(),
                entity.getName() != null ? entity.getName() : "",
                entity.getScreenName(),
                entity.getBio() != null ? entity.getBio() : "",
                entity.getFollowersCount() != null ? entity.getFollowersCount() : 0,
                entity.getFollowingCount() != null ? entity.getFollowingCount() : 0,
                entity.getTweetsCount() != null ? entity.getTweetsCount() : 0,
                Boolean.TRUE.equals(entity.getVerified()),
                postsSynced,
                postsLastFetchedAt,
                postCount);
    }

    public TwitterPostDto toPostDto(ContentArticle article, ContentDigest digest) {
        if (article == null) {
            return null;
        }
        Map<String, Object> extra = article.getExtraJson();
        String handle = extraValue(extra, "handle");
        String tweetId = extraValue(extra, "tweetId");
        String createdAt = extraValue(extra, "createdAt");
        return new TwitterPostDto(
                article.getArticleKey(),
                article.getId(),
                handle,
                tweetId,
                article.getReportDate(),
                article.getRankNo() != null ? article.getRankNo() : 0,
                article.getTitle(),
                article.getSummary(),
                article.getSourceUrl(),
                longValue(extra, "likeCount"),
                longValue(extra, "retweetCount"),
                longValue(extra, "replyCount"),
                longValue(extra, "viewCount"),
                booleanValue(extra, "isRetweet"),
                stringList(extra, "mediaUrls"),
                stringList(extra, "urls"),
                createdAt,
                digest != null ? digest.getSourceType() : null);
    }

    public TwitterPostDto toPostDto(TwitterPost post) {
        return new TwitterPostDto(
                null,
                null,
                post.handle(),
                post.tweetId(),
                post.createdAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                0,
                truncate(post.text()),
                post.text(),
                post.sourceUrl(),
                post.likeCount(),
                post.retweetCount(),
                post.replyCount(),
                post.viewCount(),
                post.retweet(),
                post.mediaUrls(),
                post.urls(),
                post.createdAt().toString(),
                TwitterProperties.sourceTypeForHandle(post.handle()));
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ').trim();
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 509) + "...";
    }

    private static String extraValue(Map<String, Object> extra, String key) {
        if (extra == null || !extra.containsKey(key)) {
            return null;
        }
        Object value = extra.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private static long longValue(Map<String, Object> extra, String key) {
        if (extra == null || !extra.containsKey(key)) {
            return 0L;
        }
        Object value = extra.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static boolean booleanValue(Map<String, Object> extra, String key) {
        if (extra == null || !extra.containsKey(key)) {
            return false;
        }
        Object value = extra.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> extra, String key) {
        if (extra == null || !extra.containsKey(key)) {
            return List.of();
        }
        Object value = extra.get(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return List.of();
    }
}
