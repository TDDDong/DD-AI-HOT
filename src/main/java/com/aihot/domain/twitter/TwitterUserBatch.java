package com.aihot.domain.twitter;

import java.time.LocalDate;
import java.util.List;

/** 指定博主在某一抓取批次内的推文集合。 */
public record TwitterUserBatch(String handle, LocalDate fetchDate, List<TwitterPost> posts) {}
