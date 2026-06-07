package com.aihot.dto.obsidian;

import com.aihot.domain.obsidian.ObsidianSaveResult;

public record SaveDailySentencesResponse(String filePath, String date, int savedCount, boolean created) {

    public static SaveDailySentencesResponse from(ObsidianSaveResult result) {
        return new SaveDailySentencesResponse(
                result.filePath(), result.date().toString(), result.savedCount(), result.created());
    }
}
