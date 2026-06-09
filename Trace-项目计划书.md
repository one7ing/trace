# Trace — 个人成长轨迹记录与分析系统 项目计划书

---

## 一、项目概述

**项目名称**：Trace — 个人成长轨迹记录与分析系统

**项目定位**：基于 Spring AI 的个人成长记录工具，帮助用户通过日记、模拟面试、专业知识获取、计划制定和周报回顾来追踪自身成长轨迹。

**AI 角色定位**：记录员 + 知识库 + 教练（非陪伴者），强调客观记录与专业指导。

**核心理念**：用数据与 AI 记录成长每一步，让进步有迹可循。

---

## 二、技术架构

### 2.1 技术栈总览

| 层级 | 技术 | 用途 |
|------|------|------|
| 框架 | Spring Boot 3.x + Spring AI 1.0+ | 项目骨架、大模型集成 |
| JDK | Java 21 | 虚拟线程，高 I/O 吞吐 |
| 大模型 | 阿里云百炼 qwen-plus（对话）+ text-embedding-v3（向量，1024维） | 对话生成、文本向量化 |
| 数据库 | PostgreSQL + PgVector 扩展 | 结构化数据 + 向量存储统一 |
| 缓存 | Redis | 短期对话上下文、面试会话状态、JWT Token |
| 文件存储 | MinIO（S3 兼容） | PDF 报告存储 |
| 消息队列 | RabbitMQ | 异步 PDF 生成、记忆持久化（前期可同步） |
| 任务调度 | Spring @Scheduled | 每周生成成长周报 |
| 外部搜索 | MCP 工具（Brave Search） | 专业知识实时互联网检索 |
| 分布式锁 | Redisson | 多实例防并发冲突 |

### 2.2 架构图

```
┌──────────────────────────────────────────────────────────────┐
│                    前端 SPA (Vue 3)                           │
│   ┌──────────┐  ┌────────────────────────────────────────┐  │
│   │ 左侧导航  │  │           右侧内容区 (聊天/表单/列表)    │  │
│   │ 知识科普  │  │                                        │  │
│   │ 模拟面试  │  │    AI 对话 / 面试交互 / 日记编辑         │  │
│   │ 日记本    │  │                                        │  │
│   │ 成长周报  │  │                                        │  │
│   │ 我的计划  │  │                                        │  │
│   └──────────┘  └────────────────────────────────────────┘  │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTP / SSE (流式输出)
┌──────────────────────▼───────────────────────────────────────┐
│                Spring Boot 3.x 单体应用                       │
│                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ 知识科普  │ │ 模拟面试  │ │  日记本   │ │ 周报 / 计划   │  │
│  │ Controller│ │ Controller│ │ Controller│ │ Controller    │  │
│  └─────┬────┘ └────┬─────┘ └────┬─────┘ └──────┬────────┘  │
│        │           │            │               │           │
│  ┌─────▼───────────▼────────────▼───────────────▼────────┐  │
│  │                  Spring AI 服务层                      │  │
│  │  意图识别 → 记忆检索 → 大模型调用 → 结果处理            │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
└───────────────────────────┼─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────┐
│    Redis     │  │  PostgreSQL      │  │    MinIO     │
│  短期记忆    │  │  + PgVector      │  │  PDF 文件    │
│  会话状态    │  │  结构化 + 向量   │  │  存储        │
│  JWT Token   │  │  统一存储        │  │              │
└──────────────┘  └──────────────────┘  └──────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
              ┌─────────────┴─────────────┐
              │         RabbitMQ          │
              │   异步任务队列 (可选)       │
              └───────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              │     外部 MCP 工具          │
              │   Brave Search 联网检索    │
              └───────────────────────────┘
```

### 2.3 前端技术选型

| 技术 | 用途 |
|------|------|
| Vue 3 | 渐进式前端框架，Composition API |
| Element Plus | UI 组件库（导航栏、卡片、表单、时间线、对话框等） |
| Axios | HTTP 请求，与后端 API 通信 |
| Vue Router 4 | 前端路由管理 |
| Pinia | 状态管理（用户信息、面试会话等） |
| Markdown-it | AI 回复 Markdown 渲染 |
| Vite | 构建工具 |

