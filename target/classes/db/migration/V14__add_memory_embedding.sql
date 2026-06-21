-- V14: long_term_memories 改为向量表
-- 1. 清空旧纯文本数据（格式不兼容）
-- 2. 重新添加 embedding 向量列（PgVector 1024 维）
-- 3. 创建 IVFFlat 索引用于向量相似度检索

TRUNCATE TABLE long_term_memories;

ALTER TABLE long_term_memories
    ADD COLUMN IF NOT EXISTS embedding vector(1024);

-- 创建向量索引（数据量 > 1000 时生效）
CREATE INDEX IF NOT EXISTS idx_memories_embedding
    ON long_term_memories
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
