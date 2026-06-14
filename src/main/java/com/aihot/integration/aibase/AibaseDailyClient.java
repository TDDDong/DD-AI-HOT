package com.aihot.integration.aibase;

import com.aihot.domain.content.AibaseArticle;
import com.aihot.domain.content.AibaseDailyBatch;
import java.time.LocalDate;
import java.util.List;

public interface AibaseDailyClient {

    /** 拉取当天 aibase AI 热点列表。 */
    AibaseDailyBatch fetchToday();

    /** 拉取指定日期热点；MVP 阶段与当天相同入口，历史日期后续扩展。 */
    AibaseDailyBatch fetchByDate(LocalDate reportDate);

    /** 拉取单篇新闻详情页正文摘要。 */
    String fetchArticleDetail(String sourceUrl);
}
