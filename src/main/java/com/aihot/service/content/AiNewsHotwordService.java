package com.aihot.service.content;

import com.aihot.common.exception.ContentNotFoundException;
import com.aihot.entity.content.ContentArticle;
import com.aihot.mapper.content.ContentArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文章热词写回。抓取流程只预留 {@code tags_json} 字段；热词内容由后续 AI 分析接口写入。
 */
@Service
public class AiNewsHotwordService {

    private final ContentArticleMapper articleMapper;

    public AiNewsHotwordService(ContentArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /** 将 AI 生成的热词写回文章（按 articleKey 或数字 id）。 */
    @Transactional
    public void updateArticleHotwords(String articleId, List<String> hotwords) {
        ContentArticle article = findArticle(articleId);
        if (article == null) {
            throw new ContentNotFoundException("未找到文章: " + articleId);
        }
        article.setTagsJson(normalizeHotwords(hotwords));
        articleMapper.updateById(article);
    }

    /** 批量写回同一 digest 下多篇文章的热词。 */
    @Transactional
    public void updateDailyHotwords(java.util.Map<String, List<String>> hotwordsByArticleId) {
        if (hotwordsByArticleId == null || hotwordsByArticleId.isEmpty()) {
            return;
        }
        for (var entry : hotwordsByArticleId.entrySet()) {
            updateArticleHotwords(entry.getKey(), entry.getValue());
        }
    }

    private ContentArticle findArticle(String articleId) {
        if (!StringUtils.hasText(articleId)) {
            return null;
        }
        String trimmed = articleId.trim();
        ContentArticle byKey = articleMapper.selectOne(new LambdaQueryWrapper<ContentArticle>()
                .eq(ContentArticle::getArticleKey, trimmed));
        if (byKey != null) {
            return byKey;
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return articleMapper.selectById(Long.parseLong(trimmed));
        }
        return null;
    }

    private static List<String> normalizeHotwords(List<String> hotwords) {
        if (hotwords == null || hotwords.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(hotwords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList());
    }
}
