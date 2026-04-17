# 05 — Power BI 输出、验证与回滚操作指南

## 概述

Power BI 报告连接 SharePoint List（或 Excel `tblIssues`）中的校验结果，提供以下功能：
- 实时数据质量仪表板（Issue 分布、严重性占比、Owner 责任归属）
- 历史趋势对比（每月问题数量变化）
- 下钻到行级别查看具体 issue
- 支持筛选（按 Owner、Function、YearMonth、RuleId、Severity）

---

## 1. 数据源接入

### 1.1 连接 SharePoint List（推荐）

1. 打开 Power BI Desktop
2. 获取数据 → SharePoint Online List
3. 输入 SharePoint 站点 URL（例如：`https://contoso.sharepoint.com/sites/HeadcountPOC`）
4. 选择 issues 列表（例如：`HeadcountValidationIssues`）
5. 加载并在 Power Query 中整理列类型

### 1.2 连接 Excel 结果表

1. 获取数据 → Excel 工作簿
2. 选择含 `tblIssues` 的 Excel 文件
3. 加载 `tblIssues` 表
4. 注意：Excel 数据源需要 Power BI 数据网关（如果文件在 SharePoint/OneDrive 上）

---

## 2. 数据模型建议

### 主要表：`IssuesTable`

| 列名 | 类型 | 说明 |
|------|------|------|
| `Severity` | 文本 | Error / Warning |
| `RuleId` | 文本 | 规则编号 |
| `YearMonth` | 文本 | 报告年月（YYYYMM） |
| `CostCenterNumber` | 文本 | 成本中心编号 |
| `Function` | 文本 | 业务功能 |
| `Team` | 文本 | 团队名称 |
| `Column` | 文本 | 出错列名 |
| `Value` | 文本 | 实际填写值 |
| `Message` | 文本 | 错误说明 |
| `FixSuggestion` | 文本 | 修正建议 |
| `Owner` | 文本 | 数据负责人（从原始数据关联） |
| `RunTimestamp` | 日期时间 | Flow 运行时间戳（用于历史趋势） |

### 辅助日历表（可选）

用于 YearMonth 的时间智能分析（按月趋势）：

```
Calendar = CALENDARAUTO()
```

---

## 3. 推荐报告页面结构

### 页面 1：执行摘要（Summary）

| 视觉对象 | 配置 |
|---------|------|
| 卡片：Total Errors | `COUNTROWS(FILTER(IssuesTable, [Severity]="Error"))` |
| 卡片：Total Warnings | `COUNTROWS(FILTER(IssuesTable, [Severity]="Warning"))` |
| 圆环图：Severity 分布 | 按 Severity 分组计数 |
| 条形图：Top 10 RuleId | 按 RuleId 分组计数，降序排列 |
| 矩阵：Owner × Severity | 行=Owner，列=Severity，值=Count |

### 页面 2：问题明细（Detail）

| 视觉对象 | 配置 |
|---------|------|
| 筛选器面板 | YearMonth / Owner / Function / Severity / RuleId |
| 表格：Issue 明细 | 所有列，支持排序和筛选 |
| 工具提示 | 悬停行显示 FixSuggestion |

### 页面 3：历史趋势（Trend）

| 视觉对象 | 配置 |
|---------|------|
| 折线图：每月 Error/Warning 数量 | X 轴=RunTimestamp（按月），Y 轴=Count，图例=Severity |
| 条形图：每月新增 vs 修复 issue | 需结合上月/本月数据对比（见"验证逻辑"） |

---

## 4. 关键 DAX 度量值

```dax
// 错误总数
Total Errors = COUNTROWS(FILTER(IssuesTable, IssuesTable[Severity] = "Error"))

// 警告总数
Total Warnings = COUNTROWS(FILTER(IssuesTable, IssuesTable[Severity] = "Warning"))

// 受影响行数（按 YearMonth+Function+Team 去重）
Affected Rows = 
COUNTROWS(
  DISTINCT(
    SELECTCOLUMNS(
      IssuesTable,
      "Key", IssuesTable[YearMonth] & "|" & IssuesTable[Function] & "|" & IssuesTable[Team]
    )
  )
)

// 最近一次运行时间
Last Run = MAX(IssuesTable[RunTimestamp])

// 环比变化（需配合日期维度）
MoM Error Change = 
VAR CurrentErrors = [Total Errors]
VAR LastMonthErrors = 
  CALCULATE(
    [Total Errors],
    DATEADD('Calendar'[Date], -1, MONTH)
  )
RETURN
  IF(ISBLANK(LastMonthErrors), BLANK(), CurrentErrors - LastMonthErrors)
```

