# EnglishQuestArena — 英语 RPG 学习系统

零基础英语 RPG 养成系统。通过营地探索→每日主线→Boss 闯关的游戏流程，让英语学习像打游戏一样有趣。

## 项目状态

| 阶段 | 状态 |
|------|------|
| 阶段一：前端框架与交互 | ✅ 完成 |
| 阶段二：内容知识图谱 & Seed 数据 | ⬜ 未开始 |
| 阶段三：后端开发 | ⬜ 未开始 |
| 阶段四：交叉验证 & 质量保证 | ⬜ 未开始 |

详细任务文档见 `Tasks/` 目录。

## 技术栈

**前端** (当前目录)
- Vue 3 + TypeScript + Vite 7
- Pinia + Vue Router 4 (Hash 模式)
- Phaser 3 (营地探索游戏引擎)
- Web Audio API (8-bit 音效)
- SpeechSynthesis API (TTS 发音)

**后端** (待开发)
- Spring Boot 3.x + Java 17
- Spring Security (Session + Cookie)
- Spring Data JPA + Flyway
- H2 (开发) / PostgreSQL (生产)

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

默认登录: 任意用户名密码 (当前 mock 模式)

## 核心功能

- 🏕️ **营地探索**: Phaser 3 驱动的 RPG 地图，遇怪触发三阶段单词学习
- ⚔️ **每日主线**: 30 道题/天，11 种题型交替
- 💀 **Boss 战**: HP 对决 + 连击系统
- 🏟️ **SRS 副本**: 间隔重复复习 (待接入 SM-2 算法)
- 🌟 **技能星图**: 技能点解锁被动技能
- 🔔 **CEFR 等级测验**: 测验通过升级英语等级

## 目录结构

```
src/
├── views/          # 8 个页面视图
├── components/ui/  # 11 种题卡 + 遭遇弹窗
├── game/           # Phaser 3 游戏 (场景/精灵/纹理/地图)
├── stores/         # Pinia 状态管理
├── composables/    # 音效系统
├── mock/           # Mock 数据 (后端接入后废弃)
├── data/           # 数据生成器
├── types/          # TypeScript 类型定义
├── layouts/        # 主布局
├── router/         # 路由配置
└── styles/         # SCSS 全局样式
```
