CREATE TABLE question_bank (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    grade           VARCHAR(20) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    user_id         BIGINT,
    source_file_url VARCHAR(500),
    status          VARCHAR(20) DEFAULT 'active',
    kp_count        INT DEFAULT 0,
    question_count  INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE knowledge_point (
    id              BIGSERIAL PRIMARY KEY,
    bank_id         BIGINT REFERENCES question_bank(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    content         VARCHAR(200) NOT NULL,
    phonetic        VARCHAR(100),
    meaning_zh      VARCHAR(500),
    example_sentence TEXT,
    example_zh      TEXT,
    difficulty      INT DEFAULT 1,
    tags            TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE question (
    id              BIGSERIAL PRIMARY KEY,
    bank_id         BIGINT REFERENCES question_bank(id) ON DELETE CASCADE,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    stem            TEXT NOT NULL,
    options         TEXT,
    answer          TEXT NOT NULL,
    explanation     TEXT,
    difficulty      INT DEFAULT 1,
    created_by      VARCHAR(20) DEFAULT 'preset',
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE srs_card (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE CASCADE,
    interval_days   INT DEFAULT 1,
    ease_factor     FLOAT DEFAULT 2.5,
    review_count    INT DEFAULT 0,
    correct_streak  INT DEFAULT 0,
    next_review_at  TIMESTAMP,
    last_reviewed   TIMESTAMP,
    UNIQUE(user_id, kp_id)
);

CREATE TABLE mistake_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    question_id     BIGINT REFERENCES question(id) ON DELETE SET NULL,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE SET NULL,
    user_answer     TEXT,
    correct_answer  TEXT,
    question_type   VARCHAR(20),
    reviewed        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE practice_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    bank_id         BIGINT REFERENCES question_bank(id),
    question_type   VARCHAR(20),
    total_count     INT DEFAULT 0,
    correct_count   INT DEFAULT 0,
    started_at      TIMESTAMP DEFAULT NOW(),
    finished_at     TIMESTAMP
);
