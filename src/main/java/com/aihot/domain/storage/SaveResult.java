package com.aihot.domain.storage;

import java.util.List;

public record SaveResult(int saved, int skipped, int failed, List<String> errors) {

    public SaveResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
