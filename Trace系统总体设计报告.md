# Trace 系统总体设计报告

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | Trace — AI 驱动的个人成长助手 |
| 文档类型 | 总体设计报告 |
| 版本 | v1.0 |

---

## 目录

- [第 1 章 引言](#第-1-章-引言)
- [第 2 章 系统设计目标与原则](#第-2-章-系统设计目标与原则)
- [第 3 章 系统总体架构设计](#第-3-章-系统总体架构设计)
- [第 4 章 系统功能结构设计](#第-4-章-系统功能结构设计)
- [第 5 章 服务划分设计](#第-5-章-服务划分设计)
- [第 6 章 数据库设计](#第-6-章-数据库设计)
- [第 7 章 接口设计](#第-7-章-接口设计)
- [第 8 章 系统详细设计](#第-8-章-系统详细设计)
- [第 9 章 安全设计](#第-9-章-安全设计)
- [第 10 章 异常处理与日志设计](#第-10-章-异常处理与日志设计)
- [第 11 章 非功能需求设计](#第-11-章-非功能需求设计)
- [第 12 章 部署方案](#第-12-章-部署方案)
- [第 13 章 关键技术实现](#第-13-章-关键技术实现)
- [第 14 章 风险分析与应对措施](#第-14-章-风险分析与应对措施)
- [第 15 章 总结](#第-15-章-总结)

---

## 第 1 章 引言

### 1.1 编写目的

本文档旨在对 **Trace 系统** 进行全面的总体设计说明。文档面向开发团队、项目评审人员及后续维护人员，阐明系统的架构设计、模块划分、数据库设计、接口规范、安全策略和关键技术方案，为详细设计和编码实现提供依据。

### 1.2 项目背景

Trace 是一款 **AI 驱动的个人成长助手** 系统，面向希望系统化管理个人成长的用户，提供以下核心能力：

- **AI 知识问答**：基于知识库 + 联网搜索的智能问答，支持 SSE 流式输出
- **模拟面试**：AI 模拟面试官进行结构化面试，自动评分并生成评价报告
- **成长日记**：记录每日心情与成长感悟
- **周报生成**：每周自动汇总学习与成长数据，生成 PDF 周报
- **学习计划**：AI 辅助制定个性化学习计划，支持打卡追踪
- **知识库管理**：支持用户上传文档（PDF/TXT/DOCX），向量化存储与语义检索
- **长期记忆**：AI 自动从对话中提取用户偏好和重要信息，持久化存储
- **成长仪表盘**：多维度可视化展示成长轨迹（热力图、趋势图、任务分布等）

### 1.3 参考资料

| 资料名称 | 说明 |
|----------|------|
| Java 21 官方文档 | 开发语言规范 |
| Spring Boot 3.4.6 参考文档 | 后端框架 |
| Spring AI 1.1.2 参考文档 | AI 集成框架 |
| MyBatis-Plus 3.5.9 文档 | ORM 框架 |
| PostgreSQL 18 + PgVector 文档 | 数据库 + 向量扩展 |
| RabbitMQ 3.x 文档 | 消息队列 |
| Redis 7.x 文档 | 缓存与分布式锁 |
| MinIO 文档 | 对象存储 |
| Vue 3 + Element Plus 文档 | 前端框架 |

### 1.4 术语与缩略语

| 术语 | 说明 |
|------|------|
| SSE (Server-Sent Events) | 服务器推送事件，用于流式输出 AI 回复 |
| MCP (Model Context Protocol) | 模型上下文协议，用于注册外部工具（如联网搜索） |
| PgVector | PostgreSQL 向量扩展，支持向量存储与余弦相似度检索 |
| Embedding | 文本向量化表示，用于语义相似度计算 |
| JWT (JSON Web Token) | 无状态用户认证令牌 |
| SETNX | Redis 原子命令，用于分布式锁和幂等性控制 |
| Agent | 智能体，根据用户意图路由到不同的 AI 处理逻辑 |
| 混合检索 | 向量语义检索 + 全文关键词检索的加权融合 |
| 长期记忆 | AI 自动提取并持久化的用户偏好与关键信息 |

---

## 第 2 章 系统设计目标与原则

### 2.1 系统设计目标

| 目标 | 说明 |
|------|------|
| **智能化** | 利用大语言模型（通义千问 qwen-plus）提供知识问答、面试模拟、计划生成、记忆提取等 AI 能力 |
| **流式交互** | 所有 AI 对话采用 SSE 流式输出，用户可实时看到生成过程，支持随时取消 |
| **持久化记忆** | 三层记忆体系（短期 → 聊天历史 → 长期），AI 从对话中自动提取用户偏好 |
| **异步解耦** | 耗时任务（面试评价、计划生成、记忆提取）通过 RabbitMQ 异步处理，不阻塞用户 |
| **可扩展性** | Agent 插件式架构，新增功能只需添加新 Agent 并注册关键词 |

### 2.2 设计原则

| 原则 | 说明 |
|------|------|
| **高内聚低耦合** | 按业务领域划分模块，Agent 之间独立，通过 AgentRouter 统一路由 |
| **单一职责** | 每个 Service 只负责一个领域；每个 Agent 只处理一类用户意图 |
| **接口隔离** | 前后端通过 RESTful API + SSE 通信，接口统一返回格式（`ApiResponse`） |
| **开闭原则** | 新增 Agent 无需修改 AgentRouter，只需实现 Agent 接口并注册 |
| **依赖倒置** | Service 层依赖接口而非实现，通过 Spring DI 注入 |
| **安全优先** | JWT 认证 + BCrypt 密码加密 + Spring Security 统一鉴权 |
| **数据最小化** | 聊天历史限 500 条/用户，短期 Redis 记忆限 20 条，长期记忆限 30 条/用户 |

### 2.3 系统设计约束

| 约束项 | 要求 |
|--------|------|
| 开发语言 | Java 21（后端）、TypeScript + Vue 3（前端） |
| 数据库 | PostgreSQL 18 + PgVector 扩展 |
| 缓存 | Redis 7.x |
| 消息队列 | RabbitMQ 3.x |
| 对象存储 | MinIO |
| AI 模型 | 阿里云百炼 DashScope（qwen-plus / text-embedding-v3） |
| 认证方式 | JWT 无状态认证，Token 有效期 7 天 |
| 文件上传 | 最大 500MB |
| 并发支持 | Spring Virtual Threads 虚拟线程 |

---

## 第 3 章 系统总体架构设计

### 3.1 系统架构概述

Trace 系统采用 **前后端分离的单体架构**，后端基于 Spring Boot 3.4.6 构建，以 **Agent 插件式路由** 为核心设计模式，通过 RabbitMQ 实现耗时任务的异步处理。

### 3.2 系统逻辑架构

系统从逻辑上分为五层：

```
┌──────────────────────────────────────────────────┐
│                   接入层                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │ Vue3 前端 │  │ MCP 工具 │  │ 外部 API 调用 │   │
│  └──────────┘  └──────────┘  └──────────────┘   │
├──────────────────────────────────────────────────┤
│                   接口层                          │
│  ┌──────────────────────────────────────────┐   │
│  │  REST APIs + SSE 流 + JWT 认证 过滤器     │   │
│  └──────────────────────────────────────────┘   │
├──────────────────────────────────────────────────┤
│                  业务层                           │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │
│  │Agent路由│ │知识问答│ │面试服务│ │计划服务│   │
│  └────────┘ └────────┘ └────────┘ └────────┘   │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │
│  │日记服务 │ │周报服务│ │记忆服务│ │知识库  │   │
│  └────────┘ └────────┘ └────────┘ └────────┘   │
├──────────────────────────────────────────────────┤
│                   中间件层                        │
│  ┌──────┐ ┌──────┐ ┌──────────┐ ┌────────┐     │
│  │Redis │ │MQ异步│ │PgVector │ │ MinIO  │     │
│  │缓存锁│ │消费者│ │向量检索  │ │对象存储│     │
│  └──────┘ └──────┘ └──────────┘ └────────┘     │
├──────────────────────────────────────────────────┤
│                   基础设施层                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐    │
│  │PostgreSQL│ │ RabbitMQ │ │ 阿里云百炼AI  │    │
│  └──────────┘ └──────────┘ └──────────────┘    │
└──────────────────────────────────────────────────┘
```

### 3.3 系统物理架构

系统部署在单台服务器上，各组件通过 Docker Compose 编排：

| 组件 | 端口 | 说明 |
|------|------|------|
| Trace App (Spring Boot) | 8080 | 主应用，Java 21 虚拟线程 |
| PostgreSQL 18 + PgVector | 5432 | 关系型数据库 + 向量存储 |
| Redis 7 | 6379 | 缓存 + 分布式锁 + 短期记忆 |
| RabbitMQ 3 | 5672 / 15672 | 消息队列 + 管理界面 |
| MinIO | 9000 / 9001 | 对象存储 + 管理界面 |
| Nginx（可选） | 80/443 | 前端静态资源 + API 反向代理 |

### 3.4 系统技术架构

| 层次 | 技术选型 | 说明 |
|------|---------|------|
| **前端** | Vue 3 + TypeScript + Element Plus + Pinia | SPA 单页应用，Vite 构建，Markdown 渲染，语音输入 |
| **后端框架** | Spring Boot 3.4.6 + Spring AI 1.1.2 | Java 21 虚拟线程，响应式 SSE 流 |
| **ORM** | MyBatis-Plus 3.5.9 | XML 自定义 SQL + BaseMapper 自动 CRUD |
| **数据库** | PostgreSQL 18 + PgVector | 关系数据 + 向量余弦相似度检索 |
| **数据库迁移** | Flyway | 版本化 DDL 管理 |
| **缓存 / 锁** | Redis 7 + Redisson | 短期记忆、取消信号、分布式锁、SETNX 幂等 |
| **消息队列** | RabbitMQ 3 | 计划生成、面试评价、记忆提取的异步消费 |
| **对象存储** | MinIO | PDF 报告存储（周报、学习计划、面试报告） |
| **AI 模型** | 阿里云百炼 DashScope | qwen-plus（对话）、text-embedding-v3（向量） |
| **PDF 生成** | Flying Saucer + Thymeleaf | HTML 模板 → PDF，支持中文 |
| **认证** | Spring Security + JWT (JJWT 0.12.6) | 无状态认证，BCrypt 密码加密 |
| **MCP 外部工具** | spring-ai-starter-mcp-client + Tavily | 联网搜索能力（stdio 模式） |

### 3.5 系统架构图

```
                 ┌──────────┐
                 │  Nginx   │  (可选)
                 └────┬─────┘
                      │
          ┌───────────┴───────────┐
          │                       │
    ┌─────┴─────┐          ┌─────┴─────┐
    │  Vue3 前端 │          │ Trace App │
    │  :5173    │◄─REST/SSE─►│  :8080    │
    └───────────┘          └─────┬─────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                  │
        ┌─────┴─────┐    ┌─────┴─────┐    ┌──────┴──────┐
        │PostgreSQL │    │  Redis 7  │    │  RabbitMQ   │
        │+PgVector  │    │  :6379    │    │  :5672      │
        │  :5432    │    └───────────┘    └──────┬──────┘
        └───────────┘                            │
                                      ┌──────────┴──────────┐
                                      │          │           │
                                 PlanConsumer  InterviewConsumer  MemoryConsumer
                                      │          │           │
                                      ▼          ▼           ▼
                                   计划生成    面试评价     记忆提取

              ┌──────────┐     ┌──────────────┐
              │  MinIO   │     │ 阿里云百炼 AI │
              │  :9000   │     │  DashScope   │
              └──────────┘     └──────────────┘
```

---

## 第 4 章 系统功能结构设计

### 4.1 系统功能模块划分

```
Trace 系统
├── 1. 用户认证模块 (Auth)
│   ├── 用户注册
│   ├── 用户登录（JWT）
│   ├── 头像上传（Base64）
│   └── 个人信息管理
│
├── 2. AI 知识问答模块 (Knowledge)
│   ├── 意图分类（DIRECT / SEARCH / WEB_SEARCH）
│   ├── 查询改写 (QueryRewriteAgent)
│   ├── 知识库语义检索 (PgVector 混合检索)
│   ├── MCP 联网搜索 (Tavily)
│   ├── SSE 流式输出
│   └── 取消机制（Redis 信号）
│
├── 3. 模拟面试模块 (Interview)
│   ├── 面试启动（行业选择 + 简历上传）
│   ├── 问题生成与流式推送
│   ├── 答案提交与即时反馈
│   ├── 异步评价生成 (RabbitMQ)
│   ├── SSE 评价结果推送
│   ├── 面试历史记录
│   └── PDF 报告下载
│
├── 4. 成长日记模块 (Diary)
│   ├── 日记创建
│   ├── 日记列表（分页）
│   ├── 日记详情
│   ├── 日记编辑
│   └── 日记删除
│
├── 5. 周报生成模块 (WeeklyReport)
│   ├── 手动生成周报
│   ├── 定时自动生成（每周日 02:00）
│   ├── 周报列表
│   ├── 周报详情
│   └── PDF 下载
│
├── 6. 学习计划模块 (Plan)
│   ├── AI 异步生成计划 (RabbitMQ)
│   ├── 手动创建计划
│   ├── SSE 生成进度推送
│   ├── 计划编辑与删除
│   ├── 每日打卡
│   ├── 打卡进度查询
│   └── PDF 下载
│
├── 7. 知识库管理模块 (KnowledgeBase)
│   ├── 文件上传（PDF/TXT/DOCX）
│   ├── 向量化存储
│   ├── 语义检索
│   ├── 混合检索（向量 + 全文）
│   ├── 文件重命名 / 编辑 / 删除
│   ├── 面试题库导入
│   └── 知识库清空
│
├── 8. 成长仪表盘模块 (Dashboard)
│   ├── 综合数据面板
│   ├── 成长评分算法
│   ├── GitHub 风格热力图
│   ├── 趋势图（30 天）
│   ├── 打卡分布统计
│   ├── 面试得分趋势
│   ├── 成长里程碑管理
│   └── 活跃计划 / 最近记忆展示
│
├── 9. 长期记忆模块 (Memory)
│   ├── 短期记忆（Redis ≤20 条）
│   ├── 聊天历史持久化（PG ≤500 条）
│   ├── AI 自动记忆提取 (Agent → MQ)
│   ├── 向量去重合并
│   ├── 相似记忆检索
│   └── 记忆淘汰（≤30 条 / 用户）
│
└── 10. 系统基础设施
    ├── JWT 认证过滤器
    ├── 全局异常处理
    ├── 统一响应格式 (ApiResponse)
    ├── Redis 分布式锁
    └── Flyway 数据库迁移
```

### 4.2 功能模块结构图

```
                          ┌─────────────┐
                          │  AgentRouter │
                          └──────┬──────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                       │
    ┌─────┴─────┐         ┌─────┴─────┐          ┌─────┴─────┐
    │DiaryAgent │         │Interview  │          │ PlanAgent │
    │  日记     │         │  Agent    │          │  学习计划  │
    └───────────┘         │  面试     │          └───────────┘
                          └───────────┘
          ┌──────────────────────┼──────────────────────┐
          │                      │                       │
    ┌─────┴─────┐         ┌─────┴─────┐          ┌─────┴─────┐
    │WeeklyRep  │         │Knowledge  │          │Knowledge  │
    │ortAgent   │         │  Agent    │◄─────────│BaseService│
    │  周报     │         │ (默认兜底) │  知识库   │  向量检索  │
    └───────────┘         └─────┬─────┘          └───────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │            │
               DIRECT    WEB_SEARCH     SEARCH
              直接回答    联网搜索      知识库检索
                           (MCP)       (混合检索)
```

### 4.3 主要功能模块说明

| 模块 | 职责 | 输入 | 输出 |
|------|------|------|------|
| **Auth** | 用户注册、登录、个人信息管理 | 用户名、密码、头像 | JWT Token、用户信息 |
| **AgentRouter** | 关键词匹配路由到对应 Agent | 用户原始输入 | 路由到的 Agent |
| **KnowledgeAgent** | 意图分类 + 检索增强 + AI 综合回答 | 改写后的查询 | SSE 流式 Markdown 回答 |
| **InterviewAgent** | 模拟面试对话 | 面试上下文 + 用户回答 | AI 面试官提问 / 评价 |
| **DiaryAgent** | 日记相关对话 | 日记内容 / 查询 | AI 日记分析 / 回顾 |
| **PlanAgent** | 学习计划对话 | 学习目标 | AI 学习建议 |
| **WeeklyReportAgent** | 周报对话 | 周报查询 | AI 周报解读 |
| **Dashboard** | 多源数据聚合 + 评分计算 | 用户 ID | 仪表盘数据（评分/热力图/趋势） |
| **MemoryExtractAgent** | 从对话文本提取长期记忆 | 聊天文本 | 记忆条目 + 向量 |
| **PDF Service** | Markdown → HTML → PDF | Markdown + 标题 | PDF 文件 URL |
| **KnowledgeBase** | 文件向量化 + 混合检索 | 文件 / 查询文本 | 检索结果 |

### 4.4 模块之间的关系

| 源模块 | 目标模块 | 关系类型 | 说明 |
|--------|---------|---------|------|
| AgentRouter | 各 Agent | 策略路由 | 按关键词匹配分发 |
| KnowledgeAgent | MemoryService | 数据依赖 | 读取长期 / 短期记忆构建上下文 |
| KnowledgeAgent | SearchRouterService | 调用 | 知识库混合检索 |
| KnowledgeAgent | RabbitMQ | 异步消息 | 触发记忆提取 |
| KnowledgeAgent | MCP Client | 工具注册 | WEB_SEARCH 时注册 Tavily 工具 |
| InterviewController | RabbitMQ | 异步消息 | 面试完成后触发评价生成 |
| PlanController | RabbitMQ | 异步消息 | 计划生成请求入队 |
| DashboardController | 多个 Service | 数据聚合 | 聚合打卡/面试/计划/日记/记忆 |
| WeeklyReportScheduler | WeeklyReportService | 定时调用 | 每周日触发周报生成 |
| MemoryExtractConsumer | MemoryExtractAgent | 调用 | 消费消息后执行提取 |
| PlanGenerationConsumer | PlanAgent + PdfService | 调用 | AI 生成 + PDF 输出 |
| InterviewEvaluationConsumer | 评价逻辑 + PdfService | 调用 | AI 评价 + 报告生成 |

---

## 第 5 章 服务划分设计

### 5.1 服务划分原则

Trace 当前采用 **单体架构**，内部按业务领域进行模块化划分。服务划分遵循以下原则：

- **按业务边界**：每个模块对应一个独立的业务领域（Auth、Knowledge、Interview、Diary 等）
- **按数据边界**：每个模块拥有自己的数据库表，通过 Service 接口隔离
- **按职责边界**：Agent 负责对话逻辑，Service 负责业务逻辑，Consumer 负责异步任务

### 5.2 服务清单

| 服务名称 | 主要职责 | 对应数据表 | 通信方式 |
|----------|---------|-----------|---------|
| **Auth 服务** | 用户注册、登录、JWT 签发 | `users` | REST API |
| **Knowledge 服务** | AI 知识问答、意图路由 | `chat_history`, `knowledge_bases` | SSE 流式 |
| **Interview 服务** | 模拟面试、评价生成 | `interview_records` | SSE + RabbitMQ |
| **Diary 服务** | 日记 CRUD | `diaries` | REST API |
| **Plan 服务** | 学习计划管理、打卡 | `study_plans`, `daily_check_ins` | SSE + RabbitMQ |
| **WeeklyReport 服务** | 周报生成与查询 | `weekly_reports` | REST API |
| **KnowledgeBase 服务** | 知识库文件管理、向量检索 | `knowledge_bases`, `vector_store` | REST API |
| **Dashboard 服务** | 数据聚合、评分计算、热力图 | 多表聚合 | REST API |
| **Memory 服务** | 三层记忆管理 | `chat_history`, `long_term_memories` | 同步 + RabbitMQ |
| **PDF 服务** | Markdown → PDF 生成 | — | 内部调用 |

### 5.3 核心服务说明

#### Auth 服务

- **职责**：用户注册（BCrypt 加密）、登录验证、JWT Token 签发（7 天有效期）
- **主要接口**：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/profile`
- **安全机制**：JWT + Spring Security Filter Chain，BCrypt 密码哈希

#### Knowledge 服务（核心）

- **职责**：作为系统默认 Agent，处理所有未被其他 Agent 匹配的用户查询
- **处理流程**：
  1. QueryRewriteAgent 改写短查询
  2. IntentType 分类（DIRECT / SEARCH / WEB_SEARCH）
  3. SEARCH 意图 → SearchRouterService 混合检索
  4. WEB_SEARCH 意图 → 注册 MCP Tavily 工具 → LLM 自主调用搜索
  5. DIRECT 意图 → 结合上下文的 AI 直接回答
- **流式输出**：SSE（Server-Sent Events），支持 Redis 信号取消
- **主要接口**：`POST /api/knowledge/chat`（SSE Flux）

#### Memory 服务

- **三层记忆体系**：
  1. **短期记忆**（Redis List）：最近 20 条对话，用于快速上下文注入
  2. **聊天历史**（PG `chat_history`）：最多 500 条/用户，支持游标分页
  3. **长期记忆**（PG `long_term_memories`）：AI 提取的关键信息，最多 30 条/用户，含向量
- **记忆提取**：事件驱动，每次对话完成后检查 30 分钟冷却期 → RabbitMQ → MemoryExtractAgent 提取
- **去重策略**：向量余弦相似度 ≥ 0.85 自动合并；无向量时基于文本包含判断

### 5.4 服务调用关系

```
┌───────────────────────────────────────────────────────────┐
│                       AgentRouter                         │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐       │
│  │Diary │ │Interv│ │Plan  │ │WeekRp│ │Knowledge │       │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └────┬─────┘       │
│     │        │        │        │           │              │
│     │    ┌───┴────┐   │   ┌────┴────┐ ┌───┴────┐         │
│     │    │RabbitMQ│   │   │RabbitMQ │ │Search  │         │
│     │    │面试评价│   │   │计划生成 │ │Router  │         │
│     │    └───┬────┘   │   └────┬────┘ └───┬────┘         │
│     │        │        │        │          │              │
│     │   ┌────┴────┐   │   ┌────┴────┐     │              │
│     │   │Interview│   │   │PlanGene-│     │              │
│     │   │Evaluation│  │   │ration   │     │              │
│     │   │Consumer │   │   │Consumer │     │              │
│     │   └─────────┘   │   └─────────┘     │              │
│     │                  │                   │              │
│     └──────────────────┼───────────────────┘              │
│                        │                                  │
│                   ┌────┴────┐                             │
│                   │ Memory  │◄── RabbitMQ 触发             │
│                   │Extract  │                             │
│                   │Consumer │                             │
│                   └─────────┘                             │
└───────────────────────────────────────────────────────────┘
```

### 5.5 服务依赖图

```
Controller 层:
  AuthController ──────► AuthService
  KnowledgeController ─► AgentRouter ─► KnowledgeAgent ─► SearchRouterService
                          │                              └► MemoryService
                          ├──► DiaryAgent                └► RabbitTemplate(MQ)
                          ├──► InterviewAgent
                          ├──► PlanAgent
                          └──► WeeklyReportAgent

Consumer 层:
  PlanGenerationConsumer ──► PlanAgent + PdfService + MinIO
  InterviewEvaluationConsumer ─► PdfService + MinIO
  MemoryExtractConsumer ──► MemoryService + MemoryExtractAgent

Scheduler 层:
  WeeklyReportScheduler ──► WeeklyReportService + Redisson Lock
```

---

## 第 6 章 数据库设计

### 6.1 数据库设计原则

| 原则 | 说明 |
|------|------|
| **规范化** | 遵循 3NF，消除数据冗余 |
| **自增主键** | 所有表使用 BIGINT 自增主键 |
| **统一时间戳** | `created_at` / `updated_at` 使用数据库默认值或自动填充 |
| **逻辑删除** | MyBatis-Plus `@TableLogic` 预留支持 |
| **向量存储** | PgVector 扩展，embedding 字段类型为 `vector` |
| **索引优化** | 高查询频率字段建立索引（`user_id`、`created_at`） |
| **数据清理** | 通过 `MAX_*` 常量限制存储上限，自动淘汰旧数据 |

### 6.2 概念结构设计 (E-R 图)

```
  ┌──────────┐          ┌─────────────────┐
  │   User   │1───────*│   ChatHistory   │
  └────┬─────┘          └─────────────────┘
       │
       │1───────*┌─────────────────┐
       │         │ LongTermMemory  │
       │         └─────────────────┘
       │
       │1───────*┌─────────────────┐
       │         │   Diary         │
       │         └─────────────────┘
       │
       │1───────*┌─────────────────┐
       │         │ InterviewRecord │
       │         └─────────────────┘
       │
       │1───────*┌─────────────────┐
       │         │  StudyPlan      │
       ├─────────└────────┬────────┘
       │                  │1───────*
       │                  ┌─────────────────┐
       │                  │ DailyCheckIn    │
       │                  └─────────────────┘
       │
       │1───────*┌─────────────────┐
       │         │ WeeklyReport    │
       │         └─────────────────┘
       │
       │1───────*┌─────────────────┐
       │         │ KnowledgeBase   │
       │         └─────────────────┘
       │
       │1───────*┌─────────────────┐
       └─────────│ GrowthAnchor    │
                 └─────────────────┘
```

### 6.3 逻辑结构设计

#### 6.3.1 用户表 (`users`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO | 用户 ID |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt 密码哈希 |
| `email` | VARCHAR(100) | — | 邮箱 |
| `avatar_url` | TEXT | — | Base64 编码头像 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 注册时间 |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | 更新时间 |

#### 6.3.2 聊天历史表 (`chat_history`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO | 消息 ID |
| `user_id` | BIGINT | FK, INDEX, NOT NULL | 用户 ID |
| `role` | VARCHAR(10) | NOT NULL | user / ai |
| `content` | TEXT | NOT NULL | 消息内容 |
| `created_at` | TIMESTAMP | DEFAULT NOW(), INDEX | 创建时间 |

约束：每用户最多 **500 条**，超出时删除最旧记录。

#### 6.3.3 长期记忆表 (`long_term_memories`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO | 记忆 ID |
| `user_id` | BIGINT | FK, INDEX, NOT NULL | 用户 ID |
| `content` | TEXT | NOT NULL | 记忆内容 |
| `source_type` | VARCHAR(50) | NOT NULL | 来源类型（chat_extract / weekly_report） |
| `source_id` | BIGINT | — | 来源记录 ID |
| `embedding` | VECTOR | — | 文本向量（PgVector） |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |

约束：每用户最多 **30 条**，超出时淘汰低相关度或最旧记录。

#### 6.3.4 面试记录表 (`interview_records`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO | 记录 ID |
| `user_id` | BIGINT | FK, INDEX, NOT NULL | 用户 ID |
| `industry` | VARCHAR(100) | — | 面试行业 |
| `skill_tags` | TEXT[] | — | 技能标签数组 |
| `total_questions` | INT | — | 题目总数 |
| `avg_score` | DECIMAL(5,1) | — | 平均评分 |
| `report_url` | TEXT | — | 评价报告 PDF URL |
| `ai_analysis` | TEXT | — | AI 评价分析（JSON） |
| `weak_skills` | TEXT[] | — | 薄弱项数组 |
| `completed_at` | TIMESTAMP | — | 完成时间 |

#### 6.3.5 知识库表 (`knowledge_bases`) + 向量存储 (`vector_store`)

`knowledge_bases` 表：

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO | 文档 ID |
| `user_id` | BIGINT | FK, INDEX | 用户 ID（NULL=共享题库） |
| `file_name` | VARCHAR(255) | NOT NULL | 文件名 |
| `file_type` | VARCHAR(10) | NOT NULL | pdf / txt / docx |
| `content` | TEXT | — | 原始文本内容 |
| `knowledge_type` | VARCHAR(20) | NOT NULL | USER / INTERVIEW |
| `chunk_index` | INT | — | 分块索引 |
| `metadata` | JSONB | — | 元数据 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |

`vector_store` 表（PgVector 管理）：

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | UUID | PK | 向量 ID |
| `content` | TEXT | — | 文本内容 |
| `metadata` | JSONB | — | 元数据 |
| `embedding` | VECTOR | INDEX (IVFFlat) | 文本向量 |

#### 6.3.6 其他表

| 表名 | 核心字段 | 说明 |
|------|---------|------|
| `diaries` | `id`, `user_id`, `title`, `content`, `mood_tag`, `created_at` | 成长日记 |
| `study_plans` | `id`, `user_id`, `goal`, `plan_content`, `plan_url`, `total_duration`, `source` | 学习计划 |
| `daily_check_ins` | `id`, `user_id`, `plan_id`, `check_date`, `created_at` | 每日打卡 |
| `weekly_reports` | `id`, `user_id`, `week_start`, `week_end`, `summary`, `full_content`, `report_url` | 周报 |
| `growth_anchors` | `id`, `user_id`, `anchor_date`, `label`, `created_at` | 成长里程碑 |

### 6.4 数据表清单

| 序号 | 表名 | 行数上限 | 说明 |
|------|------|---------|------|
| 1 | `users` | 无 | 用户 |
| 2 | `chat_history` | 500 / 用户 | 聊天历史 |
| 3 | `long_term_memories` | 30 / 用户 | 长期记忆 |
| 4 | `interview_records` | 无 | 面试记录 |
| 5 | `knowledge_bases` | 无 | 知识库文档 |
| 6 | `vector_store` | 无 | PgVector 向量存储 |
| 7 | `diaries` | 无 | 日记 |
| 8 | `study_plans` | 无 | 学习计划 |
| 9 | `daily_check_ins` | 无 | 打卡记录 |
| 10 | `weekly_reports` | 无 | 周报 |
| 11 | `growth_anchors` | 无 | 成长里程碑 |

### 6.5 数据库关系说明

- 所有业务表与 `users` 为 **1:N** 关系（通过 `user_id` 外键）
- `study_plans` 与 `daily_check_ins` 为 **1:N** 关系（通过 `plan_id`）
- `knowledge_bases` 与 `vector_store` 通过 `id` / `metadata` 关联
- `long_term_memories` 与 `chat_history` 通过 `source_id` 可选关联

### 6.6 数据一致性

| 场景 | 策略 |
|------|------|
| **聊天历史超出上限** | `saveChatHistory` 中先插入，再查询计数，超出时 `deleteOldestByUserId` 删除最旧 |
| **长期记忆超出上限** | `saveMemory` 中先插入，再查询计数，超出时淘汰低相关度或最旧记忆 |
| **短期记忆截断** | Redis `LTRIM` 保留最近 20 条 |
| **记忆提取去重** | 向量相似度 ≥ 0.85 自动合并；无向量时基于文本包含判断 |
| **定时周报防重** | Redisson 分布式锁（`distributed:lock:weekly-report`） |
| **记忆提取幂等** | Redis SETNX `chat:memory:extract:lock:{userId}`，TTL 300s |

---

## 第 7 章 接口设计

### 7.1 接口设计原则

| 原则 | 说明 |
|------|------|
| **RESTful 规范** | 资源路径命名，HTTP 方法语义化（GET/POST/PUT/DELETE） |
| **统一响应格式** | `ApiResponse<T>` 包装：`{ code: int, message: string, data: T }` |
| **流式接口** | SSE（`text/event-stream`）用于 AI 回答和面试交互 |
| **异常统一处理** | `@RestControllerAdvice` 全局捕获，返回标准错误格式 |
| **版本管理** | URL 前缀 `/api/`，暂无版本号（后续可扩展 `/api/v1/`） |

### 7.2 外部接口

| 接口 | 说明 |
|------|------|
| **阿里云百炼 DashScope API** | 对话模型 `qwen-plus`、嵌入模型 `text-embedding-v3` |
| **Tavily Search API** | 通过 MCP 协议（stdio 模式）调用的联网搜索引擎 |

### 7.3 内部接口

内部接口指前端调用的 REST API，统一前缀 `/api/`。

### 7.4 核心接口清单

| 接口名称 | 请求方式 | 接口路径 | 功能说明 | 调用方 |
|----------|---------|---------|---------|--------|
| 用户注册 | POST | `/api/auth/register` | 注册新用户 | 前端 |
| 用户登录 | POST | `/api/auth/login` | 登录返回 JWT | 前端 |
| 获取个人信息 | GET | `/api/auth/profile` | 获取当前用户信息 | 前端 |
| AI 知识问答 | POST | `/api/knowledge/chat` | SSE 流式对话 | 前端 |
| 取消 AI 回答 | POST | `/api/knowledge/stop` | 取消正在进行的回答 | 前端 |
| 聊天历史 | GET | `/api/knowledge/history` | 游标分页获取历史 | 前端 |
| 知识库上传 | POST | `/api/knowledge-base/upload` | 上传文档 | 前端 |
| 语义检索 | GET | `/api/knowledge-base/search` | 向量相似度搜索 | 前端 |
| 混合检索 | GET | `/api/knowledge-base/hybrid-search` | 向量 + 全文混合检索 | 前端 |
| 面试启动 | POST | `/api/interview/start` | 启动面试会话 | 前端 |
| 面试流式 | POST | `/api/interview/start-stream` | SSE 流式面试 | 前端 |
| 提交答案 | POST | `/api/interview/answer` | 提交面试答案 | 前端 |
| 面试历史 | GET | `/api/interview/records` | 面试记录列表 | 前端 |
| 面试详情 | GET | `/api/interview/records/{id}` | 面试详情含 AI 评价 | 前端 |
| 评价流式 | GET | `/api/interview/records/{id}/stream` | SSE 评价进度 | 前端 |
| 生成计划 | POST | `/api/plan/generate` | 异步 AI 生成计划 | 前端 |
| 计划流式 | GET | `/api/plan/{id}/stream` | SSE 生成进度 | 前端 |
| 每日打卡 | POST | `/api/checkin` | 打卡 | 前端 |
| 仪表盘 | GET | `/api/dashboard` | 综合数据面板 | 前端 |
| 成长评分 | GET | `/api/dashboard/growth` | 评分 + 热力图 + 趋势 | 前端 |
| 日记 CRUD | POST/GET/PUT/DELETE | `/api/diary` | 日记增删改查 | 前端 |
| 周报列表 | GET | `/api/weekly-report` | 周报分页列表 | 前端 |
| 周报生成 | POST | `/api/weekly-report/generate` | 手动生成周报 | 前端 |
| 周报下载 | GET | `/api/weekly-report/{id}/download` | PDF 下载 | 前端 |
| 面试题库导入 | POST | `/api/knowledge-base/import-bank` | 导入面试题库 | 前端 |

### 7.5 接口安全

| 机制 | 实现 |
|------|------|
| **认证** | JWT Token 放于 `Authorization: Bearer <token>` 请求头 |
| **鉴权** | `JwtAuthenticationFilter` 从 Token 解析用户 ID，设置 `SecurityContext` |
| **公开接口** | `/api/auth/**` 无需认证 |
| **受保护接口** | `/api/**` 全部需要认证 |
| **CORS** | 允许 `localhost:5173/5174/3000` |
| **CSRF** | 无状态 JWT，禁用 CSRF |
| **XSS 防护** | 前端 Vue 模板自动转义 |
| **SQL 注入防护** | MyBatis 参数化查询（`#{ }`） |

---

## 第 8 章 系统详细设计

### 8.1 核心业务流程概述

#### 流程 1：AI 知识问答

```
用户输入 → AgentRouter.keywordMatch
              │
    ┌─────────┴─────────┐
    │ 匹配到特定 Agent  │         │ 未匹配 → KnowledgeAgent (兜底)
    │ Diary/Interview/  │         │
    │ Plan/WeeklyReport │         ├── QueryRewriteAgent.rewrite()
    └────────┬──────────┘         ├── IntentType 分类
             │                    │     ├── DIRECT → LLM 直接回答
             │                    │     ├── SEARCH → SearchRouter.hybridSearch
             │                    │     └── WEB_SEARCH → MCP Tavily 工具注册
             │                    ├── buildContext() 注入记忆
             │                    ├── SSE 流式输出
             │                    ├── saveChatHistory()
             │                    └── triggerMemoryExtractIfNeeded()
             │                              │
             │                    ┌─────────┴──────────┐
             │                    │ 检查冷却期 (30分钟)  │
             │                    └─────────┬──────────┘
             │                              │ 超过冷却期
             │                    ┌─────────┴──────────┐
             │                    │ RabbitMQ → Consumer │
             │                    │ MemoryExtractAgent  │
             │                    │ 提取长期记忆        │
             │                    └────────────────────┘
             │
             └──────────► handleStream() → SSE → 前端
```

#### 流程 2：模拟面试

```
POST /api/interview/start-stream (SSE)
  ├── InterviewService.startInterview()
  │     ├── 保存面试记录
  │     └── 通过 SSE 返回第一个问题

POST /api/interview/answer-stream (SSE)
  ├── InterviewService.submitAnswer()
  │     ├── 保存用户回答
  │     ├── AI 评价当前回答
  │     └── 通过 SSE 返回下一个问题或结束

面试结束
  ├── RabbitMQ → InterviewEvaluationConsumer
  ├── AI 综合评价 → 生成 PDF 报告 → 上传 MinIO
  ├── 更新 InterviewRecord (aiAnalysis, reportUrl, avgScore)
  └── SSE → InterviewSseRegistry.pushCompleted()
```

#### 流程 3：学习计划生成

```
POST /api/plan/generate
  ├── PlanService.startGeneration()
  │     ├── 创建 StudyPlan 记录
  │     └── RabbitMQ → PlanGenerationConsumer

PlanGenerationConsumer
  ├── PlanAgent.handle() → AI 生成计划
  ├── PdfService.generateAndUpload() → PDF → MinIO
  ├── planMapper.updateById() → 保存计划和报告 URL
  └── PlanSseRegistry.pushCompleted() → SSE 通知前端
```

#### 流程 4：长期记忆提取

```
KnowledgeAgent.doOnComplete()
  ├── saveChatHistory() → 持久化聊天记录
  └── triggerMemoryExtractIfNeeded()
        ├── 检查 Redis: chat:last_extracted:{userId}
        ├── 超过 30 分钟冷却期 → RabbitMQ

MemoryExtractConsumer
  ├── SETNX chat:memory:extract:lock:{userId} (幂等)
  ├── getChatContext(userId, 15) → 最近 15 条
  ├── MemoryExtractAgent.extractAndSave()
  │     ├── AI 提取记忆要点 → 分条
  │     ├── EmbeddingModel.embed() → 向量化
  │     └── saveMemory()
  │           ├── findSimilarMemory() → 相似度 ≥ 0.85 合并
  │           ├── insertembding() → 新记忆写入
  │           └── 超出 MAX_MEMORIES(30) → 淘汰低相关度
  ├── 更新 Redis: chat:last_extracted:{userId} 时间戳
  └── 释放锁
```

### 8.2 业务流程图

```
┌─────────────────────────────────────────────────────────┐
│                  用户输入                                 │
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
       ┌───────────────┐
       │  AgentRouter  │  关键词匹配
       └───────┬───────┘
               │
     ┌─────────┼─────────┐
     │         │          │
     ▼         ▼          ▼
  特定Agent  KnowledgeAgent  ← 默认兜底
     │         │
     │    ┌────┴────┐
     │    │ QueryRewrite │ 短查询改写
     │    └────┬────┘
     │    ┌────┴────┐
     │    │ IntentType│ 意图分类
     │    └────┬────┘
     │    ┌────┼────────┐
     │    │    │         │
     │    ▼    ▼         ▼
     │ DIRECT SEARCH  WEB_SEARCH
     │    │    │         │
     │    │    ├─知识库检索─┤
     │    │    │ 混合检索  │ MCP搜索
     │    │    └────┬─────┘
     │    │         │
     │    └────┬────┘
     │         │
     │    ┌────┴────┐
     │    │ MemoryContext│ 注入记忆
     │    └────┬────┘
     │         │
     └────┬────┘
          │
          ▼
    ┌──────────┐
    │ SSE 流式 │
    │ 输出     │
    └────┬─────┘
         │
    ┌────┴─────┐
    │ 保存聊天 │
    │ 触发提取 │
    └──────────┘
```

### 8.3 核心模块协作

以一次完整的知识问答为例，各模块协作时序：

```
前端        AgentRouter   QueryRewrite   SearchRouter  KnowledgeAgent  MemoryService  MQ Consumer
 │              │              │                │              │              │              │
 │──input──────►│              │                │              │              │              │
 │              │──route─────►│                │              │              │              │
 │              │              │──rewrite()────│              │              │              │
 │              │              │◄─rewritten────│              │              │              │
 │              │              │                │              │              │              │
 │              │──────────────┼───────────────►│              │              │              │
 │              │              │                │──classify───►│              │              │
 │              │              │                │              │──getContext─►│              │
 │              │              │                │              │◄─memories───│              │
 │              │              │                │──search─────►│              │              │
 │              │              │                │◄─results────│              │              │
 │              │              │                │              │              │              │
 │◄──────────── SSE stream ───────────────────────────────────│              │              │
 │              │              │                │              │──saveChat───►│              │
 │              │              │                │              │──trigger───►│              │
 │              │              │                │              │              │──MQ message─►│
 │◄──────────── SSE complete ────────────────────────────────│              │              │
 │              │              │                │              │              │              │──extract──►│
 │              │              │                │              │              │              │──save─────►│
```

### 8.4 时序图说明

系统中涉及异步处理的时序：

| 场景 | 同步部分 | 异步部分（MQ） | 结果通知 |
|------|---------|---------------|---------|
| 记忆提取 | 聊天保存 | MemoryExtractAgent 分析 | 无需通知（透明） |
| 计划生成 | 创建 Plan 记录 | PlanAgent 生成 + PDF | SSE 推送完成 |
| 面试评价 | 保存答案 | 综合评价 + PDF | SSE 推送完成 |
| 周报生成 | 创建 Report 记录 | — | 同步返回 |

---

## 第 9 章 安全设计

### 9.1 用户认证机制

| 环节 | 实现 |
|------|------|
| **密码加密** | BCrypt 哈希（`BCryptPasswordEncoder`），不可逆 |
| **Token 生成** | JWT，Payload 含 `userId` + `username` + `exp`（7 天） |
| **Token 校验** | `JwtAuthenticationFilter` 从 `Authorization: Bearer <>` 提取 Token |
| **无状态会话** | `SessionCreationPolicy.STATELESS`，不依赖服务端 Session |
| **线程隔离** | `SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)`，支持 SSE 异步线程 |

### 9.2 权限控制

- 当前版本采用 **用户级隔离**：每个用户只能访问自己的数据（通过 `userId` 参数控制）
- 所有 Service 方法均传入 `userId`，从 JWT 解析，不可伪造
- 后续可扩展为 **RBAC** 模型（管理员/普通用户）

### 9.3 数据安全

| 措施 | 说明 |
|------|------|
| **密码存储** | BCrypt 哈希，不可逆 |
| **Token 签名** | JWT HMAC-SHA256 签名，Secret 从环境变量 `JWT_SECRET` 获取 |
| **文件上传** | 限制 500MB，后端校验文件类型 |
| **SQL 注入防护** | MyBatis `#{}` 参数化查询 |
| **XSS 防护** | Vue 模板自动转义 |
| **敏感信息** | API Key 通过环境变量注入（`DASHSCOPE_API_KEY`、`TAVILY_API_KEY` 等） |

### 9.4 接口安全

| 机制 | 说明 |
|------|------|
| **全局认证** | Spring Security Filter Chain 拦截 `/api/**` |
| **公开端点** | `POST /api/auth/login`、`POST /api/auth/register` 无需认证 |
| **Token 过期** | 7 天，前端检测 401 状态跳转登录页 |
| **CORS 限制** | 仅允许 `localhost:5173`、`localhost:5174`、`localhost:3000` |

### 9.5 日志审计

| 日志类型 | 记录内容 | 级别 |
|----------|---------|------|
| **认证日志** | 登录成功/失败、Token 校验 | INFO |
| **业务日志** | Agent 路由、意图分类、记忆提取 | INFO / DEBUG |
| **异常日志** | 系统异常、业务异常、异步任务失败 | ERROR |
| **消费日志** | RabbitMQ 消息消费状态 | INFO |

---

## 第 10 章 异常处理与日志设计

### 10.1 异常分类

| 类型 | 示例 | HTTP 状态码 |
|------|------|-------------|
| **参数校验异常** | `@Valid` 校验失败 | 400 |
| **认证异常** | 密码错误、Token 无效 | 401 |
| **业务异常** | 文件格式不支持、重复操作 | 400 |
| **运行时异常** | NPE、数据库连接失败 | 500 |
| **异步异常** | RabbitMQ 消费失败 | 日志记录 |

### 10.2 统一异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）集中处理：

```java
// 参数校验失败 → 400
@ExceptionHandler(MethodArgumentNotValidException.class)

// 认证失败 → 401
@ExceptionHandler(BadCredentialsException.class)

// 业务参数异常 → 400
@ExceptionHandler(IllegalArgumentException.class)

// 运行时异常 → 500（日志含具体位置）
@ExceptionHandler(RuntimeException.class)

// 兜底异常 → 500
@ExceptionHandler(Exception.class)
```

### 10.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 / 业务逻辑错误 |
| 401 | 未认证或 Token 过期 |
| 500 | 服务器内部错误 |

### 10.4 日志规范

| 规范项 | 约定 |
|--------|------|
| **日志框架** | SLF4J + Logback |
| **日志级别** | `com.trace` 包 DEBUG，`org.springframework.ai` 包 DEBUG |
| **日志格式** | `HH:mm:ss.SSS [thread] LEVEL logger - message` |
| **语言** | 全部中文 |
| **错误位置** | 每个异常日志标注具体类名.方法名 |
| **敏感信息** | 不记录密码、API Key 等敏感数据 |

---

## 第 11 章 非功能需求设计

### 11.1 性能设计

| 指标 | 目标 |
|------|------|
| **API 响应时间** | 简单查询 < 200ms |
| **SSE 首字延迟** | < 2s |
| **并发支持** | 虚拟线程，支撑数百并发用户 |
| **缓存策略** | Redis 缓存短期记忆、系统提示词懒加载 |

### 11.2 可扩展性设计

| 维度 | 设计 |
|------|------|
| **新 Agent 扩展** | 实现 `Agent` 接口 → 在 `canHandle()` 定义关键词 → 自动被 `AgentRouter` 发现 |
| **新消息队列扩展** | 在 `RabbitMQConfig` 声明新队列 → 编写新 Consumer |
| **数据库扩展** | Flyway 管理版本迁移，新增表通过新迁移脚本 |
| **水平扩展** | Redis 分布式锁（Redisson）已支持多实例部署 |

### 11.3 可维护性设计

| 措施 | 说明 |
|------|------|
| **代码分层** | Controller → Service → Mapper，职责清晰 |
| **接口隔离** | 每个 Service 独立接口，通过 Spring DI 注入 |
| **常量管理** | 所有常量集中在 `constant.java` |
| **配置文件** | 敏感信息通过环境变量注入 |
| **数据库迁移** | Flyway 版本化管理 DDL |

### 11.4 容错性设计

| 场景 | 容错措施 |
|------|---------|
| **AI 服务不可用** | 返回 "服务暂不可用" 提示，不崩溃 |
| **RabbitMQ 不可用** | 消息发送失败记录日志，主流程不受影响 |
| **Redis 不可用** | 短期记忆回退数据库，取消功能失效但主流程可用 |
| **MinIO 不可用** | PDF 生成失败记录日志，计划/面试/周报仍可查看文本 |
| **PgVector 检索失败** | 回退到按时间排序的最近记忆 |
| **Embedding 失败** | 跳过向量化，仍保存记忆内容（无去重） |
| **消息重复消费** | SETNX 幂等锁（TTL 300s） |

### 11.5 易用性设计

| 设计 | 说明 |
|------|------|
| **SSE 流式输出** | 用户实时看到 AI 回答，体验流畅 |
| **Markdown 渲染** | 前端 MarkdownRenderer 组件，支持标题、列表、代码块 |
| **语音输入** | 前端支持语音转文字 |
| **取消机制** | 用户可随时停止 AI 回答 |
| **热力图** | GitHub 风格可视化成长轨迹 |
| **移动端适配** | Element Plus 响应式布局 |

---

## 第 12 章 部署方案

### 12.1 部署环境

| 环境 | 说明 |
|------|------|
| **开发环境** | Windows / macOS，本地启动 Spring Boot + Docker 中间件 |
| **测试环境** | Linux 服务器，Docker Compose 一键启动 |
| **生产环境** | 云服务器（≥ 4C8G），Docker Compose + Nginx 反向代理 |

### 12.2 系统部署架构

```
┌─────────────────────────────────────────────┐
│                  Nginx :80/443              │
│           (前端静态资源 + API 反代)            │
└──────────┬──────────────────────────────────┘
           │
    ┌──────┴──────┐
    │  Trace App  │  :8080  (Java 21, 虚拟线程)
    └──────┬──────┘
           │
    ┌──────┼──────────┬──────────┐
    │      │          │          │
    ▼      ▼          ▼          ▼
PostgreSQL  Redis   RabbitMQ   MinIO
  :5432    :6379    :5672      :9000
```

### 12.3 服务部署方案

| 服务 | 部署方式 | 端口 | 扩展策略 |
|------|---------|------|---------|
| **Trace App** | JAR / Docker 容器 | 8080 | 多实例 + Redisson 分布式锁 |
| **PostgreSQL** | Docker 容器 | 5432 | 单实例（后续可读写分离） |
| **Redis** | Docker 容器 | 6379 | 单实例（后续可哨兵/集群） |
| **RabbitMQ** | Docker 容器 | 5672 / 15672 | 单实例 |
| **MinIO** | Docker 容器 | 9000 / 9001 | 单实例 |

### 12.4 配置管理

| 配置项 | 来源 |
|--------|------|
| `DASHSCOPE_API_KEY` | 环境变量 / Docker Secret |
| `DB_USERNAME` / `DB_PASSWORD` | 环境变量 |
| `REDIS_HOST` / `REDIS_PORT` | 环境变量 |
| `RABBITMQ_HOST` / `RABBITMQ_USER` / `RABBITMQ_PASS` | 环境变量 |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 环境变量 |
| `JWT_SECRET` | 环境变量 |
| `SERVER_PORT` | 环境变量（默认 8080） |
| `TAVILY_API_KEY` | `mcp-servers.json` 环境变量 |

### 12.5 系统启动顺序

```
1. PostgreSQL + PgVector  (必须先启动)
2. Redis                   (缓存 / 锁)
3. RabbitMQ                (消息队列)
4. MinIO                   (对象存储)
5. Trace App               (主应用)
6. Nginx (可选)            (反向代理)
```

---

## 第 13 章 关键技术实现

### 13.1 前后端分离

- **前端**：Vue 3 SPA（Vite 构建），部署于 Nginx 或独立开发服务器（:5173）
- **后端**：Spring Boot REST API（:8080），仅返回 JSON / SSE 流
- **通信**：HTTP + SSE（`text/event-stream`）
- **跨域**：Spring Security CORS 配置允许前端域名

### 13.2 数据访问层

| 技术 | 说明 |
|------|------|
| **ORM** | MyBatis-Plus 3.5.9，`BaseMapper<T>` 提供自动 CRUD |
| **自定义 SQL** | XML Mapper 文件，手写复杂 SQL |
| **连接池** | HikariCP，最大 20 连接，最小 5 空闲 |
| **分页** | MyBatis-Plus `Page<T>` + 手动游标分页（`findBefore`） |
| **类型处理** | 自定义 `ListStringTypeHandler` 处理 PG `TEXT[]` ↔ Java `List<String>` |
| **向量操作** | PgVector `<=>` 余弦距离运算符，`::vector` 类型转换 |

### 13.3 缓存设计

| 用途 | 实现 | TTL |
|------|------|-----|
| **短期记忆** | Redis List，`LTRIM` 保留 20 条 | 无（数量控制） |
| **取消信号** | Redis String `chat:cancel:{userId}` | 60s |
| **提取时间戳** | Redis String `chat:last_extracted:{userId}` | 无（覆盖更新） |
| **提取锁** | Redis SETNX `chat:memory:extract:lock:{userId}` | 300s |
| **面试会话** | Redis `interview:session:{sessionId}` | 会话期间 |
| **系统提示词** | 类字段懒加载，首次读取 classpath 文件后缓存 | 永久 |

### 13.4 消息队列

| 队列 | 交换机 | 路由键 | 消费者 | 说明 |
|------|--------|--------|--------|------|
| `trace.plan.generate` | `trace.plan.exchange` | `trace.plan.generate` | `PlanGenerationConsumer` | AI 生成学习计划 |
| `trace.interview.eval` | `trace.interview.exchange` | `trace.interview.eval` | `InterviewEvaluationConsumer` | AI 评价面试 |
| `trace.memory.extract` | `trace.memory.exchange` | `trace.memory.extract` | `MemoryExtractConsumer` | 提取长期记忆 |

消息转换：`Jackson2JsonMessageConverter`，使用 `Map<String, Object>` 作为消息载体。

### 13.5 服务注册与发现

当前版本为 **单体架构**，不使用注册中心。Agent 通过 Spring 的 `List<Agent>` 自动注入实现插件发现。

### 13.6 API 网关

当前版本无独立 API 网关，前端直接请求后端。后续可引入 Spring Cloud Gateway 或 Nginx 做统一网关。

---

## 第 14 章 风险分析与应对措施

### 14.1 技术风险

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| **AI 模型不可用** | 核心功能全部受阻 | 返回明确错误提示；缓存常用回答 |
| **AI 响应过慢** | 用户体验差 | SSE 流式输出缓解感知延迟；查询改写减少 Token 消耗 |
| **MCP 工具连接失败** | 联网搜索不可用 | 降级为知识库检索或直接回答 |
| **PgVector 性能瓶颈** | 检索变慢 | IVFFlat 索引；后续可升级为 HNSW 索引 |
| **RabbitMQ 消息堆积** | 异步任务延迟 | 设置消费者并发（2-5）；监控队列长度 |

### 14.2 业务风险

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| **AI 生成内容不准确** | 用户信任度下降 | 提示词约束 + 免责声明；联网搜索增强事实性 |
| **面试评价偏差** | 用户对评分不满 | 评价结果仅供参考；展示详细分析而非单一分数 |
| **记忆提取误判** | 记忆内容无关 | 向量去重 + 数量限制降低噪音 |
| **周报模板单一** | 用户觉得千篇一律 | 根据用户数据动态调整周报内容 |

### 14.3 数据风险

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| **聊天数据丢失** | 上下文缺失 | 双重存储（Redis + PG）；定期备份数据库 |
| **向量数据损坏** | 检索失败 | 回退到全文检索 / 最近记忆 |
| **文件上传过大** | 磁盘占满 | 限制 500MB；MinIO 生命周期策略清理旧文件 |
| **密码泄露** | 账号安全 | BCrypt 哈希；JWT Secret 定期轮换 |

### 14.4 风险应对措施汇总

| 风险等级 | 应对策略 |
|----------|---------|
| **高**（AI 不可用） | 降级 + 错误提示 + 状态监控 |
| **中**（消息堆积 / 数据丢失） | 监控告警 + 定期备份 + 消费者并发调整 |
| **低**（向量检索变慢 / 模板单一） | 索引优化 + 内容动态化 |

---

## 第 15 章 总结

### 15.1 设计总结

Trace 系统采用 **前后端分离的单体架构**，以 **Agent 插件式路由** 为核心设计模式，实现了以下关键设计：

1. **智能化**：集成大语言模型（qwen-plus）和向量检索（text-embedding-v3），提供知识问答、面试模拟、计划生成等 AI 能力
2. **流式交互**：所有 AI 对话采用 SSE 流式输出，支持用户实时取消
3. **三层记忆**：短期（Redis）→ 聊天历史（PG）→ 长期记忆（PG），AI 自动提取用户偏好
4. **异步解耦**：RabbitMQ 处理面试评价、计划生成、记忆提取，不阻塞主流程
5. **安全可靠**：JWT 认证 + BCrypt 加密，统一异常处理，中文日志标注错误位置
6. **可扩展**：Agent 插件式架构，新增功能无需修改路由逻辑
7. **数据管控**：所有存储设置上限（聊天 500 条、短期 20 条、长期 30 条），自动淘汰

### 15.2 后续规划

| 阶段 | 内容 |
|------|------|
| **近期** | 完善 MCP 联网搜索工具注册；优化混合检索权重；增加移动端适配 |
| **中期** | 引入 Spring Cloud Gateway 统一网关；拆分微服务；增加消息推送 |
| **远期** | 多租户支持；RBAC 权限模型；数据看板自定义；社区共享知识库 |

---

## 附录

### 附录 A 图表清单

| 序号 | 图表名称 | 位置 |
|------|---------|------|
| 1 | 系统逻辑架构图 | 第 3.2 节 |
| 2 | 系统架构图 | 第 3.5 节 |
| 3 | 功能模块结构图 | 第 4.2 节 |
| 4 | E-R 图 | 第 6.2 节 |
| 5 | 服务调用关系图 | 第 5.4 节 |
| 6 | 知识问答业务流程图 | 第 8.2 节 |
| 7 | 时序图 | 第 8.3 节 |
| 8 | 部署架构图 | 第 12.2 节 |

### 附录 B 数据字典

参照第 6.3 节「逻辑结构设计」，本章节详细描述了所有数据表的字段名、类型、约束和说明。

### 附录 C 接口清单

参照第 7.4 节「核心接口清单」，所有 API 接口以表格形式列出。

### 附录 D 参考资料

| 资料名称 | 说明 |
|----------|------|
| Spring Boot 3.4.x Reference | Spring Boot 官方文档 |
| Spring AI 1.1.2 Reference | Spring AI 框架文档 |
| MyBatis-Plus 3.5.9 文档 | ORM 框架 |
| PostgreSQL 18 + PgVector 文档 | 数据库文档 |
| RabbitMQ 3.x 文档 | 消息队列 |
| Redis 7.x 文档 | 缓存 |
| MinIO 文档 | 对象存储 |
| 阿里云百炼 DashScope 文档 | AI 模型 API |
| MCP 规范 | Model Context Protocol |
| 阿里巴巴 Java 开发手册 | 编码规范 |
| Google Java Style Guide | 编码风格指南 |
