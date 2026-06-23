-- V17: knowledge_bases 表新增管理字段（名称、分类、禁用）
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT '知识问答';
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS disabled INTEGER DEFAULT 0;

-- 为已有数据补充默认值
UPDATE knowledge_bases SET category = '知识问答' WHERE category IS NULL;
UPDATE knowledge_bases SET disabled = 0 WHERE disabled IS NULL;
