package com.aihot.dto.english;

import com.aihot.domain.storage.SaveResult;

public record PersistResultDto(int saved, int skipped, int failed) {

    public static PersistResultDto from(SaveResult result) {
        return new PersistResultDto(result.saved(), result.skipped(), result.failed());
    }
}
