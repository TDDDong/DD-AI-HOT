package com.aihot.common.exception;

public class EnglishWordFetchException extends RuntimeException {

    public EnglishWordFetchException(String message) {
        super(message);
    }

    public EnglishWordFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