---

## 三、功能模块详设

### 3.1 专业知识科普

**定位**：AI 作为知识库，基于互联网实时搜索提供专业知识解答。

**核心流程**：

```
用户提问 → 意图识别 → MCP Brave Search 联网检索
    → 大模型基于搜索结果组织答案
    → 附参考链接 + 免责声明 → 流式输出
```

**关键规则**：

- 每次回复必须通过 MCP 实时检索互联网，不允许仅凭模型内部知识回答
- 回复末尾固定显示免责提示：

  > ⚠️ 以上内容基于互联网搜索结果整理，仅供参考。AI 可能会出错，请保持独立判断。

- 用户可选择知识领域（IT、金融、法律等），也可直接提问由系统自动识别

**数据存储**：

- 知识问答摘要 → PgVector（长期记忆）
- 短期对话上下文 → Redis

---

### 3.2 模拟面试

**定位**：AI 作为教练，提供回合制面试模拟与评估。

**流程图**：

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 选择行业  │ →  │ 选择技能  │ →  │ 选择数量  │ →  │ 开始面试  │
│          │    │          │    │          │    │          │
└──────────┘    └──────────┘    └──────────┘    └────┬─────┘
                                                     │
                    ┌────────────────────────────────┘
                    ▼
        ┌─────────────────────┐
        │   出题 (第 N 题)     │ ← PgVector 种子题 + 大模型动态生成
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │   用户输入回答        │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  大模型评估           │
        │  打分 (1-10) + 点评  │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  还有题目？           │
        └────┬──────────┬─────┘
             │是        │否
             ▼          ▼
        回到出题    ┌──────────┐
                    │ 生成报告  │ → PDF → MinIO → 返回下载链接
                    └────┬─────┘
                         ▼
                    ┌──────────┐
                    │ 异步摘要  │ → PgVector 长期记忆
                    └──────────┘
```

**面试会话状态**（Redis Hash）：

```
interview:session:{sessionId}
  - industry: "IT"
  - skills: ["Java", "Spring"]
  - totalQuestions: 5
  - currentQuestion: 3
  - questions: [{...}, {...}]
  - scores: [8, 7, 9]
  - status: "IN_PROGRESS"
  TTL: 2小时
```

**历史记录页**：

- 列表：日期、行业、技能点、平均分
- 详情：每题题目、用户回答、AI 点评、得分
- 支持下载历史面试报告 PDF

---

### 3.3 日记本

**定位**：用户主动记录成长，AI 从中理解用户状态。

**双写机制**：

```
用户写日记
    │
    ├──→ PostgreSQL (diaries 表)
    │    用途：页面 CRUD（查看、编辑、删除）
    │    字段：id, user_id, title, content, mood_tag, created_at, updated_at
    │
    └──→ PgVector (diary_vectors 表)
         用途：语义检索，AI 理解用户近期状态
         字段：id, user_id, content, embedding(1024), created_at
```

**功能**：

- 新建日记：标题、正文、心情标签（开心/平静/焦虑/充实/疲惫等）
- 历史列表：按时间倒序，支持分页
- 查看/编辑/删除
- AI 在后续对话中可语义检索日记内容

---

### 3.4 成长周报

**定位**：AI 自动汇总一周成长轨迹，生成结构化周报。

**定时触发**：

```java
@Scheduled(cron = "0 0 2 * * SUN")  // 每周日凌晨 2:00
@RedissonLock(key = "weekly-report") // 分布式锁防重复
public void generateWeeklyReport() { ... }
```

**生成流程**：

```
定时触发 → Redisson 获取分布式锁
    → 检索本周数据：
        ├── 日记关键事件 (PgVector 语义检索)
        ├── 知识科普热点 (PgVector)
        └── 面试表现 (PostgreSQL面试记录)
    → 大模型汇总生成周报内容
    → 生成 PDF → 上传 MinIO
    → 保存记录到 PostgreSQL (weekly_reports 表)
    → 释放分布式锁
