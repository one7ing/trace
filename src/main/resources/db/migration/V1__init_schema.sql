-- Trace 数据库初始化脚本
-- 需要 PostgreSQL 16 + PgVector 扩展

-- 启用 PgVector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(100),
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 日记表
-- ============================================
CREATE TABLE IF NOT EXISTS diaries (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    mood_tag    VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_diaries_user_id ON diaries(user_id);
CREATE INDEX idx_diaries_created_at ON diaries(user_id, created_at DESC);

-- ============================================
-- 面试记录表
-- ============================================
CREATE TABLE IF NOT EXISTS interview_records (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    industry        VARCHAR(50)    NOT NULL,
    skill_tags      TEXT[],
    total_questions INT            NOT NULL DEFAULT 0,
    avg_score       DECIMAL(5,2),
    report_url      VARCHAR(500),
    completed_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interview_records_user_id ON interview_records(user_id, completed_at DESC);

-- ============================================
-- 面试题目详情表
-- ============================================
CREATE TABLE IF NOT EXISTS interview_question_details (
    id              BIGSERIAL PRIMARY KEY,
    record_id       BIGINT         NOT NULL REFERENCES interview_records(id) ON DELETE CASCADE,
    question        TEXT           NOT NULL,
    user_answer     TEXT,
    ai_comment      TEXT,
    score           DECIMAL(5,2),
    sequence_num    INT            NOT NULL DEFAULT 0
);

CREATE INDEX idx_question_details_record_id ON interview_question_details(record_id);

-- ============================================
-- 周报表
-- ============================================
CREATE TABLE IF NOT EXISTS weekly_reports (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_start      DATE         NOT NULL,
    week_end        DATE         NOT NULL,
    summary         TEXT,
    full_content    TEXT,
    report_url      VARCHAR(500),
    generated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_user_week UNIQUE (user_id, week_start)
);

CREATE INDEX idx_weekly_reports_user_id ON weekly_reports(user_id, week_start DESC);

-- ============================================
-- 学习计划表
-- ============================================
CREATE TABLE IF NOT EXISTS study_plans (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal            VARCHAR(500) NOT NULL,
    plan_content    TEXT,
    plan_url        VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_study_plans_user_id ON study_plans(user_id, created_at DESC);

-- ============================================
-- 长期记忆向量表 (PgVector)
-- ============================================
CREATE TABLE IF NOT EXISTS long_term_memories (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    content         TEXT         NOT NULL,
    embedding       vector(1024),
    source_type     VARCHAR(30)  NOT NULL,
    source_id       BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_memories_user_id ON long_term_memories(user_id);
CREATE INDEX idx_memories_source_type ON long_term_memories(user_id, source_type);

-- PgVector IVFFlat 索引 (数据量 > 1000 时创建)
-- CREATE INDEX idx_memories_embedding ON long_term_memories
--     USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ============================================
-- 面试题库向量表 (PgVector)
-- ============================================
CREATE TABLE IF NOT EXISTS interview_question_bank (
    id              BIGSERIAL PRIMARY KEY,
    industry        VARCHAR(50)  NOT NULL,
    skill           VARCHAR(50)  NOT NULL,
    question        TEXT         NOT NULL,
    embedding       vector(1024),
    difficulty      VARCHAR(10)  DEFAULT 'medium'
);

CREATE INDEX idx_question_bank_industry_skill ON interview_question_bank(industry, skill);

-- PgVector IVFFlat 索引 (数据量 > 1000 时创建)
-- CREATE INDEX idx_question_bank_embedding ON interview_question_bank
--     USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
