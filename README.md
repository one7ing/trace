# Trace · 个人成长轨迹记录与分析系统

基于 AI 的个人成长助手 —— 日记记录、知识问答、模拟面试、周报生成、学习计划。

---

## 技术栈

| 层 | 技术 |
|---|---|
| **语言** | Java 21 |
| **框架** | Spring Boot 3.4.6 + Spring AI 1.1.2 + Spring AI Alibaba DashScope |
| **AI 模型** | qwen-plus（对话）· text-embedding-v3（向量）· Tavily（联网搜索） |
| **ORM** | MyBatis-Plus 3.5.9 |
| **数据库** | PostgreSQL 18 + PgVector |
| **缓存/锁** | Redis + Redisson 3.36 |
| **消息队列** | RabbitMQ 3.12 |
| **对象存储** | MinIO |
| **认证** | Spring Security + JJWT 0.12.6 |
| **前端** | Vue 3 + TypeScript + Vite 5 + Element Plus + Pinia + ECharts |
| **PDF** | Flying Saucer + Thymeleaf |
| **DB 迁移** | Flyway |

---

## 项目结构

```
trace/
├── frontend/                    # Vue 3 前端 SPA
│   └── src/
│       ├── api/                 # Axios 封装
│       ├── components/          # SideNav、ChatMessage、MarkdownRenderer
│       ├── router/              # Vue Router 路由配置
│       ├── stores/              # Pinia 认证状态
│       ├── styles/              # 全局 SCSS + 深色模式变量
│       └── views/               # 页面组件
│           ├── dashboard/       # 仪表盘
│           ├── diary/           # 日记 CRUD
│           ├── interview/       # 面试会话 + 历史 + 详情
│           ├── KnowledgeChat    # AI 问答 (SSE)
│           ├── KnowledgeBase    # 知识库管理
│           ├── StudyPlan        # 学习计划
│           ├── WeeklyReport     # 周报
│           └── Login            # 登录/注册
│
├── src/main/java/com/trace/
│   ├── TraceApplication.java    # 启动入口
│   ├── agent/                   # AI Agent 系统
│   │   ├── Agent.java           # 核心接口
│   │   ├── AbstractAgent.java   # 抽象基类（流式/取消/上下文）
│   │   ├── AgentRouter.java     # 意图路由器
│   │   ├── KnowledgeAgent.java  # 知识问答（联网搜索/RAG/直接回答）
│   │   ├── DiaryAgent.java      # 日记相关
│   │   ├── InterviewAgent.java  # 面试相关
│   │   ├── PlanAgent.java       # 学习计划
│   │   ├── WeeklyReportAgent.java # 周报
│   │   ├── QueryRewriteAgent.java # 查询改写
│   │   └── MemoryExtractAgent.java # 长期记忆提取
│   ├── config/                  # 配置类
│   │   ├── RedisConfig.java
│   │   ├── RedissonConfig.java
│   │   ├── RabbitMQConfig.java
│   │   ├── MinioConfig.java / MinioClientConfig.java
│   │   ├── JwtConfig.java
│   │   └── ListStringTypeHandler.java
│   ├── controller/              # REST 控制器
│   │   ├── AuthController       # 注册/登录/头像/昵称
│   │   ├── KnowledgeController  # AI 聊天 (SSE) + 历史
│   │   ├── DiaryController      # 日记 CRUD
│   │   ├── InterviewController  # 面试会话管理
│   │   ├── WeeklyReportController # 周报生成
│   │   ├── PlanController       # 学习计划
│   │   ├── CheckInController    # 每日打卡
│   │   ├── DashboardController  # 仪表盘聚合
│   │   └── KnowledgeBaseController # 知识库管理
│   ├── dto/                     # 请求/响应 DTO
│   ├── entity/                  # 数据库实体 (MyBatis-Plus)
│   ├── enums/                   # IntentType, SearchType
│   ├── exception/               # 全局异常处理
│   ├── mapper/                  # MyBatis Mapper 接口 + XML
│   ├── scheduler/               # 定时任务
│   │   ├── WeeklyReportScheduler   # 每周日凌晨生成周报
│   │   └── ChatMemoryExtractScheduler # 每10分钟提取记忆
│   ├── security/                # JWT 认证
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtUtil.java
│   └── service/                 # 业务逻辑
│       └── impl/
│           ├── AuthServiceImpl
│           ├── DiaryServiceImpl
│           ├── InterviewServiceImpl
│           ├── WeeklyReportServiceImpl
│           ├── PlanServiceImpl
│           ├── KnowledgeBaseServiceImpl
│           ├── MemoryServiceImpl      # 三层记忆
│           ├── SearchRouterServiceImpl # 检索路由
│           ├── PdfServiceImpl         # Markdown → PDF
│           ├── InterviewBankService   # 导入题库
│           ├── PlanGenerationConsumer # RabbitMQ 消费者
│           ├── InterviewEvaluationConsumer
│           ├── InterviewSseRegistry
│           └── PlanSseRegistry
│
├── src/main/resources/
│   ├── application.yml           # 应用配置
│   ├── mcp-servers.json          # MCP 工具配置 (Tavily)
│   ├── db/migration/             # Flyway 迁移 (V1 ~ V13)
│   ├── agent/prompts/            # System Prompt 模板
│   └── templates/report.html     # 周报 PDF 模板
│
├── docker-compose.yml            # PostgreSQL + MinIO + RabbitMQ
└── pom.xml
```

