-- V7: study_plans 添加计划进度字段
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS total_duration INT;
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS progress INT;
