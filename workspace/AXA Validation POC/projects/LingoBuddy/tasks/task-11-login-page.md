# Task 11：前端 — 登录页面

## 目标
实现登录页面 UI 和登录/登出功能。

## 范围

### 页面 (`src/views/LoginView.vue`)
- 居中卡片式登录表单
- 字段：用户名、密码
- 登录按钮 + 加载状态
- 错误提示（用户名或密码错误）
- 登录成功后跳转 `/dashboard`

### API 调用
- `POST /api/auth/login` — `{ username, password }`
- 登录成功 → 存储用户信息到 Pinia store
- 登录失败 → ElMessage.error 提示

### 登出
- 顶栏用户头像下拉菜单 → 登出
- 调用 `POST /api/auth/logout`
- 清空 store → 跳转 `/login`

## 验收标准
- [ ] 登录页面 UI 美观，居中显示
- [ ] 输入正确账号密码可登录成功
- [ ] 错误密码有友好提示
- [ ] 登出后跳转到登录页

## 依赖
Task 03（后端认证接口）, Task 10（前端公共模块）
