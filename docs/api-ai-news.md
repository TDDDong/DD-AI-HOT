# AI 热点新闻 API 文档

供前端接入使用。默认服务地址：`http://localhost:8080`（可通过 `server.port` 配置修改）。

- 请求/响应编码：`UTF-8`
- 日期格式：`YYYY-MM-DD`（ISO-8601）
- 响应体：`application/json`
- 字段命名：JSON 使用 **camelCase**（与 Java record 一致）

---

## 一、读取接口（前端消费）

基础路径：`/api/v1/ai-news`

### 1. 新闻列表

按日期范围查询文章，支持热词、关键词过滤。

```
GET /api/v1/ai-news/listAll
```

#### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| from | string (date) | 否 | 今天往前 90 天 | 起始日期，含当天 |
| to | string (date) | 否 | 今天 | 结束日期，含当天 |
| tag | string | 否 | - | 按热词精确过滤（匹配 `hotwords` 数组中的某一项） |
| keyword | string | 否 | - | 标题或摘要模糊搜索 |
| limit | int | 否 | 20 | 返回条数上限，范围 1–100 |

#### 排序规则

- 先按 `date` 降序（最新日期在前）
- 同一天内按 `rankNo` 升序

#### 响应 `200`

返回 `AiNewsArticleDto[]` 数组。

```json
[
  {
    "articleId": "a3f2c8e1b9d04f6a8c7e2d1b5f9a0c4e6d8f1a2b3c4d5e6f7a8b9c0d1e2f3a4",
    "id": 101,
    "date": "2026-06-12",
    "rankNo": 1,
    "title": "让小店用上大连锁的智能大脑，高德问店上线AI能力开放调用",
    "summary": "高德云图宣布开放商业智能体生态……",
    "url": "https://news.aibase.com/zh/news/28871",
    "hotwords": [],
    "anchor": "article-1",
    "filePath": null
  }
]
```

#### 字段说明（AiNewsArticleDto）

| 字段 | 类型 | 说明 |
|------|------|------|
| articleId | string | 文章稳定唯一键（SHA-256），跨次同步不变 |
| id | number | 数据库主键 |
| date | string | 所属日报日期 |
| rankNo | number | 当日热点序号，从 1 开始 |
| title | string | 标题 |
| summary | string | 摘要正文 |
| url | string | 原文链接（aibase 新闻页） |
| hotwords | string[] | 热词列表；暂未 AI 生成时为 `[]` |
| anchor | string | 日报内锚点，如 `article-1`，可用于页内跳转 |
| filePath | string \| null | Obsidian 备份文件路径（未同步时为 null） |

#### 示例

```http
GET /api/v1/ai-news/listAll?from=2026-06-01&to=2026-06-12&limit=20
GET /api/v1/ai-news/listAll?keyword=高德&limit=10
GET /api/v1/ai-news/listAll?tag=大模型
```

---

### 2. 指定日期整期日报

返回某一天完整的 AI 日报批次及全部文章。

```
GET /api/v1/ai-news/daily
```

#### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| date | string (date) | 否 | 今天 | 日报日期 |

#### 响应 `200`

```json
{
  "date": "2026-06-12",
  "digestId": 10,
  "title": "AI 每日热点摘要 - 2026-06-12",
  "dailyPageUrl": "https://www.aibase.com/zh/daily/28889",
  "filePath": null,
  "articles": [
    {
      "articleId": "a3f2c8e1……",
      "id": 101,
      "date": "2026-06-12",
      "rankNo": 1,
      "title": "让小店用上大连锁的智能大脑，高德问店上线AI能力开放调用",
      "summary": "高德云图宣布开放商业智能体生态……",
      "url": "https://news.aibase.com/zh/news/28871",
      "hotwords": [],
      "anchor": "article-1",
      "filePath": null
    }
  ]
}
```

#### 字段说明（AiNewsDailyDto）

| 字段 | 类型 | 说明 |
|------|------|------|
| date | string | 日报日期 |
| digestId | number | 日报批次 ID |
| title | string | 日报标题 |
| dailyPageUrl | string \| null | aibase 当日日报详情页 URL |
| filePath | string \| null | Obsidian 备份路径 |
| articles | AiNewsArticleDto[] | 当日全部文章，按 `rankNo` 升序 |

#### 示例

```http
GET /api/v1/ai-news/daily?date=2026-06-12
GET /api/v1/ai-news/daily
```

#### 错误 `404`

指定日期无数据时：

```json
{
  "error": "未找到日期 2026-06-12 的 AI 日报"
}
```

---

## 二、数据写入接口（抓取落库）

基础路径：`/api/v1/obsidian`

一般由后端定时任务或运维触发；前端通常**不需要**调用，仅在需要手动补抓历史数据时使用。

### 3. 拉取 aibase 热点并落库

从 aibase 抓取指定日期 AI 日报，解析后幂等写入 MySQL。

