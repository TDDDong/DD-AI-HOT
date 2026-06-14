package com.aihot.entity.content;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("content_tag")
public class ContentTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tagName;

    private String tagType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
