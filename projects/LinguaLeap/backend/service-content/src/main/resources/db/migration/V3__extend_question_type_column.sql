-- 支持多题型逗号分隔存储，扩展 question_type 列宽
ALTER TABLE practice_session ALTER COLUMN question_type TYPE VARCHAR(100);
