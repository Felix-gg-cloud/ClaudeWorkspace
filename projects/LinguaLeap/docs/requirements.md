# LinguaLeap - AI 智能英语学习平台 · 需求分析文档 v3

> AI 驱动的高效英语学习网站，简约 · 舒适 · 护眼
> 创建日期：2026-03-18
> 最后更新：2026-04-01

---

## 一、项目概述

### 1.1 项目愿景
打造一个 AI 驱动的英语学习网站。用户可上传教材 PDF 或使用预制题库，AI 自动分析资料并生成针对性练习题目，支持语音跟读、智能对话等多维度学习方式，覆盖小学至高中英语。

### 1.2 目标用户
- 小学生（800 核心词汇）
- 初中生（1600 核心词汇）
- 高中生（3500 核心词汇）

### 1.3 核心理念
- **个性化**：AI 根据用户资料和水平生成内容，不超纲
- **多模态**：读/写/听/说全覆盖
- **简约高效**：简洁舒适的界面，聚焦学习本身
- **渐进式**：由浅入深，先跑通流程再加大负荷

### 1.4 设计风格
- **简约 · 舒适 · 护眼**，告别游戏化暗黑风格
- 浅色护眼模式（默认）+ 可选深色模式
- 操作方便，页面结构逻辑清晰
- 参考风格：Notion / Duolingo 浅色版 / Quizlet

### 1.5 学习路径
```
注册 → 选择年级(小学/初中/高中)
    ↓
选择/创建题库
├── 预制题库（按年级推荐）
└── 自建题库（自命名 + 上传 PDF / 导入词表）
    ↓
选择学习内容类型
├── 单词学习
├── 语法练习（Phase 2，AI 出题）
├── 听力训练（Phase 3）
└── 阅读理解（Phase 2，AI 出题）
    ↓
AI 生成学习计划（Phase 2）
├── "本周目标：掌握50个新词"
├── "每日推荐：20个单词 + 5道语法"
└── 根据难度和用户水平安排
    ↓
按计划学习
├── 每日练习界面
├── 即时反馈（对/错/解析）
└── 学习过程自动记录
    ↓
AI 评估与建议（Phase 2 后期，Agent）
├── "你的介词用法薄弱，建议加强"
├── "本周正确率提升12%"
└── 动态调整学习计划
```

---

## 二、系统架构

### 2.1 微服务架构

```
┌──────────────────────────────────────────────────┐
│               Vue 3 Frontend                      │
│       (响应式 PC 侧边栏 + 移动端底部 Tab)          │
└──────┬───────────┬───────────┬────────────────────┘
       │           │           │
  ┌────▼───────────▼───────────▼────┐
  │     Spring Cloud Gateway :8080   │
  │     路由转发 + JWT 统一鉴权       │
  └──┬──────────┬──────────┬────────┘
     │          │          │
┌────▼──┐  ┌───▼────┐  ┌──▼───────┐
│ User  │  │Content │  │   AI     │
│ :8081 │  │ :8082  │  │  :8083   │
└───┬───┘  └───┬────┘  └────┬─────┘
    │          │            │
┌───▼───┐  ┌──▼────┐   ┌───▼─────┐
│  PG   │  │  PG   │   │ GitHub  │
│ll_user│  │ll_cont│   │ Models  │
└───────┘  └───────┘   └─────────┘
```

### 2.2 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| **前端** | Vue 3.5 + TypeScript + Vite + Pinia | 全新 UI 框架 |
| **网关** | Spring Cloud Gateway | 路由/鉴权/限流 |
| **后端** | Spring Boot 3.x + Spring Cloud | Java 17 微服务 |
| **数据库** | PostgreSQL 16 | Docker 部署，1 实例多库 |
| **AI 框架** | Spring AI | 统一抽象，支持 API 调用 → Agent 演进 |
| **AI 主力** | GitHub Models (GPT-4o) | 出题/分析/知识生成/对话 |
| **文件存储** | 本地文件系统 | uploads/ 目录，存储 PDF |
| **语音** | Web Speech API | 语音识别 + 合成（Phase 3） |
| **容器化** | Docker Compose | PG（开发阶段） |
| **服务通信** | OpenFeign | 同步 HTTP |

