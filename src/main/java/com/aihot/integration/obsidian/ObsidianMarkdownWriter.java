package com.aihot.integration.obsidian;

import com.aihot.common.exception.ObsidianStorageException;
import com.aihot.config.properties.ObsidianProperties;
import com.aihot.domain.obsidian.DailySentence;
import com.aihot.domain.obsidian.DailySentenceBatch;
import com.aihot.domain.obsidian.ObsidianSaveResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ObsidianMarkdownWriter {

    private final ObsidianProperties properties;

    public ObsidianMarkdownWriter(ObsidianProperties properties) {
        this.properties = properties;
    }

    public ObsidianSaveResult writeDailySentences(DailySentenceBatch batch) {
        Path vaultPath = resolveVaultPath();
        Path targetDir = vaultPath.resolve(properties.getSentenceSubdir());
        Path targetFile = targetDir.resolve(batch.date() + ".md");
        String markdown = renderSection(batch);

        try {
            Files.createDirectories(targetDir);
            boolean created = !Files.exists(targetFile);
            if (created) {
                Files.writeString(
                        targetFile,
                        renderFileHeader(batch.date()) + markdown,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
            } else {
                Files.writeString(
                        targetFile,
                        markdown,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND);
            }
            return new ObsidianSaveResult(
                    targetFile.toString(), batch.date(), batch.sentences().size(), created);
        } catch (IOException e) {
            throw new ObsidianStorageException("写入 Obsidian 笔记失败: " + targetFile, e);
        }
    }

    private Path resolveVaultPath() {
        if (!StringUtils.hasText(properties.getVaultPath())) {
            throw new ObsidianStorageException(
                    "Obsidian Vault 未配置，请设置环境变量 OBSIDIAN_VAULT_PATH 或 obsidian.vault-path");
        }
        Path vaultPath = Path.of(properties.getVaultPath().trim());
        if (!Files.isDirectory(vaultPath)) {
            throw new ObsidianStorageException("Obsidian Vault 路径不存在或不是目录: " + vaultPath);
        }
        return vaultPath;
    }

    private static String renderFileHeader(LocalDate date) {
        return "# 每日例句 " + date + "\n\n";
    }

    static String renderSection(DailySentenceBatch batch) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(batch.word())) {
            builder.append("## ").append(batch.word().trim()).append("\n\n");
        }
        appendSentences(builder, batch.sentences());
        builder.append("\n---\n\n");
        return builder.toString();
    }

    private static void appendSentences(StringBuilder builder, List<DailySentence> sentences) {
        for (int i = 0; i < sentences.size(); i++) {
            DailySentence sentence = sentences.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(sentence.content().trim())
                    .append("\n");
            builder.append("   > ")
                    .append(sentence.cn().trim())
                    .append("\n\n");
        }
    }
}
