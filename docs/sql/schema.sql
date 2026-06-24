-- ai-hot 项目数据库结构（手动维护）
-- 使用方式：
--   1. CREATE DATABASE dd_ai_hot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   2. USE dd_ai_hot;
--   3. 执行本文件
-- 新增表或字段时，直接在本文件末尾追加 DDL 即可。

-- ---------------------------------------------------------------------------
-- 英语单词
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS english_word (
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

-- ---------------------------------------------------------------------------
-- AI 热点日报
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_digest (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    source_type     VARCHAR(64)     NOT NULL COMMENT '来源类型，如 aibase_daily',
    report_date     DATE            NOT NULL COMMENT '日报日期',
    title           VARCHAR(512)    NOT NULL COMMENT '日报标题',
    file_path       VARCHAR(512)    NULL COMMENT 'Obsidian 相对路径',
    file_name       VARCHAR(256)    NULL COMMENT 'Obsidian 文件名',
    content_hash    VARCHAR(64)     NOT NULL COMMENT '整批内容哈希',
    status          VARCHAR(32)     NOT NULL DEFAULT 'normal' COMMENT '状态',
    metadata_json   JSON            NULL COMMENT '扩展元数据',
    published_at    DATETIME(3)     NULL COMMENT '发布时间',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_digest_source_date (source_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_article (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    digest_id       BIGINT          NOT NULL COMMENT '关联 content_digest.id',
    article_key     VARCHAR(64)     NOT NULL COMMENT '文章稳定键',
    rank_no         INT             NOT NULL COMMENT '当日序号',
    title           VARCHAR(1024)   NOT NULL COMMENT '标题',
    summary         MEDIUMTEXT      NULL COMMENT '摘要',
    source_url      VARCHAR(1024)   NULL COMMENT '原文链接',
    anchor          VARCHAR(64)     NULL COMMENT '日报内锚点',
    tags_json       JSON            NULL COMMENT '热词列表（预留，AI 写入）',
    extra_json      JSON            NULL COMMENT '扩展字段',
    content_hash    VARCHAR(64)     NOT NULL COMMENT '单条内容哈希',
    report_date     DATE            NOT NULL COMMENT '冗余日报日期',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_article_key (article_key),
    KEY idx_content_article_digest_id (digest_id),
    KEY idx_content_article_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_tag (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    tag_name        VARCHAR(128)    NOT NULL COMMENT '标签/热词名',
    tag_type        VARCHAR(32)     NOT NULL DEFAULT 'hotword' COMMENT '类型：hotword/category/custom',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_article_tag (
    article_id      BIGINT          NOT NULL COMMENT '关联 content_article.id',
    tag_id          BIGINT          NOT NULL COMMENT '关联 content_tag.id',
    source          VARCHAR(32)     NOT NULL DEFAULT 'llm' COMMENT '来源：llm/manual/sync',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (article_id, tag_id),
    KEY idx_content_article_tag_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Twitter/X 关注列表（落库，列表页读库；刷新走独立接口）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS twitter_following (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    owner_screen_name   VARCHAR(64)     NOT NULL COMMENT '关注列表所属账号（当前登录用户）',
    user_id             VARCHAR(64)     NOT NULL COMMENT 'X 用户 ID',
    screen_name         VARCHAR(64)     NOT NULL COMMENT '被关注用户 @ 名（规范化小写）',
    name                VARCHAR(256)    NULL COMMENT '显示名',
    bio                 VARCHAR(512)    NULL COMMENT '简介',
    followers_count     INT             NOT NULL DEFAULT 0 COMMENT '粉丝数',
    following_count     INT             NOT NULL DEFAULT 0 COMMENT '关注数',
    tweets_count        INT             NOT NULL DEFAULT 0 COMMENT '推文数',
    verified            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否认证',
    status              VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active/removed',
    fetched_at          DATETIME(3)     NOT NULL COMMENT '最近一次从 X 同步时间',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_twitter_following_owner_screen (owner_screen_name, screen_name),
    KEY idx_twitter_following_owner_status (owner_screen_name, status),
    KEY idx_twitter_following_fetched_at (owner_screen_name, fetched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
