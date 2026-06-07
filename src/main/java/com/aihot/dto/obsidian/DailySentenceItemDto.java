package com.aihot.dto.obsidian;

import jakarta.validation.constraints.NotBlank;

public record DailySentenceItemDto(
        @NotBlank(message = "content 不能为空") String content,
        @NotBlank(message = "cn 不能为空") String cn) {}
