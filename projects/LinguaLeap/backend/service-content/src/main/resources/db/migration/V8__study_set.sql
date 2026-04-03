-- Phase 4: 用户内容引擎 — 学习集 + 学习项
-- 用户上传内容 → AI 提取分类 → 自动出题

-- 学习集（用户上传的一份学习材料）
CREATE TABLE study_set (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    source_type     VARCHAR(20) NOT NULL DEFAULT 'text',
    source_text     TEXT,
    source_file_url VARCHAR(500),
    grade           VARCHAR(20),
    status          VARCHAR(20) NOT NULL DEFAULT 'processing',
    ai_summary      TEXT,
    ai_strategy     JSONB,
    item_count      INT DEFAULT 0,
    question_count  INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_study_set_user ON study_set(user_id);
CREATE INDEX idx_study_set_status ON study_set(user_id, status);

-- 学习项（从上传内容中 AI 提取的知识点）
CREATE TABLE learning_item (
    id              BIGSERIAL PRIMARY KEY,
    study_set_id    BIGINT NOT NULL REFERENCES study_set(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL,
    category        VARCHAR(30) NOT NULL,
    content         VARCHAR(500) NOT NULL,
    meaning_zh      VARCHAR(500),
    phonetic        VARCHAR(100),
    example_sentence TEXT,
    example_zh      TEXT,
    extra_data      JSONB,
    difficulty      INT DEFAULT 1,
    ai_note         TEXT,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_learning_item_set ON learning_item(study_set_id);
CREATE INDEX idx_learning_item_user ON learning_item(user_id);
CREATE INDEX idx_learning_item_category ON learning_item(study_set_id, category);
