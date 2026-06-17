package com.aihot.service.content;

import com.aihot.common.util.ContentHashUtil;
import com.aihot.config.properties.AibaseProperties;
import com.aihot.domain.content.AibaseArticle;
import com.aihot.domain.content.AibaseDailyBatch;
import com.aihot.domain.content.AiNewsPersistResult;
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
public class AiNewsPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AiNewsPersistenceService.class);

    private final ContentDigestMapper digestMapper;
    private final ContentArticleMapper articleMapper;

    public AiNewsPersistenceService(ContentDigestMapper digestMapper, ContentArticleMapper articleMapper) {
        this.digestMapper = digestMapper;
        this.articleMapper = articleMapper;
    }

    @Transactional
    public AiNewsPersistResult upsertDailyBatch(AibaseDailyBatch batch) {
        String sourceType = AibaseProperties.SOURCE_TYPE_DAILY;
        LocalDate reportDate = batch.reportDate();
        List<AibaseArticle> articles = batch.articles();
        if (articles == null || articles.isEmpty()) {
            return new AiNewsPersistResult(reportDate, null, 0, 0, 0, 0);
        }

        String digestHash = ContentHashUtil.sha256(buildDigestFingerprint(batch));
        ContentDigest digest = findDigest(sourceType, reportDate);
        boolean digestNew = digest == null;
        if (digestNew) {
            digest = new ContentDigest();
            digest.setSourceType(sourceType);
            digest.setReportDate(reportDate);
        }
        digest.setTitle("AI 每日热点摘要 - " + reportDate);
        digest.setStatus("normal");
        digest.setContentHash(digestHash);
        digest.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        digest.setMetadataJson(buildMetadata(batch));

        if (digestNew) {
            digestMapper.insert(digest);
        } else {
            digestMapper.updateById(digest);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int rank = 1;
        for (AibaseArticle article : articles) {
            UpsertCounter counter = upsertArticle(digest, reportDate, sourceType, rank++, article);
            inserted += counter.inserted();
            updated += counter.updated();
            skipped += counter.skipped();
        }

        log.info(
                "AI 热点落库完成: date={}, digestId={}, inserted={}, updated={}, skipped={}",
                reportDate,
                digest.getId(),
                inserted,
                updated,
                skipped);
        return new AiNewsPersistResult(
                reportDate, digest.getId(), inserted, updated, skipped, articles.size());
    }

    private UpsertCounter upsertArticle(
            ContentDigest digest, LocalDate reportDate, String sourceType, int rankNo, AibaseArticle article) {
        String articleKey = ContentHashUtil.articleKey(sourceType, reportDate, rankNo);
        String contentHash = ContentHashUtil.sha256(article.title() + "|" + article.sourceUrl() + "|" + article.summary());

        ContentArticle entity = articleMapper.selectOne(new LambdaQueryWrapper<ContentArticle>()
                .eq(ContentArticle::getArticleKey, articleKey));
        boolean isNew = entity == null;
        if (isNew) {
            entity = new ContentArticle();
            entity.setArticleKey(articleKey);
            entity.setDigestId(digest.getId());
            entity.setRankNo(rankNo);
            entity.setReportDate(reportDate);
        } else if (contentHash.equals(entity.getContentHash())) {
            return new UpsertCounter(0, 0, 1);
        }

        entity.setTitle(article.title());
        entity.setSourceUrl(article.sourceUrl());
        entity.setSummary(article.summary());
        entity.setContentHash(contentHash);
        entity.setAnchor("article-" + rankNo);
        if (entity.getExtraJson() == null) {
            entity.setExtraJson(new LinkedHashMap<>());
        }
        entity.getExtraJson().put("source", sourceType);
        // tags_json 预留给后续 AI 热词写回；同步 aibase 时不覆盖已有热词
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

    private static Map<String, Object> buildMetadata(AibaseDailyBatch batch) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dailyPageUrl", batch.dailyPageUrl());
        metadata.put("articleCount", batch.articles().size());
        return metadata;
    }

    private static String buildDigestFingerprint(AibaseDailyBatch batch) {
        StringBuilder builder = new StringBuilder();
        builder.append(batch.reportDate()).append('\n');
        for (AibaseArticle article : batch.articles()) {
            builder.append(article.title())
                    .append('|')
                    .append(article.sourceUrl())
                    .append('|')
                    .append(StringUtils.hasText(article.summary()) ? article.summary() : "")
                    .append('\n');
        }
        return builder.toString();
    }

    private record UpsertCounter(int inserted, int updated, int skipped) {}
}
