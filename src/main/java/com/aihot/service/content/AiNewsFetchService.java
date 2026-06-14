package com.aihot.service.content;

import com.aihot.domain.content.AibaseDailyBatch;
import com.aihot.domain.content.AiNewsPersistResult;
import com.aihot.integration.aibase.AibaseDailyClient;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class AiNewsFetchService {

    private final AibaseDailyClient aibaseDailyClient;
    private final AiNewsPersistenceService persistenceService;

    public AiNewsFetchService(AibaseDailyClient aibaseDailyClient, AiNewsPersistenceService persistenceService) {
        this.aibaseDailyClient = aibaseDailyClient;
        this.persistenceService = persistenceService;
    }

    /** 拉取当天 aibase AI 热点并幂等落库。 */
    public AiNewsPersistResult fetchTodayAndPersist() {
        AibaseDailyBatch batch = aibaseDailyClient.fetchToday();
        return persistenceService.upsertDailyBatch(batch);
    }

    /** 拉取指定日期热点并落库；历史日期解析后续扩展，当前与当天抓取共用入口。 */
    public AiNewsPersistResult fetchByDateAndPersist(LocalDate reportDate) {
        LocalDate targetDate = reportDate != null ? reportDate : LocalDate.now();
        AibaseDailyBatch batch = aibaseDailyClient.fetchByDate(targetDate);
        if (!batch.reportDate().equals(targetDate)) {
            batch = new AibaseDailyBatch(targetDate, batch.dailyPageUrl(), batch.articles());
        }
        return persistenceService.upsertDailyBatch(batch);
    }
}
