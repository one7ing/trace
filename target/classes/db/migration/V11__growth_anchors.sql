-- V11: 成长力锚点表
CREATE TABLE IF NOT EXISTS growth_anchors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    anchor_date DATE NOT NULL,
    label VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
