package com.aihot.entity.english;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 相关词汇组内的单个词。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatedHwdItem {

    private String hwd;
    private String tran;
}
