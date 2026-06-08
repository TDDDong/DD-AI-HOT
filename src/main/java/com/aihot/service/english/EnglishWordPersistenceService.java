package com.aihot.service.english;

import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.domain.storage.SaveResult;
import com.aihot.entity.english.EnglishWord;
import com.aihot.mapper.english.EnglishWordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EnglishWordPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(EnglishWordPersistenceService.class);

    private final EnglishWordMapper wordMapper;
    private final EnglishWordEntityMapper entityMapper;

    public EnglishWordPersistenceService(EnglishWordMapper wordMapper, EnglishWordEntityMapper entityMapper) {
        this.wordMapper = wordMapper;
        this.entityMapper = entityMapper;
    }

    @Transactional
    public SaveResult saveNewWords(List<EnglishWordRecord> words) {
        if (words == null || words.isEmpty()) {
            return new SaveResult(0, 0, 0, List.of());
        }

        Set<String> incoming = new HashSet<>();
        for (EnglishWordRecord word : words) {
            incoming.add(normalizeWord(word.word()));
        }

        Set<String> existing = new HashSet<>();
        if (!incoming.isEmpty()) {
            wordMapper.selectList(new LambdaQueryWrapper<EnglishWord>().in(EnglishWord::getWord, incoming))
                    .forEach(entity -> existing.add(entity.getWord()));
        }

        int saved = 0;
        int skipped = 0;
        LocalDateTime importedAt = LocalDateTime.now(ZoneOffset.UTC);

        for (EnglishWordRecord word : words) {
            String normalized = normalizeWord(word.word());
            if (existing.contains(normalized)) {
                skipped++;
                continue;
            }
            wordMapper.insert(toEntity(word, importedAt));
            existing.add(normalized);
            saved++;
        }

        log.info("MySQL 英语单词落库：新增 {}，跳过 {}（已存在）", saved, skipped);
        return new SaveResult(saved, skipped, 0, List.of());
    }

    public List<EnglishWordRecord> listRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return wordMapper.selectList(new LambdaQueryWrapper<EnglishWord>()
                        .orderByDesc(EnglishWord::getImportedAt)
                        .last("LIMIT " + size))
                .stream()
                .map(entityMapper::toRecord)
                .toList();
    }

    public EnglishWordRecord findByWord(String word) {
        EnglishWord entity = wordMapper.selectOne(new LambdaQueryWrapper<EnglishWord>()
                .apply("LOWER(word) = {0}", normalizeWord(word)));
        return entity != null ? entityMapper.toRecord(entity) : null;
    }

    public EnglishWord findEntityByWord(String word) {
        return wordMapper.selectOne(new LambdaQueryWrapper<EnglishWord>()
                .apply("LOWER(word) = {0}", normalizeWord(word)));
    }

    private EnglishWord toEntity(EnglishWordRecord record, LocalDateTime importedAt) {
        EnglishWord entity = entityMapper.toEntity(record);
        entity.setWord(normalizeWord(record.word()));
        entity.setMarked(false);
        entity.setExported(false);
        entity.setImportedAt(importedAt);
        return entity;
    }

    private static String normalizeWord(String word) {
        if (!StringUtils.hasText(word)) {
            throw new IllegalArgumentException("word 不能为空");
        }
        return word.trim().toLowerCase(Locale.ROOT);
    }
}
