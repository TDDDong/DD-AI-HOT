package com.aihot.entity.english;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单词释义项。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordTranslationItem {

    private String pos;
    private String tranCn;
}
