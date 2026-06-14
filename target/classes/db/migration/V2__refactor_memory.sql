-- V2: 长期记忆表重构 —— 向量 → 纯文本
-- 1. 清空旧的向量化记忆数据（数据格式不兼容）
-- 2. 删除 embedding 向量列
-- 3. 添加新的复合索引（按时间检索）

-- 清空旧数据（旧格式基于向量，新架构不兼容）
TRUNCATE TABLE long_term_memories;

-- 删除向量列
ALTER TABLE long_term_memories
    DROP COLUMN IF EXISTS embedding;

-- 添加按时间检索的复合索引
CREATE INDEX IF NOT EXISTS idx_memories_user_created
    ON long_term_memories(user_id, created_at DESC);
