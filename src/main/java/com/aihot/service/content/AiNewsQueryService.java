package com.aihot.service.content;

import com.aihot.common.exception.ContentNotFoundException;
import com.aihot.config.properties.AibaseProperties;
import com.aihot.dto.content.AiNewsArticleDto;
import com.aihot.dto.content.AiNewsDailyDto;
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
public class AiNewsQueryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    private final ContentDigestMapper digestMapper;
    private final ContentArticleMapper articleMapper;
    private final AiNewsEntityMapper entityMapper;

    public AiNewsQueryService(
            ContentDigestMapper digestMapper,
            ContentArticleMapper articleMapper,
            AiNewsEntityMapper entityMapper) {
        this.digestMapper = digestMapper;
        this.articleMapper = articleMapper;
        this.entityMapper = entityMapper;
    }

    /** 按日期范围、热词、关键词查询新闻列表。 */
    public List<AiNewsArticleDto> listArticles(
            LocalDate from, LocalDate to, String tag, String keyword, int limit) {
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(DEFAULT_LOOKBACK_DAYS);
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("from 不能晚于 to");
        }

        int size = clampLimit(limit);
        LambdaQueryWrapper<ContentArticle> wrapper = new LambdaQueryWrapper<ContentArticle>()
                .ge(ContentArticle::getReportDate, startDate)
                .le(ContentArticle::getReportDate, endDate)
                .orderByDesc(ContentArticle::getReportDate)
                .orderByAsc(ContentArticle::getRankNo)
                .last("LIMIT " + size);

        if (StringUtils.hasText(tag)) {
            wrapper.apply("JSON_CONTAINS(tags_json, JSON_QUOTE({0}), '$')", tag.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(ContentArticle::getTitle, pattern).or().like(ContentArticle::getSummary, pattern));
        }

        List<ContentArticle> articles = articleMapper.selectList(wrapper);
        Map<Long, ContentDigest> digestMap = loadDigestMap(articles);
        return articles.stream()
                .map(article -> entityMapper.toArticleDto(article, digestMap.get(article.getDigestId())))
                .toList();
    }

    /** 查询指定日期的整期日报。 */
    public AiNewsDailyDto getDaily(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        ContentDigest digest = findDigestByDate(queryDate);
        if (digest == null) {
            throw new ContentNotFoundException("未找到日期 " + queryDate + " 的 AI 日报");
        }
        List<ContentArticle> articles = articleMapper.selectList(new LambdaQueryWrapper<ContentArticle>()
                .eq(ContentArticle::getDigestId, digest.getId())
                .orderByAsc(ContentArticle::getRankNo));
        List<AiNewsArticleDto> articleDtos = articles.stream()
                .map(article -> entityMapper.toArticleDto(article, digest))
                .toList();
        return new AiNewsDailyDto(
                queryDate,
                digest.getId(),
                digest.getTitle(),
                extractDailyPageUrl(digest),
                digest.getFilePath(),
                articleDtos);
    }

    private ContentDigest findDigestByDate(LocalDate reportDate) {
        return digestMapper.selectOne(new LambdaQueryWrapper<ContentDigest>()
                .eq(ContentDigest::getSourceType, AibaseProperties.SOURCE_TYPE_DAILY)
                .eq(ContentDigest::getReportDate, reportDate));
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

    private static String extractDailyPageUrl(ContentDigest digest) {
        if (digest.getMetadataJson() == null) {
            return null;
        }
        Object value = digest.getMetadataJson().get("dailyPageUrl");
        return value != null ? String.valueOf(value) : null;
    }
}
