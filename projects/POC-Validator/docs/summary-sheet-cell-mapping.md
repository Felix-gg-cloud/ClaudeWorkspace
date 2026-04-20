# POC-Validator — Summary Sheet 固定单元格映射表

**版本**：v1.0  
**工作表名称**：`Summary`（在 `ValidationReportTemplate.xlsx` 中）

---

## 说明

Summary 工作表使用**固定单元格**布局，Power Automate Flow 通过 Office Script 直接按坐标写入值，无需动态查找行列。模板文件中 A 列为标签列（只读），B 列为值列（由 Flow 写入）。

---

## 单元格映射表

| 单元格 | 标签（A 列，模板固定） | 写入值（B 列，Flow 动态填入） | Flow 表达式 / 说明 |
|--------|----------------------|----------------------------|--------------------|
| **A1** | `Validation Report` | ——（标题，不写入） | 模板标题（可合并 A1:B1 并设置样式），Flow 不修改 |
| **A2** | `Validated At` | 验证执行时间 | `formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')` |
| **B2** | ——（写入目标） | `2025-01-15 14:30:00` | 示例值 |
| **A3** | `Source File` | 源文件名 | `triggerBody()/file/name` |
| **B3** | ——（写入目标） | `tblOffshoring_2025Q1.xlsx` | 示例值 |
| **A4** | `Total Rows` | 验证的数据行总数 | `length(variables('varRawRows'))` |
| **B4** | ——（写入目标） | `250` | 示例值（整数） |
| **A5** | `Error Count` | Error 级别问题数量 | `variables('varErrorCount')` |
| **B5** | ——（写入目标） | `3` | 示例值（整数，字体颜色设为红色） |
| **A6** | `Warning Count` | Warning 级别问题数量 | `variables('varWarningCount')` |
| **B6** | ——（写入目标） | `7` | 示例值（整数，字体颜色设为橙色） |
| **A7** | `Info Count` | Info 级别问题数量 | `variables('varInfoCount')` |
| **B7** | ——（写入目标） | `2` | 示例值（整数，字体颜色设为蓝色） |
| **A8** | `Total Issues` | 所有级别问题合计 | `add(add(variables('varErrorCount'), variables('varWarningCount')), variables('varInfoCount'))` |
| **B8** | ——（写入目标） | `12` | 示例值（整数） |
| **A9** | `Validation Status` | 整体验证状态 | 若 `varErrorCount > 0` 则 `FAILED`，否则 `PASSED` |
| **B9** | ——（写入目标） | `FAILED` / `PASSED` | 示例值 |
| **A10** | `Excel Report` | Excel 报告的 OneDrive 链接 | `variables('varExcelUrl')` |
| **B10** | ——（写入目标） | OneDrive URL | 超链接写入 |
| **A11** | `PDF Report` | PDF 报告的 OneDrive 链接 | `variables('varPdfUrl')` |
| **B11** | ——（写入目标） | OneDrive URL | 超链接写入 |

---

## Office Script 完整示例

以下 Office Script 实现上述单元格的批量写入，在 Power Automate 的 **Run script** 动作中调用：

```typescript
function main(
  workbook: ExcelScript.Workbook,
  validatedAt: string,
  sourceFile: string,
  totalRows: number,
  errorCount: number,
  warningCount: number,
  infoCount: number,
  excelReportUrl: string,
  pdfReportUrl: string
) {
  const sheet = workbook.getWorksheet("Summary");

  // 写入基本信息
  sheet.getRange("B2").setValue(validatedAt);
  sheet.getRange("B3").setValue(sourceFile);
  sheet.getRange("B4").setValue(totalRows);

  // 写入问题统计（含字体颜色）
  const errorCell = sheet.getRange("B5");
  errorCell.setValue(errorCount);
  errorCell.getFormat().getFont().setColor("#C00000");

  const warningCell = sheet.getRange("B6");
  warningCell.setValue(warningCount);
  warningCell.getFormat().getFont().setColor("#ED7D31");

  const infoCell = sheet.getRange("B7");
  infoCell.setValue(infoCount);
  infoCell.getFormat().getFont().setColor("#4472C4");

  sheet.getRange("B8").setValue(errorCount + warningCount + infoCount);

  // 写入整体状态
  const statusCell = sheet.getRange("B9");
  const statusText = errorCount > 0 ? "FAILED" : "PASSED";
  statusCell.setValue(statusText);
  statusCell.getFormat().getFont().setColor(errorCount > 0 ? "#C00000" : "#375623");
  statusCell.getFormat().getFont().setBold(true);

  // 写入报告链接
  sheet.getRange("B10").setValue(excelReportUrl);
  sheet.getRange("B11").setValue(pdfReportUrl);
}
```

---

## Issues 工作表表头映射

`Issues` 工作表中存放的是 `tblIssues` 表格，列头定义如下：

| 列号 | 列名（Excel 表头） | 对应 issues[] 字段 | 说明 |
|------|-------------------|-------------------|------|
| A | `RowIndex` | `RowIndex` | **必填**，Excel 源数据行号（从 2 开始） |
| B | `Field` | `field` | 触发问题的字段名 |
| C | `RuleId` | `ruleId` | 规则 ID，如 `R-WS-ALL-001` |
| D | `Level` | `level` | `Error` / `Warning` / `Info` |
| E | `Message` | `message` | 错误描述（中文） |
| F | `ActualValue` | `actualValue` | 原始字段值 |

---

## 模板文件要求

在 `ValidationReportTemplate.xlsx` 中，需提前完成以下设置：

1. **Summary 工作表**：
   - 在 A1 添加标题"Validation Report"（可合并 A1:B1 并设置样式）
   - 在 A2:A11 填入上述标签文字（静态内容，Flow 只写 B 列）
   - B 列单元格保持空白（Flow 运行时写入）
   - 对 B5（Error Count）单元格预设红色字体格式（脚本也会设置，双重保障）

2. **Issues 工作表**：
   - 创建名为 `tblIssues` 的 Excel 表格（Table）
   - 表头按上表顺序设置（A:F 列）
   - 表格数据区域保持空白（Flow 运行时追加行）

3. **保护设置**（可选）：
   - 锁定 Summary 工作表的 A 列（标签列）防止误改
   - 解锁 B 列以允许 Flow / Office Script 写入
