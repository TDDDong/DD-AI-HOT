CREATE TABLE english_word (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    word            VARCHAR(128)    NOT NULL,
    uk_phone        VARCHAR(64)     NULL,
    us_phone        VARCHAR(64)     NULL,
    uk_speech       VARCHAR(512)    NULL,
    us_speech       VARCHAR(512)    NULL,
    detail_json     JSON            NOT NULL,
    marked          TINYINT(1)      NOT NULL DEFAULT 0,
    exported        TINYINT(1)      NOT NULL DEFAULT 0,
    imported_at     DATETIME(3)     NOT NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_english_word_word (word),
    KEY idx_english_word_imported_at (imported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
