package com.aihot.controller;

import com.aihot.dto.twitter.FetchTwitterPostsResponse;
import com.aihot.dto.twitter.RefreshTwitterFollowingResponse;
import com.aihot.dto.twitter.SyncTwitterPostsResponse;
import com.aihot.dto.twitter.TwitterAuthorSyncStateDto;
import com.aihot.dto.twitter.TwitterDailyDto;
import com.aihot.dto.twitter.TwitterFollowingListDto;
import com.aihot.dto.twitter.TwitterPostDto;
import com.aihot.dto.twitter.TwitterUserDto;
import com.aihot.service.twitter.TwitterFetchService;
import com.aihot.service.twitter.TwitterQueryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Twitter/X 博主推文：关注列表与按博主拉取推文。 */
@RestController
@RequestMapping("/api/v1/twitter")
public class TwitterController {

    private final TwitterFetchService fetchService;
    private final TwitterQueryService queryService;

    public TwitterController(TwitterFetchService fetchService, TwitterQueryService queryService) {
        this.fetchService = fetchService;
        this.queryService = queryService;
    }

    /** 当前登录用户（whoami）。 */
    @GetMapping("/whoami")
    public TwitterUserDto whoami() {
        return queryService.toUserDto(fetchService.currentUser());
    }

    /** 已落库关注列表（读库，不调用 X API）。 */
    @GetMapping("/following")
    public List<TwitterUserDto> listFollowing(
            @RequestParam(required = false) String ownerScreenName,
            @RequestParam(defaultValue = "0") int max) {
        return queryService.listStoredFollowing(ownerScreenName, max);
    }

    /**
     * 已落库关注列表 + 推文同步状态（列表页读库）。
     * 刷新关注请调用 {@link #refreshFollowing}。
     */
    @GetMapping("/following-with-status")
    public TwitterFollowingListDto listFollowingWithStatus(
            @RequestParam(required = false) String ownerScreenName,
            @RequestParam(defaultValue = "0") int max) {
        return queryService.listStoredFollowingWithStatus(ownerScreenName, max);
    }

    /** 从 X 拉取关注列表并落库（列表页「刷新关注」按钮）。 */
    @PostMapping("/following/refresh")
    public RefreshTwitterFollowingResponse refreshFollowing(
            @RequestParam(required = false) String ownerScreenName,
            @RequestParam(defaultValue = "0") int max) {
        return RefreshTwitterFollowingResponse.from(fetchService.refreshFollowing(ownerScreenName, max));
    }

    @PostMapping("/posts/sync")
    public SyncTwitterPostsResponse syncPosts(@RequestParam String handle) {
        return SyncTwitterPostsResponse.from(fetchService.syncPosts(handle));
    }

    @GetMapping("/author/sync-state")
    public TwitterAuthorSyncStateDto authorSyncState(@RequestParam String handle) {
        return TwitterAuthorSyncStateDto.from(queryService.getAuthorSyncState(handle));
    }

    @PostMapping("/posts/fetch-and-persist")
    public FetchTwitterPostsResponse fetchAndPersistPosts(
            @RequestParam String handle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int maxPosts) {
        return FetchTwitterPostsResponse.from(fetchService.fetchPostsAndPersist(handle, from, to, maxPosts));
    }

    @GetMapping("/posts/listAll")
    public List<TwitterPostDto> listPosts(
            @RequestParam(required = false) String handle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return queryService.listPosts(handle, from, to, keyword, limit);
    }

    @GetMapping("/posts/daily")
    public TwitterDailyDto getDaily(
            @RequestParam String handle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queryService.getDaily(handle, date);
    }
}
