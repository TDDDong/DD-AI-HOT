package com.aihot.entity.content;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@TableName(value = "content_article", autoResultMap = true)
public class ContentArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long digestId;

    private String articleKey;

    private Integer rankNo;

    private String title;

    private String summary;

    private String sourceUrl;

    private String anchor;

    @TableField(value = "tags_json", typeHandler = JacksonTypeHandler.class)
    private List<String> tagsJson = new ArrayList<>();

    @TableField(value = "extra_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraJson = new LinkedHashMap<>();

    private String contentHash;

    private LocalDate reportDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
