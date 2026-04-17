# Task 05：课程与今日课时 API

## 目标
实现"今日课时"核心逻辑，用户可查看并完成每日学习任务。

## 范围

### 今日课时规则
- 以用户 `startDate` 为 Day 1
- 今日 dayIndex = `(today - startDate).days + 1`
- 从已解锁 Stage（由 currentLevel 决定）中按 dayIndex 找到对应 Lesson
- 若今日已全部完成，标记为"今日已完成"

### API
- `GET /api/today` — 返回：
  - 当前 lesson 信息（title, description）
  - tasks 列表（含每个 task 的完成状态）
  - 今日是否已完成
  - 当日 dayIndex

- `POST /api/tasks/{taskId}/complete` — 完成单个 task：
  - 写入 TaskProgress（幂等：已完成不重复写）
  - 结算 task 的 xpReward，更新用户 totalXp
  - 返回：获得的 XP、用户当前 totalXp、是否升级
  - 若当日所有 task 完成 → 触发自动打卡（调用 Task 06 的打卡逻辑）

### Service 层
- `TodayLessonService` — 获取今日课时
- `TaskService` — 完成任务、结算 XP

## 验收标准
- [ ] `/api/today` 返回正确的今日 lesson 和 tasks
- [ ] 完成 task 后 XP 正确增加
- [ ] 重复完成同一 task 不重复计算 XP
- [ ] 今日所有 task 完成后自动触发打卡

## 依赖
Task 03（认证）, Task 04（seed 数据）
