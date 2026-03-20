# Excel 报告模板说明 — ValidationReportTemplate.xlsx

## 1. 概述

`ValidationReportTemplate.xlsx` 是 Power Automate 每次生成校验报告时使用的**主模板文件**。  
Flow 会先**复制**此模板，再向副本中写入数据，原模板保持不变。

### 模板存放位置（POC 阶段 — 个人 OneDrive）

```
OneDrive（个人）
└── Documents/
    └── PowerbiTest/
        └── POC-Validator/
            └── Templates/
                └── ValidationReportTemplate.xlsx   ← 此文件（手动上传一次）
```

---

## 2. Workbook 结构

| Sheet 名称 | 用途 | 写入方式 |
|-----------|------|---------|
| `Summary` | 汇总指标、Top Rules、Top Columns | 按固定单元格地址写入 |
| `Issues` | 全量问题明细（含筛选列头） | 写入 Excel 表格对象 `tblIssues` |

---

## 3. Summary Sheet — 单元格映射

在 Power Automate 中用"**更新单元格**"动作（Update a row in a table 或 Update cell）按以下地址写入：

| 单元格地址 | 内容 | 来源字段 |
|-----------|------|---------|
| `B2` | 报告标题 | 固定文字 `POC Data Validation Report`（可留在模板中不写入） |
| `B3` | 生成时间 | `report_model.generated_at` |
| `B4` | 源文件名 | `report_model.source_file_name` |
| `B5` | 表名 | `report_model.table_name` |
| `B6` | 批次 ID | `report_model.batch_id` |
| `B9` | 总行数 | `report_model.metrics.rows_total` |
| `B10` | 问题总数 | `report_model.metrics.issues_total` |
| `B11` | Error 数 | `report_model.metrics.errors_total` |
| `B12` | Warning 数 | `report_model.metrics.warnings_total` |
| `B13` | 通过率 | `report_model.metrics.pass_rate`（格式化为百分比） |
| `A17:C21` | Top 5 Rules 表格 | `report_model.top_rules[]`（RuleId / Severity / Count） |
| `A25:B29` | Top 5 Columns 表格 | `report_model.top_columns[]`（Column / Count） |

> 💡 建议在模板中用条件格式：`B11 > 0` → 单元格背景红色，直观展示是否有 Error。

---

## 4. Issues Sheet — tblIssues 表格

Issues Sheet 中内置一个 Excel 表格对象，**表名必须为 `tblIssues`**（Power Automate 通过表名引用）。

### 列定义（列头顺序）

| 列序 | 列名（表头） | 来源 | 说明 |
|------|------------|------|------|
| A | `Severity` | `issues[].Severity` | `Error` / `Warning` |
| B | `RuleId` | `issues[].RuleId` | 规则编号 |
| C | `RowIndex` | `issues[].RowIndex` | 行号，用于定位 |
| D | `YearMonth` | `issues[].YearMonth` | 原始值（raw） |
| E | `Cost Center Number` | `issues[].Cost Center Number` | 原始值（raw） |
| F | `Function` | `issues[].Function` | 原始值（raw） |
| G | `Team` | `issues[].Team` | 原始值（raw） |
| H | `Owner` | `issues[].Owner` | 原始值（raw） |
| I | `Column` | `issues[].Column` | 命中问题的列名 |
| J | `RawValue` | `issues[].RawValue` | 命中列的原始值 |
| K | `Message` | `issues[].Message` | 问题说明 |
| L | `FixSuggestion` | `issues[].FixSuggestion` | 修复建议 |
| M | `RowKey` | `issues[].RowKey` | 完整定位锚点 |

### 建议格式

- **Severity 列**：用条件格式，`Error` → 字体红色，`Warning` → 字体橙色
- **列宽**：RawValue、Message、FixSuggestion 列建议自动换行（Wrap Text）
- **筛选**：保持 Excel 表格默认开启自动筛选

---

## 5. 如何手动创建模板

1. 新建空白 Excel 文件，命名 `ValidationReportTemplate.xlsx`
2. 按上述结构创建两个 Sheet：`Summary` 和 `Issues`
3. 在 `Issues` Sheet 中：
   - 在第 1 行输入列头（A1:M1，共 13 列）
   - 选中 A1:M1，插入 → 表格，勾选"表包含标题"，命名为 `tblIssues`
4. 在 `Summary` Sheet 中按映射表填入标签文字（B 列对应行留空供 Flow 写入）
5. 上传到 OneDrive：`Documents/PowerbiTest/POC-Validator/Templates/`

---

## 6. Power Automate 中引用模板的方式

> 详见 [`runbook-power-automate.md`](./runbook-power-automate.md) Step 4。

关键字段配置示例：

| PA 动作字段 | 值 |
|------------|-----|
| Location | `OneDrive for Business` |
| Document Library | `OneDrive` |
| File | `/Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx` |
| Destination File Path | `/Documents/PowerbiTest/POC-Validator/Outputs/Excel/ValidationReport_@{variables('YearMonth')}_@{formatDateTime(utcNow(),'yyyyMMdd-HHmm')}.xlsx` |
