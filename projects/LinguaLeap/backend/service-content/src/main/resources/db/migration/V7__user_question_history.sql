-- 用户做题历史：记录每个用户做过哪些题，用于混合出题策略（排除已做过的题，优先用已有题目）
CREATE TABLE user_question_history (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    correct     BOOLEAN NOT NULL DEFAULT FALSE,
    practiced_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_uqh_user_question ON user_question_history(user_id, question_id);
CREATE INDEX idx_uqh_user ON user_question_history(user_id);
