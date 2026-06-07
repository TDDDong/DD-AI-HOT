package com.aihot.domain.storage;

import com.aihot.domain.english.EnglishWordRecord;

public record EnglishWordStorageItem(EnglishWordRecord word) {

    public static EnglishWordStorageItem of(EnglishWordRecord word) {
        return new EnglishWordStorageItem(word);
    }
}
