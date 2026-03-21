# 英文学习打卡网站（Vue + Spring Boot）需求文档（Claude Code 用，前后端分离 + Session + JSON Seed）

## 0. 项目定位
- 阶段1（MVP）：单用户英文学习打卡网站（仅站点拥有者）
- 阶段2：扩展多用户（保留 userId 外键、数据隔离）
- 前端：Vue3 + Vite + TypeScript + Pinia + Vue Router + UI 组件库（Element Plus/Naive UI）
- 后端：Spring Boot 3.x + Spring Security（**Session/Cookie**）+ Spring Data JPA + H2(dev)/PostgreSQL(prod)
- 启动形态：**前后端分离**
  - 前端：`http://localhost:5173`
  - 后端：`http://localhost:8080`
  - 认证：Cookie Session（前端请求需 `withCredentials: true`）
  - 后端配置 CORS：允许 `http://localhost:5173`，并开启 `allowCredentials=true`

## 1. 核心闭环
今日课时（5~15分钟）→ 完成任务 → 自动打卡 → 获得 XP/coins → 升级解锁下一阶段 → 成就/徽章反馈。

## 2. 登录与安全（Spring Security Session）
### 2.1 认证方式
- Spring Security + Session Cookie
- 接口：
  - `POST /api/auth/login`（JSON：{username,password}）
  - `POST /api/auth/logout`
  - `GET /api/me`（返回当前用户信息）
- 前端：
  - axios/fetch 必须开启 `withCredentials: true`
  - 未登录访问业务接口应返回 401，前端跳转 /login

### 2.2 单用户初始化（MVP）
- 系统启动时确保存在默认用户：
  - username: `admin`
  - password：来自配置 `app.bootstrap.admin-password`（开发环境可给默认值，生产必须显式配置）
- 所有业务表都必须带 `user_id`（为未来多用户扩展准备）

## 3. 课程与打卡（MVP）
### 3.1 课程结构
- Level -> Stage -> Lesson(按 dayIndex 顺序) -> Task
- 每个 Lesson 约 5 个 Task
- Task 类型（MVP 做 3 种）：
  1) VOCAB_CARD：单词卡片（展示即可，点“已学习”算完成）
  2) QUIZ_SINGLE：单选题（提交答案判对错）
  3) SPELLING：拼写/填空（输入英文，做基础归一化匹配）

### 3.2 今日课时规则（建议）
- 以“用户首次开始学习日期 startDate”为 Day1（可存到 user profile）
- 今日 lesson = 从已解锁 stage 中按 dayIndex 顺序取第 N 天
- 若今日已完成，展示“今日已完成，可复习（进入 lesson 详情）”

### 3.3 打卡规则
- 当日所有任务完成 -> 自动打卡：
  - 写入 `daily_checkin`（幂等：同一天重复触发不重复写）
  - 更新 streak
  - 结算每日奖励 XP 与随机 coins

## 4. 等级/经验/解锁���MVP）
- XP：
  - 每个 task 有 xpReward
  - 完成当日 lesson 额外 +dailyBonusXp（可配置）
  - streak 加成（可配置）
- Level：
  - 由 `level_config` 阈值表计算 currentLevel
  - 升级时解锁下一 level 对应 stage/lesson

## 5. 游戏化（MVP）
至少实现：
- streak 连胜
- achievements 成就（≥5）
- badges 徽章展示
- coins 随机奖励（装饰用途）

## 6. 数据看板（MVP）
- Dashboard：等级、XP 进度条、streak、今日任务完成度、今日奖励结果
- Calendar：按月打卡日历
- Profile：成就/徽章/称号（称号可与 Level 对应）

## 7. JSON Seed 导入（关键确定项）
### 7.1 目标
- 不用 Flyway 写大量插入 SQL
- 在后端启动时读取 `src/main/resources/seed/*.json` 并导入数据库
- 幂等：重复启动不应重复插入同一份 seed（需有唯一键/版本号/导入标记）

### 7.2 具体要求
- 建议文件结构：
  - `seed/levels.json`
  - `seed/stages.json`
  - `seed/lessons.json`
  - `seed/tasks.json`
  - `seed/achievements.json`
- 设计一个 `seed_import_log` 表或在关键表使用唯一键（如 code、(stageId,dayIndex)、taskCode）保证幂等
- 导入顺序必须保证外键依赖（levels -> stages -> lessons -> tasks）

### 7.3 seed 内容（MVP 最小可用）
- Level 1~3
- 每级 5 天 lesson（共 15 lessons）
- 每天 5 tasks（共 75 tasks）
- 主题：greetings / daily life / simple verbs / numbers / time（可简化）
- achievements 至少 5 条（code 唯一）

## 8. 技术实现要求（Claude Code）
### 8.1 后端
- Spring Boot 3.x + JPA + Security(Session) + Validation + OpenAPI
- CORS：
  - 允许来源 `http://localhost:5173`
  - 允许凭据 `allowCredentials=true`
  - 允许方法 GET/POST/PUT/DELETE
- Security：
  - 放行：`/api/auth/login`、swagger 相关路径
  - 其他 `/api/**` 需要认证
- 单元测试（至少）：
  - Level 计算：给定 totalXp -> 正确 level
  - Streak 计算：连续日期打卡 -> streak 正确
  - Checkin 幂等：同日多次调用只产生一条记录

### 8.2 前端
- axios 封装：
  - baseURL 指向 `http://localhost:8080/api`
  - `withCredentials: true`
- 页面：
  - Login
  - Dashboard
  - TodayLesson
  - Calendar
  - Profile (Achievements/Badges)

## 9. API（MVP）
- Auth
  - POST /api/auth/login
  - POST /api/auth/logout
  - GET /api/me
- Today/Lesson
  - GET /api/today
  - POST /api/tasks/{taskId}/complete
- Check-in
  - POST /api/checkin/today（可选，如果自动打卡则仍保留用于手动触发/修复）
  - GET /api/checkin/calendar?month=YYYY-MM
- Achievements
  - GET /api/achievements

## 10. 验收标准（DoD）
- 前端 5173 + 后端 8080 分离运行，登录后刷新页面仍保持登录态（cookie session 生效）
- 今日课时能完成任务、记录 progress
- 完成后自动打卡成功，月历可见
- XP 增长与升级正确，升级解锁新课程
- 成就能解锁与展示
- seed JSON 启动导入幂等，不重复插入