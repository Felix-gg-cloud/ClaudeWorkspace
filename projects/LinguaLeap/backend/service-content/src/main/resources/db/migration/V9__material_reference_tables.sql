-- Phase 5a: 教学材料参考表（AI 教师约束数据）
-- 这些表使用 L3-L12 级别编码（每年级一个级别），与现有 knowledge_level(L1-L9) 暂独立
-- L3-L6 小学3-6年级 | L7-L9 初中七-九年级 | L10-L12 高中高一-高三

-- ============================================================
-- 1. 分级词汇约束表
--    AI 出题时必须从该表查询对应级别的词汇范围
-- ============================================================
CREATE TABLE vocab_constraint (
    id          BIGSERIAL PRIMARY KEY,
    word        VARCHAR(100) NOT NULL,
    pos         VARCHAR(30),                       -- 词性: noun, verb, adj, etc.
    meaning_zh  VARCHAR(500) NOT NULL,
    level_code  VARCHAR(10) NOT NULL,              -- L3-L12
    source_book VARCHAR(50) NOT NULL,              -- 三年级上册, 初中1600词, 高考3500词
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_vc_level ON vocab_constraint(level_code);
CREATE INDEX idx_vc_word ON vocab_constraint(word);

COMMENT ON TABLE vocab_constraint IS '分级词汇约束表 — AI 出题的词汇范围';
COMMENT ON COLUMN vocab_constraint.level_code IS '最低要求级别 L3-L12';

-- ============================================================
-- 2. 语法点参考表
--    AI 出题时根据级别查询可用的语法点
-- ============================================================
CREATE TABLE grammar_point_ref (
    id              BIGSERIAL PRIMARY KEY,
    grammar_point   VARCHAR(200) NOT NULL,
    level_code      VARCHAR(10) NOT NULL,          -- L3-L12
    stage           VARCHAR(20) NOT NULL,          -- 小学阶段, 初中阶段, 高中阶段
    level_title     VARCHAR(50),                   -- L3（三年级上册）
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_gpr_level ON grammar_point_ref(level_code);

COMMENT ON TABLE grammar_point_ref IS '语法点参考表 — AI 出题的语法约束';

-- ============================================================
-- 3. 黄金样本表（参考试卷）
--    AI 参考这些真实试卷来理解各级别的题目风格和难度
-- ============================================================
CREATE TABLE golden_sample (
    id              BIGSERIAL PRIMARY KEY,
    level_code      VARCHAR(10) NOT NULL,          -- L3-L12
    title           VARCHAR(200) NOT NULL,
    source_file     VARCHAR(200),
    content_text    TEXT NOT NULL,
    has_answer      BOOLEAN DEFAULT FALSE,
    char_count      INT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_gs_level ON golden_sample(level_code);

COMMENT ON TABLE golden_sample IS '黄金样本试卷 — AI 参考的真实考题';
