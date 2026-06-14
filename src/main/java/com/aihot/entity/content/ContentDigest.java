package com.aihot.entity.content;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
@TableName(value = "content_digest", autoResultMap = true)
public class ContentDigest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceType;

    private LocalDate reportDate;

    private String title;

    private String filePath;

    private String fileName;

    private String contentHash;

    private String status;

    @TableField(value = "metadata_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadataJson = new LinkedHashMap<>();

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
