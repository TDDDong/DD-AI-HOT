package com.aihot.common.exception;

/** twitter-cli 调用或解析失败。 */
public class TwitterFetchException extends RuntimeException {

    public TwitterFetchException(String message) {
        super(message);
    }

    public TwitterFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
