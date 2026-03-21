# Task 02：数据库设计与 JPA 实体

## 目标
创建所有 MVP 所需的数据库表和 JPA Entity 类。

## 范围

### 实体清单
1. **User** — id, username, password, startDate, totalXp, coins, currentLevel, createdAt
2. **LevelConfig** — id, level, requiredXp, title, description
3. **Stage** — id, levelId, name, sortOrder
4. **Lesson** — id, stageId, dayIndex, title, description
5. **Task** — id, lessonId, taskCode(唯一), type(VOCAB_CARD/QUIZ_SINGLE/SPELLING), question, options(JSON), correctAnswer, xpReward, sortOrder
6. **TaskProgress** — id, userId, taskId, completed, completedAt
7. **DailyCheckin** — id, userId, checkinDate(唯一约束 userId+date), xpEarned, coinsEarned, streak
8. **Achievement** — id, code(唯一), name, description, iconUrl, conditionType, conditionValue
9. **UserAchievement** — id, userId, achievementId, unlockedAt
10. **SeedImportLog** — id, fileName, importedAt（幂等控制）

### 设计要点
- 所有业务表带 `user_id` 外键（为多用户扩展准备）
- DailyCheckin 加唯一约束：`(userId, checkinDate)`
- Task 加唯一约束：`taskCode`
- Achievement 加唯一约束：`code`
- Task.type 使用枚举：`TaskType { VOCAB_CARD, QUIZ_SINGLE, SPELLING }`

## 验收标准
- [ ] 所有 Entity 类创建完成，JPA 注解正确
- [ ] H2 启动后自动建表成功
- [ ] 表关系和约束符合 PRD 设计

## 依赖
Task 01（项目骨架）
