package com.aihot.service.twitter;

import com.aihot.common.exception.ContentNotFoundException;
import com.aihot.config.properties.TwitterProperties;
import com.aihot.dto.twitter.TwitterDailyDto;
import com.aihot.dto.twitter.TwitterPostDto;
import com.aihot.dto.twitter.TwitterUserDto;
import com.aihot.entity.content.ContentArticle;
import com.aihot.entity.content.ContentDigest;
import com.aihot.mapper.content.ContentArticleMapper;
import com.aihot.mapper.content.ContentDigestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final TwitterEntityMapper entityMapper;

    public TwitterQueryService(
            ContentDigestMapper digestMapper,
            ContentArticleMapper articleMapper,
            TwitterEntityMapper entityMapper) {
        this.digestMapper = digestMapper;
        this.articleMapper = articleMapper;
        this.entityMapper = entityMapper;
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
                .orderByDesc(ContentArticle::getReportDate)
                .orderByAsc(ContentArticle::getRankNo)
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

    public TwitterUserDto toUserDto(com.aihot.domain.twitter.TwitterUser user) {
        return entityMapper.toUserDto(user);
    }

    public List<TwitterUserDto> toUserDtos(List<com.aihot.domain.twitter.TwitterUser> users) {
        return users.stream().map(entityMapper::toUserDto).toList();
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
        return handle.trim().replace("@", "");
    }
}
