package com.aihot.integration.twitter;

import com.aihot.common.exception.TwitterFetchException;
import com.aihot.config.properties.TwitterProperties;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultTwitterCliClient implements TwitterCliClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultTwitterCliClient.class);

    private final TwitterProperties properties;
    private final TwitterResponseMapper responseMapper;
    private final TwitterCliCommandResolver commandResolver;

    public DefaultTwitterCliClient(
            TwitterProperties properties,
            TwitterResponseMapper responseMapper,
            TwitterCliCommandResolver commandResolver) {
        this.properties = properties;
        this.responseMapper = responseMapper;
        this.commandResolver = commandResolver;
    }

    @Override
    public TwitterUser fetchCurrentUser() {
        String output = runCommand(List.of("whoami", "--json"));
        return responseMapper.parseCurrentUser(output);
    }

    @Override
    public List<TwitterUser> fetchFollowing(String screenName, int maxCount) {
        String handle = resolveHandle(screenName);
        int limit = clampCount(maxCount, properties.getMaxFollowing());
        String output = runCommand(List.of("following", handle, "--max", String.valueOf(limit), "--json"));
        return responseMapper.parseUsers(output);
    }

    @Override
    public List<TwitterPost> fetchUserPosts(String screenName, int maxCount) {
        String handle = normalizeHandle(screenName);
        int limit = clampCount(maxCount, properties.getMaxPosts());
        String output = runCommand(List.of("user-posts", handle, "--max", String.valueOf(limit), "--json"));
        return responseMapper.parsePosts(output, handle);
    }

    private String resolveHandle(String screenName) {
        if (StringUtils.hasText(screenName)) {
            return normalizeHandle(screenName);
        }
        return fetchCurrentUser().screenName();
    }

    private String runCommand(List<String> args) {
        ensureReady();
        List<String> command = new ArrayList<>(commandResolver.commandPrefix());
        command.addAll(args);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        applyEnvironment(builder);

        log.debug("执行 twitter-cli: {}", String.join(" ", command));
        try {
            Process process = builder.start();
            String output = readProcessOutput(process);
            long timeoutSeconds = Math.max(1, properties.getProcessTimeout().getSeconds());
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TwitterFetchException("twitter-cli 执行超时（>" + timeoutSeconds + "s）: "
                        + String.join(" ", command));
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new TwitterFetchException(
                        "twitter-cli 退出码 " + exitCode + "，命令: " + String.join(" ", command) + "，输出: "
                                + truncate(output));
            }
            return output;
        } catch (TwitterFetchException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TwitterFetchException("twitter-cli 执行被中断", ex);
        } catch (IOException ex) {
            throw new TwitterFetchException(
                    "启动 twitter-cli 失败，请确认已安装且 cli-path 正确（当前尝试: "
                            + String.join(" ", commandResolver.commandPrefix())
                            + "）",
                    ex);
        }
    }

    private void ensureReady() {
        if (!properties.isEnabled()) {
            throw new TwitterFetchException("Twitter 模块未启用，请设置 twitter.enabled=true");
        }
        if (!properties.hasCredentials()) {
            throw new TwitterFetchException(
                    "Twitter Cookie 未配置，请在 application-dev.yml 或环境变量中设置 auth-token 与 ct0");
        }
    }

    private void applyEnvironment(ProcessBuilder builder) {
        builder.environment().put("TWITTER_AUTH_TOKEN", properties.getAuthToken());
        builder.environment().put("TWITTER_CT0", properties.getCt0());
        if (StringUtils.hasText(properties.getProxy())) {
            builder.environment().put("TWITTER_PROXY", properties.getProxy());
        }
    }

    private static String readProcessOutput(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private static int clampCount(int requested, int configuredMax) {
        int value = requested > 0 ? requested : configuredMax;
        return Math.max(1, Math.min(value, configuredMax));
    }

    private static String normalizeHandle(String handle) {
        if (!StringUtils.hasText(handle)) {
            throw new IllegalArgumentException("Twitter handle 不能为空");
        }
        return handle.trim().replace("@", "");
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }
}
