package com.aihot.domain.content;

import java.time.LocalDate;
import java.util.List;

/** 某一天的 aibase 热点列表。 */
public record AibaseDailyBatch(LocalDate reportDate, String dailyPageUrl, List<AibaseArticle> articles) {}
