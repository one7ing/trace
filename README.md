# Trace — 个人成长轨迹记录与分析系统

基于 AI 大模型的智能成长助手，提供知识问答、刷题练习、日记记录、成长周报、学习计划、习惯打卡等一站式个人成长管理功能。

---

## 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 主语言（虚拟线程） |
| Spring Boot | 3.4.6 | 应用框架 |
| Spring Security | 6.x | 认证与授权 |
| Spring AI | 1.1.2 | AI 对话集成 |
| Spring AI Alibaba | 1.1.2.2 | 阿里云百炼 DashScope 适配 |
| MyBatis-Plus | 3.5.9 | ORM / 数据库操作 |
| PostgreSQL + pgvector | 16+ | 主数据库 + 向量存储 |
| Redis + Redisson | 7.x / 3.36.0 | 缓存 + Token 存储 |
| RabbitMQ | 3.x | 异步消息（AI 判题 / 计划生成 / 记忆提取） |
| MinIO | — | 对象存储（周报 PDF） |
| Flyway | — | 数据库迁移 |
| JWT (jjwt) | 0.12.6 | 双 Token 认证 |
| Knife4j / Swagger | 4.5.0 | API 文档 |
| Flying Saucer | 9.4.0 | HTML → PDF 转换 |
| Thymeleaf | — | PDF 模板引擎 |
| Lombok | — | 简化代码 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.4.0 | 前端框架 (Composition API) |
| TypeScript | ~5.3.0 | 类型安全 |
| Vite | ^5.1.0 | 构建工具 |
| Element Plus | ^2.6.0 | UI 组件库 |
| Pinia | ^2.1.0 | 状态管理 |
| Vue Router | ^4.3.0 | 路由 |
| Axios | ^1.6.0 | HTTP 请求 + 拦截器 |
| ECharts | ^6.1.0 | 图表（热力图/趋势图） |
| markdown-it | ^14.2.0 | Markdown 渲染 |
| SCSS | — | 样式预处理 |

### 基础设施
| 组件 | 用途 |
|------|------|
| Docker Compose | 一键启动 PostgreSQL / Redis / RabbitMQ / MinIO |

---

## 项目结构

```
trace/
├── src/main/java/com/trace/
│   ├── agent/              # AI Agent 层（见核心模块）
│   ├── config/             # Spring 配置（JWT / Redis / MinIO / RabbitMQ）
│   ├── constant/           # 全局常量（Redis Key / Token / 业务常量）
│   ├── controller/         # REST 控制器
│   ├── dto/                # 请求/响应 DTO
│   ├── entity/             # 数据库实体
│   ├── enums/              # 枚举
│   ├── exception/          # 全局异常处理
│   ├── mapper/             # MyBatis-Plus Mapper
│   ├── scheduler/          # 定时任务（记忆整合）
│   ├── security/           # 安全模块（JWT 过滤器 / Token 工具 / Security 配置）
│   └── service/
│       └── impl/           # 业务实现（含 RabbitMQ Consumer）
├── src/main/resources/
│   ├── agent/prompts/      # AI 提示词模板
│   ├── db/migration/       # Flyway 数据库迁移脚本 (V1~V21)
│   ├── mapper/             # MyBatis XML Mapper
│   ├── templates/          # PDF 模板 (Thymeleaf HTML)
│   └── application.yml     # 主配置
├── frontend/
│   └── src/
│       ├── api/            # Axios 实例 + 拦截器（无感刷新）
│       ├── components/     # 公共组件（MarkdownRenderer / SideNav / ChatMessage）
│       ├── router/         # 路由配置 + 守卫
│       ├── stores/         # Pinia 状态（auth）
│       ├── styles/         # 全局 SCSS 样式
│       └── views/          # 页面视图
│           └── practice/   # 刷题子页面
│           └── diary/      # 日记子页面
├── docker-compose.yml      # 开发环境基础设施
└── pom.xml                 # Maven 依赖
```

---

## 核心模块

### 1. 认证模块 (`security/` + `controller/AuthController`)

JWT 双 Token 机制，配合 Redis 实现无感刷新：

```
登录 → accessToken (30min) + refreshToken (7d)
     → refreshToken 存入 Redis (Key: trace:refresh:{userId})
请求 → accessToken 过期 → 返回 401 (code:40101)
     → 前端拦截器自动用 refreshToken 换取新 accessToken
     → 重放原请求（用户无感知）
登出 → 删除 Redis 中的 refreshToken
     → accessToken 自然过期
```

**关键文件：**
- `security/JwtUtil.java` — 生成 / 解析 / 校验 Token
- `security/JwtAuthenticationFilter.java` — 拦截请求，区分过期 (40101) 与无效 (40100)
- `security/SecurityConfig.java` — Spring Security 无状态配置 + CORS
- `service/TokenService.java` — Redis 中 refreshToken 的增删查
- `controller/AuthController.java` — 注册 / 登录 / 刷新 / 登出 / 个人资料

### 2. AI 知识问答 (`agent/KnowledgeAgent` + `controller/KnowledgeController`)

支持三种模式的 SSE 流式对话：

| 模式 | 说明 |
|------|------|
| `direct` | 默认对话，结合用户记忆和历史上下文 |
| `web` | 联网搜索模式，实时获取最新信息 |
| `rag` | RAG 知识库模式，基于用户上传的文档回答 |

