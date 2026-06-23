-- V18: 删除知识库禁用字段 + 题库增加 bank_id / user_id
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS disabled;

ALTER TABLE question_bank ADD COLUMN IF NOT EXISTS bank_id BIGINT;
ALTER TABLE question_bank ADD COLUMN IF NOT EXISTS user_id BIGINT;
