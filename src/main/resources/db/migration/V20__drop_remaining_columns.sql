-- V19 (补充): 删除 knowledge_bases 残留的 embedding/content_tsv/usage 列
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS embedding;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS content_tsv;
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS usage;
