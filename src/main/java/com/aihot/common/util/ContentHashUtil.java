package com.aihot.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

public final class ContentHashUtil {

    private ContentHashUtil() {}

    public static String sha256(String content) {
        if (content == null) {
            content = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public static String articleKey(String sourceType, LocalDate reportDate, int rankNo) {
        return sha256(sourceType + "|" + reportDate + "|" + rankNo);
    }

    public static String tweetArticleKey(String tweetId) {
        return sha256("twitter|tweet|" + tweetId);
    }
}
