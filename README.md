# AI Hot

每日自动拉取随机英语单词热点，经 DeepSeek 辅助学习/翻译，写入飞书多维表格，并向群聊推送摘要；支持标记生词并手动导出至 Obsidian。

## 技术栈

- Java 21 + Spring Boot 3.4
- Maven
- 飞书官方 SDK（`oapi-sdk`）
- xxapi 随机英语单词 API（Java `HttpClient`）
- DeepSeek API（OpenAI 兼容，`openai-java`）
- MySQL 8 + MyBatis-Plus + Flyway

## 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+

## 快速开始

1. 复制环境变量模板并按需填写：

   ```bash
   cp .env.example .env
   ```

2. 创建 MySQL 数据库（也可直接执行 [`docs/sql/english_word_schema.sql`](docs/sql/english_word_schema.sql)）：

   ```sql
   CREATE DATABASE ai_hot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. 配置 `.env` 中的 `DB_*` 等变量。

4. 编译并运行（Flyway 自动执行 [`db/migration`](src/main/resources/db/migration) 初始化表结构）：

   ```bash
   mvn spring-boot:run
   ```

5. 健康检查：

   ```bash
   curl http://localhost:8080/actuator/health
   ```

6. 拉取随机英语单词：

   ```bash
   curl http://localhost:8080/api/v1/english-words/random
   ```

## 项目分层结构

```
com.aihot/
├── AiHotApplication.java          # 启动入口
├── config/                        # Spring 配置 & 外部 Client Bean
│   ├── properties/                # @ConfigurationProperties
│   ├── FeishuClientConfig.java
│   ├── EnglishHotspotClientConfig.java
│   └── DeepSeekClientConfig.java
├── controller/                    # REST 接口层
│   └── EnglishWordController.java
├── dto/                           # API 请求/响应对象
│   └── english/
├── advice/                        # 全局异常处理等 Web 切面
│   └── GlobalExceptionHandler.java
├── service/                       # 业务逻辑层
│   ├── english/                   # 英语热点拉取与落库
│   └── storage/                   # 飞书 Base 存储
├── integration/                   # 外部系统适配层
│   ├── english/                   # xxapi HttpClient 封装
│   └── feishu/                    # 飞书 Base SDK 封装
├── entity/                        # MyBatis-Plus 实体（MySQL 表映射）
│   └── english/
├── mapper/                        # MyBatis-Plus Mapper
│   └── english/
└── common/                        # 通用组件
    └── exception/
```

### 分层职责

| 层 | 职责 |
|---|---|
| `controller` | HTTP 接口、参数校验 |
| `dto` | API 层数据传输对象（与 domain 分离） |
| `advice` | 全局异常处理等 Controller 切面 |
| `service` | 业务流程编排，不直接调用第三方 SDK |
| `integration` | xxapi / 飞书 API 调用、字段映射 |
| `mapper` | 数据库 CRUD（MyBatis-Plus） |
| `entity` | 数据库实体，与 MySQL 表一一对应 |
| `domain` | 业务领域模型（EnglishWordRecord、SaveResult 等） |
| `config` | 配置属性、Bean 注册 |
| `common` | 跨层共享的异常、工具类 |

## 配置说明

| 环境变量 | 说明 |
|----------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 连接（默认 `127.0.0.1:3306/ai_hot`） |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号密码 |
| `FEISHU_APP_ID` / `FEISHU_APP_SECRET` | 飞书应用凭证 |
| `FEISHU_BASE_APP_TOKEN` | 多维表格 app_token（见 feishu.yaml） |
| `FEISHU_BASE_TABLE_ID` | 单词数据表 table_id（见 feishu.yaml） |
| `ENGLISH_HOTSPOT_API_URL` | 随机英语单词 API（默认 xxapi） |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `OBSIDIAN_VAULT_PATH` | Obsidian Vault 绝对路径（写入每日例句必需） |

**群聊 chat_id** 与 **Base 表连接/列名映射** 在 [`feishu.yaml`](src/main/resources/feishu.yaml) 中配置。

**单词数据** 存储在 MySQL 表 `english_word`（按 `word` 去重）。完整建表语句见 [`docs/sql/english_word_schema.sql`](docs/sql/english_word_schema.sql)。

## 英语热点 HTTP API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/english-words/random` | 从 xxapi 拉取一个随机英语单词（不落库） |
| POST | `/api/v1/english-words/persist` | 拉取随机单词并写入 MySQL |
| GET | `/api/v1/english-words/listAll?limit=20` | 查询最近入库单词（JSON 列解析为结构化 List） |
| GET | `/api/v1/english-words/id/{id}` | 按主键查询单词详情 |
| GET | `/api/v1/english-words/{word}` | 按单词查询详情（忽略大小写） |

## Obsidian HTTP API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/obsidian/daily-sentences` | 接收每日例句并写入 Obsidian Vault |

写入路径：`{OBSIDIAN_VAULT_PATH}/{sentence-subdir}/YYYY-MM-DD.md`（默认子目录 `每日例句`）。

请求示例：

```json
{
  "date": "2026-06-02",
  "word": "stepwise",
  "sentences": [
    {
      "content": "Through stepwise refinement, I can drive the model to finer details.",
      "cn": "通过逐步的细化，我可以从模型派生出更为详细的细节。"
    }
  ]
}
```

`date` 可省略，默认当天；`word` 可省略，用于 Markdown 二级标题分组。

## 开发状态

- [x] [1] 项目初始化
- [x] [2] 英语热点拉取（xxapi）
- [x] [3] MySQL 落库
- [x] [4] 飞书 Base 存储
- [ ] [5] LLM 翻译/学习辅助
- [ ] [6] 群聊推送
- [ ] [7] 定时调度与 Pipeline
- [x] [8] Obsidian 每日例句导出
- [ ] [9] 测试与文档
