-- V4: 给 interview_records 添加 ai_analysis 字段存储综合评价
ALTER TABLE interview_records ADD COLUMN IF NOT EXISTS ai_analysis TEXT;