```

**周报内容结构**：

- 本周学习/成长概览
- 日记关键事件回顾
- 知识探索热点
- 面试表现总结
- 下周建议

**前端展示**：

- 历史周报列表（按周倒序）
- 周报详情预览 + PDF 下载

---

### 3.5 计划生成

**定位**：用户输入目标，AI 拆解为可执行步骤并导出。

**流程**：

```
用户输入目标（如"30天学完Java基础"）
    → 大模型拆解为可执行步骤
    → 生成结构化计划（阶段 → 任务 → 时间节点 → 学习资源建议）
    → 生成 PDF → 上传 MinIO → 返回下载链接
    → 异步摘要存入 PgVector（长期记忆）
```

**计划结构示例**：

```
目标：30天学完Java基础

阶段一：基础语法 (Day 1-7)
  ├── Day 1-2: 变量、数据类型、运算符
  ├── Day 3-4: 流程控制（if/for/while）
  └── Day 5-7: 数组、方法、基础练习

阶段二：面向对象 (Day 8-15)
  ├── Day 8-10: 类与对象、封装
  ├── Day 11-13: 继承、多态
  └── Day 14-15: 抽象类、接口

阶段三：常用API与进阶 (Day 16-23)
  └── ...

阶段四：项目实战 (Day 24-30)
  └── ...
