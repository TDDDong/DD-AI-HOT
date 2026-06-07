package com.aihot.service.obsidian;

import com.aihot.domain.obsidian.DailySentence;
import com.aihot.domain.obsidian.DailySentenceBatch;
import com.aihot.domain.obsidian.ObsidianSaveResult;
import com.aihot.dto.obsidian.SaveDailySentencesRequest;
import com.aihot.integration.obsidian.ObsidianMarkdownWriter;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ObsidianDailySentenceService {

    private static final Logger log = LoggerFactory.getLogger(ObsidianDailySentenceService.class);

    private final ObsidianMarkdownWriter markdownWriter;

    public ObsidianDailySentenceService(ObsidianMarkdownWriter markdownWriter) {
        this.markdownWriter = markdownWriter;
    }

    public ObsidianSaveResult saveDailySentences(SaveDailySentencesRequest request) {
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        List<DailySentence> sentences = request.sentences().stream()
                .map(item -> new DailySentence(item.content().trim(), item.cn().trim()))
                .toList();
        DailySentenceBatch batch = new DailySentenceBatch(date, request.word(), sentences);
        ObsidianSaveResult result = markdownWriter.writeDailySentences(batch);
        log.info(
                "Obsidian 每日例句已写入: date={}, word={}, count={}, file={}",
                result.date(),
                request.word(),
                result.savedCount(),
                result.filePath());
        return result;
    }
}
