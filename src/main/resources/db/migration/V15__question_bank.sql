-- V15: 创建题库表 question_bank，用于刷题练习
CREATE TABLE IF NOT EXISTS question_bank (
    id               BIGSERIAL PRIMARY KEY,
    topic            VARCHAR(50)     NOT NULL,
    question         TEXT            NOT NULL,
    reference_answer TEXT            NOT NULL,
    difficulty       VARCHAR(10)     NOT NULL DEFAULT 'medium',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_question_bank_topic ON question_bank(topic);
