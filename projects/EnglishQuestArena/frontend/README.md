# EnglishQuestArena — 英语 RPG 学习系统

零基础英语 RPG 养成系统。通过营地探索→每日主线→Boss 闯关的游戏流程，让英语学习像打游戏一样有趣。

## 项目状态

| 阶段 | 状态 | 说明 |
|------|------|------|
| 阶段一：前端框架与交互 | ✅ 完成 | 10 页面 / 11 题型 / Phaser 营地 / 6 Store |
| 阶段 1.5：内容扩展 & 体验打磨 | ✅ 完成 | CH1 词表扩充至 40 词 / NPC VN 对话 / 怪物多样化 / 迷雾系统 |
| 阶段二：内容知识图谱 & Seed 数据 | ⬜ 未开始 | — |
| 阶段三：后端开发 | ⬜ 未开始 | — |
| 阶段四：交叉验证 & 质量保证 | ⬜ 未开始 | — |

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

- 🏕️ **营地探索**: Phaser 3 驱动的 RPG 地图 (24×18)，40 个单词怪物 + 3 NPC + 4 宝箱
  - 5 种怪物造型: 🍄蘑菇 / 🌵仙人掌 / 🐛毛毛虫 / 🦇独眼蝙蝠 / 😈暗影恶魔
  - 4 种 NPC 立绘: 向导 / 贤者 / 骑士 / 商人 (Visual Novel 全屏对话)
  - 迷雾系统: 探索时逐步揭露地图
- ⚔️ **每日主线**: 30 道题/天，11 种题型交替
- 💀 **Boss 战**: HP 对决 + 连击系统
- 🏟️ **SRS 副本**: 间隔重复复习 (待接入 SM-2 算法)
- 🌟 **技能星图**: 技能点解锁被动技能
- 📊 **竞争力分析**: 用户数据统计可视化
- 🔔 **CEFR 等级测验**: 测验通过升级英语等级

## 目录结构

```
src/
├── views/          # 10 个页面视图
├── components/     # 11 种题卡 + 遭遇弹窗 + NPC 立绘 + Boss SVG
├── game/           # Phaser 3 游戏 (场景/精灵/纹理/地图)
├── stores/         # 6 个 Pinia 状态管理
├── composables/    # 音效/奖励/题卡 组合式函数
├── mock/           # 3 章 Mock 数据 (后端接入后废弃)
├── data/           # 营地生成器 / 主线生成器 / 覆盖率检查
├── types/          # TypeScript 类型定义
├── layouts/        # 主布局
├── router/         # 路由配置
└── styles/         # SCSS 全局样式
```

## 章节数据

| 章节 | 标题 | CEFR | 天数 | 词量 | 地图 |
|------|------|------|------|------|------|
| PRE_A1_CH1 | First Contact (初次接触) | PRE_A1 | 5 | 40 词 + 8 短语 | 24×18, 45 怪物位 |
| PRE_A1_CH2 | Numbers & Colors (数字与颜色) | PRE_A1 | 5 | Mock | 20×15 |
| PRE_A1_CH3 | Family & Animals (家庭与动物) | PRE_A1 | 5 | Mock | 20×15 |

章节流程: `locked → camp → quest → boss → completed`

## 游戏流程

```
登录 → 仪表盘
         ├── 营地探索 (Phaser RPG)
         │     └── 遭遇怪物 → 三选一答题 → 击败/失败
         │     └── NPC 对话 (Visual Novel)
         │     └── 开宝箱 → 获得奖励
         ├── 每日主线 (30 题/天, 11 题型)
         ├── Boss 战 (HP 对决)
         ├── SRS 副本 (间隔重复)
         ├── 技能星图
         ├── 竞争力分析
         └── CEFR 等级测验
```

## 更新日志

### v0.2.0 — 阶段 1.5 (2026-03-13)
- **内容扩展**: CH1 词表从 6 词扩充到 40 词 + 8 短语
- **NPC 体验**: Visual Novel 全屏对话系统，4 种 NPC 立绘升级
- **怪物多样化**: 5 种全新造型 (蘑菇/仙人掌/毛毛虫/蝙蝠/恶魔)，Easy 怪物 3 种子变体轮换
- **迷雾系统**: 半径 3 格逐步揭露地图
- **竞争力分析**: StatsView 数据统计页面
- **Bug 修复**: 地图数据注入 (defaultCampMap → ch1CampMap)、tile 重叠、ellipse 参数

### v0.1.0 — 阶段一 (2026-03-12)
- 前端框架完整实现: 10 页面 / 11 题型组件 / 6 Store
- Phaser 3 营地探索: 地图渲染 / 碰撞检测 / 遭遇系统
- Boss 战: HP 对决 + 连击系统
- 程序化纹理生成: 地形 / 装饰物 / 角色 / 怪物
