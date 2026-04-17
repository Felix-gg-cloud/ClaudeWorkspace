# Task 07：等级 / 经验 / 解锁系统

## 目标
实现 XP 累积、等级计算和课程解锁逻辑。

## 范围

### 等级计算
- 根据 `LevelConfig.requiredXp` 阈值表计算 currentLevel
- 例：Level 1 需 0 XP，Level 2 需 500 XP，Level 3 需 1500 XP
- 用户 totalXp ≥ 某级 requiredXp → 该级已解锁

### 升级事件
- 每次 XP 变化后检查是否升级
- 升级时：
  - 更新 `user.currentLevel`
  - 返回升级标记给前端（`levelUp: true, newLevel: N`）
  - 新 Level 对应的 Stage/Lesson 自动解锁

### 解锁规则
- 用户当前 Level 对应的所有 Stage 可访问
- Level 高于配置最大值时，所有课程均解锁

### Service
- `LevelService.calculateLevel(totalXp)` — 纯计算方法
- `LevelService.checkAndUpgrade(user)` — 检查并触发升级

## 验收标准
- [ ] XP 增长后等级正确计算
- [ ] 升级时 currentLevel 更新
- [ ] 新等级对应 Stage 可访问
- [ ] 等级计算方法可独立测试

## 依赖
Task 05（XP 结算）
