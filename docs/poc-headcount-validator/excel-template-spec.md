# POC Headcount Validator — Excel/PDF 报告模板规格

> 由于 Excel 和 Word 二进制模板文件未提交至代码库，本文档定义所有必需的 Sheet 名称、表名（ListObject）和列定义，供管理员手动创建或通过脚本初始化模板文件。

---

## 1. Excel 报告模板（ValidationReport_Template.xlsx）

### 1.1 Workbook 结构

| Sheet 名称 | 说明 | 是否含 Table（ListObject） |
|------------|------|--------------------------|
| `Summary` | 汇总信息（运行元数据 + 统计数字） | 是，表名：`tblSummary` |
| `Issues` | 所有 Error/Warning 明细 | 是，表名：`tblIssues` |
| `Instructions` | 说明页（静态文本，不填充） | 否 |

### 1.2 Summary Sheet — tblSummary

**表名（ListObject Name）**：`tblSummary`

该表为单行键值对结构（无需循环，直接定位单元格或表行）：

| 列名（Header） | 数据类型 | 来源字段 | 示例值 |
|----------------|----------|----------|--------|
| `RunLabel` | 文本 | `run_label` | `2025Q1-校验` |
| `TriggeredAt` | 日期时间 | `triggered_at` | `2025-01-15 10:00:00` |
| `GeneratedAt` | 日期时间 | `report_model.generated_at` | `2025-01-15 10:00:45` |
| `TotalRows` | 整数 | `report_model.total_rows` | `100` |
| `ErrorCount` | 整数 | `report_model.error_count` | `8` |
| `WarningCount` | 整数 | `report_model.warning_count` | `0` |
| `PassCount` | 整数 | `report_model.pass_count` | `92` |
| `PassRate` | 百分比（公式） | 计算：`PassCount/TotalRows` | `92%` |

> **PassRate** 为公式列，模板中预置公式 `=G2/D2`（或使用列引用），Power Automate 不需写入此列。

### 1.3 Issues Sheet — tblIssues

**表名（ListObject Name）**：`tblIssues`

| 列名（Header） | 数据类型 | 来源字段 | 示例值 |
|----------------|----------|----------|--------|
| `Severity` | 文本 | `issues[].Severity` | `Error` |
| `RuleId` | 文本 | `issues[].RuleId` | `R-WS-ALL-001` |
| `RowKey` | 文本 | `issues[].RowKey` | `RowIndex=3\|YearMonth=202501\|CCN=1003\|Function=GBS \|Team=EA` |
| `RowIndex` | 整数（提取自 RowKey） | 解析 `RowKey` 第一段 | `3` |
| `Column` | 文本 | `issues[].Column` | `Function` |
| `RawValue` | 文本 | `issues[].RawValue` | `GBS ` |
| `Message` | 文本 | `issues[].Message` | `列 Function 的值包含前后空格，不允许。` |
| `FixSuggestion` | 文本 | `issues[].FixSuggestion` | `删除该值开头/结尾的空格后重新提交。` |

> **RowIndex** 列：在 Office Scripts 中从 `RowKey` 字段解析（取 `RowIndex=` 后的数字），或在 Power Automate 中预处理后传入。

### 1.4 Issues Sheet — 条件格式（Conditional Formatting）

模板中预置以下条件格式，使报告更易阅读：

| 条件 | 格式 |
|------|------|
| `Severity = "Error"` | 单元格背景色：浅红（`#FFE0E0`） |
| `Severity = "Warning"` | 单元格背景色：浅黄（`#FFFACD`） |

### 1.5 Instructions Sheet

静态说明文本，包含：
- 报告生成时间、流程名称
- 各列说明
- RuleId 速查（复制自 [validation-rules.md](./validation-rules.md) 的规则 ID 速查表）

---

## 2. Word 报告模板（ValidationReport_Template.docx）

> 用于生成 PDF 报告。若使用 Word Online (Business) → Populate a Microsoft Word template 连接器，需在 .docx 中定义以下占位符。

### 2.1 简单文本占位符（Content Controls）

| 占位符标签（Tag） | 来源字段 | 示例值 |
|-------------------|----------|--------|
| `RunLabel` | `run_label` | `2025Q1-校验` |
| `TriggeredAt` | `triggered_at` | `2025-01-15 10:00:00` |
| `GeneratedAt` | `report_model.generated_at` | `2025-01-15 10:00:45` |
| `TotalRows` | `report_model.total_rows` | `100` |
| `ErrorCount` | `report_model.error_count` | `8` |
| `WarningCount` | `report_model.warning_count` | `0` |
| `PassCount` | `report_model.pass_count` | `92` |

