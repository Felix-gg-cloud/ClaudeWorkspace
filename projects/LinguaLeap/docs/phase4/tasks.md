# Phase 4：用户内容引擎 + AI 智能学习集

> 目标：用户上传自己的学习材料（PDF/文本/词表），AI 自动提取、分类、制定出题策略，生成个性化练习。
> 前置完成：Phase 0-3a（基础架构、练习系统、AI 出题、SRS 复习全部就绪）
> 核心理念：从"平台预制内容"转向"用户内容驱动"

---

## 核心架构

```
用户上传内容           用户备注/诉求
  (PDF/文本/词表)       ("下周考试，重点背单词")
        ↓                    ↓
   ┌─────────────────────────────────┐
   │         AI 内容引擎              │
   │  1. 文本提取 (PDF→文本)          │
   │  2. 知识点提取 + 自动分类        │
   │     → 词汇 (word/phrase)        │
   │     → 语法 (grammar)            │
   │     → 句型 (sentence pattern)   │
   │     → 阅读段落 (passage)        │
   │  3. 策略生成 (内容+备注→权重)    │
   └─────────────────────────────────┘
        ↓
   学习集 (StudySet)
   ├── 学习项 (LearningItem) × N
   ├── 出题策略 (JSON)
   └── AI 总结
        ↓
   按策略自动出题 → 练习 → SRS 复习
```

---

## 数据模型

### study_set（学习集）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT NOT NULL | 所属用户 |
| title | VARCHAR(200) | 学习集名称 |
| description | TEXT | 用户备注/诉求 |
| source_type | VARCHAR(20) | text / pdf / wordlist |
| source_text | TEXT | 原始文本（text/wordlist 类型保存） |
| source_file_url | VARCHAR(500) | 本地文件路径（PDF 类型） |
| grade | VARCHAR(20) | 年级 |
| status | VARCHAR(20) | processing / ready / failed |
| ai_summary | TEXT | AI 对内容的总结 |
| ai_strategy | JSONB | AI 出题策略 |
| item_count | INT DEFAULT 0 | 学习项数量 |
| question_count | INT DEFAULT 0 | 已生成题目数 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### learning_item（学习项 — 从上传内容提取）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | |
| study_set_id | BIGINT FK | 所属学习集 |
| user_id | BIGINT NOT NULL | |
| category | VARCHAR(30) | vocabulary / grammar / sentence_pattern / passage |
| content | VARCHAR(500) | 英文内容 |
| meaning_zh | VARCHAR(500) | 中文释义 |
| phonetic | VARCHAR(100) | 音标 |
| example_sentence | TEXT | 例句 |
| example_zh | TEXT | 例句翻译 |
| extra_data | JSONB | 扩展（语法规则、句型结构等） |
| difficulty | INT DEFAULT 1 | 1-5 |
| ai_note | TEXT | AI 对该项的备注（为什么提取、重点提示） |
| created_at | TIMESTAMP | |

### ai_strategy JSON 结构示例

```json
{
  "focus": "vocabulary",
  "reasoning": "用户备注重点是背单词备考，内容以课文词汇为主",
  "weights": {
    "vocabulary": 0.6,
    "grammar": 0.2,
    "sentence_pattern": 0.15,
    "passage": 0.05
  },
  "questionTypePreference": {
    "en2zh_choice": 0.3,
    "zh2en_choice": 0.3,
    "fill_blank": 0.2,
    "translate": 0.2
  },
  "totalRecommended": 30,
  "dailyTarget": 10
}
```

---

## 任务总览