---

## 数据库表

| 表 | 说明 |
|---|---|
| `users` | 用户表 |
| `diaries` | 日记（每人每天 1 条） |
| `interview_records` | 面试记录（含 AI 分析） |
| `weekly_reports` | 周报（每周唯一） |
| `study_plans` | 学习计划 |
| `long_term_memories` | 长期记忆（≤30 条/人） |
| `chat_history` | 聊天历史（≤500 条/人） |
| `daily_check_ins` | 每日打卡 |
| `growth_anchors` | 成长锚点 |
| `knowledge_bases` | 用户知识库（文件上传） |
| `vector_store` | Spring AI PgVector（向量嵌入） |

---

## 核心架构

### Agent 路由系统

```
用户输入 → AgentRouter
  ├── DiaryAgent?       (日记/心情/回顾/记录)
  ├── InterviewAgent?   (面试/出题/评分/评估)
  ├── PlanAgent?        (计划/目标/规划/学习路线)
  ├── WeeklyReportAgent?(周报/总结/汇总)
  └── KnowledgeAgent   ← 兜底，意图分类：
       ├── DIRECT      → AI 直接回答
       ├── WEB_SEARCH  → MCP Tavily 联网搜索 → SSE 流式输出
       └── SEARCH      → PgVector 混合检索 → AI 综合回答
```

### 三层记忆

```
┌─ 短期上下文 (Redis List)    ≤20 条  ── Prompt 注入
├─ 会话历史 (PG chat_history)  ≤500 条 ── 前端分页展示
└─ 长期记忆 (PG long_term_memories) ≤30 条 ── AI 特征提取
```

### 取消机制

```
用户点击"停止" → Redis SET chat:cancel:{userId} (TTL 60s)
                    ↓
流式生成 takeUntil → GET chat:cancel:{userId} → 非 null → 终止 Flux
```

### 混合检索

```
向量相似度 (0.7) × cosine distance
  + 全文 ts_rank (0.3) × keyword match
  = 混合分数 → 排序 → Top-K
```

### 异步处理

```
计划生成 ──→ RabbitMQ queue ──→ PlanGenerationConsumer ──→ SSE 推送完成
面试评估 ──→ RabbitMQ queue ──→ InterviewEvaluationConsumer ──→ SSE 推送完成
周报定时 ──→ @Scheduled (每周日 02:00) + Redisson 分布式锁
记忆提取 ──→ @Scheduled (每 10 分钟) + 人均 10 分钟冷却
```

---

## 基础设施

```yaml
# docker-compose.yml
postgres:  pgvector/pgvector:pg18   → localhost:5432
minio:     minio/minio              → localhost:9000 (API) / 9001 (Console)
rabbitmq:  rabbitmq:3.12-management → localhost:5672 (AMQP) / 15672 (管理界面)
redis:     redis                    → localhost:6379
```

