# Trace — 个人成长轨迹记录与分析系统

基于 AI 的个人成长管理平台，支持学习计划生成、刷题练习、每日打卡、成长仪表盘、知识库管理、周报自动生成等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4.6 + Java 21（虚拟线程） |
| 前端框架 | Vue 3 + TypeScript + Vite 5 |
| UI 组件 | Element Plus + ECharts |
| 数据库 | PostgreSQL 18 + PgVector（向量存储） |
| ORM | MyBatis-Plus 3.5.9 |
| 缓存 | Redis + Redisson |
| 消息队列 | RabbitMQ 3.12 |
| 对象存储 | MinIO |
| 认证 | Spring Security + JWT（jjwt 0.12） |
| AI | Spring AI + 阿里云百炼 DashScope（Qwen） |
| PDF | Flying Saucer + Thymeleaf |
| 文档 | Knife4j + SpringDoc OpenAPI 2.7 |
| 数据库迁移 | Flyway |

## 项目亮点
多 Agent 协作架构：4 个独立 AI Agent（知识问答、计划生成、记忆提取、查询改写），通过 Spring AI + DashScope（Qwen）驱动，各司其职

SSE 全链路流式响应：AI 对话、计划生成、刷题判题全部通过 Server-Sent Events 实时推送，避免长轮询，用户体验流畅

RabbitMQ 异步解耦：耗时的 AI 任务（计划生成、记忆提取、判题）全部入队异步处理，接口秒级响应，消费者处理完成后通过 SSE 推送给前端

Redis ZSET 滑动窗口限流：自研 @RateLimit 注解 + Lua 原子脚本，对 AI 接口做用户级精准限流，防止 API 费用失控

RAG 知识库检索增强：基于 PgVector 向量存储 + Spring AI Embedding，用中文本地知识库增强 AI 回答质量

虚拟线程全开：Spring Boot 3.4 + Java 21 虚拟线程，Tomcat/Redis/RabbitMQ 全线异步，轻松承载高并发

JWT 双 Token 机制：accessToken（30分钟）+ refreshToken（7天），Redis 存储长 Token，兼顾安全与体验

一键 Docker 部署：docker compose up -d 启动 PostgreSQL(PgVector) + MinIO + RabbitMQ，附带完整 JMeter 压测计划


## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

启动 PostgreSQL（PgVector）、MinIO、RabbitMQ。

### 2. 配置环境变量

```bash
# 阿里云百炼 API Key（必需）
export DASHSCOPE_API_KEY=your-api-key

# 以下可选，使用默认值
export DB_USERNAME=postgres
export DB_PASSWORD=123456
export REDIS_HOST=localhost
export REDIS_PORT=6379
export RABBITMQ_HOST=localhost
export RABBITMQ_USER=trace
export RABBITMQ_PASS=123456
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=admin
export MINIO_SECRET_KEY=qwer1234
export JWT_SECRET=your-jwt-secret
export SERVER_PORT=8080
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8080`，API 文档：`http://localhost:8080/doc.html`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

## 项目结构

```
trace/
├── src/main/java/com/trace/
│   ├── agent/          # AI Agent（知识问答、计划生成、记忆提取、查询改写）
│   ├── config/         # 配置类（JWT、MinIO、Redis、RabbitMQ、Knife4j）
│   ├── controller/     # REST 控制器（9 个模块）
│   ├── dto/            # 请求/响应 DTO
│   ├── entity/         # 数据库实体
│   ├── exception/      # 全局异常处理
│   ├── mapper/         # MyBatis-Plus Mapper
│   ├── scheduler/      # 定时任务（周报自动生成）
│   ├── security/       # Spring Security + JWT 过滤器 + 限流
│   └── service/        # 业务逻辑层
├── src/main/resources/
│   ├── agent/          # AI 提示词模板
│   ├── db/migration/   # Flyway 数据库迁移脚本
│   ├── mapper/         # MyBatis XML 映射
│   └── application.yml
├── frontend/           # Vue 3 前端
├── docker-compose.yml  # 基础设施编排
└── trace-jmeter-plan.jmx  # JMeter 压测计划
```

## 核心功能模块

### 用户认证 (`/api/auth`)
- 邮箱注册 / 用户名+密码登录
- JWT 双 Token（accessToken 30分钟 + refreshToken 7天）
- 头像上传、用户名修改、密码重置

### 学习计划 (`/api/plan`)
- AI 自动生成学习计划（SSE 流式）
- 手动创建计划、编辑内容
- 计划 PDF 报告生成与下载

### 刷题练习 (`/api/practice`)
- 按方向随机抽题、逐题/全部作答
- AI 判题 + 点评（SSE 流式）
- 自定义题库管理（文本/文件导入）

### 每日打卡 (`/api/checkin`)
- 学习计划每日签到
- 周打卡状态、打卡进度统计

### 成长仪表盘 (`/api/dashboard`)
- 综合数据面板、成长力评分
- 成长锚点管理、趋势图

### 成长日记 (`/api/diary`)
- 日记 CRUD、分页列表

### AI 知识问答 (`/api/knowledge`)
- SSE 流式对话，支持三种模式：
  - `direct`：直接对话
  - `web`：联网搜索
  - `rag`：本地知识库检索增强
- 聊天历史游标分页

### 知识库管理 (`/api/knowledge-base`)
- 文件上传（PDF/TXT）、搜索
- 条目 CRUD、文件内容编辑

### 周报生成 (`/api/weekly-report`)
- 手动/自动生成周报 PDF
- 周报列表、详情、下载

## API 认证说明

| 路径 | 认证 |
|------|------|
| `/api/auth/**` | 无需认证 |
| `/api/**`（其他） | Bearer Token |
| `/doc.html`, `/swagger-ui/**` | 无需认证 |

登录后返回 `accessToken`，后续请求头携带 `Authorization: Bearer <accessToken>`。

## 基础设施端口

| 服务 | 端口 |
|------|------|
| 后端 API | 8080 |
| 前端 Dev | 5173 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| RabbitMQ | 5672 (AMQP) / 15672 (管理界面) |
| MinIO | 9000 (API) / 9001 (控制台) |

## 压测

```bash
# 使用 JMeter 5.6.3+
jmeter -t trace-jmeter-plan.jmx
```

压测计划包含 6 个线程组，覆盖认证链路、核心 CRUD、知识库查询和高并发只读混合场景。详见 `.jmx` 文件内注释。
