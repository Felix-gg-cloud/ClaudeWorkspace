# Claude Code 最终开发提示词（直接整段发给它）

请从零创建一个新仓库（前后端分离）实现“0基础英语 RPG 养成网站”，严格遵守以下硬规格：

## 基本
- 后端：Spring Boot 3.x + Java17 + Maven
- 前端：Vue3 + Vite + TS
- 前端 http://localhost:5173
- 后端 http://localhost:8080
- 登录：Spring Security Session + Cookie（不是 JWT）
- 前端 axios 必须 withCredentials=true
- 后端必须 CORS 允许 http://localhost:5173 且 allowCredentials=true
- MVP 单用户，但所有业务数据都带 user_id（为多用户扩展准备）

## 路由/页面（必须）
- /login
- /dashboard（强引导 + Start Main Quest）
- /quest/today（今日主线）
- /arena（SRS训练副本）
- /skill-tree
- /boss（第11天Boss）

## 首次登录引导（必须）
- 首次登录进入 /dashboard 弹窗要求设置 displayName（可跳过但持续提醒）
- API：PUT /api/me/profile { displayName, ttsVoice: "en-US"|"en-GB" }
- 所有内容文本支持 {playerName} 占位符，渲染/播放 TTS 前替换为 displayName 或 "Hero"

## Seed（必须）
- 后端启动读取 resources/seed/*.json 并幂等导入（code 唯一键 upsert）
- 建立 seed_import_log(version, importedAt)
- 你将根据这些 seed 文件实现导入器、实体与 API：
  - chapters.json, lessons.json, content_items.json, tasks_day01.json ... tasks_dayXX.json
  - boss_pools.json + boss_items_*.json
  - skill_nodes.json
- 每个 content_item 必须有 source + cefrLevel

## API（必须）
- POST /api/auth/login {username,password}
- POST /api/auth/logout
- GET /api/me
- PUT /api/me/profile
- GET /api/today  -> 返回今日 lesson + tasks(包含自动补题后的列表) + progress
- POST /api/tasks/{taskKey}/complete -> 提交答案、返回是否正确、解释、更新进度；若当日完成触发结算与打卡
- GET /api/checkin/calendar?month=YYYY-MM
- GET /api/arena/today -> 返回今日 SRS 10 题
- POST /api/arena/{taskKey}/complete
- GET /api/boss/today -> 若今日为Boss日返回抽题后的试卷
- POST /api/boss/answer -> 逐题结算（HP/Combo）
- POST /api/boss/finish -> 通关/失败结算与解锁

## Auto-Generated Drills（必须实现，写死规则）
- lesson.targetTaskCount=30
- 若 seed 任务不足则自动生成 drill（50%词义MCQ、30%听音选词MCQ、20%拼写）
- 自动题 code 前缀 AUTO_，奖励较低 xpReward=2~4
- 自动题不��求写入 Task 表，可用虚拟任务模型，但必须记录 progress（taskKey=code）

## Boss（必须）
- BossHP=10 PlayerHP=5 连击>=5 额外伤害+1
- 每天允许重试1次，不做花金币复活
- 抽题计划：LISTEN_WORD 4，MEANING 4，PATTERN_ORDER 4，SPELLING 2，DIALOGUE 1
- coverage=CORE_70_PERCENT（���考核心70%）
- 题库随机抽题，每次挑战不同

## 前端交互（必须）
- Dashboard：角色卡(等级/XP/金币/技能点) + Start Main Quest + Arena + Skill Tree + Chapter Map
- Quest：按顺序展示任务，完成即反馈；含 TTS 播放按钮（可切换美音/英音）
- 结算页：必须强反馈（XP/金币/升级/技能点/解锁）
- Arena：显示连击与进度
- Boss：显示 BossHP/PlayerHP/Combo，闯关式结算

## 测试（后端至少）
- Seed 幂等导入测试
- 自动补题数量正确测试
- Boss 抽题符合 drawPlan 测试

## 文档
- 根 README：如何启动前后端、默认账号密码、Swagger 地址、CORS+cookie常见问题