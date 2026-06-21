-- V13: 向量索引 —— 为 vector_store 的 embedding 列创建 HNSW 索引
-- text-embedding-v3 默认 1024 维，使用 vector_cosine_ops 余弦相似度

-- HNSW 索引：构建快、无需训练、查询性能高，适合动态数据
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON public.vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
