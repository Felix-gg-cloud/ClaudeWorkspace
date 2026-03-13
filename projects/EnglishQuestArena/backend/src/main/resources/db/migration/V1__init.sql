-- V1__init.sql — EnglishQuestArena 初始数据库结构

-- ===================== 用户相关 =====================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) DEFAULT 'Hero',
    tts_voice VARCHAR(10) DEFAULT 'en-US',
    cefr_level VARCHAR(10) DEFAULT 'PRE_A1',
    current_level INT DEFAULT 1,
    total_xp INT DEFAULT 0,
    xp_to_next_level INT DEFAULT 200,
    coins INT DEFAULT 0,
    skill_points INT DEFAULT 0,
    streak INT DEFAULT 0,
    total_checkins INT DEFAULT 0,
    first_login BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE checkin_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    checkin_date DATE NOT NULL,
    streak INT NOT NULL,
    xp_earned INT DEFAULT 0,
    coins_earned INT DEFAULT 0,
    UNIQUE(user_id, checkin_date)
);

-- ===================== 内容相关 =====================

CREATE TABLE chapters (
    code VARCHAR(50) PRIMARY KEY,
    cefr_level VARCHAR(10) NOT NULL,
    title_en VARCHAR(200) NOT NULL,
    title_zh VARCHAR(200) NOT NULL,
    order_index INT NOT NULL,
    days INT NOT NULL,
    camp_unlock_rate DECIMAL(3,2) DEFAULT 0.80
);

CREATE TABLE lessons (
    code VARCHAR(50) PRIMARY KEY,
    chapter_code VARCHAR(50) NOT NULL REFERENCES chapters(code),
    day_index INT NOT NULL,
    title_en VARCHAR(200),
    title_zh VARCHAR(200),
    estimated_minutes INT DEFAULT 30,
    target_task_count INT DEFAULT 30,
    auto_drill_enabled BOOLEAN DEFAULT TRUE
);

CREATE TABLE content_items (
    code VARCHAR(50) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    cefr_level VARCHAR(10) NOT NULL,
    chapter_code VARCHAR(50) NOT NULL REFERENCES chapters(code),
    day_index INT DEFAULT 1,
    text_en VARCHAR(200) NOT NULL,
    text_zh VARCHAR(200) NOT NULL,
    ipa VARCHAR(100),
    example_en TEXT,
    example_zh TEXT,
    distractors TEXT,
    tags TEXT,
    source_name VARCHAR(100),
    source_url VARCHAR(500),
    source_note VARCHAR(200)
);

CREATE TABLE tasks (
    code VARCHAR(100) PRIMARY KEY,
    lesson_code VARCHAR(50) REFERENCES lessons(code),
    order_index INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    prompt_en TEXT NOT NULL,
    prompt_zh_hint TEXT,
    options TEXT,
    answer TEXT NOT NULL,
    explanation_en TEXT,
    explanation_zh TEXT,
    xp_reward INT DEFAULT 5,
    gold_reward INT DEFAULT 0,
    tts_enabled BOOLEAN DEFAULT FALSE,
    tts_text_en VARCHAR(500),
    content_item_codes TEXT
);

-- Boss 配置
CREATE TABLE boss_configs (
    code VARCHAR(50) PRIMARY KEY,
    chapter_code VARCHAR(50) NOT NULL REFERENCES chapters(code),
    boss_name VARCHAR(100) NOT NULL,
    boss_title VARCHAR(200),
    boss_hp INT DEFAULT 100,
    player_hp INT DEFAULT 100,
    time_limit_sec INT DEFAULT 300,
    damage_correct INT DEFAULT 15,
    damage_wrong INT DEFAULT 20
);

-- Boss 题池
CREATE TABLE boss_pool_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    boss_code VARCHAR(50) NOT NULL REFERENCES boss_configs(code),
    task_code VARCHAR(100) NOT NULL,
    task_type VARCHAR(30) NOT NULL,
    prompt_en TEXT NOT NULL,
    prompt_zh_hint TEXT,
    options TEXT,
    answer TEXT NOT NULL,
    explanation_en TEXT,
    explanation_zh TEXT,
    tts_enabled BOOLEAN DEFAULT FALSE,
    tts_text_en VARCHAR(500),
    content_item_codes TEXT
);

-- ===================== 进度相关 =====================

CREATE TABLE chapter_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    chapter_code VARCHAR(50) NOT NULL REFERENCES chapters(code),
    phase VARCHAR(20) DEFAULT 'locked',
    boss_defeated BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, chapter_code)
);

CREATE TABLE camp_defeated (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    chapter_code VARCHAR(50) NOT NULL,
    encounter_id VARCHAR(100) NOT NULL,
    defeated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    attempts INT DEFAULT 1,
    UNIQUE(user_id, encounter_id)
);

CREATE TABLE quest_day_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    lesson_code VARCHAR(50) NOT NULL REFERENCES lessons(code),
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    UNIQUE(user_id, lesson_code)
);

CREATE TABLE task_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    task_key VARCHAR(100) NOT NULL,
    correct BOOLEAN NOT NULL,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    time_spent_ms INT
);
CREATE INDEX idx_task_progress_user_task ON task_progress(user_id, task_key);

CREATE TABLE srs_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    content_item_code VARCHAR(50) NOT NULL REFERENCES content_items(code),
    ease_factor DECIMAL(4,2) DEFAULT 2.50,
    interval_days INT DEFAULT 1,
    repetitions INT DEFAULT 0,
    next_review_date DATE NOT NULL,
    total_reviews INT DEFAULT 0,
    total_correct INT DEFAULT 0,
    last_reviewed_at TIMESTAMP,
    UNIQUE(user_id, content_item_code)
);

CREATE TABLE boss_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    boss_code VARCHAR(50) NOT NULL,
    attempt_date DATE NOT NULL,
    victory BOOLEAN NOT NULL,
    boss_hp_remaining INT,
    player_hp_remaining INT,
    max_combo INT,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, boss_code, attempt_date)
);

CREATE TABLE skill_unlocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    skill_code VARCHAR(50) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, skill_code)
);

CREATE TABLE cefr_exam_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    cefr_level VARCHAR(10) NOT NULL,
    attempt_date DATE NOT NULL,
    score DECIMAL(5,2),
    passed BOOLEAN NOT NULL,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seed_import_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(50),
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