```
POST /api/v1/obsidian/ai-news/fetch-and-persist
```

#### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| date | string (date) | 否 | 今天 | 要抓取的日报日期 |

#### 响应 `200`

```json
{
  "reportDate": "2026-06-12",
  "digestId": 10,
  "insertedArticles": 8,
  "updatedArticles": 0,
  "skippedArticles": 0,
  "totalArticles": 8
}
```

#### 字段说明（FetchAiNewsResponse）

| 字段 | 类型 | 说明 |
|------|------|------|
| reportDate | string | 抓取的目标日期 |
| digestId | number | 写入/更新后的日报批次 ID |
| insertedArticles | number | 本次新增文章数 |
| updatedArticles | number | 本次更新文章数 |
| skippedArticles | number | 内容未变化、跳过的文章数 |
| totalArticles | number | 本批文章总数 |

#### 示例

```http
POST /api/v1/obsidian/ai-news/fetch-and-persist
POST /api/v1/obsidian/ai-news/fetch-and-persist?date=2026-06-12
```

#### 错误 `502`

aibase 抓取失败时：

```json
{
  "error": "aibase 日报详情页未解析到文章: https://www.aibase.com/zh/daily/28889"
}
```

---

## 三、通用错误格式

所有错误响应均为：

```json
{
  "error": "错误描述信息"
}
```

| HTTP 状态码 | 场景 |
|-------------|------|
| 400 | 参数非法，如 `from` 晚于 `to` |
| 404 | 资源不存在，如指定日期无日报 |
| 502 | 外部依赖失败，如 aibase 抓取异常 |

---

## 四、前端接入建议

### 典型页面场景

| 场景 | 推荐接口 |
|------|----------|
| 首页/列表页展示近期热点 | `GET /api/v1/ai-news/listAll` |
| 某日完整日报详情页 | `GET /api/v1/ai-news/daily?date=YYYY-MM-DD` |
| 搜索 | `GET /api/v1/ai-news/listAll?keyword=关键词` |

### 数据流

```
1. 后端先 POST /api/v1/obsidian/ai-news/fetch-and-persist  （抓取落库）
2. 前端 GET  /api/v1/ai-news/daily 或 /listAll               （读取展示）
```

### 热词字段说明

- 响应中的 `hotwords` 字段已预留，当前默认为空数组 `[]`
- 后续由 AI 分析接口写入后，列表/日报接口会自动返回热词数据
- `listAll` 的 `tag` 参数可在热词写入后用于按热词筛选

### TypeScript 类型参考

```typescript
interface AiNewsArticle {
  articleId: string;
  id: number;
  date: string;       // YYYY-MM-DD
  rankNo: number;
  title: string;
  summary: string;
  url: string;
  hotwords: string[];
  anchor: string;
  filePath: string | null;
}

interface AiNewsDaily {
  date: string;
  digestId: number;
  title: string;
  dailyPageUrl: string | null;
  filePath: string | null;
  articles: AiNewsArticle[];
}

interface FetchAiNewsResult {
  reportDate: string;
  digestId: number;
  insertedArticles: number;
  updatedArticles: number;
  skippedArticles: number;
  totalArticles: number;
}

interface ApiError {
  error: string;
}
```

### fetch 示例

```typescript
const BASE = 'http://localhost:8080';

// 获取某日日报
async function getDaily(date: string): Promise<AiNewsDaily> {
  const res = await fetch(`${BASE}/api/v1/ai-news/daily?date=${date}`);
  if (!res.ok) {
    const err: ApiError = await res.json();
    throw new Error(err.error);
  }
  return res.json();
}

// 获取新闻列表
async function listArticles(params?: {
  from?: string;
  to?: string;
  tag?: string;
  keyword?: string;
  limit?: number;
}): Promise<AiNewsArticle[]> {
  const qs = new URLSearchParams();
  if (params?.from) qs.set('from', params.from);
  if (params?.to) qs.set('to', params.to);
  if (params?.tag) qs.set('tag', params.tag);
  if (params?.keyword) qs.set('keyword', params.keyword);
  if (params?.limit) qs.set('limit', String(params.limit));
  const res = await fetch(`${BASE}/api/v1/ai-news/listAll?${qs}`);
  if (!res.ok) {
    const err: ApiError = await res.json();
    throw new Error(err.error);
  }
  return res.json();
}
```

---

## 五、接口一览

| 方法 | 路径 | 用途 | 调用方 |
|------|------|------|--------|
| GET | `/api/v1/ai-news/listAll` | 新闻列表（可筛选） | 前端 |
| GET | `/api/v1/ai-news/daily` | 指定日期整期日报 | 前端 |
| POST | `/api/v1/obsidian/ai-news/fetch-and-persist` | 抓取 aibase 并落库 | 后端/运维 |

---

## 六、健康检查

```
GET /actuator/health
```

用于确认服务是否正常运行。
