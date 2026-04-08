-- Phase 5a: 重建 knowledge_level 为 L3-L12 体系
-- 旧 L1-L9 数据已在上一轮清理中删除

-- 重新插入 L3-L12（每年级一个级别）
INSERT INTO knowledge_level (code, name, description, grade_group, sort_order) VALUES
('L3',  '三年级', '字母、问候、颜色、数字、动物、家庭', 'primary', 1),
('L4',  '四年级', '食物、天气、时间、身体、教室', 'primary', 2),
('L5',  '五年级', '日常活动、方位、职业、季节', 'primary', 3),
('L6',  '六年级', '爱好、交通、节日、比较级初步', 'primary', 4),
('L7',  '七年级', '基础句型、一般时态、日常对话', 'junior', 5),
('L8',  '八年级', '过去时、进行时、被动语态入门', 'junior', 6),
('L9',  '九年级', '完成时、复合句、阅读词汇', 'junior', 7),
('L10', '高一', '从句、虚拟语气、学术词汇', 'senior', 8),
('L11', '高二', '高级语法、长难句、议论文写作', 'senior', 9),
('L12', '高三', '高考核心词、写作高级词汇、综合应用', 'senior', 10)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    grade_group = EXCLUDED.grade_group,
    sort_order = EXCLUDED.sort_order;
