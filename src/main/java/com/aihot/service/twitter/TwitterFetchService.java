package com.aihot.service.twitter;

import com.aihot.config.properties.TwitterProperties;
import com.aihot.domain.twitter.TwitterAuthorSyncState;
import com.aihot.domain.twitter.TwitterFollowingRefreshResult;
import com.aihot.domain.twitter.TwitterPersistResult;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterSyncResult;
import com.aihot.domain.twitter.TwitterSyncResult.SyncMode;
import com.aihot.domain.twitter.TwitterUser;
import com.aihot.domain.twitter.TwitterUserBatch;
import com.aihot.integration.twitter.TwitterCliClient;
import com.aihot.integration.twitter.TwitterResponseMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TwitterFetchService {

    private static final Logger log = LoggerFactory.getLogger(TwitterFetchService.class);

    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    private final TwitterCliClient twitterCliClient;
    private final TwitterResponseMapper responseMapper;
    private final TwitterPersistenceService persistenceService;
    private final TwitterFollowingPersistenceService followingPersistenceService;
    private final TwitterQueryService queryService;
    private final TwitterProperties properties;

    public TwitterFetchService(
            TwitterCliClient twitterCliClient,
            TwitterResponseMapper responseMapper,
            TwitterPersistenceService persistenceService,
            TwitterFollowingPersistenceService followingPersistenceService,
            TwitterQueryService queryService,
            TwitterProperties properties) {
        this.twitterCliClient = twitterCliClient;
        this.responseMapper = responseMapper;
        this.persistenceService = persistenceService;
        this.followingPersistenceService = followingPersistenceService;
        this.queryService = queryService;
        this.properties = properties;
    }

    /** 从 X 拉取关注列表并落库（列表页「刷新关注」专用）。 */
    public TwitterFollowingRefreshResult refreshFollowing(String ownerScreenName, int maxCount) {
        String owner = resolveOwnerForRefresh(ownerScreenName);
        log.info("Twitter 刷新关注列表开始: owner={}, max={}", owner, maxCount);
        List<TwitterUser> users = twitterCliClient.fetchFollowing(owner, maxCount);
        TwitterFollowingRefreshResult result = followingPersistenceService.upsertFollowingList(owner, users);
        log.info(
                "Twitter 刷新关注列表完成: owner={}, inserted={}, updated={}, removed={}, total={}",
                result.ownerScreenName(),
                result.inserted(),
                result.updated(),
                result.removed(),
                result.total());
        return result;
    }

    private String resolveOwnerForRefresh(String ownerScreenName) {
        if (StringUtils.hasText(ownerScreenName)) {
            return normalizeHandle(ownerScreenName);
        }
        return normalizeHandle(currentUser().screenName());
    }

    /** 获取关注列表（仅刷新接口内部使用 CLI）。 */
    public List<TwitterUser> fetchFollowingFromCli(String screenName, int maxCount) {
        log.info("Twitter 拉取关注列表: screenName={}, max={}", screenName, maxCount);
        List<TwitterUser> users = twitterCliClient.fetchFollowing(screenName, maxCount);
        log.info("Twitter 关注列表完成: count={}", users.size());
        return users;
    }

    /** 获取当前登录用户。 */
    public TwitterUser currentUser() {
        log.info("Twitter 查询当前用户 whoami");
        TwitterUser user = twitterCliClient.fetchCurrentUser();
        log.info("Twitter whoami 完成: screenName={}", user.screenName());
        return user;
    }

    /**
     * 智能同步：首次拉取最新 {@code initial-fetch-count} 条；之后以最新落库时间为游标增量拉取。
     * 游标 = {@code MAX(content_article.created_at)}（按博主）。
     */
    public TwitterSyncResult syncPosts(String handle) {
        String normalizedHandle = normalizeHandle(handle);
        Optional<LocalDateTime> lastFetchedAt = queryService.findLastFetchedAt(normalizedHandle);

        List<TwitterPost> rawPosts;
        List<TwitterPost> toPersist;
        SyncMode syncMode;
        LocalDateTime cursorUsed = null;

        if (lastFetchedAt.isEmpty()) {
            syncMode = SyncMode.INITIAL;
            int initialCount = Math.max(1, properties.getInitialFetchCount());
            log.info(
                    "Twitter 智能同步开始: handle={}, mode=INITIAL, initialCount={}",
                    normalizedHandle,
                    initialCount);
            rawPosts = twitterCliClient.fetchUserPosts(normalizedHandle, initialCount);
            toPersist = rawPosts.stream().limit(initialCount).toList();
        } else {
            syncMode = SyncMode.INCREMENTAL;
            cursorUsed = lastFetchedAt.get();
            Instant cursorInstant = cursorUsed.atZone(ZoneOffset.UTC).toInstant();
            log.info(
                    "Twitter 智能同步开始: handle={}, mode=INCREMENTAL, cursor={}, maxPosts={}",
                    normalizedHandle,
                    cursorUsed,
                    properties.getMaxPosts());
            rawPosts = twitterCliClient.fetchUserPosts(normalizedHandle, properties.getMaxPosts());
            toPersist = responseMapper.filterPostsAfter(rawPosts, cursorInstant);
        }

        TwitterUserBatch batch =
                new TwitterUserBatch(normalizedHandle, LocalDate.now(ZoneOffset.UTC), toPersist);
        TwitterPersistResult persistResult = persistenceService.upsertBatch(batch);
        TwitterSyncResult result = new TwitterSyncResult(
                normalizedHandle,
                syncMode,
                cursorUsed,
                persistResult.fetchDate(),
                persistResult.digestId(),
                persistResult.insertedArticles(),
                persistResult.updatedArticles(),
                persistResult.skippedArticles(),
                rawPosts.size(),
                toPersist.size());
        log.info(
                "Twitter 智能同步完成: handle={}, mode={}, fetched={}, persisted={}, inserted={}, updated={}, skipped={}",
                result.handle(),
                result.syncMode(),
                result.fetchedCount(),
                result.persistedCount(),
                result.insertedArticles(),
                result.updatedArticles(),
                result.skippedArticles());
        return result;
    }

    /** 拉取指定博主推文并按日期范围过滤后落库（手动模式，保留兼容）。 */
    public TwitterPersistResult fetchPostsAndPersist(
            String handle, LocalDate from, LocalDate to, int maxPosts) {
        String normalizedHandle = normalizeHandle(handle);
        LocalDate endDate = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = from != null ? from : endDate.minusDays(DEFAULT_LOOKBACK_DAYS);
        log.info(
                "Twitter 手动拉取开始: handle={}, from={}, to={}, maxPosts={}",
                normalizedHandle,
                startDate,
                endDate,
                maxPosts);

        List<TwitterPost> rawPosts = twitterCliClient.fetchUserPosts(normalizedHandle, maxPosts);
        List<TwitterPost> filtered = responseMapper.filterByDateRange(rawPosts, startDate, endDate);
        TwitterUserBatch batch = new TwitterUserBatch(normalizedHandle, LocalDate.now(ZoneOffset.UTC), filtered);
        TwitterPersistResult result = persistenceService.upsertBatch(batch);
        log.info(
                "Twitter 手动拉取完成: handle={}, cliCount={}, filtered={}, inserted={}, updated={}, skipped={}",
                normalizedHandle,
                rawPosts.size(),
                filtered.size(),
                result.insertedArticles(),
                result.updatedArticles(),
                result.skippedArticles());
        return result;
    }

    private static String normalizeHandle(String handle) {
        if (!StringUtils.hasText(handle)) {
            throw new IllegalArgumentException("handle 不能为空");
        }
        return handle.trim().replace("@", "").toLowerCase();
    }
}
