package com.aihot.service.twitter;

import com.aihot.domain.twitter.TwitterFollowingRefreshResult;
import com.aihot.domain.twitter.TwitterUser;
import com.aihot.entity.twitter.TwitterFollowing;
import com.aihot.mapper.twitter.TwitterFollowingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TwitterFollowingPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TwitterFollowingPersistenceService.class);

    private final TwitterFollowingMapper followingMapper;

    public TwitterFollowingPersistenceService(TwitterFollowingMapper followingMapper) {
        this.followingMapper = followingMapper;
    }

    @Transactional
    public TwitterFollowingRefreshResult upsertFollowingList(String ownerScreenName, List<TwitterUser> users) {
        String owner = normalizeHandle(ownerScreenName);
        LocalDateTime fetchedAt = LocalDateTime.now(ZoneOffset.UTC);
        if (users == null || users.isEmpty()) {
            int removed = markRemovedExcept(owner, Set.of());
            log.info("Twitter 关注列表落库: owner={}, 远程为空, removed={}", owner, removed);
            return new TwitterFollowingRefreshResult(owner, fetchedAt, 0, 0, removed, 0);
        }

        int inserted = 0;
        int updated = 0;
        Set<String> activeHandles = new HashSet<>();

        for (TwitterUser user : users) {
            String screenName = normalizeHandle(user.screenName());
            activeHandles.add(screenName);
            TwitterFollowing entity = followingMapper.selectOne(new LambdaQueryWrapper<TwitterFollowing>()
                    .eq(TwitterFollowing::getOwnerScreenName, owner)
                    .eq(TwitterFollowing::getScreenName, screenName));

            boolean isNew = entity == null;
            if (isNew) {
                entity = new TwitterFollowing();
                entity.setOwnerScreenName(owner);
                entity.setScreenName(screenName);
            }

            entity.setUserId(user.id());
            entity.setName(user.name());
            entity.setBio(user.bio());
            entity.setFollowersCount(user.followers());
            entity.setFollowingCount(user.following());
            entity.setTweetsCount(user.tweets());
            entity.setVerified(user.verified());
            entity.setStatus(TwitterFollowing.STATUS_ACTIVE);
            entity.setFetchedAt(fetchedAt);

            if (isNew) {
                followingMapper.insert(entity);
                inserted++;
            } else {
                followingMapper.updateById(entity);
                updated++;
            }
        }

        int removed = markRemovedExcept(owner, activeHandles);
        int total = activeHandles.size();
        log.info(
                "Twitter 关注列表落库完成: owner={}, inserted={}, updated={}, removed={}, total={}",
                owner,
                inserted,
                updated,
                removed,
                total);
        return new TwitterFollowingRefreshResult(owner, fetchedAt, inserted, updated, removed, total);
    }

    private int markRemovedExcept(String ownerScreenName, Set<String> activeHandles) {
        LambdaQueryWrapper<TwitterFollowing> wrapper = new LambdaQueryWrapper<TwitterFollowing>()
                .eq(TwitterFollowing::getOwnerScreenName, ownerScreenName)
                .eq(TwitterFollowing::getStatus, TwitterFollowing.STATUS_ACTIVE);
        if (!activeHandles.isEmpty()) {
            wrapper.notIn(TwitterFollowing::getScreenName, activeHandles);
        }
        List<TwitterFollowing> toRemove = followingMapper.selectList(wrapper);
        if (toRemove.isEmpty()) {
            return 0;
        }
        followingMapper.update(
                null,
                new LambdaUpdateWrapper<TwitterFollowing>()
                        .eq(TwitterFollowing::getOwnerScreenName, ownerScreenName)
                        .eq(TwitterFollowing::getStatus, TwitterFollowing.STATUS_ACTIVE)
                        .notIn(!activeHandles.isEmpty(), TwitterFollowing::getScreenName, activeHandles)
                        .set(TwitterFollowing::getStatus, TwitterFollowing.STATUS_REMOVED));
        return toRemove.size();
    }

    private static String normalizeHandle(String handle) {
        if (!StringUtils.hasText(handle)) {
            throw new IllegalArgumentException("owner screenName 不能为空");
        }
        return handle.trim().replace("@", "").toLowerCase();
    }
}
