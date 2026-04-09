# Excel 报告规范 — Offshoring POC 校验器

## 1. 文件命名规范

```
ValidationReport_<YearMonth>_<YYYYMMDD-HHMMSS>.xlsx
```

示例：`ValidationReport_202501_20250115-093000.xlsx`

- `YearMonth`：来自 `report_model.period`（若多月，用下划线分隔，如 `202501_202502`）
- `YYYYMMDD-HHMMSS`：报告生成时间（UTC），来自 `report_model.generated_at`

---

## 2. 工作表结构

工作簿包含两张工作表，顺序固定：

| 顺序 | 工作表名 | 内容 |
|------|----------|------|
| 1 | Summary | 校验摘要与统计 |
| 2 | Issues | 所有 issue 明细 |

---

## 3. Summary 工作表

### 3.1 列定义

| 行号 | A 列（Label） | B 列（Value） |
|------|--------------|--------------|
| 1 | 报告标题 | Offshoring 数据校验报告 |
| 2 | 数据来源 | tblOffshoring |
| 3 | 数据期间 | `report_model.period` |
| 4 | 报告生成时间 | `report_model.generated_at`（本地时区显示）|
| 5 | （空行） | |
| 6 | 检查总行数 | `report_model.summary.total_rows_checked` |
| 7 | 含问题行数 | `report_model.summary.rows_with_issues` |
| 8 | 无问题行数 | `report_model.summary.rows_clean` |
| 9 | （空行） | |
| 10 | 问题总数 | `report_model.summary.total_issues` |
| 11 | Error 数量 | `report_model.summary.error_count` |
| 12 | Warning 数量 | `report_model.summary.warning_count` |
| 13 | （空行） | |
| 14 | 校验结果 | PASSED ✅ 或 FAILED ❌（由 validation_passed 决定）|
| 15 | （空行） | |
| 16 | 按规则统计标题 | 规则 / 严重级别 / 问题数 |
| 17+ | 各规则统计 | 来自 `report_model.issues_by_rule`，每条规则一行 |

### 3.2 格式要求

| 元素 | 格式 |
|------|------|
| 标题行（第1行 B1） | 字体：Calibri 14pt，加粗，蓝色（#1F497D） |
| Label 列（A列） | 字体：Calibri 11pt，加粗，右对齐 |
| Value 列（B列） | 字体：Calibri 11pt，左对齐 |
| 校验结果 PASSED | 字体颜色绿色（#375623），单元格背景浅绿（#E2EFDA）|
| 校验结果 FAILED | 字体颜色红色（#9C0006），单元格背景浅红（#FFCCCC）|
| Error 数量（行11）| 若 > 0，字体红色（#9C0006）加粗 |
| Warning 数量（行12）| 若 > 0，字体橙色（#9C5700）加粗 |
| A列宽度 | 约 25 字符 |
| B列宽度 | 约 30 字符 |

---

## 4. Issues 工作表

### 4.1 列定义（固定顺序）

| 列 | 列名（Header） | 数据来源 | 说明 |
|----|---------------|----------|------|
| A | # | 行序号，从 1 开始 | 自动编号 |
| B | Severity | `issue.Severity` | Error 或 Warning |
| C | Rule ID | `issue.RuleId` | 如 R-WS-ALL-001 |
| D | Row Key | `issue.RowKey` | 行定位复合键 |
| E | Column | `issue.Column` | 问题所在列名 |
| F | Raw Value | `issue.RawValue` | 原始值（含空格） |
| G | Message | `issue.Message` | 问题描述 |
| H | Fix Suggestion | `issue.FixSuggestion` | 修正建议 |

### 4.2 表格样式

- 使用 Excel 表格（Table）格式，表名：`tblIssues`
- 表格样式：TableStyleMedium2（蓝色系标准样式）
- 开启自动筛选（AutoFilter）

### 4.3 条件格式

| 条件 | 格式 |
|------|------|
| B 列 = "Error" | 单元格背景：浅红（#FFCCCC），字体红色（#9C0006），加粗 |
| B 列 = "Warning" | 单元格背景：浅黄（#FFEB9C），字体橙色（#9C5700）|

### 4.4 列宽参考

| 列 | 推荐宽度（字符数）|
|----|-----------------|
| A（#） | 5 |
| B（Severity） | 12 |
| C（Rule ID） | 18 |
| D（Row Key） | 55 |
| E（Column） | 28 |
| F（Raw Value） | 25 |
| G（Message） | 45 |
| H（Fix Suggestion） | 45 |

- F、G、H 列开启自动换行（WrapText = true）

---

## 5. Power Automate 生成步骤（使用 Excel Online 连接器）

```
步骤 1：解析 AI JSON 输出
  - 动作：Parse JSON
  - 内容：Copilot Studio 返回的完整 JSON
  - Schema：使用 ai-output-schema.json

步骤 2：创建工作簿
  - 动作：OneDrive/SharePoint → Create file
  - 文件名：concat('ValidationReport_', report_model.period, '_', formatDateTime(utcNow(),'yyyyMMdd-HHmmss'), '.xlsx')
  - 内容：使用 Office Scripts 或 Excel 模板填充（见下方 Office Scripts 参考）

步骤 3：填写 Summary 工作表
  - 使用「Excel Online (Business) → Run script」调用 Office Script
  - 传入参数：report_model 对象（JSON 序列化）

步骤 4：填写 Issues 工作表
  - 使用「Excel Online (Business) → Add a row into a table」循环写入 issues 数组
  - 或使用 Office Script 批量写入（性能更好）

步骤 5：生成 PDF
  - 参考 docs/pdf-generation.md 中的 Word 模板方案或 HTML→PDF 方案
```

