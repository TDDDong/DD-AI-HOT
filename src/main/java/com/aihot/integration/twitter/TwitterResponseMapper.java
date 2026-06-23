package com.aihot.integration.twitter;

import com.aihot.common.exception.TwitterFetchException;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TwitterResponseMapper {

    private static final DateTimeFormatter TWITTER_CREATED_AT = DateTimeFormatter.ofPattern(
                    "EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    private final ObjectMapper objectMapper;

    public TwitterResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TwitterUser parseCurrentUser(String rawOutput) {
        JsonNode data = unwrapSuccessData(rawOutput);
        JsonNode userNode = data.has("user") ? data.get("user") : data;
        return parseUser(userNode);
    }

    public List<TwitterUser> parseUsers(String rawOutput) {
        JsonNode data = unwrapSuccessData(rawOutput);
        if (!data.isArray()) {
            throw new TwitterFetchException("twitter-cli 关注列表格式异常：data 不是数组");
        }
        List<TwitterUser> users = new ArrayList<>();
        for (JsonNode node : data) {
            users.add(parseUser(node));
        }
        return users;
    }

    public List<TwitterPost> parsePosts(String rawOutput, String expectedHandle) {
        JsonNode data = unwrapSuccessData(rawOutput);
        if (!data.isArray()) {
            throw new TwitterFetchException("twitter-cli 推文列表格式异常：data 不是数组");
        }
        List<TwitterPost> posts = new ArrayList<>();
        for (JsonNode node : data) {
            posts.add(parsePost(node, expectedHandle));
        }
        return posts;
    }

    public List<TwitterPost> filterByDateRange(List<TwitterPost> posts, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return posts;
        }
        LocalDate start = from != null ? from : LocalDate.ofEpochDay(0);
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from 不能晚于 to");
        }
        return posts.stream()
                .filter(post -> {
                    LocalDate tweetDate = post.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
                    return !tweetDate.isBefore(start) && !tweetDate.isAfter(end);
                })
                .toList();
    }

    private JsonNode unwrapSuccessData(String rawOutput) {
        if (!StringUtils.hasText(rawOutput)) {
            throw new TwitterFetchException("twitter-cli 返回空输出");
        }
        try {
            JsonNode root = objectMapper.readTree(rawOutput);
            if (root.has("ok") && !root.get("ok").asBoolean()) {
                String message = root.path("error").path("message").asText("twitter-cli 执行失败");
                throw new TwitterFetchException(message);
            }
            if (root.has("data")) {
                return root.get("data");
            }
            return root;
        } catch (TwitterFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TwitterFetchException("解析 twitter-cli JSON 失败", ex);
        }
    }

    private TwitterUser parseUser(JsonNode node) {
        String screenName = firstText(node, "screenName", "username", "screen_name");
        if (!StringUtils.hasText(screenName)) {
            throw new TwitterFetchException("twitter-cli 用户缺少 screenName");
        }
        return new TwitterUser(
                node.path("id").asText(""),
                node.path("name").asText(""),
                screenName,
                node.path("bio").asText(""),
                node.path("followers").asInt(0),
                node.path("following").asInt(0),
                node.path("tweets").asInt(0),
                node.path("verified").asBoolean(false));
    }

    private TwitterPost parsePost(JsonNode node, String expectedHandle) {
        String tweetId = node.path("id").asText("");
        if (!StringUtils.hasText(tweetId)) {
            throw new TwitterFetchException("twitter-cli 推文缺少 id");
        }
        JsonNode author = node.path("author");
        String handle = firstText(author, "screenName", "screen_name");
        if (!StringUtils.hasText(handle)) {
            handle = expectedHandle;
        }
        handle = normalizeHandle(handle);
        JsonNode metrics = node.path("metrics");
        Instant createdAt = parseCreatedAt(node);
        return new TwitterPost(
                tweetId,
                handle,
                node.path("text").asText(""),
                createdAt,
                "https://x.com/" + handle + "/status/" + tweetId,
                metrics.path("likes").asLong(0),
                metrics.path("retweets").asLong(0),
                metrics.path("replies").asLong(0),
                metrics.path("views").asLong(0),
                node.path("isRetweet").asBoolean(false),
                parseMediaUrls(node.path("media")),
                parseStringList(node.path("urls")));
    }

    private Instant parseCreatedAt(JsonNode node) {
        String iso = node.path("createdAtISO").asText("");
        if (StringUtils.hasText(iso)) {
            try {
                return Instant.parse(iso);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }
        String createdAt = node.path("createdAt").asText("");
        if (!StringUtils.hasText(createdAt)) {
            return Instant.now();
        }
        try {
            return TWITTER_CREATED_AT.parse(createdAt, Instant::from);
        } catch (DateTimeParseException ex) {
            throw new TwitterFetchException("无法解析推文时间: " + createdAt, ex);
        }
    }

    private static List<String> parseMediaUrls(JsonNode mediaNode) {
        if (!mediaNode.isArray()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (JsonNode item : mediaNode) {
            String url = item.path("url").asText("");
            if (StringUtils.hasText(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    private static List<String> parseStringList(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        Iterator<JsonNode> iterator = arrayNode.elements();
        while (iterator.hasNext()) {
            String value = iterator.next().asText("");
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private static String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private static String normalizeHandle(String handle) {
        return handle == null ? "" : handle.trim().replace("@", "");
    }
}
