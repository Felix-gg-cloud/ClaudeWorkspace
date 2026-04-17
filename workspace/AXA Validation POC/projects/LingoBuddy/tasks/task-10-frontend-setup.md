# Task 10：前端项目初始化与公共模块

## 目标
搭建 Vue3 前端基础框架，完成公共模块配置。

## 范围

### 项目配置
- Vite + Vue3 + TypeScript
- 安装并配置：vue-router、pinia、axios、element-plus
- vite.config.ts 确认端口 5173

### axios 封装 (`src/api/request.ts`)
- baseURL: `http://localhost:8080/api`
- `withCredentials: true`
- 响应拦截器：401 → 跳转 `/login`
- 错误统一处理（ElMessage 提示）

### 路由配置 (`src/router/index.ts`)
- `/login` — 登录页
- `/dashboard` — 仪表盘（需登录）
- `/lesson/today` — 今日课时（需登录）
- `/calendar` — 打卡日历（需登录）
- `/profile` — 个人成就（需登录）
- 路由守卫：未登录重定向到 `/login`

### Store (`src/stores/user.ts`)
- Pinia store 管理用户状态
- 登录/登出 action
- 持久化登录态判断

### 布局组件
- `AppLayout.vue` — 侧边栏 + 顶栏 + 内容区
- 导航菜单：Dashboard、今日课时、打卡日历、我的成就

## 验收标准
- [ ] `npm run dev` 启动成功
- [ ] 路由跳转正常
- [ ] axios 封装可正确发送带 cookie 的请求
- [ ] Element Plus 组件可正常渲染

## 依赖
Task 01（前端骨架）