### 2.3 已确认的技术决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 服务注册发现 | 暂不引入 Nacos/Eureka | Gateway 写死端口，后续部署再加 |
| 数据库部署 | 1 个 PG 实例，多库 | ll_user / ll_content / ll_ai |
| 开发阶段容器化 | 只 Docker 化 PG | Java 服务本地运行，便于调试 |
| AI 演进路线 | 先 API 调用 → 后 Agent | Phase 2 初期=API，后期=Agent |
| AI 提供商 | GitHub Models GPT-4o | 100次/天, 10RPM, 40k tokens/min |
| 前端风格 | 全新简约 UI | 去掉所有游戏化元素，重写 |
| 旧数据 | 不迁移 | 全新项目从零开始 |
| 网络环境 | 可访问外网 | GitHub Models API 已确认可达 |
| 内容策略 | 预制为主 + 模板 fallback | KP 预制完成，题目按需生成 |

### 2.4 服务职责划分

#### service-user（用户服务 :8081）
- 用户注册 / 登录 / JWT 鉴权
- 用户信息（年级、学习偏好）
- 学习进度
- 每日学习统计

#### service-content（题库服务 :8082）
- 预制题库管理（小学/初中/高中）
- 用户自建题库（命名 + PDF 上传 / 词表导入）
- PDF 上传 + 本地文件存储 + 文本提取
- 知识点管理（单词/短语/语法）
- **知识库体系**（9个等级 L1-L9，单元管理，学习进度跟踪）
- 题目 CRUD + 模板出题
- SRS 记忆系统
- 错题本
- 练习会话管理

#### service-ai（AI 服务 :8083）
- Spring AI ChatClient 封装（GitHub Models GPT-4o）
- Prompt 模板管理
- PDF 智能分析（提取知识点）
- AI 出题（按题型/难度/年级）
- **AI 知识内容生成**（按等级/主题/单元生成知识点）
- AI 学习计划生成
- AI 评估建议（Agent 模式，Phase 2 后期）
- 错题智能分析
- 请求缓存 + 限流

---

## 三、数据模型

### 3.1 用户服务 (ll_user)

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    grade           VARCHAR(20) NOT NULL DEFAULT 'junior', -- primary/junior/senior
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE learning_progress (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    kp_id           BIGINT NOT NULL,                       -- 关联知识点（service-content 的 ID）
    unit_id         BIGINT,                                -- 关联单元
    level_id        BIGINT,                                -- 关联等级
    status          VARCHAR(20) DEFAULT 'new',             -- new/learning/mastered
    review_count    INT DEFAULT 0,
    last_reviewed_at TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, kp_id)
);

