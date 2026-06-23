package com.aihot.integration.twitter;

import com.aihot.domain.twitter.TwitterPost;
import com.aihot.domain.twitter.TwitterUser;
import java.util.List;

/** 通过 twitter-cli 读取 Twitter/X 数据。 */
public interface TwitterCliClient {

    /** 当前登录用户（whoami）。 */
    TwitterUser fetchCurrentUser();

    /** 指定用户的关注列表；screenName 为空时使用当前用户。 */
    List<TwitterUser> fetchFollowing(String screenName, int maxCount);

    /** 指定用户的最近推文（不做日期过滤）。 */
    List<TwitterPost> fetchUserPosts(String screenName, int maxCount);
}
