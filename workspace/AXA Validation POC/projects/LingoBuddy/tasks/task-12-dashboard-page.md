# Task 12：前端 — Dashboard 页面

## 目标
实现数据看板，展示学习概览。

## 范围

### 页面 (`src/views/DashboardView.vue`)

#### 顶部概览卡片（一行 4 个）
- 当前等级（Level N + 称号）
- XP 进度条（当前 XP / 下一级所需 XP）
- 连续打卡天数（Streak 🔥）
- 累计 Coins

#### 今日任务卡片
- 今日 Lesson 标题
- 任务完成进度（如 3/5）
- 进度条
- "开始学习" 按钮 → 跳转 `/lesson/today`
- 若今日已完成 → 显示 ✅ 已完成

#### 最近成就
- 展示最近解锁的 2~3 个成就徽章
- "查看全部" → 跳转 `/profile`

### API 调用
- `GET /api/me` — 用户基本信息
- `GET /api/today` — 今日任务进度
- `GET /api/achievements` — 成就列表（取最近解锁的）

## 验收标准
- [ ] 页面数据正确展示
- [ ] XP 进度条比例正确
- [ ] Streak 数值正确
- [ ] 响应式布局（移动端 2 列，桌面 4 列）

## 依赖
Task 05（today API）, Task 07（等级数据）, Task 10（前端框架）