CREATE TABLE daily_stats (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    stat_date       DATE NOT NULL,
    tasks_completed INT DEFAULT 0,
    correct_count   INT DEFAULT 0,
    wrong_count     INT DEFAULT 0,
    words_learned   INT DEFAULT 0,
    study_minutes   INT DEFAULT 0,
    UNIQUE(user_id, stat_date)
);
```

### 3.2 题库服务 (ll_content)

```sql
CREATE TABLE question_bank (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    grade           VARCHAR(20) NOT NULL,              -- primary/junior/senior
    type            VARCHAR(20) NOT NULL,              -- preset/user_upload
    user_id         BIGINT,                            -- NULL = 预制题库
    source_file_url VARCHAR(500),                      -- 本地文件路径
    status          VARCHAR(20) DEFAULT 'active',      -- active/processing/error
    kp_count        INT DEFAULT 0,
    question_count  INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE knowledge_level (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(10) UNIQUE NOT NULL,       -- L1-L9
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    grade_group     VARCHAR(20) NOT NULL,              -- primary/junior/senior
    sort_order      INT NOT NULL
);

CREATE TABLE knowledge_unit (
    id              BIGSERIAL PRIMARY KEY,
    level_id        BIGINT REFERENCES knowledge_level(id),
    name            VARCHAR(200) NOT NULL,
    topic           VARCHAR(100),
    description     TEXT,
    sort_order      INT DEFAULT 0,
    kp_count        INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE knowledge_point (
    id              BIGSERIAL PRIMARY KEY,
    bank_id         BIGINT REFERENCES question_bank(id) ON DELETE CASCADE,
    level_id        BIGINT REFERENCES knowledge_level(id),
    unit_id         BIGINT REFERENCES knowledge_unit(id),
    type            VARCHAR(20) NOT NULL,              -- word/phrase/grammar
    content         VARCHAR(200) NOT NULL,             -- 英文内容
    phonetic        VARCHAR(100),                      -- 音标
    meaning_zh      VARCHAR(500),                      -- 中文释义
    example_sentence TEXT,                              -- 例句
    example_zh      TEXT,                               -- 例句翻译
    difficulty      INT DEFAULT 1,                     -- 1-5
    tags            TEXT,                               -- JSON: ["noun", "unit1"]
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE question (
    id              BIGSERIAL PRIMARY KEY,
    bank_id         BIGINT REFERENCES question_bank(id) ON DELETE CASCADE,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,              -- choice/fill/translate/spell
    stem            TEXT NOT NULL,                      -- 题干
    options         TEXT,                               -- JSON: 选择题选项
    answer          TEXT NOT NULL,                      -- 正确答案
    explanation     TEXT,                               -- 解析
    difficulty      INT DEFAULT 1,                     -- 1-5
    created_by      VARCHAR(20) DEFAULT 'preset',      -- preset/ai/template
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE srs_card (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE CASCADE,
    interval_days   INT DEFAULT 1,
    ease_factor     FLOAT DEFAULT 2.5,
    review_count    INT DEFAULT 0,
    correct_streak  INT DEFAULT 0,
    next_review_at  TIMESTAMP,
    last_reviewed   TIMESTAMP,
    UNIQUE(user_id, kp_id)
);

CREATE TABLE mistake_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    question_id     BIGINT REFERENCES question(id) ON DELETE SET NULL,
    kp_id           BIGINT REFERENCES knowledge_point(id) ON DELETE SET NULL,
    user_answer     TEXT,
    correct_answer  TEXT,
    question_type   VARCHAR(20),
    reviewed        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE practice_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    bank_id         BIGINT REFERENCES question_bank(id),
    question_type   VARCHAR(20),                       -- 练习的题型
    total_count     INT DEFAULT 0,
    correct_count   INT DEFAULT 0,
    started_at      TIMESTAMP DEFAULT NOW(),
    finished_at     TIMESTAMP
);
```

### 3.3 AI 服务 (ll_ai)

```sql
CREATE TABLE ai_analysis_cache (
    id              BIGSERIAL PRIMARY KEY,
    content_hash    VARCHAR(64) UNIQUE NOT NULL,       -- SHA256
    analysis_type   VARCHAR(50) NOT NULL,              -- extract_kp/generate_q/recommend
    input_summary   TEXT,
    result          TEXT NOT NULL,                      -- JSON
    model           VARCHAR(50),
    tokens_used     INT,
    created_at      TIMESTAMP DEFAULT NOW(),
    expires_at      TIMESTAMP
);

CREATE TABLE ai_call_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    api_provider    VARCHAR(50),                       -- github-models
    call_type       VARCHAR(50),                       -- extract/generate/recommend/evaluate
    tokens_in       INT,
    tokens_out      INT,
    latency_ms      INT,
    status          VARCHAR(20),                       -- success/error/rate_limited
    created_at      TIMESTAMP DEFAULT NOW()
);
```

---

## 四、UI 设计规范

### 4.1 配色方案

**浅色模式（默认护眼）**：
| 用途 | 色值 | 说明 |
|------|------|------|
| 页面背景 | `#FAFBFC` | 微灰白，不刺眼 |
| 卡片背景 | `#FFFFFF` | 纯白 |
| 主色 | `#4F46E5` | 靛蓝，专业感 |
| 主色浅 | `#EEF2FF` | 靛蓝淡底 |
| 成功/正确 | `#10B981` | 绿 |
| 警告 | `#F59E0B` | 琥珀 |
| 错误 | `#EF4444` | 红 |
| 文字主 | `#1F2937` | 深灰（非纯黑） |
| 文字次 | `#6B7280` | 中灰 |
| 边框 | `#E5E7EB` | 浅灰 |

**深色模式（可选）**：
| 用途 | 色值 |
|------|------|
| 页面背景 | `#111827` |
| 卡片背景 | `#1F2937` |
| 主色 | `#818CF8` |
| 文字主 | `#F3F4F6` |
| 边框 | `#374151` |

### 4.2 页面布局

```
PC 端 (≥768px):
┌─────────────────────────────────────────────┐
│  顶部栏: Logo   [搜索]            🌙/☀️  👤 │
├────────┬────────────────────────────────────┤
│ 侧边栏 │                                    │
│        │        主内容区                     │
│ 🏠 首页│                                    │
│ 📚 题库│                                    │
│ ✏️ 学习│                                    │
│ 🔄 复习│                                    │
│ ❌ 错题│                                    │
│ 📊 统计│                                    │
│ ⚙️ 设置│                                    │
└────────┴────────────────────────────────────┘

移动端 (<768px):
┌─────────────────────────┐
│  顶部栏: Logo     🌙 👤 │
├─────────────────────────┤
│                         │
│       主内容区           │
│                         │
├─────────────────────────┤
│ [🏠] [📚] [✏️] [📊] [👤]│
│ 首页  题库  学习  统计 我的│
└─────────────────────────┘
```

### 4.3 核心页面清单

| 页面 | 路由 | 功能 |
|------|------|------|
| 首页/仪表盘 | `/` | 今日学习进度、快速开始、学习日历 |
| 知识库 | `/levels` | 9级知识体系，按年级组分类展示 |
| 等级详情 | `/levels/:id` | 该级所有单元列表 + 学习进度 |
| 学习模式 | `/learn/:unitId` | 翻转卡片学习，掌握度标记 |
| 题库列表 | `/banks` | 卡片列表、按年级筛选、搜索 |
| 题库详情 | `/banks/:id` | 知识点列表、练习入口、统计 |
| 创建题库 | `/banks/create` | 上传 PDF / 导入词表 / 手动添加 |
| 练习页 | `/practice` | 答题界面（多题型统一） |
| 练习结果 | `/practice/result` | 本次统计、错题回顾 |
| 复习中心 | `/review` | SRS 待复习列表、一键开始 |
| 错题本 | `/mistakes` | 错题列表、筛选、专项练习 |
| 统计 | `/stats` | 学习趋势图、正确率、词汇量 |
| 个人中心 | `/settings` | 年级、偏好、主题切换 |
| 登录/注册 | `/login` | 登录注册页 |

---

## 五、已废弃的功能

以下 EnglishQuestArena 的功能在 LinguaLeap 中**不再保留**：

| 废弃功能 | 原因 |
|---------|------|
| 营地探索 (Phaser 游戏) | 转型为纯学习平台 |
| Boss 战 | 不再需要游戏战斗 |
| RPG 数值 (XP/金币/技能点) | 简化为学习数据 |
| 暗黑游戏风格 UI | 改为简约护眼风格 |
| 成就/徽章系统 | 后续可用简洁方式重新引入 |
| 角色头像系统 | 不需要 |
| 章节固定课时 (Lesson/Day) | 改为动态题库 + AI 推荐 |
| Phaser 依赖 | 移除（3MB+） |

---

## 六、Phase 详细规划

### Phase 0：微服务架构 + 全新 UI 框架

**目标**：搭建微服务基础设施 + 全新前端 UI 框架，跑通注册→登录→基础页面。

| # | 任务 | 产出 |
|---|------|------|
| 0.1 | Maven 多模块骨架 | 父 POM + common + gateway + 3 个 service |
| 0.2 | Docker Compose (PG) | 一键启动基础设施 |
| 0.3 | common 模块 | JWT 工具 / 统一响应 / 异常处理 |
| 0.4 | Gateway 路由 + JWT 过滤器 | 网关可运行 |
| 0.5 | service-user 核心 | 注册/登录/用户信息/JWT 鉴权 |
| 0.6 | service-content 骨架 | 健康检查 + Flyway + 空 API |
| 0.7 | service-ai 骨架 | 健康检查 + 空 API |
| 0.8 | 前端全新 UI 框架 | 布局组件/导航/路由/主题切换 |
| 0.9 | 前端登录注册页 | 对接 service-user |
| 0.10 | 前端仪表盘骨架 | 首页基本框架 |
| 0.11 | 全流程验证 | 注册→登录→进入仪表盘 |

### Phase 1：题库系统 + 练习核心

**目标**：完整的题库管理 + 4 种题型练习 + SRS 复习 + 错题本。

| # | 任务 | 说明 |
|---|------|------|
| 1.1 | 预制词库数据 | 每级 100-200 核心词，跑通后扩充 |
| 1.2 | 题库 CRUD API | service-content |
| 1.3 | 知识点 CRUD API | service-content |
| 1.4 | 模板出题引擎 | 4 种题型（选择/填空/翻译/拼写） |
| 1.5 | 练习会话 API | 开始/答题/提交/结果 |
| 1.6 | PDF 上传 + 本地存储 | service-content |
| 1.7 | PDF 文本提取 | Apache PDFBox，存原始文本 |
| 1.8 | SRS 记忆系统 API | service-content |
| 1.9 | 错题本 API | service-content |
| 1.10 | 每日统计 API | service-user |
| 1.11 | 前端：题库列表/详情/创建 | 3 个页面 |
| 1.12 | 前端：统一练习界面 | 4 种题型组件 |
| 1.13 | 前端：练习结果页 | 统计 + 错题回顾 |
| 1.14 | 前端：复习中心 | SRS 列表 + 练习 |
| 1.15 | 前端：错题本 | 列表 + 筛选 + 专项练习 |
| 1.16 | 前端：统计页 | 趋势图 + 正确率（chart.js） |

**题型**（4 种基础题型，交互形式随年级自适应）：

| 题型 | 练的能力 | 小学 | 初中 | 高中 |
|------|---------|------|------|------|
| 英译中选择 | L1 词义辨认 | 选择（简单干扰）+音标+发音 | 选择（同类干扰）+音标+例句 | 选择（近义辨析）+语境 |
| 中译英 | L2 词形回忆 | 选择+音标+发音 | 选择+偶尔拼写+发音 | 拼写+🔊发音提示唤醒记忆 |
| 填空 | L3 语境运用 | **选择填空**（选词填入） | **形态选择**（apple/apples等） | 开放输入+给提示 |
| 翻译(中→英) | L4 综合表达 | **句子排序**（排打散的词） | **半填空**+AI评判 | **完整翻译**+AI评判 |

**能力层次模型**：L1 辨认 → L2 回忆 → L3 运用 → L4 表达（由浅到深）

**年级适配原则**：
- 小学 → 全选择/点击操作，零拼写压力
- 初中 → 选择+输入混合，有脚手架辅助
- 高中 → 输入为主，独立产出，接近真题

**答题增强**：
- 中译英答错 → 必须手动键入正确答案才能继续
- 填空题 → 无论对错都展示详细知识点解析（单词+语法+形态）
- 翻译题（初中/高中）→ AI 实时评判并给出详细解析
- 全局 TTS 发音支持（Web Speech API，零成本）

**出题策略**：
- 全部由 AI 生成，取消模板引擎；AI 出的题自动入库
- AI 不可用时从库里已有题中随机抽取（降级）

### Phase 2：AI 智能功能 ✅

**目标**：接入 GitHub Models GPT-4o，实现 AI 出题、质量校验、缓存/限流/日志。

| # | 任务 | 模式 | 状态 |
|---|------|------|------|
| 2.1 | Spring AI + GitHub Models 集成 | API 调用 | ✅ |
| 2.2 | Prompt 模板系统 | API 调用 | ✅ |
| 2.3 | AI 出题（按题型/难度/年级） | API 调用 | ✅ |
| 2.4 | AI 出题质量校验 | API 调用 | ✅ |
| 2.5 | 结果缓存 + 调用日志 | — | ✅ |
| 2.6 | 调用限流（用户级 + 全局级） | — | ✅ |
| 2.7 | AI 知识点解析 | API 调用 | ✅ |
| 2.8 | 前端：AI 出题/分析页面 | — | ✅ |

### Phase 2b：题型改造 + 年级自适应（已完成）

**目标**：重构练习系统，实现年级自适应题型（4题型×3年级=12变体）、AI 翻译评判、TTS 语音辅助。

> 详细任务规划见 [docs/phase2b/tasks.md](../phase2b/tasks.md)

| # | 任务 | 类型 |
|---|------|------|
| B.1 | 全 AI 出题 Prompt 重构（4题型×3年级=12套） | 后端 |
| B.2 | Question 数据结构扩展（knowledgePoints/words/extraData/grade） | 后端 |
| B.3 | AI 出题服务改造（年级感知+自动入库+库里抽题降级） | 后端 |
| B.4 | 答题判定逻辑重构（模糊匹配/AI评判/retype） | 后端 |
| B.5 | AI 翻译评判 API | 后端 |
| B.6 | 前端 TTS 语音模块 | 前端 |
| B.7 | 前端：英译中选择组件改造（音标+发音+例句） | 前端 |
| B.8 | 前端：中译英组件改造（拼写+发音提示+答错retype） | 前端 |
| B.9 | 前端：填空题组件改造（选择填空+知识点解析面板） | 前端 |
| B.10 | 前端：翻译题组件改造（排序/半填空/完整翻译+AI判） | 前端 |
| B.11 | 练习流程串联 + 年级适配逻辑 | 全栈 |
| B.12 | 联调测试（三年级全流程） | 测试 |

**AI Agent 工具**（Phase 2 后期）：

```java
@Tool("查询用户的错题记录")
List<Mistake> getUserMistakes(Long userId) { ... }

@Tool("查询用户的SRS复习进度")
SrsProgress getSrsProgress(Long userId) { ... }

@Tool("从题库中查找相关题目")
List<Question> findQuestions(String topic, int difficulty) { ... }
```

### Phase 3a：知识库体系（已完成）

**目标**：构建分级知识库（L1-L9），单元化学习内容，翻转卡片学习模式，AI 自动生成知识内容。

| # | 任务 | 类型 | 状态 |
|---|------|------|------|
| 3a.1 | 知识等级表 knowledge_level（L1-L9，3个年级组） | 后端 | ✅ |
| 3a.2 | 知识单元表 knowledge_unit（按主题分组） | 后端 | ✅ |
| 3a.3 | 学习进度表 learning_progress（用户+知识点维度） | 后端 | ✅ |
| 3a.4 | knowledge_point 扩展（level_id, unit_id） | 后端 | ✅ |
| 3a.5 | LevelService（等级列表+进度统计） | 后端 | ✅ |
| 3a.6 | LearningService（学习卡片+进度标记） | 后端 | ✅ |
| 3a.7 | LevelController REST API | 后端 | ✅ |
| 3a.8 | KnowledgeGenerateService（AI 生成单元知识内容） | AI | ✅ |
| 3a.9 | 前端：知识库等级列表页 LevelListView | 前端 | ✅ |
| 3a.10 | 前端：等级详情+单元列表 LevelDetailView | 前端 | ✅ |
| 3a.11 | 前端：翻转卡片学习 LearnView | 前端 | ✅ |

**知识库等级体系**（实际已部署）：

| 等级 | 名称 | 年级组 | 知识点数 |
|------|------|--------|----------|
| L1 | 小学三年级 | 小学 | ~25/单元 |
| L2 | 小学四年级 | 小学 | ~24/单元 |
| L3 | 小学五年级 | 小学 | ~24/单元 |
| L4 | 小学六年级 | 小学 | ~24/单元 |
| L5 | 初一 | 初中 | ~40/单元 |
| L6 | 初二 | 初中 | ~40/单元 |
| L7 | 初三 | 初中 | ~40/单元 |
| L8 | 高一高二 | 高中 | ~43/单元 |
| L9 | 高三 | 高中 | ~44/单元 |

> 已完成：45单元全部预制，共 1512 个知识点。词汇量参考 2022 版英语新课标：小学 505 词 / 初中累计 1600 词 / 高中累计 3500 词。

### Phase 3a+：预制内容 + 混合出题 + AI 练习分析（已完成）

**目标**：管理员预制全部知识内容，混合出题策略（预制+模板），AI Lily老师练习分析。

| # | 任务 | 类型 | 状态 |
|---|------|------|------|
| 3a+.1 | 批量生成脚本 `scripts/batch_generate.py` | 工具 | ✅ |
| 3a+.2 | 45单元KP全部预制（1512个知识点） | 数据 | ✅ |
| 3a+.3 | 选定单元AI题目生成（80道，L1/L5/L8各1-2单元） | 数据 | ✅ |
| 3a+.4 | 用户做题历史 user_question_history（V7迁移） | 后端 | ✅ |
| 3a+.5 | 混合出题策略（排除已做→AI补生成→模板fallback） | 后端 | ✅ |
| 3a+.6 | AI 练习分析（Lily老师点评，答题后AI分析） | 全栈 | ✅ |
| 3a+.7 | 移除前端AI生成按钮（预制模式） | 前端 | ✅ |
| 3a+.8 | 导航简化（移除题库/练习，知识库为主入口） | 前端 | ✅ |

**内容策略决策**：
- 知识点+题目：管理员用 AI 批量生成后入库，用户无需等待
- 出题顺序：预制AI题 → 排除用户已做题 → 不够时模板补生成
- AI 动态使用：答题分析点评(Lily老师)、AI老师对话、翻译题判分

### Phase 3b：语音练习（未来）

**目标**：支持听力、跟读、听写、口语对话。

| # | 任务 | 说明 |
|---|------|------|
| 3.1 | TTS 集成 | 浏览器 SpeechSynthesis |
| 3.2 | STT 集成 | Web Speech API（可访问外网） |
| 3.3 | 单词跟读 + 评分 | 播放→跟读→文本对比 |
| 3.4 | 句子跟读 + 高亮反馈 | 匹配/错误部分高亮 |
| 3.5 | 听写模式 | 播放→键盘输入→评分 |
| 3.6 | AI 口语对话 | 场景化多轮对话 |
| 3.7 | 发音纠错提示 | 音标 + 示范 + 常见错误 |
| 3.8 | 录音回放 | 对比标准发音 |

### Phase 4a：用户内容引擎 + AI 学习集 ✅

**目标**：用户可上传自己的学习材料，AI 自动提取知识点并生成练习。

| # | 功能 | 说明 |
|---|------|------|
| 4a.1 | 学习集上传 | 支持 PDF/文本/词表，本地文件存储 |
| 4a.2 | AI 内容提取 | GPT-4o 提取知识点，分类: 词汇/语法/句型/篇章 |
| 4a.3 | AI 策略生成 | 根据内容+年级生成出题策略 |
| 4a.4 | 学习集详情 | 管理提取的知识点，开始练习 |
| 4a.5 | AI 老师 Lily | 入学评估 + 英语对话练习 |
| 4a.6 | Dashboard 改版 | 每日学习教练，三步任务引导 |

### Phase 4b：AI 智能体（未来愿景）

**目标**：打造 AI 智能体系统，超越预制知识库的限制，实现真正智能化学习。

| # | 功能 | 说明 |
|---|------|------|
| 4.1 | AI 老师（深度辅导） | Agent 模式，个性化教学，根据学生弱项动态调整 |
| 4.2 | AI 自由训练 | 用户上传任意内容（题目/知识点/电子书），AI 生成学习内容+练习题，不依赖预制库 |
| 4.3 | AI 内容分析 | 用户粘贴英语内容 → AI 解析知识点/翻译 → 询问是否生成知识库及训练 |
| 4.4 | 阅读理解 + 作文批改 | AI 生成阅读题 / 批改作文 |
| 4.5 | 学习报告 | 周/月报告，AI 总结分析 |
| 4.6 | 语音练习 | TTS/STT + 跟读 + 听写 + 口语对话 |
| 4.7 | 部署方案 | Docker 全量 + 云部署 |

---

## 七、免费资源预算

| 资源 | 方案 | 免费额度 | 超额处理 |
|------|------|---------|---------|
| AI 出题/分析 | GitHub Models (GPT-4o) | 100 次/天, 10 RPM | 拒绝请求，不扣费 |
| 数据库 | PostgreSQL Docker | 无限 | 本地运行 |
| 文件存储 | 本地文件系统 | 无限 | 本地存储 |
| 语音识别 | Web Speech API | 无限 | 浏览器原生 |
| 语音合成 | SpeechSynthesis | 无限 | 浏览器原生 |

---

## 八、实施路线总览

```
Phase 0 (微服务 + UI 框架) ✅
    │  ├─ Maven 多模块骨架
    │  ├─ Docker Compose (PG)
    │  ├─ Gateway + JWT
    │  ├─ service-user (注册/登录)
    │  ├─ service-content / service-ai 骨架
    │  ├─ 全新前端 UI 框架 + 主题
    │  └─ 注册→登录→首页 全流程验证
    ▼
Phase 1 (题库 + 练习) ✅
    │  ├─ 预制词库 (100-200词/级)
    │  ├─ 题库 CRUD + 知识点管理
    │  ├─ 模板出题 (4种题型)
    │  ├─ 统一练习界面
    │  ├─ SRS 记忆 + 错题本
    │  └─ 统计图表
    ▼
Phase 2 (AI 基础设施) ✅
    │  ├─ Spring AI + GitHub Models GPT-4o 集成
    │  ├─ Prompt 模板系统
    │  ├─ AI 出题 + 质量校验
    │  ├─ 缓存 + 限流 + 日志
    │  ├─ AI 知识点解析
    │  └─ 前端 AI 出题/分析页面
    ▼
Phase 2b (年级自适应题型) ✅
    │  ├─ 年级自适应题型（小学全选择 / 初中混合 / 高中输入）
    │  ├─ AI 翻译实时评判（初中/高中）
    │  ├─ TTS 语音辅助（Web Speech API）
    │  └─ 答错强制 retype + 知识点解析
    ▼
Phase 3a (知识库体系) ✅
    │  ├─ 9级知识库（L1-L9，按年级分组）
    │  ├─ 45单元学习内容
    │  ├─ 翻转卡片学习模式
    │  └─ AI Lily老师聊天
    ▼
Phase 3a+ (预制内容 + 混合出题) ✅  ← 已完成
    │  ├─ 1512个知识点预制完成
    │  ├─ 80道AI题目（L1/L5/L8验证单元）
    │  ├─ 混合出题策略（预制→排除已做→模板补充）
    │  ├─ AI 练习分析（Lily老师点评）
    │  └─ 导航简化 + 前端AI按钮移除
    ▼
Phase 4a (用户内容引擎 + AI 学习集) ✅  ← 已完成
    │  ├─ 学习集上传 (PDF/文本/词表)
    │  ├─ AI 内容提取 + 分类
    │  ├─ AI Lily 老师对话 + 入学评估
    │  ├─ Dashboard 每日学习教练改版
    │  └─ 本地文件存储 (uploads/)
    ▼
Phase 4b (AI 智能体) ← 未来方向
    │  ├─ AI 老师（深度个性化辅导，Agent模式）
    │  ├─ 自适应练习 + 学习链连接
    │  └─ 语音练习 / 阅读理解 / 作文批改
```

---

## 九、已确认事项清单

| # | 事项 | 决策 |
|---|------|------|
| 1 | 数据库 | PostgreSQL，1 实例多库，Docker 部署 |
| 2 | 网关 | Spring Cloud Gateway |
| 3 | 服务注册发现 | 暂不引入，写死端口 |
| 4 | AI 方案 | GitHub Models GPT-4o（免费，100次/天） |
| 5 | AI 演进 | 先 API 调用 → 后 Spring AI Agent |
| 6 | 网络 | 可访问外网，GitHub Models API 已确认可达 |
| 7 | 前端风格 | 简约护眼，浅色+可选深色 |
| 8 | 游戏化 | 全部去掉（营地/Boss/XP/Phaser） |
| 9 | 章节系统 | 改为题库系统，章节概念后续可能移除 |
| 10 | ChapterProgress | 暂放 service-user |
| 11 | 旧数据 | 不迁移，新项目从零开始 |
| 12 | 开发容器化 | 只 Docker 化 PG，Java 服务本地跑 |
| 13 | 预制词库 | 先每级 100-200 词，跑通后扩充 |
| 14 | Phase 1 题型 | 4 种（选择/填空/翻译/拼写），语音放 Phase 3 |
| 15 | Phase 1 PDF | 只做存储+文本提取，AI 分析等 Phase 2 |
| 16 | 语法出题 | Phase 1 聚焦单词，语法放 Phase 2 用 AI 出 |
| 17 | 开发宗旨 | 尽量解耦，由浅入深，先跑通再加负荷 |
| 18 | 题型年级自适应 | 同一题型在不同年级有不同交互形式（选择/拼写/排序等） |
| 19 | 出题策略 | 预制AI题为主，模板生成为 fallback；混合出题策略排除已做题 |
| 20 | 翻译题方向 | 只做中→英翻译；初中/高中由 AI 实时评判 |
| 21 | 答错强化 | 中译英答错必须手动键入正确答案；填空对错都展示知识点解析 |
| 22 | TTS 语音 | Phase 2b 即引入 Web Speech API 发音（不等 Phase 3） |
| 23 | 能力层次 | L1辨认→L2回忆→L3运用→L4表达，题型与能力对位 |
