package com.aihot.entity.english;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 包含该单词的例句。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordSentenceItem {

    private String content;
    private String cn;
}
