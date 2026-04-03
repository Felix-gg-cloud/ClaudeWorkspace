-- Phase 2b: 扩展 question 表，支持年级自适应题型
ALTER TABLE question ADD COLUMN IF NOT EXISTS knowledge_points TEXT;    -- 知识点解析 JSON
ALTER TABLE question ADD COLUMN IF NOT EXISTS words TEXT;               -- 涉及的单词详情 JSON [{word,phonetic,meaning}]
ALTER TABLE question ADD COLUMN IF NOT EXISTS example_sentence TEXT;    -- 例句
ALTER TABLE question ADD COLUMN IF NOT EXISTS example_zh TEXT;          -- 例句翻译
ALTER TABLE question ADD COLUMN IF NOT EXISTS extra_data TEXT;          -- 扩展数据 JSON（排序题词序等）
ALTER TABLE question ADD COLUMN IF NOT EXISTS grade VARCHAR(20);       -- 适用年级 primary/junior/senior
