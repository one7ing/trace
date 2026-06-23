-- V21: knowledge_bases 加回 content 列，存储原始全文（非切块）
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS content TEXT;
