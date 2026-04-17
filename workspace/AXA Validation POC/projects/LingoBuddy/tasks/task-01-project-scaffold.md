# Task 01：项目骨架初始化

## 目标
创建前后端分离的项目结构，确保两端可独立启动。

## 范围

### 后端 `/backend`
- 使用 Spring Initializr 或手动创建 Maven 项目
- pom.xml 依赖：
  - spring-boot-starter-web
  - spring-boot-starter-validation
  - spring-boot-starter-security
  - spring-boot-starter-data-jpa
  - H2 database（dev）
  - PostgreSQL driver（可选 prod profile）
  - springdoc-openapi-starter-webmvc-ui
  - spring-boot-starter-test
- 创建 `application.yml`（端口 8080、H2 控制台、JPA 配置）
- 主启动类可正常运行

### 前端 `/frontend`
- `npm create vite@latest frontend -- --template vue-ts`
- 安装依赖：vue-router、pinia、axios、element-plus
- 创建基础目录结构：`src/{views,components,api,stores,router}`
- 配置 axios 封装（baseURL: `http://localhost:8080/api`，`withCredentials: true`）
- vite.config.ts 确认 dev server 端口 5173

### 根目录
- 创建 README.md（启动说明、默认账号、Swagger 地址）

## 验收标准
- [ ] 后端 `mvn spring-boot:run` 启动成功，端口 8080
- [ ] 前端 `npm run dev` 启动成功，端口 5173
- [ ] README 包含双端启动命令

## 依赖
无（首个任务）
