# Task 06：打卡与 Streak 逻辑

## 目标
实现每日打卡、连续打卡 Streak 计算和奖励结算。

## 范围

### 打卡逻辑
- 当日所有 task 完成 → 自动写入 `DailyCheckin`
- 幂等：同一用户同一天只能有一条记录（唯一约束 `userId + checkinDate`）
- 保留手动打卡接口 `POST /api/checkin/today`（用于修复/补卡场景）

### Streak 计算
- 查询用户最近打卡记录，计算连续天数
- streak 写入当天的 DailyCheckin 记录
- 断打卡 streak 归零

### 奖励结算
- 每日基础 XP 奖励（可配置，如 +20 XP）
- Streak 加成（如 streak ≥ 3 额外 +10 XP）
- 随机 coins（1~10 随机）
- 奖励写入 DailyCheckin 的 xpEarned / coinsEarned
- 更新用户 totalXp 和 coins

### 日历 API
- `GET /api/checkin/calendar?month=YYYY-MM` — 返回该月所有打卡日期及详情

## 验收标准
- [ ] 完成当日所有 task 后自动生成打卡记录
- [ ] 同日重复触发不产生多条记录
- [ ] Streak 连续计算正确，断打卡归零
- [ ] 日历 API 返回正确的月度打卡数据

## 依赖
Task 05（完成任务触发打卡）
