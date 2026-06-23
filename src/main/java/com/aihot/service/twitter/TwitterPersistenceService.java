package com.aihot.service.twitter;

import com.aihot.common.util.ContentHashUtil;
import com.aihot.config.properties.TwitterProperties;
import com.aihot.domain.twitter.TwitterPersistResult;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUserBatch;
import com.aihot.entity.content.ContentArticle;
import com.aihot.entity.content.ContentDigest;
import com.aihot.mapper.content.ContentArticleMapper;
import com.aihot.mapper.content.ContentDigestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TwitterPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TwitterPersistenceService.class);

    private final ContentDigestMapper digestMapper;
    private final ContentArticleMapper articleMapper;

    public TwitterPersistenceService(ContentDigestMapper digestMapper, ContentArticleMapper articleMapper) {
        this.digestMapper = digestMapper;
        this.articleMapper = articleMapper;
    }

    @Transactional
    public TwitterPersistResult upsertBatch(TwitterUserBatch batch) {
        String handle = batch.handle();
        String sourceType = TwitterProperties.sourceTypeForHandle(handle);
        LocalDate fetchDate = batch.fetchDate();
        List<TwitterPost> posts = batch.posts();

        if (posts == null || posts.isEmpty()) {
            return new TwitterPersistResult(handle, fetchDate, null, 0, 0, 0, 0);
        }

        Map<LocalDate, List<TwitterPost>> grouped = new LinkedHashMap<>();
        for (TwitterPost post : posts) {
            LocalDate tweetDate = post.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
            grouped.computeIfAbsent(tweetDate, ignored -> new ArrayList<>()).add(post);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        Long lastDigestId = null;

        for (Map.Entry<LocalDate, List<TwitterPost>> entry : grouped.entrySet()) {
            LocalDate tweetDate = entry.getKey();
            List<TwitterPost> dayPosts = entry.getValue();
            ContentDigest digest = upsertDigest(sourceType, handle, tweetDate, dayPosts);
            lastDigestId = digest.getId();
            int rank = 1;
            for (TwitterPost post : dayPosts) {
                UpsertCounter counter = upsertPost(digest, sourceType, handle, tweetDate, rank++, post);
                inserted += counter.inserted();
                updated += counter.updated();
                skipped += counter.skipped();
            }
        }

        log.info(
                "Twitter 推文落库完成: handle={}, fetchDate={}, inserted={}, updated={}, skipped={}",
                handle,
                fetchDate,
                inserted,
                updated,
                skipped);
        return new TwitterPersistResult(handle, fetchDate, lastDigestId, inserted, updated, skipped, posts.size());
    }

    private ContentDigest upsertDigest(
            String sourceType, String handle, LocalDate tweetDate, List<TwitterPost> dayPosts) {
        String digestHash = ContentHashUtil.sha256(buildDigestFingerprint(handle, tweetDate, dayPosts));
        ContentDigest digest = findDigest(sourceType, tweetDate);
        boolean isNew = digest == null;
        if (isNew) {
            digest = new ContentDigest();
            digest.setSourceType(sourceType);
            digest.setReportDate(tweetDate);
        }
        digest.setTitle("@%s 推文 - %s".formatted(handle, tweetDate));
        digest.setStatus("normal");
        digest.setContentHash(digestHash);
        digest.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        digest.setMetadataJson(buildDigestMetadata(handle, tweetDate, dayPosts.size()));

        if (isNew) {
            digestMapper.insert(digest);
        } else {
            digestMapper.updateById(digest);
        }
        return digest;
    }

    private UpsertCounter upsertPost(
            ContentDigest digest,
            String sourceType,
            String handle,
            LocalDate tweetDate,
            int rankNo,
            TwitterPost post) {
        String articleKey = ContentHashUtil.tweetArticleKey(post.tweetId());
        String title = buildTitle(post.text());
        String contentHash = ContentHashUtil.sha256(post.text() + "|" + post.sourceUrl() + "|" + post.tweetId());

        ContentArticle entity = articleMapper.selectOne(new LambdaQueryWrapper<ContentArticle>()
                .eq(ContentArticle::getArticleKey, articleKey));
        boolean isNew = entity == null;
        if (isNew) {
            entity = new ContentArticle();
            entity.setArticleKey(articleKey);
            entity.setDigestId(digest.getId());
            entity.setRankNo(rankNo);
            entity.setReportDate(tweetDate);
        } else if (contentHash.equals(entity.getContentHash())) {
            return new UpsertCounter(0, 0, 1);
        }

        entity.setTitle(title);
        entity.setSummary(post.text());
        entity.setSourceUrl(post.sourceUrl());
        entity.setContentHash(contentHash);
        entity.setAnchor("tweet-" + post.tweetId());
        entity.setExtraJson(buildExtraJson(sourceType, handle, post));
        if (isNew && entity.getTagsJson() == null) {
            entity.setTagsJson(new ArrayList<>());
        }

        if (isNew) {
            articleMapper.insert(entity);
            return new UpsertCounter(1, 0, 0);
        }
        articleMapper.updateById(entity);
        return new UpsertCounter(0, 1, 0);
    }

    private ContentDigest findDigest(String sourceType, LocalDate reportDate) {
        return digestMapper.selectOne(new LambdaQueryWrapper<ContentDigest>()
                .eq(ContentDigest::getSourceType, sourceType)
                .eq(ContentDigest::getReportDate, reportDate));
    }

    private static Map<String, Object> buildDigestMetadata(String handle, LocalDate tweetDate, int count) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("handle", handle);
        metadata.put("tweetDate", tweetDate.toString());
        metadata.put("postCount", count);
        return metadata;
    }

    private static Map<String, Object> buildExtraJson(String sourceType, String handle, TwitterPost post) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("source", sourceType);
        extra.put("handle", handle);
        extra.put("tweetId", post.tweetId());
        extra.put("createdAt", post.createdAt().toString());
        extra.put("likeCount", post.likeCount());
        extra.put("retweetCount", post.retweetCount());
        extra.put("replyCount", post.replyCount());
        extra.put("viewCount", post.viewCount());
        extra.put("isRetweet", post.retweet());
        extra.put("mediaUrls", post.mediaUrls());
        extra.put("urls", post.urls());
        return extra;
    }

    private static String buildDigestFingerprint(String handle, LocalDate tweetDate, List<TwitterPost> posts) {
        StringBuilder builder = new StringBuilder();
        builder.append(handle).append('|').append(tweetDate).append('\n');
        for (TwitterPost post : posts) {
            builder.append(post.tweetId()).append('|').append(post.text()).append('\n');
        }
        return builder.toString();
    }

    private static String buildTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return "Tweet";
        }
        String normalized = text.replace('\n', ' ').trim();
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 509) + "...";
    }

    private record UpsertCounter(int inserted, int updated, int skipped) {}
}
