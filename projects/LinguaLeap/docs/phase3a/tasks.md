# Phase 3a：知识库体系 + 分级学习 ✅

> 目标：将“刷题工具”升级为“学习系统”——建立分级知识库体系，知识点由 AI 生成，学习页面用于掌握知识，练习用于巩固。
> 前置完成：Phase 2b（年级自适应题型）
> 状态：已完成（含 Phase 3a+ 预制内容）
> 创建日期：2026-03-30

---

## 核心理念转变

```
旧：题库(单词列表) → AI出题 → 练习
新：分级知识库(体系化) → 学习掌握 → 练习巩固 → 进阶
```

---

## 一、分级体系（9 级）

| 级别 | 对应 | 累计词汇 | 核心内容 |
|------|------|---------|---------|
| L1 | 小学三年级 | ~150 | 字母、问候、颜色、数字、动物、家庭 |
| L2 | 小学四年级 | ~300 | 食物、天气、时间、身体、教室 |
| L3 | 小学五年级 | ~500 | 日常活动、方位、职业、季节 |
| L4 | 小学六年级 | ~700 | 爱好、交通、节日、比较级初步 |
| L5 | 初一 | ~1000 | 基础句型、一般时态、日常对话 |
| L6 | 初二 | ~1400 | 过去时、进行时、被动语态入门 |
| L7 | 初三 | ~1800 | 完成时、复合句、阅读词汇 |
| L8 | 高一高二 | ~2800 | 从句、虚拟语气、学术词汇 |
| L9 | 高三 | ~3500 | 高考核心词、写作高级词汇 |

---

## 二、知识点类型

| type | 名称 | 说明 | 适用级别 | 学习形式 |
|------|------|------|---------|---------|
| `word` | 单词 | 单词+音标+释义+例句 | 全部 | 卡片翻转 |
| `phrase` | 短语 | 词组+释义+例句 | L2+ | 卡片翻转 |
| `sentence` | 日常用语 | 实用句子+中文+场景 | L1~L4 | 对话卡片 |

> 后续扩展：phonics（自然拼读）、grammar（语法）、dialogue（对话）、reading（阅读）

---

## 三、数据模型

### 新增表

```sql
-- 级别表
knowledge_level (id, code, name, description, grade_group, sort_order, created_at)

-- 单元表
knowledge_unit (id, level_id, name, description, topic, sort_order, kp_count, created_at)

-- 学习进度表
learning_progress (id, user_id, kp_id, unit_id, level_id, status, review_count, last_review_at, next_review_at, created_at)
```

### 改造表

```sql
-- knowledge_point 增加 unit_id、level_id
ALTER TABLE knowledge_point ADD COLUMN unit_id BIGINT;
ALTER TABLE knowledge_point ADD COLUMN level_id BIGINT;
```

---

## 四、任务清单

| # | 任务 | 类型 | 状态 |
|---|------|------|------|
| 3a.1 | 数据库 migration V4 | 后端 | ✅ |
| 3a.2 | 新增 Entity 类 (KnowledgeLevel, KnowledgeUnit, LearningProgress) | 后端 | ✅ |
| 3a.3 | 新增 Repository 接口 | 后端 | ✅ |
| 3a.4 | 新增 Service 层 (LevelService, UnitService, LearningService) | 后端 | ✅ |
| 3a.5 | 新增 Controller API | 后端 | ✅ |
| 3a.6 | AI 知识库生成 Prompt + 接口 | AI 服务 | ✅ |
| 3a.7 | 前端 API + 路由 | 前端 | ✅ |
| 3a.8 | 知识库总览页 (LevelListView) | 前端 | ✅ |
| 3a.9 | 学习模式页 (LearnView) | 前端 | ✅ |
| 3a.10 | 更新项目文档 | 文档 | ✅ |
