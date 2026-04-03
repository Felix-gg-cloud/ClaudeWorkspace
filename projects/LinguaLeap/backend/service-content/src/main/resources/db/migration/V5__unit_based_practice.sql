-- V5: 支持基于知识库单元的练习（脱离题库依赖）

-- practice_session 增加 unit_id 字段
ALTER TABLE practice_session ADD COLUMN IF NOT EXISTS unit_id BIGINT REFERENCES knowledge_unit(id);

-- question.bank_id 允许为空（知识库生成的题目不需要挂在题库下）
ALTER TABLE question ALTER COLUMN bank_id DROP NOT NULL;

-- 按 kp_id 查询题目的索引
CREATE INDEX IF NOT EXISTS idx_question_kp_id ON question(kp_id);