### 2.2 重复区域（Repeating Section Content Control）

重复区域标签：`IssuesTable`

每个重复项包含以下子占位符：

| 子占位符标签 | 来源字段 |
|-------------|----------|
| `IssueSeverity` | `issues[].Severity` |
| `IssueRuleId` | `issues[].RuleId` |
| `IssueRowKey` | `issues[].RowKey` |
| `IssueColumn` | `issues[].Column` |
| `IssueRawValue` | `issues[].RawValue` |
| `IssueMessage` | `issues[].Message` |
| `IssueFixSuggestion` | `issues[].FixSuggestion` |

---

## 3. Office Scripts 参考（批量写入 Excel 报告）

> 以下脚本供参考，可直接在 Excel Online 的 Office Scripts 编辑器中创建并命名为 `FillValidationReport`。

```typescript
function main(
  workbook: ExcelScript.Workbook,
  reportModel: {
    total_rows: number;
    error_count: number;
    warning_count: number;
    pass_count: number;
    generated_at: string;
  },
  runLabel: string,
  triggeredAt: string,
  issues: Array<{
    Severity: string;
    RuleId: string;
    RowKey: string;
    Column: string;
    RawValue: string;
    Message: string;
    FixSuggestion: string;
  }>
) {
  // 1. 填充 Summary 表
  const summarySheet = workbook.getWorksheet("Summary");
  const summaryTable = summarySheet.getTable("tblSummary");
  const summaryRow = summaryTable.getRows().getItemAt(0);
  summaryRow.getRange().setValues([[
    runLabel,
    triggeredAt,
    reportModel.generated_at,
    reportModel.total_rows,
    reportModel.error_count,
    reportModel.warning_count,
    reportModel.pass_count
  ]]);

  // 2. 填充 Issues 表
  const issuesSheet = workbook.getWorksheet("Issues");
  const issuesTable = issuesSheet.getTable("tblIssues");

  // 清空现有数据行（保留表头）
  const existingRows = issuesTable.getRangeBetweenHeaderAndTotal();
  if (issuesTable.getRowCount() > 0) {
    existingRows.clear(ExcelScript.ClearApplyTo.contents);
    issuesTable.deleteRowsAt(0, issuesTable.getRowCount());
  }

  // 批量添加 issues 行
  if (issues.length > 0) {
    const rowData = issues.map(issue => {
      const rowIndexMatch = issue.RowKey.match(/RowIndex=(\d+)/);
      const rowIndex = rowIndexMatch ? parseInt(rowIndexMatch[1]) : 0;
      return [
        issue.Severity,
        issue.RuleId,
        issue.RowKey,
        rowIndex,
        issue.Column,
        issue.RawValue,
        issue.Message,
        issue.FixSuggestion
      ];
    });
    issuesTable.addRows(-1, rowData);
  }
}
```

### Power Automate 中调用此脚本

```
动作：Excel Online (Business) → 运行脚本（Run script）
工作簿位置：SharePoint
文件：ValidationReport_<生成文件名>.xlsx
脚本：FillValidationReport
脚本参数：
  reportModel: @{body('Parse_JSON')?['report_model']}
  runLabel: @{triggerBody()?['text']}
  triggeredAt: @{triggerBody()?['timestamp']}
  issues: @{body('Parse_JSON')?['issues']}
```

---

## 4. 模板文件初始化检查清单

管理员在首次部署时，需确认以下项目均已完成：

- [ ] 已创建 `ValidationReport_Template.xlsx` 并上传至 SharePoint `Templates/` 目录
  - [ ] Workbook 包含 `Summary`、`Issues`、`Instructions` 三个 Sheet
  - [ ] `Summary` Sheet 含名为 `tblSummary` 的 Table，列顺序与规格一致
  - [ ] `Issues` Sheet 含名为 `tblIssues` 的 Table，列顺序与规格一致
  - [ ] `Issues` Sheet 已配置条件格式（Error 红底、Warning 黄底）
  - [ ] Office Script `FillValidationReport` 已在该工作簿中创建
- [ ] 已创建 `ValidationReport_Template.docx` 并上传至 SharePoint `Templates/` 目录（如使用 Word → PDF 方案）
  - [ ] 所有简单文本 Content Control 的 Tag 与规格一致
  - [ ] IssuesTable 重复区域已配置，子 Tag 与规格一致
- [ ] Power Automate 流程的模板文件路径配置正确
- [ ] 已测试从模板复制、填充、存储的完整流程
