package com.aihot.service.content;

import com.aihot.dto.content.AiNewsArticleDto;
import com.aihot.entity.content.ContentArticle;
import com.aihot.entity.content.ContentDigest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiNewsEntityMapper {

    public AiNewsArticleDto toArticleDto(ContentArticle article, ContentDigest digest) {
        if (article == null) {
            return null;
        }
        return new AiNewsArticleDto(
                article.getArticleKey(),
                article.getId(),
                article.getReportDate(),
                article.getRankNo() != null ? article.getRankNo() : 0,
                article.getTitle(),
                article.getSummary(),
                article.getSourceUrl(),
                safeTags(article.getTagsJson()),
                article.getAnchor(),
                digest != null ? digest.getFilePath() : null);
    }

    private static List<String> safeTags(List<String> tags) {
        return tags == null ? List.of() : List.copyOf(tags);
    }
}
