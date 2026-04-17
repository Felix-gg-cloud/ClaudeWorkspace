# EnglishQuestArena — Copilot Instructions

## 项目概述
RPG 游戏化零基础英语学习系统。Vue 3 + Phaser 3 前端，计划 Spring Boot 3 后端。

## 核心约定

### 目标平台
- **仅桌面端浏览器**，不考虑移动端适配
- 不需要 responsive 布局、触控优化、PWA 等移动特性

### 前端技术栈
- Vue 3 + Vite + TypeScript (strict)
- Phaser 3 (Canvas 2D, 程序化纹理)
- Pinia 状态管理
- Vue Router 4 (hash mode)
- SCSS (Dark Fantasy 主题)
- Web Audio API (8-bit 音效合成)
- SpeechSynthesis API (TTS)

### 代码组织
- `src/composables/` — 可复用逻辑（useSound, useTaskCard, useReward）
- `src/data/` — 数据生成器（campGenerator, questGenerator）
- `src/mock/` — Mock 数据（后端接入后废弃）
- `src/game/` — Phaser 游戏引擎代码
- `src/components/ui/` — 11 种题卡组件
- `src/views/` — 8 个页面视图
- `src/stores/` — Pinia stores（user, chapter）

### 关键设计决策
1. **题卡组件映射**：使用 `composables/useTaskCard.ts` 的 `resolveTaskComponent()`，不要在 View 中重复导入 11 个题卡
2. **奖励计算**：使用 `composables/useReward.ts` 的 `grantReward()`，不要在各 View 中分散计算 XP/金币
3. **Quest 任务生成**：使用 `data/questGenerator.ts` 从 ContentItem[] 自动生成每日任务，不要硬编码题目
4. **Camp 地图生成**：使用 `data/campGenerator.ts` 从 ContentItem[] 自动生成怪物遭遇
5. **数据源**：所有词汇/句型使用 ContentItem 单一数据源（mock/chapters.ts），Camp/Quest/Boss/Arena 共用
6. **前端不判卷**：当前由题卡组件 emit correct/incorrect，后端接入后改为 POST 提交 + 后端判定

### 后端接入预留
- 所有 API 调用将集中在 `src/services/api.ts`（待创建）
- Stores 中的 mock 数据读取将替换为 API 调用
- 判卷逻辑将从前端移至后端
- XP/金币/等级计算将由后端返回

### 编码规范
- TypeScript strict 模式，不使用 `any`（用 `Component` 替代组件类型的 `any`）
- 组件类型用 `Record<string, Component>` 而非 `Record<string, any>`
- Phaser 物理体用 `as Phaser.Physics.Arcade.Body` 类型断言
- 数组随机取值后检查 undefined（`if (!item) return`）
- 迷雾等高频更新逻辑要做脏检查，避免每帧重绘

### 不做的事
- ❌ 不添加移动端相关代码（触控、响应式断点、PWA）
- ❌ 不在 View 中重复导入题卡组件映射
- ❌ 不在 View 中硬编码 XP/金币计算
- ❌ 不使用 console.log（开发调试除外且限于 DEV 环境）
