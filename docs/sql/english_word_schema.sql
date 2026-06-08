-- 英语单词表（V2 结构化 JSON 列，含字段注释）
CREATE TABLE english_word (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    word                VARCHAR(128)    NOT NULL COMMENT '单词',
    book_id             VARCHAR(64)     NULL COMMENT '词书 ID',
    uk_phone            VARCHAR(64)     NULL COMMENT '英式音标',
    us_phone            VARCHAR(64)     NULL COMMENT '美式音标',
    uk_speech           VARCHAR(512)    NULL COMMENT '英式发音链接',
    us_speech           VARCHAR(512)    NULL COMMENT '美式发音链接',
    translations_json   JSON            NULL COMMENT '单词释义列表',
    phrases_json        JSON            NULL COMMENT '相关短语列表',
    rel_words_json      JSON            NULL COMMENT '相关词汇列表',
    sentences_json      JSON            NULL COMMENT '例句列表',
    synonyms_json       JSON            NULL COMMENT '同义词列表',
    marked              TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已标记',
    exported            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已导出 Obsidian',
    imported_at         DATETIME(3)     NOT NULL COMMENT '入库时间',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_english_word_word (word),
    KEY idx_english_word_imported_at (imported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
