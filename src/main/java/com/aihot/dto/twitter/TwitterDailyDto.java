package com.aihot.dto.twitter;

import java.time.LocalDate;
import java.util.List;

/** 指定博主某日的推文批次。 */
public record TwitterDailyDto(
        String handle,
        LocalDate date,
        Long digestId,
        String title,
        List<TwitterPostDto> posts) {}
