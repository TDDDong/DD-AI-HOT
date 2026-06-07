package com.aihot.domain.obsidian;

import java.time.LocalDate;
import java.util.List;

public record DailySentenceBatch(LocalDate date, String word, List<DailySentence> sentences) {}