```

---

## 四、记忆体系（双层记忆）

| 记忆类型 | 存储位置 | 内容 | 特点 |
|----------|----------|------|------|
| 短期记忆 | Redis | 最近 10 轮对话历史 | 快速读写，过期自动清除（TTL 24h） |
| 长期记忆 | PgVector | 日记摘要、面试总结、知识问答摘要、计划摘要 | 持久化，语义检索，跨会话可用 |

**记忆存取时机**：

| 触发场景 | 动作 |
|----------|------|
| 用户写日记 | → 向量化存入 PgVector |
| 面试结束 | → 异步摘要存入 PgVector |
| 知识问答结束 | → 摘要存入 PgVector |
| 计划生成 | → 摘要存入 PgVector |
| 用户回顾/周报生成 | → 从 PgVector 语义检索相关记忆 |

---

## 五、数据库设计

### 5.1 PostgreSQL 表结构

**用户表 (users)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password_hash | VARCHAR(255) | 密码哈希 |
| email | VARCHAR(100) | 邮箱 |
| avatar_url | VARCHAR(500) | 头像 URL |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**日记表 (diaries)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| user_id | BIGINT FK | 用户 ID |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 正文 |
| mood_tag | VARCHAR(20) | 心情标签 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**面试记录表 (interview_records)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| user_id | BIGINT FK | 用户 ID |
| industry | VARCHAR(50) | 行业 |
| skill_tags | TEXT[] | 技能标签 |
| total_questions | INT | 题目总数 |
| avg_score | DECIMAL(5,2) | 平均分 |
| report_url | VARCHAR(500) | 报告 PDF MinIO 链接 |
| completed_at | TIMESTAMP | 完成时间 |

**面试题目详情表 (interview_question_details)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| record_id | BIGINT FK | 面试记录 ID |
| question | TEXT | 题目 |
| user_answer | TEXT | 用户回答 |
| ai_comment | TEXT | AI 点评 |
| score | DECIMAL(5,2) | 得分 (1-10) |
| sequence_num | INT | 题号 |

**周报表 (weekly_reports)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| user_id | BIGINT FK | 用户 ID |
| week_start | DATE | 周起始日 |
| week_end | DATE | 周结束日 |
| summary | TEXT | 周报摘要 |
| full_content | TEXT | 完整内容 |
| report_url | VARCHAR(500) | PDF MinIO 链接 |
| generated_at | TIMESTAMP | 生成时间 |

**计划表 (study_plans)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| user_id | BIGINT FK | 用户 ID |
| goal | VARCHAR(500) | 目标描述 |
| plan_content | TEXT | 计划详细内容 |
| plan_url | VARCHAR(500) | PDF MinIO 链接 |
| created_at | TIMESTAMP | 创建时间 |

### 5.2 PgVector 向量表

**长期记忆向量表 (long_term_memories)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| user_id | BIGINT | 用户 ID |
| content | TEXT | 记忆内容 |
| embedding | vector(1024) | text-embedding-v3 向量 |
| source_type | VARCHAR(30) | 来源：diary / interview / knowledge / plan |
| source_id | BIGINT | 来源记录 ID |
| created_at | TIMESTAMP | 创建时间 |

**面试题库向量表 (interview_question_bank)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| industry | VARCHAR(50) | 行业 |
| skill | VARCHAR(50) | 技能 |
| question | TEXT | 题目 |
| embedding | vector(1024) | 题目向量 |
| difficulty | VARCHAR(10) | 难度：easy/medium/hard |

### 5.3 Redis 数据结构

| Key 模式 | 类型 | 用途 | TTL |
|----------|------|------|------|
| `chat:context:{userId}` | List | 最近 10 轮对话上下文 | 24h |
| `interview:session:{sessionId}` | Hash | 面试会话状态 | 2h |
| `jwt:token:{userId}` | String | JWT Token 缓存 | 7d |
| `distributed:lock:weekly-report` | String | 周报生成分布式锁 | 5min |
| `rate_limit:api:{userId}` | String | API 调用频率限制 | 1min |

---

## 六、前端设计

### 6.1 页面布局

```
┌─────────────────────────────────────────────────────┐
│                    Trace 顶部栏                       │
├──────────┬──────────────────────────────────────────┤
│          │                                          │
│  知识科普  │                                          │
│          │        主内容区 (Router View)              │
│  模拟面试  │                                          │
│          │    - 知识科普：聊天界面                     │
│  日记本    │    - 模拟面试：配置页 / 面试页 / 历史页     │
│          │    - 日记本：列表 / 编辑页                  │
│  成长周报  │    - 成长周报：周报列表 + 详情             │
│          │    - 我的计划：目标输入 / 历史计划           │
│  我的计划  │                                          │
│          │                                          │
├──────────┴──────────────────────────────────────────┤
│                   底部状态栏                          │
└─────────────────────────────────────────────────────┘
```

### 6.2 路由设计

| 路径 | 组件 | 说明 |
|------|------|------|
| `/knowledge` | KnowledgeChat.vue | 知识科普聊天页 |
| `/interview` | InterviewHome.vue | 面试首页（配置 + 历史入口） |
| `/interview/start` | InterviewSession.vue | 面试进行中页面 |
| `/interview/history` | InterviewHistory.vue | 面试历史列表 |
| `/interview/history/:id` | InterviewDetail.vue | 面试详情 |
| `/diary` | DiaryList.vue | 日记列表 |
| `/diary/write` | DiaryWrite.vue | 写日记 |
| `/diary/:id` | DiaryDetail.vue | 日记详情 |
| `/weekly-report` | WeeklyReport.vue | 周报列表 + 详情 |
| `/plan` | StudyPlan.vue | 计划生成 + 历史 |

### 6.3 关键组件

**左侧导航栏**：使用 Element Plus `el-menu` 垂直模式，配合 `vue-router` 激活状态高亮。

**聊天组件**（知识科普 / 部分计划交互）：
- 消息列表：`el-scrollbar` + 自动滚动
- 消息气泡：用户右侧蓝色 / AI 左侧灰色
- AI 消息使用 `markdown-it` 渲染
- 代码块语法高亮
- 流式输出：SSE 逐字渲染

**面试组件**：
- 配置页：`el-select` 行业/技能 + `el-input-number` 题目数量
- 面试页：题目卡片 → `el-input` 回答区 → 提交 → 评分展示
- 进度条显示当前题号/总题数

**日记组件**：
- 编辑器：`el-input` + `el-input type="textarea"`
- 心情标签：`el-tag` 选择器
- 历史列表：`el-timeline` 时间线组件

---

## 七、API 接口设计

### 7.1 知识科普

```
POST /api/knowledge/chat
  Request: { "message": "什么是虚拟线程？", "domain": "IT" }
  Response: SSE 流式输出
    event: message
    data: {"content": "虚拟线程是...", "references": ["https://..."]}
    
    event: disclaimer
    data: {"content": "⚠️ 以上内容基于互联网搜索结果整理..."}
```

### 7.2 模拟面试

```
POST /api/interview/start
  Request: { "industry": "IT", "skills": ["Java","Spring"], "questionCount": 5 }

