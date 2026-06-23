package com.aihot.dto.twitter;

/** Twitter 用户摘要（关注列表 / whoami）。 */
public record TwitterUserDto(
        String id,
        String name,
        String screenName,
        String bio,
        int followers,
        int following,
        int tweets,
        boolean verified) {}
