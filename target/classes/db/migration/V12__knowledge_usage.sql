-- V12: 知识库用途字段 + 清理 WEB 类型
-- 1. knowledge_bases 表新增 usage 字段
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS usage VARCHAR(20) DEFAULT 'chat';

-- 2. 删除旧的 WEB 类型数据（不存在实际数据，安全操作）
DELETE FROM knowledge_bases WHERE knowledge_type = 'WEB';
