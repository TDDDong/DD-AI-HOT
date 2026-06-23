package com.aihot.domain.twitter;

/** twitter-cli 返回的用户摘要。 */
public record TwitterUser(
        String id,
        String name,
        String screenName,
        String bio,
        int followers,
        int following,
        int tweets,
        boolean verified) {}
