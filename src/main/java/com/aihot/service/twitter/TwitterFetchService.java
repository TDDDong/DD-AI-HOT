package com.aihot.service.twitter;

import com.aihot.common.util.ContentHashUtil;
import com.aihot.config.properties.TwitterProperties;
import com.aihot.domain.twitter.TwitterPersistResult;
import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUser;
import com.aihot.domain.twitter.TwitterUserBatch;
import com.aihot.entity.content.ContentArticle;
import com.aihot.entity.content.ContentDigest;
import com.aihot.integration.twitter.TwitterCliClient;
import com.aihot.integration.twitter.TwitterResponseMapper;
import com.aihot.mapper.content.ContentArticleMapper;
import com.aihot.mapper.content.ContentDigestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TwitterFetchService {

    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    private final TwitterCliClient twitterCliClient;
    private final TwitterResponseMapper responseMapper;
    private final TwitterPersistenceService persistenceService;

    public TwitterFetchService(
            TwitterCliClient twitterCliClient,
            TwitterResponseMapper responseMapper,
            TwitterPersistenceService persistenceService) {
        this.twitterCliClient = twitterCliClient;
        this.responseMapper = responseMapper;
        this.persistenceService = persistenceService;
    }

    /** 获取关注列表；screenName 为空时使用当前登录用户。 */
    public List<TwitterUser> listFollowing(String screenName, int maxCount) {
        return twitterCliClient.fetchFollowing(screenName, maxCount);
    }

    /** 获取当前登录用户。 */
    public TwitterUser currentUser() {
        return twitterCliClient.fetchCurrentUser();
    }

    /** 拉取指定博主推文并按日期范围过滤后落库。 */
    public TwitterPersistResult fetchPostsAndPersist(
            String handle, LocalDate from, LocalDate to, int maxPosts) {
        String normalizedHandle = normalizeHandle(handle);
        LocalDate endDate = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = from != null ? from : endDate.minusDays(DEFAULT_LOOKBACK_DAYS);

        List<TwitterPost> rawPosts = twitterCliClient.fetchUserPosts(normalizedHandle, maxPosts);
        List<TwitterPost> filtered = responseMapper.filterByDateRange(rawPosts, startDate, endDate);
        TwitterUserBatch batch = new TwitterUserBatch(normalizedHandle, LocalDate.now(ZoneOffset.UTC), filtered);
        return persistenceService.upsertBatch(batch);
    }

    private static String normalizeHandle(String handle) {
        if (!StringUtils.hasText(handle)) {
            throw new IllegalArgumentException("handle 不能为空");
        }
        return handle.trim().replace("@", "");
    }
}
