-- V3: 会话记录表 —— 用 PostgreSQL 持久化聊天历史
-- 替代 Redis 短期会话上下文，支持分页查询和无限滚动

CREATE TABLE IF NOT EXISTS chat_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    role            VARCHAR(10)   NOT NULL CHECK (role IN ('user', 'ai')),
    content         TEXT          NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_history_user_time
    ON chat_history(user_id, created_at DESC);

-- 每个用户最多保留 500 条，超出部分由应用层定期清理