启动：`docker-compose up -d`

---

## 快速开始

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 启动后端
./mvnw spring-boot:run

# 3. 启动前端 (开发模式)
cd frontend && npm install && npm run dev

# 4. 访问
# 前端: http://localhost:5173
# 后端: http://localhost:8080
# MinIO 控制台: http://localhost:9001
# RabbitMQ 管理: http://localhost:15672
```

环境变量（可选）：

| 变量 | 默认值 |
|---|---|
| `DASHSCOPE_API_KEY` | 必填 |
| `DB_USERNAME` / `DB_PASSWORD` | postgres / 123456 |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 |
| `RABBITMQ_HOST` / `RABBITMQ_USER` / `RABBITMQ_PASS` | localhost / trace / 123456 |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | localhost:9000 / admin / qwer1234 |
| `JWT_SECRET` | trace-jwt-secret-key-change-in-production |
| `SERVER_PORT` | 8080 |

---

## API 概要

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| **认证** | POST | `/api/auth/register` | 注册 |
| | POST | `/api/auth/login` | 登录 |
| **AI 聊天** | POST | `/api/knowledge/chat` | SSE 流式问答 |
| | POST | `/api/knowledge/stop` | 停止生成 |
| | GET | `/api/knowledge/history` | 聊天历史（游标分页） |
| **日记** | CRUD | `/api/diary` | 日记增删改查 |
| **面试** | POST | `/api/interview/start` | 开始面试 |
| | POST | `/api/interview/answer` | 提交答案 |
| | GET | `/api/interview/records` | 面试记录 |
| **周报** | POST | `/api/weekly-report/generate` | 生成周报 |
| | GET | `/api/weekly-report` | 周报列表 |
| **计划** | POST | `/api/plan/generate` | 异步生成计划 |
| | POST | `/api/plan/create` | 手动创建计划 |
| **打卡** | POST | `/api/checkin` | 每日打卡 |
| **仪表盘** | GET | `/api/dashboard` | 周概览 |
| | GET | `/api/dashboard/growth` | 成长分数+趋势 |
| **知识库** | POST | `/api/knowledge-base/upload` | 上传文件 |
| | GET | `/api/knowledge-base/search` | 搜索知识库 |
| | GET | `/api/knowledge-base/hybrid-search` | 混合检索 |

---

## 前端页面

| 路由 | 页面 | 功能 |
|---|---|---|
| `/dashboard` | Dashboard | 成长仪表盘（周概览 + 趋势图 + 热力图 + 打卡分布） |
| `/knowledge` | KnowledgeChat | AI 问答（Markdown 渲染 · SSE 流式 · 语音输入 · 取消） |
| `/interview` | InterviewHome | 面试首页（行业选择 + 简历） |
| `/interview/start` | InterviewSession | 实时面试（SSE 题目流 · 语音输入） |
| `/interview/history` | InterviewHistory | 面试历史列表 |
| `/interview/history/:id` | InterviewDetail | 面试详情 + AI 分析 |
| `/diary` | DiaryList | 日记列表（分页） |
| `/diary/write` | DiaryWrite | 写日记 |
| `/diary/:id` | DiaryDetail | 日记详情 |
| `/weekly-report` | WeeklyReport | 周报列表 |
| `/plan` | StudyPlan | 学习计划 |
| `/knowledge-base` | KnowledgeBase | 知识库管理（上传/搜索/删除/编辑） |
| `/login` | Login | 登录注册 |

---

## 设计决策

- **Agent 模式**：关键词路由 + KnowledgeAgent 兜底，简单高效
- **SSE 优先**：聊天流式、计划生成进度、面试评估结果都走 SSE 推送
- **异步解耦**：耗时任务（计划生成、面试评估）通过 RabbitMQ 异步处理
- **Redis 取消**：`chat:cancel:{userId}` 键实现分布式流式取消信号
- **PgVector HNSW**：余弦相似度索引，无需训练，支持动态增删
- **纯文本记忆**：长期记忆存纯文本而非向量，AI 直接注入 Prompt 更可控
