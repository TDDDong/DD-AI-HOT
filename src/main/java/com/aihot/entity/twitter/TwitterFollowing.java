package com.aihot.entity.twitter;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("twitter_following")
public class TwitterFollowing {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_REMOVED = "removed";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ownerScreenName;

    private String userId;

    private String screenName;

    private String name;

    private String bio;

    private Integer followersCount;

    private Integer followingCount;

    private Integer tweetsCount;

    private Boolean verified;

    private String status;

    private LocalDateTime fetchedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
