package com.aihot.integration.twitter;

import org.springframework.util.StringUtils;

/** 从 twitter-cli 混合输出中提取可解析的 JSON 正文（兜底）。 */
final class TwitterCliOutputSanitizer {

    private TwitterCliOutputSanitizer() {}

    static String extractJsonPayload(String rawOutput) {
        if (!StringUtils.hasText(rawOutput)) {
            return "";
        }
        String trimmed = rawOutput.trim();

        int envelopeStart = trimmed.indexOf("{\"ok\"");
        if (envelopeStart >= 0) {
            return extractBalancedJson(trimmed, envelopeStart);
        }

        if (trimmed.startsWith("{")) {
            return extractBalancedJson(trimmed, 0);
        }

        int jsonStart = trimmed.indexOf('{');
        if (jsonStart >= 0) {
            return extractBalancedJson(trimmed, jsonStart);
        }
        return trimmed;
    }

    private static String extractBalancedJson(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return text.substring(start);
    }
}
