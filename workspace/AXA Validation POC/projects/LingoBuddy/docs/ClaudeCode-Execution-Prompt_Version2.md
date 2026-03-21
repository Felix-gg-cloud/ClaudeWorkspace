# 给 Claude Code 的执行提示词（直接复制粘贴使用）— Maven 版本

你现在是资深全栈工程师，请基于下述 PRD 生成一个可运行的前后端分离项目（Vue3 + Spring Boot 3），并保证本地可一键启动（分别启动前端与后端即可）。要求优先完成 MVP 的可运行闭环。

## 关键信息（必须遵守）
- 构建工具：后端使用 **Maven**
- 前端地址：`http://localhost:5173`
- 后端地址：`http://localhost:8080`
- 鉴权：Spring Security **Session + Cookie**（不是 JWT）
- 前端请求必须 `withCredentials: true`
- 后端必须配置 CORS：允许 origin `http://localhost:5173` 且 `allowCredentials=true`
- MVP 为单用户，但数据库设计必须可扩展多用户：所有业务数据都带 `user_id`

## 需要你生成的仓库结构（建议）
- `/backend`：Spring Boot 项目（Maven）
- `/frontend`：Vue3 项目
- 根目录 README：分别如何启动、默认账号密码、如何导入 seed、Swagger 地址

## 后端（/backend）要求
1. 技术栈（pom.xml 体现）
   - Spring Boot 3.x
   - spring-boot-starter-web
   - spring-boot-starter-validation
   - spring-boot-starter-security（Session）
   - spring-boot-starter-data-jpa
   - H2（dev）
   - PostgreSQL driver（可选，prod profile）
   - springdoc-openapi-starter-webmvc-ui（Swagger）
   - test：spring-boot-starter-test
2. 安全与接口
   - 放行：`POST /api/auth/login`、Swagger 相关
   - 其余 `/api/**` 需登录
   - 提供接口：
     - `POST /api/auth/login`（JSON：{username,password}）
     - `POST /api/auth/logout`
     - `GET /api/me`
     - `GET /api/today`
     - `POST /api/tasks/{taskId}/complete`
     - `GET /api/checkin/calendar?month=YYYY-MM`
     - `POST /api/checkin/today`（即使自动打卡也保留，幂等）
     - `GET /api/achievements`
3. 业务逻辑（MVP 闭环）
   - 今日课时：按 dayIndex 顺序取“今日 lesson”，返回 lesson、tasks、完成状态
   - 完成 task：写入 task_progress；结算 task XP；若当日 lesson 全部完成则自动打卡
   - 打卡：写入 daily_checkin（同日幂等），计算 streak，结算每日奖励与随机 coins
   - 等级：由 level_config（requiredXp）计算 currentLevel；升级时返回给前端一个“升级事件”字段
4. Seed（JSON 导入）
   - 启动时读取 `src/main/resources/seed/*.json` 导入数据库
   - 必须幂等（避免重复导入）
   - 至少提供：
     - Level 1~3（level_config）
     - 每级 5 天 lesson，每天 5 tasks（≥75 tasks）
     - achievements ≥ 5
   - 建议加 `seed_import_log` 或唯一 code 防重
5. 测试（JUnit）
   - 至少 3 个单元测试：
     - Level 计算
     - Streak 计算
     - Checkin 幂等

## 前端（/frontend）要求
1. 技术栈
   - Vue3 + Vite + TypeScript
   - Pinia、Vue Router
   - axios（统一封装 API，`withCredentials: true`）
   - UI：Element Plus 或 Naive UI（二选一）
2. 页面
   - `/login`：登录
   - `/dashboard`：等级、XP 进度、streak、今日任务进度
   - `/lesson/today`：今日课时任务列表与交互（完成任务/答题）
   - `/calendar`：月历打卡展示
   - `/profile`：成��/徽章
3. 交互
   - 完成任务后立即刷新状态
   - 今日全部完成后显示“打卡成功 + 获得 XP/coins + 是否升级”
   - 升级时弹窗提示（简单即可）

## 文档与启动
- README 必须包含：
  - 后端启动：`cd backend && mvn spring-boot:run`（端口 8080）
  - 前端启动：`cd frontend && npm i && npm run dev`（端口 5173）
  - 默认账号密码来源（`app.bootstrap.admin-password` 或 dev 默认值）
  - Swagger 地址
  - 常见问题：CORS + cookie、401 处理
- 提供 `.env.example`（前端）与 `application.yml`（后端）示例配置

## 输出要求
- 直接生成可编译运行的代码与文件
- 任何关键文件（security 配置、CORS、seed 导入器、核心 service）请写清晰注释