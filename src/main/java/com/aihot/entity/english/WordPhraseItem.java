package com.aihot.entity.english;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 与单词相关的短语。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordPhraseItem {

    private String content;
    private String cn;
}
