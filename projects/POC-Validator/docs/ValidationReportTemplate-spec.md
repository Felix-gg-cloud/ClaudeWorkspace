# ValidationReportTemplate.xlsx — 非二进制规格说明

本文件描述 `ValidationReportTemplate.xlsx` 的完整规格，包括工作表结构、Excel Table 定义及格式要求。由于 `.xlsx` 为二进制格式，实际文件须按本规格在 Excel 中手动创建，再上传至 SharePoint。

---

## 文件元数据

| 项目 | 值 |
|------|-----|
| 文件名 | `ValidationReportTemplate.xlsx` |
| 存放位置 | `POC-Validator/Templates/ValidationReportTemplate.xlsx`（SharePoint） |
| Excel 格式 | `.xlsx`（Office Open XML） |
| 兼容版本 | Excel 2016+ / Excel 网页版 |

---

## 工作表清单

| Sheet 序号 | Sheet 名称 | 用途 |
|-----------|-----------|------|
| 1 | `Summary` | 批次汇总指标 |
| 2 | `Issues` | 问题明细（Excel Table：`tblIssues`） |

> Sheet 名称**严格区分大小写**，Power Automate 按此名称定位。

---

## Sheet 1：Summary

### 预留单元格布局

| 单元格 | 标签（静态文本） | 单元格 | 值（由 Flow 写入） |
|--------|----------------|--------|------------------|
| A1 | `生成时间` | B1 | ISO 8601 时间戳，如 `2024-03-15T08:30:00Z` |
| A2 | `输入文件` | B2 | 输入 Excel 文件名 |
| A3 | `批次年月` | B3 | 格式 `YYYY-MM`，如 `2024-03` |
| A4 | `总行数` | B4 | 整数 |
| A5 | `Error 数` | B5 | 整数 |
| A6 | `Warning 数` | B6 | 整数 |
| A7 | `通过率` | B7 | 百分比字符串，如 `98.5%` |

### 格式建议

- A 列（标签列）：加粗，宽度约 15 字符
- B 列（值列）：宽度约 30 字符
- 无 Excel Table（Summary 页使用单元格写入，非表格）

---

## Sheet 2：Issues

### Excel Table：tblIssues

#### 表属性

| 属性 | 值 |
|------|-----|
| 表名称（Table Name） | `tblIssues` |
| 表样式（Table Style） | `TableStyleMedium2`（或任意样式，不影响功能） |
| 表头行 | 第 1 行（A1:M1） |
| 数据起始行 | 第 2 行 |
| 自动筛选 | 启用（建表后默认开启） |

#### 列定义

| 列序 | 列索引 | 列名（精确） | 数据类型 | 说明 |
|-----|--------|------------|---------|------|
| 1 | A | `Severity` | 文本 | `Error` / `Warning` / `Info` |
| 2 | B | `RuleId` | 文本 | 规则编号，如 `R001` |
| 3 | C | `RowKey` | 文本 | 数据行唯一标识 |
| 4 | D | `RowIndex` | 整数 | 源文件中的原始行号（从 2 开始，跳过表头） |
| 5 | E | `YearMonth` | 文本 | 格式 `YYYY-MM` |
| 6 | F | `Cost Center Number` | 文本 | 含空格，请勿改名 |
| 7 | G | `Function` | 文本 | 职能部门名称 |
| 8 | H | `Team` | 文本 | 团队名称 |
| 9 | I | `Owner` | 文本 | 负责人姓名/邮箱 |
| 10 | J | `Column` | 文本 | 触发问题的列名 |
| 11 | K | `RawValue` | 文本 | 原始单元格值（转为文本） |
| 12 | L | `Message` | 文本 | 问题描述（自然语言） |
| 13 | M | `FixSuggestion` | 文本 | 修复建议 |

> ⚠️ 列名中的空格（如 `Cost Center Number`）必须原样保留，Power Automate 按列名映射字段。

#### 建表步骤（Excel 操作）

```
1. 打开空白 Excel 文件，切换到 Sheet 2，将其重命名为 "Issues"
2. 在 A1 输入 "Severity"，B1 输入 "RuleId"，...（按上表顺序）
3. 选中 A1:M1
4. 菜单：插入（Insert） → 表格（Table）
5. 勾选 "我的表具有标题（My table has headers）"，点击确定
6. 选中表中任意单元格
7. 菜单：表设计（Table Design） → 表名称（Table Name）→ 输入 "tblIssues"
8. 按 Enter 确认
```

#### 条件格式（可选，提升可读性）

| 规则 | 范围 | 格式 |
|------|------|------|
| `=$A2="Error"` | `$A:$M`（整行） | 背景色：浅红（`#FFB3B3`） |
| `=$A2="Warning"` | `$A:$M`（整行） | 背景色：浅黄（`#FFFACD`） |

#### 视图建议

- 冻结首行：视图（View） → 冻结窗格（Freeze Panes） → 冻结首行（Freeze Top Row）
- 列宽建议：A=10, B=8, C=12, D=8, E=10, F=18, G=12, H=12, I=15, J=12, K=15, L=40, M=30

---

## 验证检查清单

在上传到 SharePoint 之前，请确认：

- [ ] 文件名为 `ValidationReportTemplate.xlsx`
- [ ] 包含两个 Sheet：`Summary` 和 `Issues`（名称拼写正确）
- [ ] `Issues` Sheet 中已创建 Excel Table，表名为 `tblIssues`
- [ ] `tblIssues` 共 13 列，列名与上表完全一致（含空格）
- [ ] 文件已保存为 `.xlsx` 格式（非 `.xls` 或 `.xlsb`）
- [ ] 文件无密码保护
- [ ] 文件不包含宏（`.xlsx` 不支持宏，宏文件请用 `.xlsm`，但 Power Automate 不支持）

---

## 与 Power Automate 字段映射关系

Power Automate `Add a row into a table` 动作中，`tblIssues` 的字段映射如下（左为 Excel 列名，右为 Flow 中 `report_model.issues[]` 对应字段）：

| Excel 列名 | Flow 字段（items('Apply_to_each')） |
|-----------|-----------------------------------|
| Severity | `['severity']` |
| RuleId | `['rule_id']` |
| RowKey | `['row_key']` |
| RowIndex | `['row_index']` |
| YearMonth | `['year_month']` |
| Cost Center Number | `['cost_center_number']` |
| Function | `['function']` |
| Team | `['team']` |
| Owner | `['owner']` |
| Column | `['column']` |
| RawValue | `['raw_value']` |
| Message | `['message']` |
| FixSuggestion | `['fix_suggestion']` |
