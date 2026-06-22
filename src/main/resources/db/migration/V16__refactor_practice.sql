-- V16: 模拟面试改造为刷题练习

-- 1. 重命名 interview_records → practice_records
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'interview_records') THEN
        ALTER TABLE interview_records RENAME TO practice_records;
    END IF;
END $$;

-- 清理废弃字段
ALTER TABLE practice_records DROP COLUMN IF EXISTS skill_tags;
ALTER TABLE practice_records DROP COLUMN IF EXISTS report_url;
ALTER TABLE practice_records DROP COLUMN IF EXISTS weak_skills;

-- 重命名列（如果还是旧名）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'practice_records' AND column_name = 'industry') THEN
        ALTER TABLE practice_records RENAME COLUMN industry TO topic;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'practice_records' AND column_name = 'avg_score') THEN
        ALTER TABLE practice_records RENAME COLUMN avg_score TO score;
    END IF;
END $$;

-- 新增 correct_count
ALTER TABLE practice_records ADD COLUMN IF NOT EXISTS correct_count INT NOT NULL DEFAULT 0;

-- 重建索引
DROP INDEX IF EXISTS idx_interview_records_user_id;
CREATE INDEX IF NOT EXISTS idx_practice_records_user_id ON practice_records(user_id, completed_at DESC);

-- 2. 新建刷题详情表
CREATE TABLE IF NOT EXISTS practice_question_details (
    id               BIGSERIAL PRIMARY KEY,
    record_id        BIGINT         NOT NULL REFERENCES practice_records(id) ON DELETE CASCADE,
    question         TEXT           NOT NULL,
    reference_answer TEXT           NOT NULL DEFAULT '',
    user_answer      TEXT           NOT NULL DEFAULT '',
    is_correct       BOOLEAN,
    score            DECIMAL(5,2),
    ai_comment       TEXT,
    sequence_num     INT            NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_practice_qd_record_id ON practice_question_details(record_id);
