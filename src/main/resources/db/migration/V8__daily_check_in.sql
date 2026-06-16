-- V8: 打卡功能 + 移除计划进度字段
CREATE TABLE IF NOT EXISTS daily_check_ins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT,
    check_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, plan_id, check_date)
);

ALTER TABLE study_plans DROP COLUMN IF EXISTS total_duration;
ALTER TABLE study_plans DROP COLUMN IF EXISTS progress;
