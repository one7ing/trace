-- V9: 恢复计划时长字段（用于打卡进度计算）
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS total_duration INT;
