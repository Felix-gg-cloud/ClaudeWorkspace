# Task 03：Spring Security Session 认证

## 目标
实现基于 Session + Cookie 的登录认证体系。

## 范围

### Security 配置
- SecurityFilterChain 配置：
  - 放行：`POST /api/auth/login`、Swagger 路径（`/swagger-ui/**`、`/v3/api-docs/**`）
  - 其余 `/api/**` 需认证
  - 未认证返回 401 JSON（不要重定向到登录页）
  - 启用 CORS

### CORS 配置
- 允许 origin：`http://localhost:5173`
- 允许方法：GET, POST, PUT, DELETE, OPTIONS
- 允许凭证：`allowCredentials = true`
- 允许 headers：`*`

### API 接口
- `POST /api/auth/login` — 接收 JSON `{username, password}`，认证成功返回用户信息 + 设置 Session
- `POST /api/auth/logout` — 销毁 Session
- `GET /api/me` — 返回当前登录用户信息

### MVP 单用户初始化
- 启动时自动创建默认用户（若不存在）：
  - username: `admin`
  - password: 读取配置 `app.bootstrap.admin-password`（dev 默认值 `admin123`）
  - 密码使用 BCrypt 加密存储

## 验收标准
- [ ] 未登录访问 `/api/me` 返回 401
- [ ] 登录成功后访问 `/api/me` 返回用户信息
- [ ] 刷新页面登录态保持（Session Cookie 生效）
- [ ] 登出后 Session 失效
- [ ] CORS 配置正确，前端 5173 可跨域请求

## 依赖
Task 01, Task 02
