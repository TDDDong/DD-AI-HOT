package com.aihot.common.exception;

public class AibaseFetchException extends RuntimeException {

    public AibaseFetchException(String message) {
        super(message);
    }

    public AibaseFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
