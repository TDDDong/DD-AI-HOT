package com.aihot.entity.english;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 与该单词相关的同义词列表项。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordSynonymItem {

    private String pos;
    private String tran;
    private List<SynonymWordItem> words = new ArrayList<>();
}
