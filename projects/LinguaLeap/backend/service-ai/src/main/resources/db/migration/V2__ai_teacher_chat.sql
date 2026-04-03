-- Phase T1: AI 老师对话系统

-- 对话会话
CREATE TABLE chat_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    type            VARCHAR(30) NOT NULL DEFAULT 'chat',  -- chat / assessment
    title           VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'active', -- active / closed
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_chat_session_user ON chat_session(user_id);
CREATE INDEX idx_chat_session_user_type ON chat_session(user_id, type);

-- 对话消息
CREATE TABLE chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,   -- system / assistant / user
    content         TEXT NOT NULL,
    msg_type        VARCHAR(30) DEFAULT 'text', -- text / quiz / quiz_result / assessment_result
    metadata        TEXT,                   -- JSON: 额外数据（题目信息、评估结果等）
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_chat_message_session ON chat_message(session_id);

-- 学生画像
CREATE TABLE student_profile (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT UNIQUE NOT NULL,
    vocabulary_level VARCHAR(20),           -- beginner / elementary / intermediate / upper / advanced
    grammar_level   VARCHAR(20),
    listening_level VARCHAR(20),
    interests       TEXT,                   -- JSON 数组: ["games","sports","music"]
    weak_points     TEXT,                   -- JSON 数组: ["介词搭配","时态"]
    strong_points   TEXT,                   -- JSON 数组: ["词汇量","阅读"]
    learning_style  VARCHAR(50),            -- visual / auditory / kinesthetic
    self_description TEXT,                  -- 用户的自我描述
    ai_assessment   TEXT,                   -- AI 的综合评估（自然语言）
    assessed_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_student_profile_user ON student_profile(user_id);
