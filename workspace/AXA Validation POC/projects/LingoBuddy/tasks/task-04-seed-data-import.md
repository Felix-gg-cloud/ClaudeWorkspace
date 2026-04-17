# Task 04：Seed JSON 导入机制

## 目标
实现启动时自动读取 JSON 文件导入种子数据，保证幂等。

## 范围

### 导入器
- 创建 `SeedDataImporter`（实现 `ApplicationRunner` 或 `@PostConstruct`）
- 读取 `src/main/resources/seed/*.json`
- 导入顺序（外键依赖）：
  1. `level_config.json` → LevelConfig
  2. `stages.json` → Stage
  3. `lessons.json` → Lesson
  4. `tasks.json` → Task
  5. `achievements.json` → Achievement

### 幂等机制
- 使用 `SeedImportLog` 表记录已导入文件
- 每次启动检查 `fileName` 是否已存在，跳过已导入
- 或使用实体唯一键（code/taskCode）做 insertIfNotExists

### Seed 数据内容（MVP 最小量）
- **LevelConfig**: Level 1~3，对应 requiredXp 阈值
- **Stage**: 每 Level 1 个 Stage（共 3 个）
- **Lesson**: 每 Stage 5 天（共 15 个 Lesson），dayIndex 1~5
- **Task**: 每 Lesson 5 个 Task（共 75 个）
  - 类型分布：VOCAB_CARD / QUIZ_SINGLE / SPELLING
  - 主题：greetings, daily life, simple verbs, numbers, time
- **Achievement**: 至少 5 条
  - 如：首次登录、连续打卡3天、完成Level 1、学习50个单词、连续打卡7天

## 验收标准
- [ ] 首次启动成功导入所有 seed 数据
- [ ] 重复启动不重复插入
- [ ] 数据库中可查到 75 个 task、15 个 lesson、5 个 achievement

## 依赖
Task 02（实体定义）
