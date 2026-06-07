package com.aihot.integration.feishu;

import com.aihot.common.exception.FeishuStorageException;
import com.aihot.config.properties.FeishuBaseFieldMapping;
import com.aihot.config.properties.FeishuBaseProperties;
import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.domain.storage.EnglishWordStorageItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FeishuBaseFieldMapper {

    private final ObjectMapper objectMapper;

    public FeishuBaseFieldMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> toFields(
            FeishuBaseFieldMapping mapping, EnglishWordStorageItem item, Instant importedAt) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(mapping.getWord(), item.word().word());
        fields.put(mapping.getTranslation(), formatTranslations(item));
        if (StringUtils.hasText(item.word().ukPhone())) {
            fields.put(mapping.getUkPhone(), item.word().ukPhone());
        }
        if (StringUtils.hasText(item.word().usPhone())) {
            fields.put(mapping.getUsPhone(), item.word().usPhone());
        }
        if (StringUtils.hasText(item.word().ukSpeech())) {
            fields.put(mapping.getUkSpeech(), linkField(item.word().ukSpeech()));
        }
        if (StringUtils.hasText(item.word().usSpeech())) {
            fields.put(mapping.getUsSpeech(), linkField(item.word().usSpeech()));
        }
        fields.put(mapping.getDetail(), toJson(item.word()));
        fields.put(mapping.getMarked(), false);
        fields.put(mapping.getImportedAt(), importedAt.toEpochMilli());
        return fields;
    }

    private String formatTranslations(EnglishWordStorageItem item) {
        return item.word().translations().stream()
                .map(t -> "%s %s".formatted(t.pos() != null ? t.pos() : "", t.tranCn()))
                .collect(Collectors.joining("\n"));
    }

    private String toJson(EnglishWordRecord word) {
        try {
            return objectMapper.writeValueAsString(word);
        } catch (JsonProcessingException e) {
            throw new FeishuStorageException("序列化单词详情失败", e);
        }
    }

    private static Map<String, String> linkField(String url) {
        Map<String, String> link = new HashMap<>();
        link.put("link", url);
        link.put("text", url);
        return link;
    }
}