---

## 5. 验证流程（数据修正后再次校验）

当填报人完成数据修正后，需按以下步骤验证：

```
1. 填报人修正 Excel 中的数据（按 FixSuggestion 操作）
2. 通知校验团队（或自行触发）重新运行 Power Automate Flow
3. Flow 运行完成后，自动将新的 issues 写入 SharePoint List（追加，带新的 RunTimestamp）
4. Power BI 刷新数据集（手动刷新或定时刷新）
5. 在"历史趋势"页面确认本次运行的 Error 数量是否减少
6. 在"问题明细"页面按最新 RunTimestamp 筛选，确认之前的问题是否已清零
```

### 5.1 验证通过标准

| 通过条件 | 说明 |
|---------|------|
| Total Errors = 0 | 所有 Error 级别问题已修正 |
| Total Warnings ≤ N（业务约定阈值） | Warning 数量在可接受范围内 |
| Affected Rows 对应原始数据 | 未遗漏任何行 |

---

## 6. 回滚操作

当发现 Flow 运行异常（例如：规则配置错误导致误报、数据写入错误等），需要回滚时：

### 6.1 识别需要回滚的场景

| 场景 | 判断依据 |
|------|---------|
| 规则误配置 | 某个 RuleId 对大量明显正常的行触发了 Error |
| Flow 重复运行 | SharePoint List 中出现相同 RunTimestamp 的重复 issues |
| 数据源错误 | Flow 读取到错误版本的 Excel 文件 |

### 6.2 回滚步骤

**步骤 1：暂停后续 Flow 运行**

在 Power Automate 中将 Flow 关闭（Turn off），防止继续产生错误数据。

**步骤 2：删除错误的 issues 批次**

进入 SharePoint List：
1. 筛选对应的 `RunTimestamp`（出错的那次运行时间）
2. 全选并删除该批次的所有 issue 记录
3. 或者使用 Power Automate 批量删除：

```
Flow：Delete Issues by RunTimestamp
触发：手动触发（输入 RunTimestamp）
操作：
  1. Get items from SharePoint List（筛选 RunTimestamp = 输入值）
  2. Apply to each → Delete item
```

**步骤 3：修复规则或数据源**

- 若是规则配置错误：更新 Flow 中的校验逻辑（参考 [03 文档](./03-power-automate-flow.md)）
- 若是数据源问题：确认正确版本的 Excel 文件路径

**步骤 4：重新运行 Flow**

修复完成后重新触发 Flow，确认新一批 issues 符合预期。

**步骤 5：刷新 Power BI 并验证**

确认历史趋势图中的错误批次已移除，当前数据准确反映真实问题。

---

## 7. 报告发布与权限

### 7.1 发布到 Power BI Service

1. Power BI Desktop → 发布 → 选择工作区
2. 在 Power BI Service 中配置数据集的定时刷新（推荐每天或每次 Flow 运行后刷新）
3. 如使用 SharePoint List 数据源，需配置数据网关

### 7.2 权限管理

| 角色 | 权限 |
|------|------|
| 数据质量管理员 | 查看所有 Owner 的报告，管理员级别筛选 |
| 业务 Owner | 仅查看自己的 issues（通过 RLS 行级别安全实现） |
| 校验团队 | 可触发 Flow，可删除/重置 issues |

**RLS（行级别安全）配置示例**：

```dax
// 在 Power BI Desktop 中定义 RLS 角色
[Owner] = USERPRINCIPALNAME()
```

---

## 8. 常见问题

| 问题 | 解决方案 |
|------|----------|
| Power BI 数据未刷新 | 检查数据网关是否在线，手动触发刷新，或检查定时刷新计划 |
| SharePoint 数据量超大导致刷新慢 | 设置增量刷新，仅刷新最近 N 个月的数据 |
| RLS 不生效 | 确认已在 Power BI Service 中为用户分配了正确的 RLS 角色 |
| 历史趋势图看不到数据 | 确认 IssuesTable 中 RunTimestamp 列格式为日期时间，且已建立日历表关联 |
| 误删 SharePoint List 数据 | 检查 SharePoint 回收站，通常保留 93 天，可从中恢复 |
