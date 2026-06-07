package com.aihot.service.storage;

import com.aihot.config.properties.FeishuBaseFieldMapping;
import com.aihot.config.properties.FeishuProperties;
import com.aihot.domain.storage.EnglishWordStorageItem;
import com.aihot.integration.feishu.FeishuBitableClient;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EnglishWordDedupService {

    private final FeishuBitableClient bitableClient;
    private final FeishuProperties feishuProperties;

    public EnglishWordDedupService(FeishuBitableClient bitableClient, FeishuProperties feishuProperties) {
        this.bitableClient = bitableClient;
        this.feishuProperties = feishuProperties;
    }

    public Set<String> findExistingWords(List<EnglishWordStorageItem> items) {
        Set<String> words = new HashSet<>();
        for (EnglishWordStorageItem item : items) {
            words.add(item.word().word());
        }
        FeishuBaseFieldMapping fields = feishuProperties.getBase().getFields();
        return bitableClient.findExistingFieldValues(words, fields.getWord());
    }
}
