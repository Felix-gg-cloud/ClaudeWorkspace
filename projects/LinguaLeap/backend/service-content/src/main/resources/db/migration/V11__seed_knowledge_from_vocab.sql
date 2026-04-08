-- V11: 从 vocab_constraint 导入知识点，填充知识库
-- 为有词汇数据的级别 (L3,L4,L5,L6,L7,L10) 创建单元和知识点
-- 为无词汇数据的级别 (L8,L9,L11,L12) 创建空白单元占位

-- 清理旧数据（如有残留）
DELETE FROM knowledge_point WHERE unit_id IS NOT NULL;
DELETE FROM knowledge_unit;

-- ============================================================
-- 1. 为有 vocab_constraint 数据的级别批量创建单元+知识点
-- ============================================================
DO $$
DECLARE
    lvl RECORD;
    unit_size INT := 20;  -- 每单元 20 个词
    total_words INT;
    num_units INT;
    new_unit_id BIGINT;
    i INT;
    word_offset INT;
BEGIN
    FOR lvl IN
        SELECT kl.id AS level_id, kl.code, kl.name AS level_name
        FROM knowledge_level kl
        WHERE kl.code IN ('L3','L4','L5','L6','L7','L10')
        ORDER BY kl.sort_order
    LOOP
        -- 统计该级别词汇总数
        SELECT COUNT(*) INTO total_words
        FROM vocab_constraint WHERE level_code = lvl.code;

        IF total_words = 0 THEN
            CONTINUE;
        END IF;

        num_units := CEIL(total_words::FLOAT / unit_size);

        FOR i IN 1..num_units LOOP
            word_offset := (i - 1) * unit_size;

            -- 创建单元
            INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count)
            VALUES (
                lvl.level_id,
                'Unit ' || i,
                lvl.level_name || ' 词汇第 ' || i || ' 组',
                'vocab_' || i,
                i,
                LEAST(unit_size, total_words - word_offset)
            )
            RETURNING id INTO new_unit_id;

            -- 从 vocab_constraint 导入词汇作为 knowledge_point
            INSERT INTO knowledge_point (level_id, unit_id, type, content, meaning_zh, difficulty, tags)
            SELECT
                lvl.level_id,
                new_unit_id,
                CASE
                    WHEN vc.pos IN ('n.','noun','n.&v.','noun/verb','verb/noun','adj.&n.','noun/adjective','adjective/noun','adverb/noun','noun/adverb','noun/numeral') THEN 'word'
                    WHEN vc.pos IN ('v.','verb','modal v.','v.&n.','verb/auxiliary','verb/modal','verb/preposition','adjective/verb') THEN 'word'
                    WHEN vc.pos IN ('adj.','adjective','adv.','adverb','adj.&adv.','adjective/adverb','adjective/pronoun') THEN 'word'
                    WHEN vc.pos IN ('phr.','phrase') THEN 'phrase'
                    WHEN vc.pos IN ('pron.','pronoun','prep.','preposition','conj.','conjunction','num.','numeral','int.','interjection','article') THEN 'word'
                    WHEN vc.pos IN ('adverb/conjunction','adverb/preposition','conjunction/preposition','preposition/adjective','interjection/noun','interjection/noun/verb') THEN 'word'
                    ELSE 'word'
                END,
                vc.word,
                vc.meaning_zh,
                CASE
                    WHEN lvl.code IN ('L3','L4') THEN 1
                    WHEN lvl.code IN ('L5','L6') THEN 2
                    WHEN lvl.code IN ('L7') THEN 3
                    WHEN lvl.code IN ('L10') THEN 4
                    ELSE 3
                END,
                vc.pos
            FROM (
                SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS rn
                FROM vocab_constraint
                WHERE level_code = lvl.code
            ) vc
            WHERE vc.rn > word_offset AND vc.rn <= word_offset + unit_size;
        END LOOP;

        RAISE NOTICE 'Level % (%) : % units, % words', lvl.code, lvl.level_name, num_units, total_words;
    END LOOP;
END $$;

-- ============================================================
-- 2. 为无词汇数据的级别 (L8, L9, L11, L12) 创建主题单元
-- ============================================================

-- L8 八年级
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 1: Past Tense', '一般过去时', 'past_tense', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 2: Progressive Tense', '进行时态', 'progressive', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 3: Passive Voice', '被动语态入门', 'passive', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 4: Daily Expressions', '日常表达', 'expressions', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 5: Reading & Writing', '阅读与写作', 'reading', 5, 0);

-- L9 九年级
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 1: Present Perfect', '现在完成时', 'perfect_tense', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 2: Compound Sentences', '复合句', 'compound', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 3: Reading Vocabulary', '阅读核心词汇', 'reading', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 4: Writing Basics', '写作基础', 'writing', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 5: Exam Preparation', '中考备考', 'exam', 5, 0);

-- L11 高二
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L11'), 'Unit 1: Advanced Grammar', '高级语法', 'adv_grammar', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L11'), 'Unit 2: Long Sentences', '长难句分析', 'long_sentences', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L11'), 'Unit 3: Essay Writing', '议论文写作', 'essay', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L11'), 'Unit 4: Academic Reading', '学术阅读', 'academic', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L11'), 'Unit 5: Listening & Speaking', '听说训练', 'listening', 5, 0);

-- L12 高三
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L12'), 'Unit 1: Core Vocabulary', '高考核心词', 'core_vocab', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L12'), 'Unit 2: Advanced Vocabulary', '高级写作词汇', 'adv_vocab', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L12'), 'Unit 3: Comprehensive Grammar', '语法综合', 'grammar', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L12'), 'Unit 4: Reading Comprehension', '阅读理解', 'reading', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L12'), 'Unit 5: Mock Exam Practice', '模拟考试', 'mock_exam', 5, 0);

-- ============================================================
-- 3. 更新单元名称为更有意义的名字（基于每单元首尾词）
-- ============================================================
DO $$
DECLARE
    u RECORD;
    first_word TEXT;
    last_word TEXT;
BEGIN
    FOR u IN
        SELECT ku.id, ku.level_id, ku.sort_order
        FROM knowledge_unit ku
        WHERE ku.kp_count > 0
        ORDER BY ku.level_id, ku.sort_order
    LOOP
        SELECT content INTO first_word
        FROM knowledge_point
        WHERE unit_id = u.id
        ORDER BY id ASC LIMIT 1;

        SELECT content INTO last_word
        FROM knowledge_point
        WHERE unit_id = u.id
        ORDER BY id DESC LIMIT 1;

        IF first_word IS NOT NULL AND last_word IS NOT NULL THEN
            UPDATE knowledge_unit
            SET name = 'Unit ' || u.sort_order || ': ' || first_word || ' ~ ' || last_word
            WHERE id = u.id;
        END IF;
    END LOOP;
END $$;
