-- V19: knowledge_bases 瘦身为展示表，删除与 vector_store 重复的字段
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS content;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS chunk_index;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS metadata;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS knowledge_type;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS file_type;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS name;