POST /api/interview/answer
  Request: { "sessionId": "xxx", "answer": "我认为..." }
  Response: { "score": 8.5, "comment": "...", "nextQuestion": "...", "isLast": false }

GET  /api/interview/records
GET  /api/interview/records/{id}/details
GET  /api/interview/report/{id}/download
```

### 7.3 日记本

```
POST   /api/diary            # 新建日记
GET    /api/diary             # 分页列表
GET    /api/diary/{id}       # 查看详情
PUT    /api/diary/{id}       # 编辑
DELETE /api/diary/{id}       # 删除
```

### 7.4 成长周报

```
GET /api/weekly-report          # 历史周报列表
GET /api/weekly-report/{id}    # 周报详情
GET /api/weekly-report/{id}/download  # 下载 PDF
```

### 7.5 计划

```
POST /api/plan/generate         # 生成计划
  Request: { "goal": "30天学完Java基础" }
  Response: { "planId": 123, "planUrl": "http://minio/..." }

GET  /api/plan                  # 历史计划列表
GET  /api/plan/{id}/download    # 下载 PDF
```

---

## 八、关键设计决策

| 决策 | 说明 |
|------|------|
| 单体架构 + 多实例支持 | 单体应用降低复杂度，JWT 无状态 + 共享 Redis/PgVector 支持水平扩展 |
| PgVector 统一存储 | PostgreSQL 同时处理业务表与向量检索，不引入 Elasticsearch，降低运维成本 |
| MCP 强制联网搜索 | 知识科普不走模型内部知识，每次必调 Brave Search，确保信息时效性 |
| 兜底免责声明 | 所有知识回复末尾固定免责提示，建立用户对 AI 的正确预期 |
| @Scheduled 替代 XXL-JOB | 简化部署，单体架构下 Spring 原生定时任务足够 |
| 虚拟线程 | Java 21 虚拟线程优化大模型调用、向量检索、文件上传等 I/O 密集操作 |
| RabbitMQ 可选异步 | 前期可同步处理 PDF 生成和记忆持久化，后期按需引入 RabbitMQ |

---

## 九、项目结构

```
trace/
├── pom.xml                          # 父 POM (Spring Boot 3.x + Spring AI)
├── src/main/java/com/trace/
│   ├── TraceApplication.java        # 启动类
│   ├── config/
│   │   ├── AiConfig.java            # Spring AI 配置（百炼）
│   │   ├── PgVectorConfig.java      # PgVector 配置
│   │   ├── RedisConfig.java         # Redis 配置
│   │   ├── MinioConfig.java         # MinIO 配置
│   │   └── McpConfig.java           # MCP Brave Search 配置
│   ├── controller/
│   │   ├── KnowledgeController.java # 知识科普
│   │   ├── InterviewController.java # 模拟面试
│   │   ├── DiaryController.java     # 日记本
│   │   ├── WeeklyReportController.java # 成长周报
│   │   └── PlanController.java      # 计划
│   ├── service/
│   │   ├── KnowledgeService.java
│   │   ├── InterviewService.java
│   │   ├── DiaryService.java
│   │   ├── WeeklyReportService.java
│   │   ├── PlanService.java
│   │   ├── MemoryService.java       # 记忆存取服务
│   │   └── PdfService.java          # PDF 生成服务
│   ├── repository/
│   │   ├── DiaryRepository.java
│   │   ├── InterviewRecordRepository.java
│   │   ├── WeeklyReportRepository.java
│   │   └── PlanRepository.java
│   ├── vector/
│   │   ├── LongTermMemoryRepository.java  # PgVector 记忆操作
│   │   └── QuestionBankRepository.java    # PgVector 题库操作
│   ├── entity/
│   │   ├── User.java
│   │   ├── Diary.java
│   │   ├── InterviewRecord.java
│   │   ├── InterviewQuestionDetail.java
│   │   ├── WeeklyReport.java
│   │   ├── StudyPlan.java
│   │   └── LongTermMemory.java
│   ├── dto/
│   │   ├── ChatRequest.java
│   │   ├── InterviewStartRequest.java
│   │   └── DiaryRequest.java
│   ├── scheduler/
│   │   └── WeeklyReportScheduler.java  # @Scheduled 周报任务
│   ├── security/
│   │   ├── JwtUtil.java
│   │   └── SecurityConfig.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── db/
│       └── migration/                  # 数据库迁移脚本
├── frontend/                           # Vue 3 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/index.ts
│       ├── stores/                     # Pinia
│       ├── views/
│       │   ├── KnowledgeChat.vue
│       │   ├── interview/
│       │   │   ├── InterviewHome.vue
│       │   │   ├── InterviewSession.vue
│       │   │   ├── InterviewHistory.vue
│       │   │   └── InterviewDetail.vue
│       │   ├── diary/
│       │   │   ├── DiaryList.vue
│       │   │   ├── DiaryWrite.vue
│       │   │   └── DiaryDetail.vue
│       │   ├── WeeklyReport.vue
│       │   └── StudyPlan.vue
│       ├── components/
│       │   ├── ChatMessage.vue         # 聊天消息气泡
│       │   ├── SideNav.vue             # 左侧导航
│       │   └── MarkdownRenderer.vue    # Markdown 渲染
│       ├── api/
│       │   └── index.ts                # Axios 封装
│       └── styles/
│           └── main.scss
└── docker-compose.yml                  # PostgreSQL + Redis + MinIO
```

---

## 十、部署配置

### 10.1 Docker Compose（中间件）

```yaml
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: trace-postgres
    environment:
      POSTGRES_DB: trace
      POSTGRES_USER: trace
      POSTGRES_PASSWORD: trace123
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: trace-redis
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    container_name: trace-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - miniodata:/data

