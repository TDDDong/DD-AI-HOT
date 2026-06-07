package com.aihot.dto.obsidian;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record SaveDailySentencesRequest(
        LocalDate date,
        String word,
        @NotEmpty(message = "sentences 不能为空") @Valid List<DailySentenceItemDto> sentences) {}
