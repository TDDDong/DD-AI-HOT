package com.aihot.domain.content;

/** aibase 拉取到的单条热点新闻。 */
public record AibaseArticle(String title, String sourceUrl, String summary) {}
