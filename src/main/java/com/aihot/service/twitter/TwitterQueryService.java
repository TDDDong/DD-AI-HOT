package com.aihot.service.twitter;

import com.aihot.common.exception.ContentNotFoundException;
import com.aihot.config.properties.TwitterProperties;
import com.aihot.domain.twitter.TwitterAuthorSyncState;
import com.aihot.dto.twitter.TwitterDailyDto;
import com.aihot.dto.twitter.TwitterFollowingItemDto;
import com.aihot.dto.twitter.TwitterFollowingListDto;
import com.aihot.dto.twitter.TwitterPostDto;
import com.aihot.dto.twitter.TwitterUserDto;
import com.aihot.entity.content.ContentArticle;
import com.aihot.entity.content.ContentDigest;
import com.aihot.entity.twitter.TwitterFollowing;
import com.aihot.mapper.content.ContentArticleMapper;
import com.aihot.mapper.content.ContentDigestMapper;
import com.aihot.mapper.twitter.TwitterFollowingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TwitterQueryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    private final ContentDigestMapper digestMapper;
    private final ContentArticleMapper articleMapper;
    private final TwitterFollowingMapper followingMapper;
    private final TwitterEntityMapper entityMapper;
    private final TwitterProperties twitterProperties;

    public TwitterQueryService(
            ContentDigestMapper digestMapper,
            ContentArticleMapper articleMapper,
            TwitterFollowingMapper followingMapper,
            TwitterEntityMapper entityMapper,
            TwitterProperties twitterProperties) {
        this.digestMapper = digestMapper;
        this.articleMapper = articleMapper;
        this.followingMapper = followingMapper;
        this.entityMapper = entityMapper;
        this.twitterProperties = twitterProperties;
    }

    public List<TwitterPostDto> listPosts(
            String handle, LocalDate from, LocalDate to, String keyword, int limit) {
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(DEFAULT_LOOKBACK_DAYS);
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("from 不能晚于 to");
        }

        List<Long> digestIds = findDigestIds(handle, startDate, endDate);
        if (digestIds.isEmpty()) {
            return List.of();
        }

        int size = clampLimit(limit);
        LambdaQueryWrapper<ContentArticle> wrapper = new LambdaQueryWrapper<ContentArticle>()
                .in(ContentArticle::getDigestId, digestIds)
                .orderByDesc(ContentArticle::getCreatedAt)
                .last("LIMIT " + size);

        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(ContentArticle::getTitle, pattern).or().like(ContentArticle::getSummary, pattern));
        }

        List<ContentArticle> articles = articleMapper.selectList(wrapper);
        Map<Long, ContentDigest> digestMap = loadDigestMap(articles);
        return articles.stream()
                .map(article -> entityMapper.toPostDto(article, digestMap.get(article.getDigestId())))
                .toList();
    }

    public TwitterDailyDto getDaily(String handle, LocalDate date) {
        if (!StringUtils.hasText(handle)) {
            throw new IllegalArgumentException("handle 不能为空");
        }
        LocalDate queryDate = date != null ? date : LocalDate.now();
        String sourceType = TwitterProperties.sourceTypeForHandle(handle);
        ContentDigest digest = digestMapper.selectOne(new LambdaQueryWrapper<ContentDigest>()
                .eq(ContentDigest::getSourceType, sourceType)
                .eq(ContentDigest::getReportDate, queryDate));
        if (digest == null) {
            throw new ContentNotFoundException("未找到 @" + normalizeHandle(handle) + " 在 " + queryDate + " 的推文");
        }

        List<ContentArticle> articles = articleMapper.selectList(new LambdaQueryWrapper<ContentArticle>()
                .eq(ContentArticle::getDigestId, digest.getId())
                .orderByAsc(ContentArticle::getRankNo));
        List<TwitterPostDto> postDtos =
                articles.stream().map(article -> entityMapper.toPostDto(article, digest)).toList();
        return new TwitterDailyDto(
                normalizeHandle(handle),
                queryDate,
                digest.getId(),
                digest.getTitle(),
                postDtos);
    }

    public List<TwitterUserDto> listStoredFollowing(String ownerScreenName, int max) {
        String owner = resolveOwnerScreenName(ownerScreenName);
        if (owner == null) {
            return List.of();
        }
        return listActiveFollowingEntities(owner, max).stream().map(entityMapper::toUserDto).toList();
    }

    /** 从库读取关注列表并附带推文同步状态（列表页专用，不调用 X API）。 */
    public TwitterFollowingListDto listStoredFollowingWithStatus(String ownerScreenName, int max) {
        String owner = resolveOwnerScreenName(ownerScreenName);
        if (owner == null) {
            return new TwitterFollowingListDto(null, null, 0, List.of());
        }
        List<TwitterFollowing> rows = listActiveFollowingEntities(owner, max);
        LocalDateTime listFetchedAt = rows.stream()
                .map(TwitterFollowing::getFetchedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        List<TwitterFollowingItemDto> items = rows.stream().map(this::toStoredFollowingItem).toList();
        return new TwitterFollowingListDto(owner, listFetchedAt, items.size(), items);
    }

    public TwitterUserDto toUserDto(com.aihot.domain.twitter.TwitterUser user) {
        return entityMapper.toUserDto(user);
    }

    public List<TwitterUserDto> toUserDtos(List<com.aihot.domain.twitter.TwitterUser> users) {
        return users.stream().map(entityMapper::toUserDto).toList();
    }

    /** 某博主最新一条推文的落库时间（增量同步游标）。 */
    public Optional<LocalDateTime> findLastFetchedAt(String handle) {
        ContentArticle latest = findLatestArticle(normalizeHandle(handle));
        if (latest == null || latest.getCreatedAt() == null) {
            return Optional.empty();
        }
        return Optional.of(latest.getCreatedAt());
    }

    /** 某博主已落库推文数量。 */
    public long countPosts(String handle) {
        return articleMapper.selectCount(buildHandleWrapper(normalizeHandle(handle)));
    }

    public TwitterAuthorSyncState getAuthorSyncState(String handle) {
        String normalized = normalizeHandle(handle);
        Optional<LocalDateTime> lastFetchedAt = findLastFetchedAt(normalized);
        long postCount = countPosts(normalized);
        return new TwitterAuthorSyncState(
                normalized, postCount > 0, lastFetchedAt.orElse(null), postCount);
    }

    private TwitterFollowingItemDto toStoredFollowingItem(TwitterFollowing row) {
        TwitterAuthorSyncState state = getAuthorSyncState(row.getScreenName());
        return entityMapper.toFollowingItemDto(
                row, state.synced(), state.lastFetchedAt(), state.postCount());
    }

    private List<TwitterFollowing> listActiveFollowingEntities(String owner, int max) {
        LambdaQueryWrapper<TwitterFollowing> wrapper = new LambdaQueryWrapper<TwitterFollowing>()
                .eq(TwitterFollowing::getOwnerScreenName, owner)
                .eq(TwitterFollowing::getStatus, TwitterFollowing.STATUS_ACTIVE)
                .orderByAsc(TwitterFollowing::getScreenName);
        int limit = clampFollowingMax(max);
        if (limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return followingMapper.selectList(wrapper);
    }

    private String resolveOwnerScreenName(String ownerScreenName) {
        if (StringUtils.hasText(ownerScreenName)) {
            return normalizeHandle(ownerScreenName);
        }
        TwitterFollowing latest = followingMapper.selectOne(new LambdaQueryWrapper<TwitterFollowing>()
                .orderByDesc(TwitterFollowing::getFetchedAt)
                .last("LIMIT 1"));
        return latest != null ? latest.getOwnerScreenName() : null;
    }

    private int clampFollowingMax(int max) {
        if (max <= 0) {
            return twitterProperties.getMaxFollowing();
        }
        return Math.min(max, twitterProperties.getMaxFollowing());
    }

    private ContentArticle findLatestArticle(String handle) {
        return articleMapper.selectOne(buildHandleWrapper(handle)
                .orderByDesc(ContentArticle::getCreatedAt)
                .last("LIMIT 1"));
    }

    private LambdaQueryWrapper<ContentArticle> buildHandleWrapper(String handle) {
        String sourceType = TwitterProperties.sourceTypeForHandle(handle);
        return new LambdaQueryWrapper<ContentArticle>()
                .apply("JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.source')) = {0}", sourceType);
    }

    private List<Long> findDigestIds(String handle, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ContentDigest> wrapper = new LambdaQueryWrapper<ContentDigest>()
                .ge(ContentDigest::getReportDate, startDate)
                .le(ContentDigest::getReportDate, endDate);

        if (StringUtils.hasText(handle)) {
            wrapper.eq(ContentDigest::getSourceType, TwitterProperties.sourceTypeForHandle(handle));
        } else {
            wrapper.likeRight(ContentDigest::getSourceType, TwitterProperties.SOURCE_TYPE_PREFIX);
        }

        return digestMapper.selectList(wrapper).stream()
                .map(ContentDigest::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, ContentDigest> loadDigestMap(List<ContentArticle> articles) {
        Set<Long> digestIds = articles.stream()
                .map(ContentArticle::getDigestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (digestIds.isEmpty()) {
            return Map.of();
        }
        List<ContentDigest> digests = digestMapper.selectBatchIds(digestIds);
        Map<Long, ContentDigest> digestMap = new LinkedHashMap<>();
        for (ContentDigest digest : digests) {
            digestMap.put(digest.getId(), digest);
        }
        return digestMap;
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String normalizeHandle(String handle) {
        return handle.trim().replace("@", "").toLowerCase();
    }
}
