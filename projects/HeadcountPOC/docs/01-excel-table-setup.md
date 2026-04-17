# 01 — Excel 表格搭建指南

## 1. 目标表格：`tblOffshoring`

将以下结构创建为 Excel **Table（格式化为表格）**，表名设为 `tblOffshoring`。

> **注意**：必须使用 Excel 的"插入 → 表格"功能，Power Automate 的 Excel Online 连接器才能逐行读取数据。

---

## 2. 列定义

| 列名 | 数据类型 | 说明 | 必填规则 |
|------|----------|------|----------|
| `Owner` | 文本 | 数据负责人 | Total 行必填；Non-Total 行可选 |
| `YearMonth` | 文本 | 报告年月，格式 `YYYYMM` | **所有行必填**，格式 6 位数字，月份 01-12 |
| `Cost Center Number` | 文本 | 成本中心编号 | 可选；非空时必须是 7 位纯数字 |
| `Function` | 文本 | 业务功能分类 | 可选；非空时必须在白名单内（见下） |
| `Team` | 文本 | 团队名称 | 可选；非空时必须在白名单内（见下） |
| `Actual_GBS_TeamMember` | 数字/文本 | GBS 团队成员实际人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Actual_GBS_TeamLeaderAM` | 数字/文本 | GBS 团队长/AM 实际人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Actual_EA` | 数字/文本 | EA 实际人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Actual_HKT` | 数字/文本 | HKT 实际人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Planned_GBS_TeamMember` | 数字/文本 | GBS 团队成员计划人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Planned_GBS_TeamLeaderAM` | 数字/文本 | GBS 团队长/AM 计划人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Planned_EA` | 数字/文本 | EA 计划人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Planned_HKT` | 数字/文本 | HKT 计划人数 | 可选；非空时必须是数字且 ≥ 0 |
| `Target_YearEnd` | 数字/文本 | 年末目标人数 | Non-Total 行必填；非空时必须是数字且 ≥ 0 |
| `Target_2030YearEnd` | 数字/文本 | 2030 年末目标人数 | Non-Total 行必填；非空时必须是数字且 ≥ 0 |
| `ShoringRatio` | 文本 | Offshoring 比率（百分比格式） | Non-Total 行必填；非空时必须是 `0%–100%` 含小数的百分比格式 |

---

## 3. Function 白名单

以下是 `Function` 列允许的所有合法值（**大小写和空格须完全一致**）：

```
Total (All)
Total (Core Operations)
Finance
HR
IT
Legal
Marketing
Operations
Procurement
Risk & Compliance
Strategy
Tax
Treasury
Audit
Customer Service
Data & Analytics
```

> **提示**：如需增加 Function 值，需同步更新 Power Automate Flow 中的白名单数组，并更新本文档。

---

## 4. Team 白名单

以下是 `Team` 列允许的所有合法值（非空时校验，Warning 级别）：

```
GBS
EA
HKT
Local
Shared Service
```

> **提示**：Team 名称变动较频繁，POC 阶段设为 Warning 而非 Error，避免因拼写差异导致大量误报。

---

## 5. Function-Team 映射表（允许组合）

| Function | Team（空字符串表示允许为空） |
|----------|------------------------------|
| `Total (All)` | `""` (空) |
| `Total (Core Operations)` | `""` (空) |
| `Finance` | `GBS` |
| `Finance` | `Local` |
| `HR` | `GBS` |
| `HR` | `Local` |
| `IT` | `GBS` |
| `IT` | `Local` |
| `Legal` | `Local` |
| `Marketing` | `Local` |
| `Operations` | `GBS` |
| `Operations` | `Local` |
| `Procurement` | `GBS` |
| `Procurement` | `Local` |
| `Risk & Compliance` | `Local` |
| `Strategy` | `Local` |
| `Tax` | `Local` |
| `Treasury` | `Local` |
| `Audit` | `Local` |
| `Customer Service` | `GBS` |
| `Customer Service` | `Local` |
| `Data & Analytics` | `GBS` |
| `Data & Analytics` | `Local` |

> **说明**：`Total` 行的 `Team` 允许为空，这是有效的组合，不应报错。如有其他组合需要，与业务确认后更新此表并同步至 Flow 配置。

---

## 6. 数据样例

| Owner | YearMonth | Cost Center Number | Function | Team | Actual_GBS_TeamMember | ... | Target_YearEnd | Target_2030YearEnd | ShoringRatio |
|-------|-----------|-------------------|----------|------|-----------------------|-----|----------------|-------------------|--------------|
| Alice | 202501 | 1234567 | Finance | GBS | 10 | ... | 12 | 15 | 83.3% |
| Bob | 202501 | 1234567 | Finance | Local | 5 | ... | 6 | 8 | 62.5% |
| Alice | 202501 | | Total (All) | | | ... | | | |
| Alice | 202501 | | Total (Core Operations) | | | ... | | | |

---

## 7. 常见搭建问题

| 问题 | 解决方案 |
|------|----------|
| Power Automate 读不到表格 | 确认已通过"插入 → 表格"创建，并设置了表名 `tblOffshoring` |
| ShoringRatio 列显示为数字 | 将该列单元格格式改为"文本"，避免 Excel 自动转换 `23%` 为 `0.23` |
| 数值列有公式 | Power Automate 读取到的是公式结果（数值），请确保公式输出为数字 |
| YearMonth 被 Excel 识别为日期 | 将该列单元格格式改为"文本"，并重新输入数据 |
