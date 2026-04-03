# Phase 0：微服务架构 + 全新 UI 框架

> 目标：搭建微服务基础设施 + 全新前端 UI 框架，跑通注册→登录→首页全流程。

---

## 任务总览

| # | 任务 | 类型 | 状态 |
|---|------|------|------|
| 0.1 | [Maven 多模块骨架](#01-maven-多模块骨架) | 后端 | ✅ |
| 0.2 | [Docker Compose (PG + MinIO)](#02-docker-compose) | 基础设施 | ✅ |
| 0.3 | [common 公共模块](#03-common-公共模块) | 后端 | ✅ |
| 0.4 | [Gateway 路由 + JWT 过滤器](#04-gateway) | 后端 | ✅ |
| 0.5 | [service-user 核心功能](#05-service-user) | 后端 | ✅ |
| 0.6 | [service-content 骨架](#06-service-content-骨架) | 后端 | ✅ |
| 0.7 | [service-ai 骨架](#07-service-ai-骨架) | 后端 | ✅ |
| 0.8 | [前端全新 UI 框架](#08-前端全新-ui-框架) | 前端 | ✅ |
| 0.9 | [前端登录注册页](#09-前端登录注册页) | 前端 | ✅ |
| 0.10 | [前端仪表盘骨架](#010-前端仪表盘骨架) | 前端 | ✅ |
| 0.11 | [全流程验证](#011-全流程验证) | 测试 | ✅ |

---

## 0.1 Maven 多模块骨架

**目标**：创建 Maven 父子模块结构，所有模块可编译通过。

**产出目录结构**：
```
LinguaLeap/
├── pom.xml                          ← 父 POM
├── common/
│   ├── pom.xml
│   └── src/main/java/com/ll/common/
│       ├── dto/                     ← 公共 DTO
│       ├── exception/               ← 全局异常
│       └── util/                    ← 工具类
├── gateway/
│   ├── pom.xml
│   └── src/main/java/com/ll/gateway/
├── service-user/
│   ├── pom.xml
│   └── src/main/java/com/ll/user/
├── service-content/
│   ├── pom.xml
│   └── src/main/java/com/ll/content/
├── service-ai/
│   ├── pom.xml
│   └── src/main/java/com/ll/ai/
├── frontend/                        ← Vue 3（已有）
└── docker-compose.yml
```

**父 POM 关键配置**：
- Java 17
- Spring Boot 3.4.x (parent)
- Spring Cloud 2024.x
- 统一依赖版本管理

**各模块依赖关系**：
- `common` → 无外部依赖，纯 Java
- `gateway` → Spring Cloud Gateway + common
- `service-user` → Spring Boot Web + JPA + PostgreSQL + common
- `service-content` → Spring Boot Web + JPA + PostgreSQL + MinIO + common
- `service-ai` → Spring Boot Web + Spring AI + common

**验收标准**：
- [ ] `mvn clean compile` 全部模块通过
- [ ] 各模块可独立启动（空的 main class）

---

## 0.2 Docker Compose

**目标**：一键启动 PostgreSQL + MinIO 基础设施。

**docker-compose.yml 内容**：
- `postgres:16` — 端口 5432，创建 3 个数据库：ll_user / ll_content / ll_ai
- `minio/minio` — 端口 9000(API) + 9001(Console)，创建 bucket: `ll-files`

**环境变量**：
```
POSTGRES_USER=lingualeap
POSTGRES_PASSWORD=lingualeap123
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
```

**验收标准**：
- [ ] `docker compose up -d` 一键启动
- [ ] 可用 psql/pgAdmin 连接 3 个数据库
- [ ] 可访问 MinIO Console http://localhost:9001

---

## 0.3 common 公共模块

**目标**：提取公共代码，供所有服务使用。

**内容**：

| 类 | 用途 |
|----|------|
| `ApiResponse<T>` | 统一响应包装：code/message/data |
| `JwtUtil` | JWT 生成/解析/验证 |
| `GlobalExceptionHandler` | 统一异常处理（返回 ApiResponse） |
| `BizException` | 业务异常（含 code） |
| `UserContext` | ThreadLocal 存储当前用户 ID（从 Gateway 传递） |

**验收标准**：
- [ ] common 可编译
- [ ] JwtUtil 单元测试通过

---

## 0.4 Gateway

**目标**：Spring Cloud Gateway 网关，路由转发 + JWT 统一鉴权。

**路由配置**：
```yaml
routes:
  - id: user-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/auth/**, /api/user/**
  - id: content-service
    uri: http://localhost:8082
    predicates:
      - Path=/api/content/**
  - id: ai-service
    uri: http://localhost:8083
    predicates:
      - Path=/api/ai/**
```

**JWT 过滤器逻辑**：
- `/api/auth/**` 放行（登录/注册不需要 token）
- 其余路由检查 `Authorization: Bearer <token>`
- 校验通过：把 `X-User-Id` 写入请求头，转发给下游
- 校验失败：返回 401

**验收标准**：
- [ ] Gateway 启动在 8080
- [ ] 无 token 请求 /api/user/** → 401
- [ ] 有效 token 请求 → 转发到下游服务

---

## 0.5 service-user

**目标**：用户注册/登录/信息查询，JWT 鉴权。

**API 列表**：
| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册 | 无 |
| POST | `/api/auth/login` | 登录，返回 JWT | 无 |
| GET | `/api/user/me` | 获取当前用户信息 | 需要 |
| PUT | `/api/user/me` | 更新用户信息（年级等） | 需要 |

**数据库表**：
```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    grade           VARCHAR(20) NOT NULL DEFAULT 'junior',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

**关键实现**：
- 密码用 BCrypt 加密
- 登录成功返回 JWT（含 userId）
- `/api/user/me` 从请求头 `X-User-Id` 获取用户 ID

**验收标准**：
- [ ] 注册接口可创建用户
- [ ] 登录接口返回有效 JWT
- [ ] 通过 Gateway 访问 /api/user/me 返回用户信息

---

## 0.6 service-content 骨架

**目标**：创建服务骨架，Flyway 初始化数据库，健康检查可用。

**最小实现**：
- Spring Boot 启动类
- `application.yml` 连接 ll_content 数据库
- Flyway V1 迁移脚本（创建 question_bank / knowledge_point / question 表）
- 健康检查端点 `GET /api/content/health`

**验收标准**：
- [ ] 服务启动在 8082
- [ ] Flyway 自动建表
- [ ] 健康检查返回 200

---

## 0.7 service-ai 骨架

**目标**：创建服务骨架，预留 AI 接口。

**最小实现**：
- Spring Boot 启动类
- `application.yml` 连接 ll_ai 数据库
- Flyway V1 迁移脚本（创建 ai_analysis_cache / ai_call_log 表）
- 健康检查端点 `GET /api/ai/health`

**验收标准**：
- [ ] 服务启动在 8083
- [ ] Flyway 自动建表
- [ ] 健康检查返回 200

---

## 0.8 前端全新 UI 框架

**目标**：重写前端骨架，建立全新简约 UI。

**步骤**：
1. 删除所有游戏相关代码（Phaser/CampScene/BossView 等）
2. 移除 phaser 依赖
3. 创建新的布局组件（AppLayout）：
   - PC 端：顶部栏 + 左侧导航 + 内容区
   - 移动端：顶部栏 + 内容区 + 底部 Tab
4. 建立设计 token（CSS 变量 / SCSS 变量）：
   - 浅色模式配色
   - 深色模式配色
   - 间距/圆角/阴影
5. 主题切换功能（浅色/深色）
6. 路由重构（新页面结构）

**新路由表**：
```typescript
/login          → LoginView
/               → DashboardView (需要登录)
/banks          → BankListView
/banks/create   → BankCreateView
/banks/:id      → BankDetailView
/practice       → PracticeView
/practice/result → PracticeResultView
/review         → ReviewView
/mistakes       → MistakeListView
/stats          → StatsView
/settings       → SettingsView
```

**验收标准**：
- [ ] 新布局在 PC 和移动端正常显示
- [ ] 浅色/深色主题可切换
- [ ] 所有路由可访问（页面内容可以是占位符）
- [ ] Phaser 及游戏代码完全移除

---

## 0.9 前端登录注册页

**目标**：简洁的登录/注册页面，对接 service-user API。

**设计**：
- 单页切换（登录 ↔ 注册 Tab）
- 注册：用户名 + 密码 + 确认密码 + 选择年级
- 登录：用户名 + 密码
- 登录成功：存储 JWT → 跳转首页
- 错误提示：友好的行内提示

**API 对接**：
- 通过 Gateway 走 `/api/auth/register` 和 `/api/auth/login`
- JWT 存储到 localStorage

**验收标准**：
- [ ] 注册 → 自动登录 → 进入首页
- [ ] 登录 → 进入首页
- [ ] 未登录访问其他页面 → 跳转登录页
- [ ] 错误提示正常显示

---

## 0.10 前端仪表盘骨架

**目标**：首页基本框架，展示占位内容。

**布局**：
```
┌─────────────────────────────────┐
│  👋 你好，{用户名}              │
│  当前年级：{年级}               │
├─────────────────────────────────┤
│  📊 今日学习    [ 开始学习 → ]   │
│  0/0 题完成     0% 正确率       │
├─────────────────────────────────┤
│  📚 我的题库                    │
│  (Phase 1 实现)                 │
├─────────────────────────────────┤
│  🔄 待复习                      │
│  (Phase 1 实现)                 │
└─────────────────────────────────┘
```

**验收标准**：
- [ ] 显示当前用户名和年级
- [ ] 各区块占位正常
- [ ] 布局美观、响应式

---

## 0.11 全流程验证

**目标**：端到端验证所有组件协同工作。

**测试流程**：
1. `docker compose up -d` → PG + MinIO 启动
2. 启动 gateway (8080)
3. 启动 service-user (8081)
4. 启动 service-content (8082)
5. 启动 service-ai (8083)
6. 启动 frontend (5173)
7. 打开浏览器 → 注册新用户 → 登录 → 看到首页
8. 访问各页面（骨架页）确认路由正常
9. 验证 Gateway 鉴权（无 token → 401）

**验收标准**：
- [ ] 全部服务启动无错误
- [ ] 注册 → 登录 → 首页 全流程通
- [ ] Gateway 鉴权正常工作
- [ ] 前端所有路由可访问

---

## 执行顺序建议

```
0.1 Maven 骨架
 ↓
0.2 Docker Compose
 ↓
0.3 common 模块
 ↓
0.4 Gateway ←─── 0.5 service-user (可并行)
 ↓
0.6 service-content 骨架
 ↓
0.7 service-ai 骨架
 ↓
0.8 前端 UI 框架
 ↓
0.9 前端登录注册
 ↓
0.10 前端仪表盘
 ↓
0.11 全流程验证
```
