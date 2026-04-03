# Phase 1：题库系统 + 练习核心

> 目标：完整的题库管理 + 4 种题型练习 + SRS 复习 + 错题本。

---

## 任务总览

| # | 任务 | 类型 | 状态 |
|---|------|------|------|
| 1.1 | [预制词库数据](#11-预制词库数据) | 数据 | ✅ |
| 1.2 | [题库 CRUD API](#12-题库-crud-api) | 后端 | ✅ |
| 1.3 | [知识点 CRUD API](#13-知识点-crud-api) | 后端 | ✅ |
| 1.4 | [模板出题引擎](#14-模板出题引擎) | 后端 | ✅ |
| 1.5 | [练习会话 API](#15-练习会话-api) | 后端 | ✅ |
| 1.6 | [PDF 上传 + 本地存储](#16-pdf-上传--本地存储) | 后端 | ✅ |
| 1.7 | [PDF 文本提取](#17-pdf-文本提取) | 后端 | ✅ |
| 1.8 | [SRS 记忆系统 API](#18-srs-记忆系统-api) | 后端 | ✅ |
| 1.9 | [错题本 API](#19-错题本-api) | 后端 | ✅ |
| 1.10 | [每日统计 API](#110-每日统计-api) | 后端 | ✅ |
| 1.11 | [前端：题库列表/详情/创建](#111-前端题库列表详情创建) | 前端 | ✅ |
| 1.12 | [前端：统一练习界面](#112-前端统一练习界面) | 前端 | ✅ |
| 1.13 | [前端：练习结果页](#113-前端练习结果页) | 前端 | ✅ |
| 1.14 | [前端：复习中心](#114-前端复习中心) | 前端 | ✅ |
| 1.15 | [前端：错题本](#115-前端错题本) | 前端 | ✅ |
| 1.16 | [前端：统计页](#116-前端统计页) | 前端 | ✅ |

---

## 建议开发顺序

```
1.1 预制词库 ──────────────────────────────────────────┐
1.2 题库 CRUD ─────┬─► 1.11 前端：题库页面             │
1.3 知识点 CRUD ───┘                                    │
       │                                                │
       ▼                                                │
1.4 模板出题引擎 ──┬─► 1.12 前端：练习界面              │
1.5 练习会话 API ──┘   1.13 前端：练习结果页             │
       │                                                │
       ▼                                                │
1.6 PDF 上传 ──► 1.7 PDF 文本提取 ◄─────────────────────┘
       │
       ▼
1.8 SRS API ───────►  1.14 前端：复习中心
1.9 错题本 API ────►  1.15 前端：错题本
1.10 每日统计 API ──► 1.16 前端：统计页
```

---

## 1.1 预制词库数据

**目标**：准备小学/初中/高中各 100-200 个核心单词，作为预制题库入库。

**产出**：
- `backend/service-content/src/main/resources/data/primary_words.json`
- `backend/service-content/src/main/resources/data/junior_words.json`
- `backend/service-content/src/main/resources/data/senior_words.json`

**数据格式**：
```json
[
  {
    "content": "apple",
    "phonetic": "/ˈæpl/",
    "meaning_zh": "苹果",
    "example_sentence": "I like to eat apples.",
    "example_zh": "我喜欢吃苹果。",
    "difficulty": 1,
    "tags": ["noun", "food", "unit1"]
  }
]
```

**来源**：可参考公开的英语课标词汇表，人工整理。

**验收标准**：
- [ ] 小学 100+ 词（difficulty 1-2）
- [ ] 初中 150+ 词（difficulty 2-3）
- [ ] 高中 150+ 词（difficulty 3-5）
- [ ] 每个词有音标、中文释义、例句
- [ ] 启动时通过 DataLoader 自动导入（如果题库不存在）

---

## 1.2 题库 CRUD API

**目标**：service-content 实现题库的增删改查。

**API 设计**：
| 方法 | Path | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/content/banks` | 题库列表（支持筛选/分页） | ✅ |
| GET | `/api/content/banks/:id` | 题库详情 | ✅ |
| POST | `/api/content/banks` | 创建题库 | ✅ |
| PUT | `/api/content/banks/:id` | 更新题库 | ✅ (owner) |
| DELETE | `/api/content/banks/:id` | 删除题库 | ✅ (owner) |

**查询参数**：
- `grade` — 按年级筛选 (primary/junior/senior)
- `type` — 按类型筛选 (preset/user_upload)
- `keyword` — 名称模糊搜索
- `page` / `size` — 分页

**实现要点**：
- 预制题库 `user_id = null`，所有人可见
- 用户自建题库只有创建者可见/可编辑
- 返回 `kp_count`, `question_count` 统计字段

**验收标准**：
- [ ] 5 个 REST 接口可用
- [ ] 支持按 grade/type/keyword 筛选
- [ ] 分页返回正确
- [ ] 权限校验：只有 owner 可改/删自建题库
- [ ] 预制题库不可删除

---

## 1.3 知识点 CRUD API

**目标**：service-content 实现知识点（单词/短语/语法）的管理。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| GET | `/api/content/banks/:bankId/kps` | 知识点列表（分页） |
| GET | `/api/content/kps/:id` | 知识点详情 |
| POST | `/api/content/banks/:bankId/kps` | 添加知识点 |
| PUT | `/api/content/kps/:id` | 更新知识点 |
| DELETE | `/api/content/kps/:id` | 删除知识点 |
| POST | `/api/content/banks/:bankId/kps/batch` | 批量导入知识点 |

**查询参数**：
- `type` — word/phrase/grammar
- `keyword` — 搜索
- `difficulty` — 难度筛选

**验收标准**：
- [ ] 单个和批量 CRUD 可用
- [ ] 关联的题库 `kp_count` 自动更新
- [ ] 支持筛选和分页

---

## 1.4 模板出题引擎

**目标**：根据知识点自动生成 4 种题型（不依赖 AI，用模板规则生成）。

**4 种题型**：

| 题型 | type 字段 | 生成逻辑 |
|------|-----------|---------|
| 英译中选择 | `en2zh_choice` | 给英文，从同题库选 3 个干扰中文释义 |
| 中译英选择 | `zh2en_choice` | 给中文，从同题库选 3 个干扰英文 |
| 填空 | `fill_blank` | 用例句挖空目标单词 |
| 翻译 | `translate` | 给例句，要求翻译 |

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| POST | `/api/content/questions/generate` | 为指定知识点生成题目 |
| GET | `/api/content/banks/:bankId/questions` | 题库下的题目列表 |
| GET | `/api/content/questions/:id` | 题目详情 |

**生成请求体**：
```json
{
  "bankId": 1,
  "kpIds": [1, 2, 3],
  "types": ["en2zh_choice", "fill_blank"],
  "count": 10
}
```

**选择题干扰项策略**：
1. 优先从同题库、同难度的知识点中选
2. 不足时放宽到同年级
3. 保证 4 个选项不重复，正确答案随机位置

**验收标准**：
- [ ] 4 种题型均可正确生成
- [ ] 选择题干扰项来自同库/同年级
- [ ] 生成的题目入库（`created_by = 'template'`）
- [ ] 题库 `question_count` 自动更新

---

## 1.5 练习会话 API

**目标**：管理一次练习的完整流程：开始 → 逐题答 → 提交 → 出结果。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| POST | `/api/content/practice/start` | 开始练习（创建 session） |
| GET | `/api/content/practice/:sessionId/next` | 获取下一题 |
| POST | `/api/content/practice/:sessionId/answer` | 提交答案 |
| POST | `/api/content/practice/:sessionId/finish` | 结束练习 |
| GET | `/api/content/practice/:sessionId/result` | 获取练习结果 |

**开始练习请求体**：
```json
{
  "bankId": 1,
  "questionType": "en2zh_choice",
  "count": 10
}
```

**答题响应**（即时反馈）：
```json
{
  "correct": false,
  "correctAnswer": "apple",
  "explanation": "'apple'的意思是苹果",
  "sessionProgress": { "current": 3, "total": 10, "correctCount": 2 }
}
```

**练习结果**：
```json
{
  "sessionId": 1,
  "totalCount": 10,
  "correctCount": 7,
  "accuracy": 0.7,
  "duration": 180,
  "mistakes": [
    { "questionId": 5, "userAnswer": "banana", "correctAnswer": "apple" }
  ]
}
```

**验收标准**：
- [ ] 练习全流程可跑通
- [ ] 即时判定对错并返回解析
- [ ] 错题自动写入 `mistake_record`
- [ ] session 记录 `started_at` / `finished_at`
- [ ] 结果包含正确率和错题列表

---

## 1.6 PDF 上传 + 本地存储

**目标**：用户上传 PDF 文件，存储到本地文件系统，记录文件路径。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| POST | `/api/content/upload/pdf` | 上传 PDF（multipart） |

**实现要点**：
- 本地存储目录: `uploads/`
- 文件名规则: `pdf/{userId}/{timestamp}_{originalName}`
- 上传后在 `question_bank.source_file_url` 记录路径
- 文件大小限制: 10MB
- 仅接受 `.pdf` 格式

**依赖**：
- `FileStorageService.java`（本地文件存储封装）
- 无需外部存储服务

**验收标准**：
- [ ] PDF 上传成功存入本地文件系统
- [ ] 返回文件 URL
- [ ] 限制文件大小和格式
- [ ] 关联到题库记录

---

## 1.7 PDF 文本提取

**目标**：使用 Apache PDFBox 提取 PDF 中的文本（Phase 1 只做提取，AI 分析留 Phase 2）。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| POST | `/api/content/banks/:bankId/extract` | 触发 PDF 文本提取 |

**实现要点**：
- 依赖: `org.apache.pdfbox:pdfbox:3.x`
- 从本地文件系统读取 PDF → PDFBox 提取文本 → 存储提取结果
- 题库状态变更: `active` → `processing` → `active`/`error`
- 提取的原始文本存到新字段或关联表，为 Phase 2 AI 分析做准备

**验收标准**：
- [ ] 可从本地文件系统读取 PDF
- [ ] PDFBox 正确提取文本
- [ ] 题库状态流转正确
- [ ] 错误处理（PDF 损坏/无文本内容）

---

## 1.8 SRS 记忆系统 API

**目标**：基于间隔重复算法（SM-2 变体）的复习系统。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| GET | `/api/content/srs/due` | 获取当前待复习的知识点 |
| POST | `/api/content/srs/review` | 提交复习结果 |
| GET | `/api/content/srs/stats` | SRS 统计（待复习/已掌握/学习中） |

**SM-2 算法核心**：
```
回答正确:
  ease_factor = max(1.3, ease_factor + 0.1 - (5 - quality) * 0.08)
  interval = previous_interval * ease_factor
  correct_streak += 1

回答错误:
  interval = 1
  correct_streak = 0
  ease_factor = max(1.3, ease_factor - 0.2)
```

**触发时机**：
- 练习中答题时自动创建/更新 SRS 卡片
- 用户进入复习中心时查询到期卡片

**验收标准**：
- [ ] 练习时自动创建 SRS 卡片
- [ ] 回答正确/错误后间隔正确更新
- [ ] 到期列表按 `next_review_at` 排序
- [ ] 统计接口返回各状态数量

---

## 1.9 错题本 API

**目标**：记录和管理用户的错题。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| GET | `/api/content/mistakes` | 错题列表（分页/筛选） |
| GET | `/api/content/mistakes/:id` | 错题详情 |
| PUT | `/api/content/mistakes/:id/review` | 标记已复习 |
| DELETE | `/api/content/mistakes/:id` | 删除错题 |
| POST | `/api/content/mistakes/practice` | 错题专项练习 |

**查询参数**：
- `bankId` — 按题库筛选
- `questionType` — 按题型筛选
- `reviewed` — true/false
- `dateFrom` / `dateTo` — 时间范围

**验收标准**：
- [ ] 练习答错时自动写入
- [ ] 支持多维度筛选
- [ ] 标记已复习功能正常
- [ ] 错题专项练习从错题中抽题

---

## 1.10 每日统计 API

**目标**：service-user 记录用户每日学习数据。

**API 设计**：
| 方法 | Path | 说明 |
|------|------|------|
| GET | `/api/user/stats/today` | 今日统计 |
| GET | `/api/user/stats/range` | 日期范围统计 |
| POST | `/api/user/stats/record` | 记录学习数据（内部调用） |
| GET | `/api/user/stats/streak` | 连续学习天数 |

**数据来源**：
- 练习完成时，service-content 通过 HTTP 调用 service-user 记录统计
- 或前端在练习结束后调用统计接口

**验收标准**：
- [ ] 每日统计正确累加
- [ ] 范围查询支持（用于图表）
- [ ] 连续天数计算正确
- [ ] 跨天边界处理正确

---

## 1.11 前端：题库列表/详情/创建

**目标**：3 个完整页面 + 路由。

**页面**：
| 路由 | 页面 | 功能 |
|------|------|------|
| `/banks` | BankListView | 题库卡片列表、年级筛选、搜索、创建入口 |
| `/banks/:id` | BankDetailView | 知识点列表、统计概览、练习入口 |
| `/banks/create` | BankCreateView | 命名、选年级、上传 PDF / 手动添加 |

**UI 要点**：
- 卡片列表用 grid 布局
- 筛选器放顶部（年级 tabs + 搜索框）
- 详情页顶部统计条（知识点数/题目数/正确率）
- 创建页支持 PDF 拖拽上传区域

**验收标准**：
- [ ] 3 个页面可正常跳转
- [ ] 题库列表支持筛选和搜索
- [ ] 详情页展示知识点列表
- [ ] 创建题库流程跑通（含 PDF 上传）

---

## 1.12 前端：统一练习界面

**目标**：一个练习页面组件，根据题型渲染不同 UI。

**路由**：`/practice`（带 query 参数：`bankId`, `type`, `count`）

**题型组件**：
| 题型 | 组件 | UI |
|------|------|-----|
| en2zh_choice | ChoiceQuestion | 题干 + 4 按钮 |
| zh2en_choice | ChoiceQuestion | 题干 + 4 按钮 |
| fill_blank | FillQuestion | 题干(含空) + 输入框 |
| translate | TranslateQuestion | 题干 + 文本输入框 |

**练习流程 UI**：
```
[进度条 3/10] ─────────────────────
┌─────────────────────────────────┐
│  "apple" 的中文意思是？          │
│                                 │
│  ┌──────────┐  ┌──────────┐    │
│  │ A. 苹果   │  │ B. 香蕉   │    │
│  └──────────┘  └──────────┘    │
│  ┌──────────┐  ┌──────────┐    │
│  │ C. 西瓜   │  │ D. 橘子   │    │
│  └──────────┘  └──────────┘    │
│                                 │
│  [跳过]              [下一题]   │
└─────────────────────────────────┘
```

**即时反馈**：
- 答对：选项变绿 + ✓ 动画
- 答错：选项变红 + 正确答案高亮绿 + 解析展示
- 1.5 秒后自动进入下一题（或手动点击）

**验收标准**：
- [ ] 4 种题型 UI 组件可渲染
- [ ] 进度条实时更新
- [ ] 即时反馈（对/错 + 解析）
- [ ] 练习完成自动跳转结果页

---

## 1.13 前端：练习结果页

**目标**：练习结束后展示本次成绩和错题回顾。

**路由**：`/practice/result?sessionId=xxx`

**UI 要素**：
- 大字显示：正确率（如 70%）+ 得分环形图
- 统计行：总题数 / 正确 / 错误 / 用时
- 错题回顾列表：题目 + 你的答案 + 正确答案
- 按钮：再来一轮 / 错题练习 / 返回题库

**验收标准**：
- [ ] 正确率和统计数据显示正确
- [ ] 错题列表可展开查看详情
- [ ] 可跳转到错题专项练习

---

## 1.14 前端：复习中心

**目标**：展示 SRS 待复习列表，一键开始复习。

**路由**：`/review`

**UI 要素**：
- 统计卡片：今日待复习 / 已掌握 / 学习中
- 待复习列表（按紧迫程度排序）
- 开始复习按钮 → 进入练习界面（题目来自 SRS 到期列表）

**验收标准**：
- [ ] 显示 SRS 统计
- [ ] 列表展示待复习知识点
- [ ] 点击复习进入练习流程
- [ ] 复习后 SRS 数据更新

---

## 1.15 前端：错题本

**目标**：查看和管理历史错题。

**路由**：`/mistakes`

**UI 要素**：
- 筛选栏：题库 / 题型 / 是否已复习 / 时间范围
- 错题列表卡片：题干 + 你的答案 + 正确答案 + 时间
- 操作：标记已复习 / 删除 / 专项练习
- 底部：错题专项练习按钮

**验收标准**：
- [ ] 错题列表可正常展示
- [ ] 支持多维度筛选
- [ ] 标记已复习状态更新
- [ ] 错题专项练习跑通

---

## 1.16 前端：统计页

**目标**：用图表展示学习数据趋势。

**路由**：`/stats`

**依赖**：`chart.js` + `vue-chartjs`

**UI 要素**：
- 总览卡片：总学习天数 / 总练习题数 / 总正确率 / 连续天数
- 折线图：近 7/30 天每日练习量趋势
- 折线图：近 7/30 天正确率趋势
- 饼图：各题型练习分布
- 柱状图：各题库学习进度

**验收标准**：
- [ ] 4 种图表可正常渲染
- [ ] 数据来源正确
- [ ] 支持 7 天 / 30 天切换
- [ ] 空数据时有友好提示

---

## 数据库迁移

Phase 1 需要新增的 Flyway 迁移文件：

**service-content**：
- `V2__add_extracted_text.sql` — `question_bank` 表新增 `extracted_text TEXT` 字段
- `V2__add_practice_answer.sql` — `practice_session` 新增答题详情表

**service-user**：
- `V2__add_learning_progress.sql` — 创建 `learning_progress` 表（如果 V1 未包含）

---

## 依赖项新增

**service-content/pom.xml**：
```xml
<!-- MinIO 已移除，改用本地文件存储 -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

**frontend/package.json**：
```bash
npm install chart.js vue-chartjs
```

---

## 验收：Phase 1 全流程

```
用户登录 → 看到题库列表 → 选择"初中核心词汇"
    → 看到知识点列表 → 点击"开始练习"
    → 选择题型和数量 → 答 10 道题（选择/填空/翻译）
    → 即时反馈（对/错+解析）
    → 练习结束看结果（70% 正确率）
    → 查看错题本（3 道错题）
    → 进入复习中心（SRS 推荐复习）
    → 查看统计图表（今日进度）
```
