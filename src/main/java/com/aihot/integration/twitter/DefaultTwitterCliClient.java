package com.aihot.integration.twitter;

import com.aihot.common.exception.TwitterFetchException;
import com.aihot.config.properties.TwitterProperties;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
        String subcommand = "whoami";
        try {
            String output = runCommand(subcommand, List.of("whoami", "--json"));
            TwitterUser user = responseMapper.parseCurrentUser(output);
            log.info("twitter-cli 解析成功: subcommand={}, screenName={}", subcommand, user.screenName());
            return user;
        } catch (TwitterFetchException ex) {
            log.error("twitter-cli 业务失败: subcommand={}, reason={}", subcommand, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<TwitterUser> fetchFollowing(String screenName, int maxCount) {
        String handle = resolveHandle(screenName);
        int limit = clampCount(maxCount, properties.getMaxFollowing());
        String subcommand = "following @" + handle;
        try {
            String output =
                    runCommand(subcommand, List.of("following", handle, "--max", String.valueOf(limit), "--json"));
            List<TwitterUser> users = responseMapper.parseUsers(output);
            log.info("twitter-cli 解析成功: subcommand={}, count={}", subcommand, users.size());
            return users;
        } catch (TwitterFetchException ex) {
            log.error("twitter-cli 业务失败: subcommand={}, reason={}", subcommand, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<TwitterPost> fetchUserPosts(String screenName, int maxCount) {
        String handle = normalizeHandle(screenName);
        int limit = clampCount(maxCount, properties.getMaxPosts());
        String subcommand = "user-posts @" + handle;
        try {
            String output =
                    runCommand(subcommand, List.of("user-posts", handle, "--max", String.valueOf(limit), "--json"));
            List<TwitterPost> posts = responseMapper.parsePosts(output, handle);
            log.info("twitter-cli 解析成功: subcommand={}, count={}", subcommand, posts.size());
            return posts;
        } catch (TwitterFetchException ex) {
            log.error("twitter-cli 业务失败: subcommand={}, reason={}", subcommand, ex.getMessage());
            throw ex;
        }
    }

    private String resolveHandle(String screenName) {
        if (StringUtils.hasText(screenName)) {
            return normalizeHandle(screenName);
        }
        return fetchCurrentUser().screenName();
    }

    private String runCommand(String subcommand, List<String> args) {
        ensureReady();
        List<String> command = new ArrayList<>(commandResolver.commandPrefix());
        command.addAll(args);
        String commandLine = formatCommandForLog(command);

        log.info("twitter-cli 开始执行: subcommand={}, command={}", subcommand, commandLine);
        long startNanos = System.nanoTime();

        ProcessBuilder builder = new ProcessBuilder(command);
        applyEnvironment(builder);

        try {
            Process process = builder.start();
            ProcessStreams streams = readProcessStreams(process);
            long timeoutSeconds = Math.max(1, properties.getProcessTimeout().getSeconds());
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            logCliStderr(subcommand, streams.stderr());

            if (!finished) {
                process.destroyForcibly();
                log.error(
                        "twitter-cli 执行超时: subcommand={}, elapsedMs={}, timeoutSeconds={}, command={}",
                        subcommand,
                        elapsedMs,
                        timeoutSeconds,
                        commandLine);
                throw new TwitterFetchException("twitter-cli 执行超时（>" + timeoutSeconds + "s）: " + commandLine);
            }

            int exitCode = process.exitValue();
            String stdout = streams.stdout();
            if (exitCode != 0) {
                String combined = combineForError(stdout, streams.stderr());
                log.error(
                        "twitter-cli 执行失败: subcommand={}, exitCode={}, elapsedMs={}, summary={}, output={}",
                        subcommand,
                        exitCode,
                        elapsedMs,
                        responseMapper.summarizeForLog(stdout),
                        truncate(combined));
                throw new TwitterFetchException(
                        "twitter-cli 退出码 " + exitCode + "，命令: " + commandLine + "，输出: " + truncate(combined));
            }

            String summary = responseMapper.summarizeForLog(stdout);
            log.info(
                    "twitter-cli 执行成功: subcommand={}, exitCode={}, elapsedMs={}, summary={}",
                    subcommand,
                    exitCode,
                    elapsedMs,
                    summary);
            if (log.isDebugEnabled()) {
                log.debug("twitter-cli stdout: subcommand={}, output={}", subcommand, truncate(stdout));
            }
            return stdout;
        } catch (TwitterFetchException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("twitter-cli 执行被中断: subcommand={}, command={}", subcommand, commandLine, ex);
            throw new TwitterFetchException("twitter-cli 执行被中断", ex);
        } catch (IOException ex) {
            log.error(
                    "twitter-cli 启动失败: subcommand={}, command={}, reason={}",
                    subcommand,
                    commandLine,
                    ex.getMessage(),
                    ex);
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
            log.debug("twitter-cli 使用代理: {}", properties.getProxy());
        }
    }

    private static String formatCommandForLog(List<String> command) {
        if (command.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(command.get(0));
        for (int i = 1; i < command.size(); i++) {
            builder.append(' ').append(command.get(i));
        }
        return builder.toString();
    }

    private void logCliStderr(String subcommand, String stderr) {
        if (!StringUtils.hasText(stderr)) {
            return;
        }
        for (String line : stderr.split("\n")) {
            if (StringUtils.hasText(line)) {
                log.warn("twitter-cli stderr: subcommand={}, line={}", subcommand, line.trim());
            }
        }
    }

    private static String combineForError(String stdout, String stderr) {
        if (!StringUtils.hasText(stderr)) {
            return stdout;
        }
        if (!StringUtils.hasText(stdout)) {
            return stderr;
        }
        return stderr + "\n" + stdout;
    }

    private static ProcessStreams readProcessStreams(Process process) throws IOException, InterruptedException {
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();
        Thread stdoutThread = new Thread(
                () -> readStream(process.getInputStream(), stdoutBuilder), "twitter-cli-stdout");
        Thread stderrThread = new Thread(
                () -> readStream(process.getErrorStream(), stderrBuilder), "twitter-cli-stderr");
        stdoutThread.start();
        stderrThread.start();
        stdoutThread.join();
        stderrThread.join();
        return new ProcessStreams(stdoutBuilder.toString().trim(), stderrBuilder.toString().trim());
    }

    private static void readStream(InputStream stream, StringBuilder builder) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } catch (IOException ex) {
            builder.append("[read error: ").append(ex.getMessage()).append(']');
        }
    }

    private record ProcessStreams(String stdout, String stderr) {}

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
