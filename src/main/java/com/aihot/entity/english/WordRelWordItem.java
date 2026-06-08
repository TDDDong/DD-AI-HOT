package com.aihot.entity.english;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 与单词相关的词汇列表项。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordRelWordItem {

    private String pos;
    private List<RelatedHwdItem> hwds = new ArrayList<>();
}
