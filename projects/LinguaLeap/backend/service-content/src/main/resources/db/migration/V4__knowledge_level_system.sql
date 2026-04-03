-- Phase 3a: 知识库体系 + 分级学习

-- 1. 级别表
CREATE TABLE knowledge_level (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(10) NOT NULL UNIQUE,       -- L1, L2, ... L9
    name        VARCHAR(50) NOT NULL,              -- 小学三年级
    description TEXT,
    grade_group VARCHAR(20) NOT NULL,              -- primary / junior / senior
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 2. 单元表
CREATE TABLE knowledge_unit (
    id          BIGSERIAL PRIMARY KEY,
    level_id    BIGINT NOT NULL REFERENCES knowledge_level(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,             -- Unit 1: Greetings
    description TEXT,
    topic       VARCHAR(50),                       -- greetings, colors, food...
    sort_order  INT NOT NULL DEFAULT 0,
    kp_count    INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 3. 知识点表增加 level_id, unit_id
ALTER TABLE knowledge_point ADD COLUMN IF NOT EXISTS level_id BIGINT REFERENCES knowledge_level(id);
ALTER TABLE knowledge_point ADD COLUMN IF NOT EXISTS unit_id BIGINT REFERENCES knowledge_unit(id);

CREATE INDEX IF NOT EXISTS idx_kp_level ON knowledge_point(level_id);
CREATE INDEX IF NOT EXISTS idx_kp_unit ON knowledge_point(unit_id);

-- 4. 学习进度表
CREATE TABLE learning_progress (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE CASCADE,
    unit_id         BIGINT REFERENCES knowledge_unit(id) ON DELETE CASCADE,
    level_id        BIGINT REFERENCES knowledge_level(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'new',    -- new / learning / mastered
    review_count    INT DEFAULT 0,
    last_review_at  TIMESTAMP,
    next_review_at  TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, kp_id)
);

CREATE INDEX IF NOT EXISTS idx_lp_user_level ON learning_progress(user_id, level_id);
CREATE INDEX IF NOT EXISTS idx_lp_user_unit ON learning_progress(user_id, unit_id);

-- 5. 初始化 9 个级别
INSERT INTO knowledge_level (code, name, description, grade_group, sort_order) VALUES
('L1', '小学三年级', '字母、问候、颜色、数字、动物、家庭', 'primary', 1),
('L2', '小学四年级', '食物、天气、时间、身体、教室', 'primary', 2),
('L3', '小学五年级', '日常活动、方位、职业、季节', 'primary', 3),
('L4', '小学六年级', '爱好、交通、节日、比较级初步', 'primary', 4),
('L5', '初一', '基础句型、一般时态、日常对话', 'junior', 5),
('L6', '初二', '过去时、进行时、被动语态入门', 'junior', 6),
('L7', '初三', '完成时、复合句、阅读词汇', 'junior', 7),
('L8', '高一高二', '从句、虚拟语气、学术词汇', 'senior', 8),
('L9', '高三', '高考核心词、写作高级词汇', 'senior', 9);