volumes:
  pgdata:
  miniodata:
```

### 10.2 应用配置

```yaml
spring:
  application:
    name: trace
  threads:
    virtual:
      enabled: true
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
      embedding:
        options:
          model: text-embedding-v3
  datasource:
    url: jdbc:postgresql://localhost:5432/trace
    username: trace
    password: trace123
  data:
    redis:
      host: localhost
      port: 6379

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: trace-reports

mcp:
  brave-search:
    enabled: true
    api-key: ${BRAVE_SEARCH_API_KEY}
```

---

## 十一、开发计划

| 阶段 | 周期 | 任务 |
|------|------|------|
| 一、基础设施 | Week 1 | 项目骨架、Docker 中间件部署、Spring AI 对接百炼、PgVector 建表 |
| 二、核心能力 | Week 2 | 用户认证 (JWT)、双层记忆体系、PDF 生成服务、Redisson 分布式锁 |
| 三、知识科普 | Week 3 | MCP Brave Search 集成、知识问答 API、免责声明、流式输出 |
| 四、日记本 | Week 3-4 | CRUD + 双写机制、PgVector 日记向量化 |
| 五、模拟面试 | Week 4-5 | 配置页、回合制面试、评分引擎、报告生成、历史记录 |
| 六、计划 + 周报 | Week 5-6 | 目标拆解、@Scheduled 周报、PDF 导出 |
| 七、前端开发 | Week 4-7 | Vue 3 脚手架、左侧导航布局、各页面组件开发、SSE 流式渲染 |
| 八、联调测试 | Week 8 | 全流程联调、多实例测试、性能优化 |

---

## 十二、风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 大模型 API 延迟 | 对话卡顿 | SSE 流式输出、合理超时、重试机制 |
| Brave Search 不可用 | 知识科普无法联网 | 降级为模型内部知识 + 明确标注 |
| PgVector 检索不精准 | 记忆召回差 | 调优相似度阈值、混合关键词检索 |
| PDF 生成耗时长 | 用户体验差 | 异步生成 + 进度提示 |
| 多实例 @Scheduled 重复 | 周报重复生成 | Redisson 分布式锁 |

---

## 十三、总结

Trace 项目与 Odyssey 的核心差异在于**定位**：Odyssey 是"有记忆的 AI 朋友"，Trace 是"记录员 + 知识库 + 教练"。Trace 强调客观记录与专业指导，知识科普强制联网搜索并附带免责声明，模拟面试提供结构化评分，功能更加务实和专业导向。技术架构上采用单体应用简化部署复杂度，同时通过 JWT + Redis + Redisson 保留多实例扩展能力。