**处理流程：**
1. 用户发送消息 → `QueryRewriteAgent` 改写查询
2. 检索相关记忆（pgvector 向量搜索）和近期对话
3. 构建 System Prompt → 调用 DashScope LLM (qwen-plus)
4. SSE 流式返回，前端 `MarkdownRenderer` 实时渲染

### 3. 记忆系统 (`agent/MemoryExtractAgent` + `service/MemoryService`)

三层记忆架构：

| 层级 | 存储 | 容量 | 说明 |
|------|------|------|------|
| 短期记忆 | Redis (`chat:short:{userId}`) | 15 条 | 当前会话上下文 |
| 会话记忆 | PostgreSQL (`chat_history`) | 500 条 | 近期对话历史 |
| 长期记忆 | PostgreSQL + pgvector (`long_term_memory`) | 30 条 | AI 提取的结构化记忆 |

**异步提取流程：**
- 用户对话达到阈值 → RabbitMQ 投递消息 → `MemoryExtractConsumer` 消费
- 调用 LLM 从对话中提取关键记忆 → pgvector 向量化存储
- 定时任务 (`MemoryConsolidationScheduler`) 定期整合相似记忆

### 4. 刷题练习 (`PracticeController` + `PracticeService`)

智能刷题系统：

- **题库管理**：支持自定义题库，题型包含单选 / 多选 / 填空 / 简答
- **AI 判题**：用户提交答案 → RabbitMQ 异步投递 → `PracticeJudgmentConsumer` 调用 LLM 判分
- **SSE 实时反馈**：判题结果通过 SSE 实时推送到前端
- **练习记录**：历史记录查看，含 AI 评语和正确率统计
- **分页滚动**：支持上滑加载更多历史记录

### 5. 学习计划 (`PlanController` + `PlanService`)

AI 驱动的个性化学习计划：

- 用户输入目标 → RabbitMQ 投递 → `PlanGenerationConsumer`
- AI 生成结构化学习计划（阶段 / 任务 / 时间线）
- **SSE 进度跟踪**：前端通过 `EventSource` 实时展示生成进度
- 支持计划查看、进度更新

### 6. 仪表盘 (`DashboardController` + `DashboardService`)

成长数据可视化：

- **热力图**：12 周打卡记录 (ECharts Heatmap)
- **趋势图**：30 天综合评分趋势
- **评分算法**：打卡 (40%) + 刷题 (30%) + 计划执行 (20%) + 日记 (10%)
- 每日评分封顶 100 分

### 7. 日记本 (`DiaryController` + `DiaryService`)

- 日记的增删改查
- 支持 Markdown 编辑和渲染
- AI 辅助写作（可扩展）

### 8. 成长周报 (`WeeklyReportController` + `WeeklyReportService` + `PdfService`)

- 根据本周活动自动生成周报
- **PDF 导出**：Thymeleaf 模板 → HTML → Flying Saucer → PDF
- PDF 上传至 MinIO 对象存储

### 9. 每日打卡 (`CheckInController` + `CheckInService`)

- 每日心情 / 学习 / 运动多维度打卡
- 与仪表盘热力图联动

### 10. 知识库管理 (`KnowledgeBaseController` + `KnowledgeBaseService`)

- 文档上传与管理
- pgvector 向量化索引
- 为 RAG 模式提供检索数据源

---

## 数据库

PostgreSQL + pgvector 扩展，Flyway 管理迁移（21 个版本脚本）。

核心表：

| 表名 | 说明 |
|------|------|
| `users` | 用户（用户名 / 邮箱 / 密码哈希 / 头像） |
| `chat_history` | 对话历史 |
| `long_term_memory` | 长期记忆（含 pgvector embedding） |
| `daily_check_in` | 每日打卡 |
| `diary` | 日记 |
| `study_plan` | 学习计划 |
| `question_bank` | 题库 |
| `practice_question_detail` | 题目详情 |
| `practice_record` | 刷题记录 |
| `weekly_report` | 周报 |
| `knowledge_base` | 知识库文档 |
| `growth_anchor` | 成长里程碑 |

---

## 消息队列

RabbitMQ 异步处理耗时任务：

| 队列 | 用途 | Consumer |
|------|------|----------|
| `trace.plan.generate` | AI 生成学习计划 | `PlanGenerationConsumer` |
| `trace.practice.judge` | AI 判题 | `PracticeJudgmentConsumer` |
| `trace.memory.extract` | AI 提取长期记忆 | `MemoryExtractConsumer` |

Redis 用于：短期对话缓存 / 分布式锁 / refreshToken 存储 / AI 任务取消信号。

---

## 快速启动

### 1. 启动基础设施

```bash
docker-compose up -d
```

启动 PostgreSQL、Redis、RabbitMQ、MinIO。

### 2. 配置环境变量

```bash
export DASHSCOPE_API_KEY=your-api-key
export DB_USERNAME=postgres
export DB_PASSWORD=123456
export MINIO_ACCESS_KEY=admin
export MINIO_SECRET_KEY=qwer1234
export RABBITMQ_USER=trace
export RABBITMQ_PASS=123456
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`，Flyway 自动建表。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，Vite 自动代理 `/api` 到后端。

### 5. 访问

- 前端：`http://localhost:5173`
- API 文档：`http://localhost:8080/doc.html`

---

## License

MIT
