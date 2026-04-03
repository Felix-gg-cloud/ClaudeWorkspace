CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    grade           VARCHAR(20) NOT NULL DEFAULT 'junior',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE daily_stats (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    stat_date       DATE NOT NULL,
    tasks_completed INT DEFAULT 0,
    correct_count   INT DEFAULT 0,
    wrong_count     INT DEFAULT 0,
    words_learned   INT DEFAULT 0,
    study_minutes   INT DEFAULT 0,
    UNIQUE(user_id, stat_date)
);
