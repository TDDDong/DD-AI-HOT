package com.aihot.integration.twitter;

import com.aihot.config.properties.TwitterProperties;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 解析 twitter-cli 可执行路径（Windows 上常见未加入 PATH 的情况）。 */
@Component
public class TwitterCliCommandResolver {

    private static final Logger log = LoggerFactory.getLogger(TwitterCliCommandResolver.class);

    private final TwitterProperties properties;
    private volatile List<String> resolvedPrefix;

    public TwitterCliCommandResolver(TwitterProperties properties) {
        this.properties = properties;
    }

    public List<String> commandPrefix() {
        List<String> cached = resolvedPrefix;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (resolvedPrefix == null) {
                resolvedPrefix = resolve();
            }
            return resolvedPrefix;
        }
    }

    private List<String> resolve() {
        String configured = properties.getCliPath();
        if (StringUtils.hasText(configured) && !"twitter".equals(configured)) {
            if (isRunnable(configured)) {
                log.info("使用配置的 twitter-cli: {}", configured);
                return List.of(configured);
            }
            log.warn("配置的 twitter.cli-path 不可用: {}", configured);
        }

        for (String candidate : buildCandidates()) {
            if (isRunnable(candidate)) {
                log.info("自动检测到 twitter-cli: {}", candidate);
                return List.of(candidate);
            }
        }

        if (isRunnable("py")) {
            log.info("使用 py -3 -m twitter_cli 调用 twitter-cli");
            return List.of("py", "-3", "-m", "twitter_cli");
        }

        log.warn("未找到 twitter-cli，将尝试默认命令: {}", configured);
        return List.of(StringUtils.hasText(configured) ? configured : "twitter");
    }

    private List<String> buildCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add("twitter");
        candidates.add("twitter.exe");

        String pythonHome = System.getenv("PYTHONHOME");
        if (StringUtils.hasText(pythonHome)) {
            candidates.add(join(pythonHome, "Scripts", "twitter.exe"));
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (StringUtils.hasText(localAppData)) {
            candidates.add(join(localAppData, "Programs", "Python", "Python314", "Scripts", "twitter.exe"));
            candidates.add(join(localAppData, "Programs", "Python", "Python313", "Scripts", "twitter.exe"));
        }

        candidates.add("F:\\Python\\Scripts\\twitter.exe");
        candidates.add("C:\\Python\\Scripts\\twitter.exe");

        return candidates;
    }

    private static boolean isRunnable(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        File file = new File(path);
        if (file.isFile()) {
            return file.exists();
        }
        return false;
    }

    private static String join(String... parts) {
        return String.join(File.separator, parts);
    }
}
