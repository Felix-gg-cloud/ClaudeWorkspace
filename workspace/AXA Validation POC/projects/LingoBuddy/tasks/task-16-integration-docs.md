# Task 16：联调测试与文档完善

## 目标
前后端联调验证 MVP 完整闭环，完善项目文档。

## 范围

### 联调测试清单
1. 登录 → 进入 Dashboard → 数据正确
2. 点击"开始学习" → 今日课时页面 → 完成所有 task
3. 全部完成 → 打卡成功弹窗 → XP/Coins 增加
4. Dashboard 数据刷新（Streak +1、XP 更新、签到状态）
5. 日历页面显示今日打卡
6. 成就页面新解锁的成就已显示
7. 累计 XP 达到阈值 → 升级提示
8. 登出 → 重新登录 → 数据保持

### 文档完善

#### README.md
- 项目简介
- 技术栈说明
- 后端启动：`cd backend && mvn spring-boot:run`
- 前端启动：`cd frontend && npm i && npm run dev`
- 默认账号密码
- Swagger 地址：`http://localhost:8080/swagger-ui.html`
- 常见问题 FAQ

#### .env.example（前端）
```
VITE_API_BASE_URL=http://localhost:8080/api
```

#### application.yml 说明
- 数据库配置（H2 / PostgreSQL）
- 默认密码配置
- Session 配置

## 验收标准
- [ ] 完整闭环流程跑通
- [ ] 前后端无报错
- [ ] README 清晰可用
- [ ] 新人根据 README 可从零启动项目

## 依赖
Task 01 ~ 15（所有任务完成后）
