-- V6: 为每个级别创建默认学习单元

-- L1 小学三年级
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L1'), 'Unit 1: Letters & Greetings', '字母与问候语', 'greetings', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L1'), 'Unit 2: Colors & Shapes', '颜色与形状', 'colors', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L1'), 'Unit 3: Numbers 1-20', '数字 1-20', 'numbers', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L1'), 'Unit 4: Animals', '动物名称', 'animals', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L1'), 'Unit 5: My Family', '家庭成员', 'family', 5, 0);

-- L2 小学四年级
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L2'), 'Unit 1: Food & Drinks', '食物与饮料', 'food', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L2'), 'Unit 2: Weather', '天气表达', 'weather', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L2'), 'Unit 3: Time & Daily Routine', '时间与日常活动', 'time', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L2'), 'Unit 4: My Body', '身体部位', 'body', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L2'), 'Unit 5: In the Classroom', '教室里', 'classroom', 5, 0);

-- L3 小学五年级
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L3'), 'Unit 1: Daily Activities', '日常活动', 'activities', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L3'), 'Unit 2: Directions & Places', '方位与地点', 'directions', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L3'), 'Unit 3: Jobs & Occupations', '职业', 'jobs', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L3'), 'Unit 4: Seasons & Holidays', '季节与节日', 'seasons', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L3'), 'Unit 5: Shopping', '购物', 'shopping', 5, 0);

-- L4 小学六年级
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L4'), 'Unit 1: Hobbies & Sports', '爱好与运动', 'hobbies', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L4'), 'Unit 2: Transportation', '交通工具', 'transportation', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L4'), 'Unit 3: Festivals Around the World', '世界节日', 'festivals', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L4'), 'Unit 4: Comparing Things', '比较级初步', 'comparisons', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L4'), 'Unit 5: My Dream', '我的梦想', 'dreams', 5, 0);

-- L5 初一
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L5'), 'Unit 1: Self Introduction', '自我介绍与基础句型', 'introduction', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L5'), 'Unit 2: Present Tenses', '一般现在时与现在进行时', 'present_tense', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L5'), 'Unit 3: School Life', '校园生活', 'school', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L5'), 'Unit 4: Daily Conversations', '日常对话', 'conversations', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L5'), 'Unit 5: Describing People', '描述人物', 'describing', 5, 0);

-- L6 初二
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L6'), 'Unit 1: Past Tense', '一般过去时', 'past_tense', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L6'), 'Unit 2: Future Plans', '将来时与计划', 'future', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L6'), 'Unit 3: Passive Voice Basics', '被动语态入门', 'passive', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L6'), 'Unit 4: Travel & Culture', '旅行与文化', 'travel', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L6'), 'Unit 5: Health & Lifestyle', '健康与生活方式', 'health', 5, 0);

-- L7 初三
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L7'), 'Unit 1: Present Perfect', '现在完成时', 'perfect_tense', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L7'), 'Unit 2: Compound Sentences', '复合句', 'compound', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L7'), 'Unit 3: Reading Vocabulary', '阅读核心词汇', 'reading', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L7'), 'Unit 4: Writing Basics', '写作基础', 'writing', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L7'), 'Unit 5: Exam Preparation', '考试备考', 'exam', 5, 0);

-- L8 高一高二
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 1: Clauses', '各类从句', 'clauses', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 2: Subjunctive Mood', '虚拟语气', 'subjunctive', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 3: Academic Vocabulary', '学术词汇', 'academic', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 4: Reading Comprehension', '阅读理解策略', 'reading_strategy', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L8'), 'Unit 5: Essay Writing', '作文写作', 'essay', 5, 0);

-- L9 高三
INSERT INTO knowledge_unit (level_id, name, description, topic, sort_order, kp_count) VALUES
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 1: Core Exam Vocabulary', '高考核心词汇', 'exam_vocab', 1, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 2: Advanced Writing', '高级写作词汇', 'adv_writing', 2, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 3: Idioms & Phrases', '常用短语与习语', 'idioms', 3, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 4: Listening Skills', '听力技巧', 'listening', 4, 0),
((SELECT id FROM knowledge_level WHERE code = 'L9'), 'Unit 5: Mock Exam Practice', '模拟考试', 'mock_exam', 5, 0);
