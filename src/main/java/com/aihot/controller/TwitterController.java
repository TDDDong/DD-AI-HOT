package com.aihot.controller;

import com.aihot.dto.twitter.FetchTwitterPostsResponse;
import com.aihot.dto.twitter.TwitterDailyDto;
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

    /**
     * 获取关注列表。
     * screenName 为空时使用当前登录用户；与拉取推文解耦，不落库。
     */
    @GetMapping("/following")
    public List<TwitterUserDto> listFollowing(
            @RequestParam(required = false) String screenName,
            @RequestParam(defaultValue = "0") int max) {
        return queryService.toUserDtos(fetchService.listFollowing(screenName, max));
    }

    /**
     * 拉取指定博主推文（user-posts），按 from/to 在 Java 侧过滤后幂等落库。
     */
    @PostMapping("/posts/fetch-and-persist")
    public FetchTwitterPostsResponse fetchAndPersistPosts(
            @RequestParam String handle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int maxPosts) {
        return FetchTwitterPostsResponse.from(fetchService.fetchPostsAndPersist(handle, from, to, maxPosts));
    }

    /** 已落库推文列表。 */
    @GetMapping("/posts/listAll")
    public List<TwitterPostDto> listPosts(
            @RequestParam(required = false) String handle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return queryService.listPosts(handle, from, to, keyword, limit);
    }

    /** 指定博主某日的已落库推文。 */
    @GetMapping("/posts/daily")
    public TwitterDailyDto getDaily(
            @RequestParam String handle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queryService.getDaily(handle, date);
    }
}
