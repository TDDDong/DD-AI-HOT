package com.aihot.domain.obsidian;

import java.time.LocalDate;

public record ObsidianSaveResult(String filePath, LocalDate date, int savedCount, boolean created) {}
