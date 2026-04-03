# LinguaLeap — AI 智能英语学习平台

AI 驱动的英语学习网站。支持分级知识库、间隔复习、AI 出题、入学评估、Lily 老师对话等功能，覆盖小学至高中英语。

## 项目状态

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 0：微服务架构 + UI 框架 | ✅ 完成 | 4 个 Spring Boot 服务 + Vue 3 前端 |
| Phase 1：题库系统 + 练习核心 | ✅ 完成 | 知识点/题目 CRUD、练习系统、SRS 复习、错题本 |
| Phase 2：AI 智能功能 | ✅ 完成 | GPT-4o 出题、知识点分析、AI 翻译判定 |
| Phase 2b：题型改造 + 年级自适应 | ✅ 完成 | 4 种题型 × 3 个年级段，全 AI 出题 |
| Phase 3a：知识库体系 + 分级学习 | ✅ 完成 | L1-L9 九级体系、闪卡学习、预制内容 |
| Phase 4：用户内容引擎 | ✅ 完成 | 学习集上传 (PDF/文本)、AI 提取知识点、AI 老师 Lily |
| 教学增强：Dashboard 改版 | ✅ 完成 | 每日学习教练、三步任务引导、连续学习天数 |

## 技术栈

**前端**
- Vue 3.5 + TypeScript + Vite 8
- Pinia 状态管理 + Vue Router 4
- Tailwind CSS v4 + SCSS
- Axios HTTP（JWT 拦截器 + 全局 Toast 错误处理）

**后端（微服务）**
- Spring Cloud Gateway :8080（路由/JWT 鉴权）
- service-user :8081（注册/登录/统计）
- service-content :8082（题库/练习/SRS/错题/知识库/学习集）
- service-ai :8083（GPT-4o 出题/评估/对话/内容提取）

**数据库** — PostgreSQL 16（ll_user / ll_content / ll_ai）
**AI** — GitHub Models GPT-4o via Spring AI

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建
npm run build

# 类型检查
npx vue-tsc --noEmit
```

后端服务需要先启动（gateway:8080, user:8081, content:8082, ai:8083）。

## 核心功能

- 📋 **每日学习教练** — Dashboard 三步任务引导：复习 → 学习 → 练习
- 📚 **分级知识库** — L1-L9 九级体系，按年级推荐，闪卡学习
- 🔄 **间隔复习 (SRS)** — SM-2 算法，自动安排复习时间
- ✏️ **智能练习** — 选择题/填空/翻译/拼写，AI 根据年级出题
- 📝 **错题本** — 自动收集错题，支持重练
- 🤖 **AI 老师 Lily** — 入学评估 + 英语对话练习
- 📄 **学习集上传** — PDF/文本上传，AI 自动提取知识点并生成练习
- 📊 **学习统计** — 每日数据、连续学习天数、正确率趋势

## 目录结构

```
src/
├── api/           # API 接口层 (10 个模块)
├── components/    # 通用组件 (AppIcon, SpeakButton, Toast)
├── composables/   # 组合式函数 (Toast, TTS)
├── layouts/       # AppLayout 主布局
├── lib/           # 工具函数
├── router/        # 路由配置
├── stores/        # Pinia 状态 (user, theme)
├── styles/        # SCSS + Tailwind 全局样式
├── types/         # TypeScript 类型定义
└── views/         # 17 个页面视图
    ├── DashboardView      # 首页（每日学习教练）
    ├── LoginView          # 登录
    ├── AssessmentView     # 入学评估
    ├── ChatView           # AI Lily 对话
    ├── LevelListView      # 知识库等级列表
    ├── LevelDetailView    # 等级详情（单元列表）
    ├── LearnView          # 闪卡学习
    ├── PracticeView       # 练习
    ├── ReviewView         # SRS 间隔复习
    ├── MistakesView       # 错题本
    ├── StatsView          # 学习统计
    ├── BankListView       # 题库列表
    ├── BankDetailView     # 题库详情
    ├── UploadView         # 上传材料
    ├── StudySetDetailView # 学习集详情
    ├── StudySetReaderView # 学习集阅读
    └── SettingsView       # 设置
```
