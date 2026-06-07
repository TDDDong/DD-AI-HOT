package com.aihot.common.exception;

public class ObsidianStorageException extends RuntimeException {

    public ObsidianStorageException(String message) {
        super(message);
    }

    public ObsidianStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
