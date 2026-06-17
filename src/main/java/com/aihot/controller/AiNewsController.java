package com.aihot.controller;

import com.aihot.dto.content.AiNewsArticleDto;
import com.aihot.dto.content.AiNewsDailyDto;
import com.aihot.service.content.AiNewsQueryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI 热点新闻读取接口，供外部前端消费。 */
@RestController
@RequestMapping("/api/v1/ai-news")
public class AiNewsController {

    private final AiNewsQueryService queryService;

    public AiNewsController(AiNewsQueryService queryService) {
        this.queryService = queryService;
    }

    /** 新闻列表：支持日期范围、热词、关键词过滤。 */
    @GetMapping("/listAll")
    public List<AiNewsArticleDto> listArticles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return queryService.listArticles(from, to, tag, keyword, limit);
    }

    /** 指定日期的整期日报（按 rankNo 排序）。 */
    @GetMapping("/daily")
    public AiNewsDailyDto getDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queryService.getDaily(date);
    }
}