| # | 任务 | 类型 | 依赖 | 状态 |
|---|------|------|------|------|
| 4.1 | [数据库 Migration V8](#41-数据库-migration-v8) | 后端 | — | ✅ |
| 4.2 | [StudySet + LearningItem Entity/Repo](#42-entity-和-repository) | 后端 | 4.1 | ✅ |
| 4.3 | [本地文件存储 + PDF 提取](#43-文件存储与pdf提取) | 后端 | — | ✅ |
| 4.4 | [AI 内容提取 + 分类 Prompt](#44-ai-内容提取分类-prompt) | AI | — | ✅ |
| 4.5 | [AI 出题策略生成 Prompt](#45-ai-策略生成-prompt) | AI | 4.4 | ✅ |
| 4.6 | [StudySet 服务层 + 上传管线](#46-studyset-服务层) | 后端 | 4.2,4.3,4.4,4.5 | ✅ |
| 4.7 | [StudySet Controller API](#47-controller-api) | 后端 | 4.6 | ✅ |
| 4.8 | [出题系统对接学习集](#48-出题系统对接) | 后端 | 4.6 | ✅ |
| 4.9 | [前端：上传页面 (UploadView)](#49-前端上传页面) | 前端 | 4.7 | ✅ |
| 4.10 | [前端：学习集详情页 (StudySetView)](#410-前端学习集详情页) | 前端 | 4.7 | ✅ |
| 4.11 | [前端：Dashboard 引导卡片](#411-dashboard-引导卡片) | 前端 | 4.9 | ✅ |
| 4.12 | [联调测试](#412-联调测试) | 测试 | ALL | ✅ |

---

## 详细任务

### 4.1 数据库 Migration V8

**文件**: `service-content/src/main/resources/db/migration/V8__study_set.sql`

创建 `study_set` 和 `learning_item` 表，加索引。

### 4.2 Entity 和 Repository

- `StudySet.java` — JPA 实体
- `LearningItem.java` — JPA 实体
- `StudySetRepository.java` — findByUserId, countByUserId
- `LearningItemRepository.java` — findByStudySetId, countByStudySetIdAndCategory

### 4.3 本地文件存储 + PDF 提取

- `FileStorageService.java` — 本地文件存储封装 (upload/download/delete)
- `PdfExtractorService.java` — PDFBox 提取 PDF 文本
- 存储目录: `uploads/` （可通过 application.yml 配置）
- 无需外部存储服务，简单可靠

### 4.4 AI 内容提取 + 分类 Prompt

在 service-ai 的 PromptTemplates 中新增：

**`contentExtract` Prompt**：
- 输入：原始文本 + 年级 + 用户备注
- 输出：分类后的学习项列表
- 分类：vocabulary / grammar / sentence_pattern / passage
- 每项包含：content, meaningZh, phonetic, example, difficulty, category, aiNote

关键：用户备注影响提取侧重点。

### 4.5 AI 策略生成 Prompt

**`studyStrategy` Prompt**：
- 输入：提取结果统计 + 用户备注 + 年级
- 输出：ai_strategy JSON（各分类权重、题型偏好、每日目标）

### 4.6 StudySet 服务层

`StudySetService.java` 核心方法：

```java
// 完整上传管线（异步处理）
StudySet createFromText(userId, title, text, note, grade)
StudySet createFromFile(userId, title, file, note, grade)

// Step 1: 保存原始内容 → status=processing
// Step 2: AI 提取+分类 → 保存 LearningItem
// Step 3: AI 策略生成 → 保存 ai_strategy
// Step 4: status=ready
```

### 4.7 Controller API

```
POST   /api/content/study-sets              创建(文本)
POST   /api/content/study-sets/upload       创建(文件上传)
GET    /api/content/study-sets              我的学习集列表
GET    /api/content/study-sets/:id          学习集详情(含学习项)
DELETE /api/content/study-sets/:id          删除
POST   /api/content/study-sets/:id/generate 为学习集生成练习题
```

### 4.8 出题系统对接

- 修改 PracticeService：支持 `studySetId` 参数
- 开始练习时按 `ai_strategy` 权重选知识点 + 分配题型
- LearningItem 需要映射为 KnowledgePoint 或直接作为出题输入

### 4.9 前端：上传页面

**路由**: `/upload`
**核心交互**:
1. 选择上传方式（粘贴文本 / 上传文件 / 输入词表）
2. 填写标题 + 备注/诉求
3. 年级自动从用户画像填入
4. 提交 → 显示处理进度
5. 完成 → 跳转学习集详情

### 4.10 前端：学习集详情页

**路由**: `/study-sets/:id`
**展示**:
- AI 总结 + 出题策略可视化
- 分类标签页（词汇/语法/句型/阅读）
- 每个分类下的学习项卡片
- "开始练习"按钮 → 跳转 PracticeView

### 4.11 Dashboard 引导卡片

新用户(无学习集)时显示：
- 引导卡片组："上传 PDF" / "粘贴文本" / "输入词表"
- 简洁的使用说明

有学习集时显示：
- 最近学习集 + 进度
- "继续练习" / "上传新内容"

### 4.12 联调测试

- 文本上传 → AI 提取 → 查看学习集 → 开始练习 → 全流程
- 不同年级、不同备注测试 AI 策略差异
- 错误处理：AI 失败、空内容、超长文本

---

## 开发顺序

```
第一批（并行）：4.1 + 4.3 + 4.4 + 4.5
    ↓
第二批：4.2 → 4.6 → 4.7
    ↓
第三批（并行）：4.8 + 4.9 + 4.10
    ↓
第四批：4.11 → 4.12
```

---

## 与旧系统的关系

| 旧组件 | Phase 4 策略 |
|--------|-------------|
| Level/Unit 体系 | 保留但不再是主入口，未来考虑废弃 |
| 预制题库 | 保留作为"官方示例"，不再扩展 |
| KnowledgePoint 表 | 继续用于预制内容；新内容用 LearningItem |
| AI 出题 Prompt | 复用，但需适配 LearningItem 输入 |
| SRS 复习 | 直接复用，learning_item 的 kpId 映射方式待定 |
| 错题本/统计 | 直接复用，不变 |
