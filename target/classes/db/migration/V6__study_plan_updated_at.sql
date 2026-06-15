-- V6: study_plans 添加 updated_at 字段
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
