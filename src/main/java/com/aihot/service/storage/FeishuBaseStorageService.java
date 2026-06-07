package com.aihot.service.storage;

import com.aihot.common.exception.FeishuStorageException;
import com.aihot.config.properties.FeishuBaseFieldMapping;
import com.aihot.config.properties.FeishuProperties;
import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.domain.storage.EnglishWordStorageItem;
import com.aihot.domain.storage.SaveResult;
import com.aihot.integration.feishu.FeishuBaseFieldMapper;
import com.aihot.integration.feishu.FeishuBitableClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FeishuBaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(FeishuBaseStorageService.class);

    private final FeishuProperties feishuProperties;
    private final EnglishWordDedupService dedupService;
    private final FeishuBitableClient bitableClient;
    private final FeishuBaseFieldMapper fieldMapper;

    public FeishuBaseStorageService(
            FeishuProperties feishuProperties,
            EnglishWordDedupService dedupService,
            FeishuBitableClient bitableClient,
            FeishuBaseFieldMapper fieldMapper) {
        this.feishuProperties = feishuProperties;
        this.dedupService = dedupService;
        this.bitableClient = bitableClient;
        this.fieldMapper = fieldMapper;
    }

    public SaveResult saveNewWords(List<EnglishWordStorageItem> items) {
        if (items == null || items.isEmpty()) {
            return new SaveResult(0, 0, 0, List.of());
        }
        if (!feishuProperties.getBase().isConfigured()) {
            throw new FeishuStorageException(
                    "飞书 Base 未配置，请在 feishu.yaml 中设置 feishu.base.app-token 与 table-id");
        }

        Set<String> existing = dedupService.findExistingWords(items);
        List<EnglishWordStorageItem> toSave = items.stream()
                .filter(item -> !existing.contains(item.word().word()))
                .toList();
        int skipped = items.size() - toSave.size();

        if (toSave.isEmpty()) {
            log.info("Base 写入：{} 条均已存在，跳过", items.size());
            return new SaveResult(0, skipped, 0, List.of());
        }

        FeishuBaseFieldMapping mapping = feishuProperties.getBase().getFields();
        Instant importedAt = Instant.now();
        List<Map<String, Object>> fieldRows = toSave.stream()
                .map(item -> fieldMapper.toFields(mapping, item, importedAt))
                .toList();

        int batchSize = Math.min(Math.max(feishuProperties.getBase().getBatchSize(), 1), 500);
        int saved = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < fieldRows.size(); i += batchSize) {
            List<Map<String, Object>> batch = fieldRows.subList(i, Math.min(i + batchSize, fieldRows.size()));
            try {
                bitableClient.batchCreateRecords(batch);
                saved += batch.size();
            } catch (FeishuStorageException e) {
                failed += batch.size();
                errors.add(e.getMessage());
                log.error("Base 批量写入失败 (batch {}-{}): {}", i, i + batch.size(), e.getMessage());
            }
        }

        log.info("Base 写入完成：新增 {}，跳过 {}，失败 {}", saved, skipped, failed);
        return new SaveResult(saved, skipped, failed, errors);
    }

    public SaveResult saveNewWordRecords(List<EnglishWordRecord> words) {
        return saveNewWords(words.stream().map(EnglishWordStorageItem::of).toList());
    }
}