---

## 6. Office Script 参考（填充 Summary 工作表）

```typescript
function main(workbook: ExcelScript.Workbook, reportModelJson: string): void {
  const rm = JSON.parse(reportModelJson);
  const summary = rm.summary;
  
  // 获取或创建 Summary 工作表
  let sheet = workbook.getWorksheet("Summary");
  if (!sheet) sheet = workbook.addWorksheet("Summary");
  sheet.activate();
  
  // 写入数据
  const data: (string | number | boolean)[][] = [
    ["报告标题", "Offshoring 数据校验报告"],
    ["数据来源", rm.source_table],
    ["数据期间", rm.period],
    ["报告生成时间", rm.generated_at],
    [],
    ["检查总行数", summary.total_rows_checked],
    ["含问题行数", summary.rows_with_issues],
    ["无问题行数", summary.rows_clean],
    [],
    ["问题总数", summary.total_issues],
    ["Error 数量", summary.error_count],
    ["Warning 数量", summary.warning_count],
    [],
    ["校验结果", summary.validation_passed ? "PASSED ✅" : "FAILED ❌"],
    [],
    ["规则", "严重级别", "问题数"],
    ...rm.issues_by_rule.map((r: {rule_id: string, severity: string, count: number}) => 
      [r.rule_id, r.severity, r.count])
  ];
  
  const range = sheet.getRangeByIndexes(0, 0, data.length, 3);
  range.setValues(data);
  
  // 样式：校验结果行
  const resultRow = 14; // 行索引（0-based）= 行14，即第15行
  const resultCell = sheet.getCell(resultRow - 1, 1);
  if (summary.validation_passed) {
    resultCell.getFormat().getFill().setColor("#E2EFDA");
    resultCell.getFormat().getFont().setColor("#375623");
  } else {
    resultCell.getFormat().getFill().setColor("#FFCCCC");
    resultCell.getFormat().getFont().setColor("#9C0006");
  }
  resultCell.getFormat().getFont().setBold(true);
  
  // 调整列宽
  sheet.getRange("A:A").getFormat().setColumnWidth(130);
  sheet.getRange("B:B").getFormat().setColumnWidth(160);
}
```

---

## 7. Office Script 参考（填充 Issues 工作表）

```typescript
function main(workbook: ExcelScript.Workbook, issuesJson: string): void {
  const issues: {
    Severity: string; RuleId: string; RowKey: string;
    Column: string; RawValue: string; Message: string; FixSuggestion: string;
  }[] = JSON.parse(issuesJson);
  
  let sheet = workbook.getWorksheet("Issues");
  if (!sheet) sheet = workbook.addWorksheet("Issues");
  
  // 表头
  const headers = ["#", "Severity", "Rule ID", "Row Key", "Column", "Raw Value", "Message", "Fix Suggestion"];
  const headerRange = sheet.getRangeByIndexes(0, 0, 1, headers.length);
  headerRange.setValues([headers]);
  
  // 数据行
  if (issues.length > 0) {
    const rows = issues.map((issue, idx) => [
      idx + 1,
      issue.Severity,
      issue.RuleId,
      issue.RowKey,
      issue.Column,
      issue.RawValue,
      issue.Message,
      issue.FixSuggestion
    ]);
    const dataRange = sheet.getRangeByIndexes(1, 0, rows.length, headers.length);
    dataRange.setValues(rows);
    
    // 转为表格
    const tableRange = sheet.getRangeByIndexes(0, 0, rows.length + 1, headers.length);
    const table = workbook.addTable(tableRange, true);
    table.setName("tblIssues");
    table.setPredefinedTableStyle("TableStyleMedium2");
    
    // 条件格式（Severity 列）
    for (let i = 0; i < issues.length; i++) {
      const rowRange = sheet.getRangeByIndexes(i + 1, 0, 1, headers.length);
      const severityCell = sheet.getCell(i + 1, 1);
      if (issues[i].Severity === "Error") {
        rowRange.getFormat().getFill().setColor("#FFCCCC");
        severityCell.getFormat().getFont().setColor("#9C0006");
        severityCell.getFormat().getFont().setBold(true);
      } else if (issues[i].Severity === "Warning") {
        rowRange.getFormat().getFill().setColor("#FFEB9C");
        severityCell.getFormat().getFont().setColor("#9C5700");
      }
    }
  }
  
  // 列宽
  const colWidths = [30, 65, 100, 300, 150, 140, 260, 260];
  colWidths.forEach((w, i) => {
    sheet.getColumnByIndex(i).getFormat().setColumnWidth(w);
  });
  
  // 自动换行（F、G、H 列）
  ["F", "G", "H"].forEach(col => {
    sheet.getRange(`${col}:${col}`).getFormat().setWrapText(true);
  });
}
```
