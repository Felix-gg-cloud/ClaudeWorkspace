-- Phase 5a: 扩展 student_profile 为 Learner Model
-- 新增字段用于支持教学编排引擎

-- 学生当前级别码（L3-L12），由评估或编排引擎设定
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS level_code VARCHAR(10);

-- 知识点掌握度快照（JSON 格式）
-- 结构: {"L7_grammar_passive_voice": 0.6, "L7_vocab_abandon": 0.9, ...}
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS kp_mastery TEXT;

-- 薄弱标签（JSON 数组），编排引擎据此决定复习内容
-- 结构: ["被动语态", "完成时", "词汇:abandon"]
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS weak_tags TEXT;

-- i+1 参数（JSON），控制扩展词汇/语法的投放量
-- 结构: {"vocab_extend_count": 5, "grammar_extend": false, "target_correct_rate": 0.7}
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS i1_params TEXT;

-- 累计学习统计
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS total_sessions INT DEFAULT 0;
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS total_correct INT DEFAULT 0;
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS total_answered INT DEFAULT 0;

-- 上次学习的阶段状态（JSON），编排引擎用于恢复上下文
-- 结构: {"phase": "practice", "current_kps": ["passive_voice"], "remaining": 2}
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS last_session_state TEXT;

CREATE INDEX IF NOT EXISTS idx_sp_level ON student_profile(level_code);
