package com.aihot.service.english;

import com.aihot.dto.english.EnglishWordDetailDto;
import com.aihot.entity.english.EnglishWord;
import com.aihot.mapper.english.EnglishWordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnglishWordQueryService {

    private final EnglishWordMapper wordMapper;
    private final EnglishWordEntityMapper entityMapper;

    public EnglishWordQueryService(EnglishWordMapper wordMapper, EnglishWordEntityMapper entityMapper) {
        this.wordMapper = wordMapper;
        this.entityMapper = entityMapper;
    }

    /** 按主键查询，JSON 列由 MyBatis 解析为实体内的 List 字段。 */
    public EnglishWordDetailDto findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id 无效");
        }
        EnglishWord entity = wordMapper.selectById(id);
        return entityMapper.toDetailDto(entity);
    }

    /** 按单词查询（忽略大小写）。 */
    public EnglishWordDetailDto findByWord(String word) {
        EnglishWord entity = findEntityByWord(word);
        return entityMapper.toDetailDto(entity);
    }

    /** 列出最近入库的单词。 */
    public List<EnglishWordDetailDto> listRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return wordMapper.selectList(new LambdaQueryWrapper<EnglishWord>()
                        .orderByDesc(EnglishWord::getImportedAt)
                        .last("LIMIT " + size))
                .stream()
                .map(entityMapper::toDetailDto)
                .toList();
    }

    private EnglishWord findEntityByWord(String word) {
        if (!StringUtils.hasText(word)) {
            throw new IllegalArgumentException("word 不能为空");
        }
        return wordMapper.selectOne(new LambdaQueryWrapper<EnglishWord>()
                .apply("LOWER(word) = {0}", normalizeWord(word)));
    }

    private static String normalizeWord(String word) {
        return word.trim().toLowerCase(Locale.ROOT);
    }
}
