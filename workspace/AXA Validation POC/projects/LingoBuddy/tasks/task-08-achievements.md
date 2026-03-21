# Task 08：成就与徽章系统

## 目标
实现成就解锁检测和展示。

## 范围

### 成就检测
- 在关键事件后触发成就检查：
  - 完成 task 后
  - 打卡后
  - 升级后
- 条件类型（conditionType）：
  - `FIRST_LOGIN` — 首次登录
  - `STREAK_N` — 连续打卡 N 天（conditionValue = N）
  - `LEVEL_N` — 达到 N 级
  - `TOTAL_TASKS_N` — 累计完成 N 个 task
  - `TOTAL_CHECKIN_N` — 累计打卡 N 天

### 解锁逻辑
- 检查用户是否满足条件且尚未解锁
- 满足则写入 `UserAchievement`
- 返回新解锁的成就信息给前端

### API
- `GET /api/achievements` — 返回所有成就列表 + 用户解锁状态

## 验收标准
- [ ] 满足条件时自动解锁成就
- [ ] 不重复解锁
- [ ] `/api/achievements` 正确区分已解锁/未解锁
- [ ] 至少 5 个成就可正常触发

## 依赖
Task 06（打卡触发）, Task 07（升级触发）
