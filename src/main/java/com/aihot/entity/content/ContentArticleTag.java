package com.aihot.entity.content;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("content_article_tag")
public class ContentArticleTag {

    private Long articleId;

    private Long tagId;

    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
