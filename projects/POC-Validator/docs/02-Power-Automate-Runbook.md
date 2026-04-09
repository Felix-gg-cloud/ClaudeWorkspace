# 02 — Power Automate Flow 操作手顺（手动触发版）

本文档提供完整的逐动作操作手顺，用于搭建 POC 阶段的数据校验 Flow。

---

## 架构概览

```
[手动触发]
    │
    ▼
[Step 1] 读取 OneDrive Excel（tblOffshoring，原样读取）
    │
    ▼
[Step 2] 组装 AI 输入 Payload（每行加 RowIndex）
    │
    ▼
[Step 3] 调用 Copilot Studio Agent（AI-only 校验）
    │
    ▼
[Step 4] 复制报告模板（从 Templates/ 复制到 Outputs/Excel/）
    │
    ▼
[Step 5] 写入 Summary 工作表
    │
    ▼
[Step 6] 循环写入 Issues 行到 tblIssues 表格
    │
    ▼
[Step 7] 生成 PDF 报告（HTML → PDF，Top N）
    │
    ▼
[Step 8] Teams 通知（附 Excel + PDF 链接）
```

---

## 前置条件

- [ ] 已按 [01-Excel-Template-Guide.md](01-Excel-Template-Guide.md) 创建 `ValidationReportTemplate.xlsx`
- [ ] 模板位于：`Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx`
- [ ] 已在 Power Automate 中登录 OneDrive 连接器
- [ ] 已在 Power Automate 中登录 Copilot Studio 连接器（需要有 Copilot Studio 许可）
- [ ] 已在 Power Automate 中登录 Microsoft Teams 连接器

---

## 动作详情

### Step 0：手动触发 — 配置输入参数

**动作类型**：`手动触发流`（Manually trigger a flow）

在触发器中添加以下输入字段：

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `YearMonth` | 文本 | 报告年月（YYYYMM） | `202501` |
| `TopNIssuesInPDF` | 数字 | PDF 中显示的最大 Issue 数 | `200` |
| `InputFilePath` | 文本 | 源数据文件相对路径（可选） | `/Documents/PowerbiTest/Inputs/Master Excel.xlsx` |

---

### Step 1：读取 Excel 表数据

**动作类型**：`列出表中现有行`（Excel Online (Business) › List rows present in a table）

| 参数 | 值 |
|------|-----|
| 位置 | OneDrive for Business |
| 文档库 | OneDrive |
| 文件 | `/Documents/PowerbiTest/Inputs/Master Excel_Strategy_OnOffshoring.xlsx` |
| 表 | `tblOffshoring` |
| 分页 | 开启（选择"显示高级选项"→ 开启 Skip count / Top count 或使用分页设置） |

> ⚠️ **不要**在此步骤对数据做任何 trim、替换或格式化。  
> 保留原始值（含空格、特殊字符），让 AI 识别这些问题。

**输出变量命名**：`tblOffshoringRows`（在后续步骤中引用 `outputs('List_rows')['body/value']`）

---

### Step 2：组装 AI 输入 Payload

**动作类型**：`初始化变量`（Initialize variable）

创建以下变量：

| 变量名 | 类型 | 初始值 |
|--------|------|--------|
| `varRowIndex` | 整数 | `0` |
| `varPayloadRows` | 数组 | `[]` |

**动作类型**：`应用到每一个`（Apply to each）循环

- 输入：`tblOffshoringRows`
- 循环内部：

  1. **递增 RowIndex**
     - 动作：`递增变量`（Increment variable）
     - 变量：`varRowIndex`，值：`1`

  2. **追加到 Payload 数组**
     - 动作：`追加到数组变量`（Append to array variable）
     - 变量：`varPayloadRows`
     - 值（JSON 对象，将每列映射为字段）：

     ```json
     {
       "RowIndex": @{variables('varRowIndex')},
       "RowKey": @{items('Apply_to_each')['RowKey']},
       "YearMonth": @{triggerBody()['YearMonth']},
       "Cost Center Number": @{items('Apply_to_each')['Cost Center Number']},
       "Function": @{items('Apply_to_each')['Function']},
       "Team": @{items('Apply_to_each')['Team']},
       "Owner": @{items('Apply_to_each')['Owner']}
     }
     ```

     > 根据源表实际列名调整字段映射。

---

### Step 3：调用 Copilot Studio Agent（AI 校验）

**动作类型**：`运行操作`（Copilot Studio › Run an action）

| 参数 | 值 |
|------|-----|
| 代理 | 你的 Copilot Studio Agent 名称 |
| 操作 | `ValidateOffshoringData`（或你在 Agent 中定义的 Action 名） |
| 输入 - rows | `@{variables('varPayloadRows')}` |
| 输入 - yearMonth | `@{triggerBody()['YearMonth']}` |

**期望输出**（由 Copilot Studio Agent 返回）：

```json
{
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "SPACE_001",
      "RowKey": "R-005",
      "RowIndex": 5,
      "YearMonth": "202501",
      "Cost Center Number": "CC-1234",
      "Function": "Finance",
      "Team": "FP&A",
      "Owner": " Alice",
      "Column": "Owner",
      "RawValue": " Alice",
      "Message": "Leading space detected in Owner field",
      "FixSuggestion": "Remove leading/trailing spaces from the Owner column"
    }
  ],
  "report_model": {
    "totalRows": 150,
    "errorCount": 3,
    "warningCount": 12,
    "topRules": ["SPACE_001", "R002", "R005"],
    "topColumns": ["Owner", "Cost Center Number", "Team"]
  }
}
```

**输出变量命名**：
- `varIssues`：`@{outputs('Run_action')['body/issues']}`
- `varReportModel`：`@{outputs('Run_action')['body/report_model']}`

---

### Step 4：复制报告模板

**动作类型**：`复制文件`（OneDrive › Copy file）

| 参数 | 值 |
|------|-----|
| 源文件路径 | `/Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx` |
| 目标文件夹 | `/Documents/PowerbiTest/POC-Validator/Outputs/Excel/` |
| 新文件名 | `ValidationReport_@{triggerBody()['YearMonth']}_@{formatDateTime(utcNow(),'yyyyMMddHHmmss')}.xlsx` |
| 覆盖 | 否 |

**输出变量命名**：`varOutputExcelPath`（记录新文件完整路径，供后续步骤使用）

> 复制而非直接写入模板，确保模板始终保持空白状态，可重复使用。

---

### Step 5：写入 Summary 工作表

**动作类型**：`更新行`（Excel Online (Business) › Update a row）

> `Summary` 工作表使用单元格直接写入方式，而非表格行方式。

用以下动作序列，逐一更新 Summary 工作表各行（A 列为标签，B 列为值）：

**方式**：使用 `运行脚本`（Run Script）动作（Office Scripts），一次性写入所有 Summary 字段：

```javascript
// Office Script：写入 Summary 工作表
function main(workbook: ExcelScript.Workbook, reportTitle: string, generatedAt: string,
              yearMonth: string, totalRows: number, errorCount: number, warningCount: number,
              topRule1: string, topRule2: string, topRule3: string,
              topCol1: string, topCol2: string, topCol3: string) {
  const sheet = workbook.getWorksheet("Summary");
  const values = [
    ["Report Title", reportTitle],
    ["Generated At", generatedAt],
    ["YearMonth", yearMonth],
    ["Total Rows", totalRows],
    ["Error Count", errorCount],
    ["Warning Count", warningCount],
    ["Top Rule (1)", topRule1],
    ["Top Rule (2)", topRule2],
    ["Top Rule (3)", topRule3],
    ["Top Column (1)", topCol1],
    ["Top Column (2)", topCol2],
    ["Top Column (3)", topCol3],
  ];
  sheet.getRange("A1:B12").setValues(values);
}
```

Office Script 动作参数：

| 参数 | 值 |
|------|-----|
| 位置 | OneDrive for Business |
| 文档库 | OneDrive |
| 文件 | `@{varOutputExcelPath}` |
| 脚本 | `WriteSummary`（在 Excel 网页端提前创建并保存上述脚本） |
| reportTitle | `Offshoring Validation Report` |
| generatedAt | `@{utcNow()}` |
| yearMonth | `@{triggerBody()['YearMonth']}` |
| totalRows | `@{variables('varReportModel')['totalRows']}` |
| errorCount | `@{variables('varReportModel')['errorCount']}` |
| warningCount | `@{variables('varReportModel')['warningCount']}` |
| topRule1 | `@{variables('varReportModel')['topRules'][0]}` |
| topRule2 | `@{variables('varReportModel')['topRules'][1]}` |
| topRule3 | `@{variables('varReportModel')['topRules'][2]}` |
| topCol1 | `@{variables('varReportModel')['topColumns'][0]}` |
| topCol2 | `@{variables('varReportModel')['topColumns'][1]}` |
| topCol3 | `@{variables('varReportModel')['topColumns'][2]}` |

---

### Step 6：循环写入 Issues 行到 tblIssues

**动作类型**：`应用到每一个`（Apply to each）循环

- 输入：`@{variables('varIssues')}`
- 循环内部：

  **动作**：`在表中添加行`（Excel Online (Business) › Add a row into a table）

  | 参数 | 值 |
  |------|-----|
  | 位置 | OneDrive for Business |
  | 文档库 | OneDrive |
  | 文件 | `@{varOutputExcelPath}` |
  | 表 | `tblIssues` |

  列值映射（**列名必须与 tblIssues 表格标题完全一致**）：

  | 列标题 | 动态值 |
  |--------|--------|
  | Severity | `@{items('Apply_to_each_issues')['Severity']}` |
  | RuleId | `@{items('Apply_to_each_issues')['RuleId']}` |
  | RowKey | `@{items('Apply_to_each_issues')['RowKey']}` |
  | RowIndex | `@{items('Apply_to_each_issues')['RowIndex']}` |
  | YearMonth | `@{items('Apply_to_each_issues')['YearMonth']}` |
  | Cost Center Number | `@{items('Apply_to_each_issues')['Cost Center Number']}` |
  | Function | `@{items('Apply_to_each_issues')['Function']}` |
  | Team | `@{items('Apply_to_each_issues')['Team']}` |
  | Owner | `@{items('Apply_to_each_issues')['Owner']}` |
  | Column | `@{items('Apply_to_each_issues')['Column']}` |
  | RawValue | `@{items('Apply_to_each_issues')['RawValue']}` |
  | Message | `@{items('Apply_to_each_issues')['Message']}` |
  | FixSuggestion | `@{items('Apply_to_each_issues')['FixSuggestion']}` |

> **关键**：表名 `tblIssues` 需在下拉列表中选择（Power Automate 会自动识别目标文件中的所有 Excel 表格）。  
> 列映射界面中显示的列名由 `tblIssues` 的实际标题决定，需与模板完全一致。

---

### Step 7：生成 PDF 报告（Top N Issues）

**动作类型**：`撰写`（Compose）

构建 HTML 字符串，包含 Summary 和 Top N Issues（`take(variables('varIssues'), triggerBody()['TopNIssuesInPDF'])`）：

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }
    th { background-color: #4472C4; color: white; }
    .error { background-color: #FF0000; color: white; }
    .warning { background-color: #FFC000; }
    h2 { color: #4472C4; }
  </style>
</head>
<body>
  <h1>Offshoring Data Validation Report</h1>
  <h2>Summary</h2>
  <table>
    <tr><td>YearMonth</td><td>@{triggerBody()['YearMonth']}</td></tr>
    <tr><td>Generated At</td><td>@{utcNow()}</td></tr>
    <tr><td>Total Rows</td><td>@{variables('varReportModel')['totalRows']}</td></tr>
    <tr><td>Error Count</td><td>@{variables('varReportModel')['errorCount']}</td></tr>
    <tr><td>Warning Count</td><td>@{variables('varReportModel')['warningCount']}</td></tr>
  </table>

  <h2>Top Issues (Max @{triggerBody()['TopNIssuesInPDF']})</h2>
  <table>
    <tr>
      <th>Severity</th><th>RuleId</th><th>RowKey</th><th>RowIndex</th>
      <th>Column</th><th>RawValue</th><th>Message</th><th>FixSuggestion</th>
    </tr>
    <!-- 通过 Power Automate 循环拼接 tr 行 -->
  </table>
</body>
</html>
```

**动作类型**：`转换为 PDF`（OneDrive › Convert file）

| 参数 | 值 |
|------|-----|
| 内容 | 上一步 Compose 输出的 HTML（需先存为临时 HTML 文件再转换） |
| 目标格式 | PDF |

**保存 PDF**：`创建文件`（OneDrive › Create file）

| 参数 | 值 |
|------|-----|
| 文件夹路径 | `/Documents/PowerbiTest/POC-Validator/Outputs/PDF/` |
| 文件名 | `ValidationReport_@{triggerBody()['YearMonth']}_@{formatDateTime(utcNow(),'yyyyMMddHHmmss')}.pdf` |
| 文件内容 | PDF 转换结果 |

---

### Step 8：Teams 通知

**动作类型**：`发布消息（在聊天或频道中）`（Microsoft Teams › Post a message in a chat or channel）

| 参数 | 值 |
|------|-----|
| 发布为 | Flow Bot |
| 发布到 | Chat with Flow Bot（或选择频道） |
| 接收人 | 你的 Teams 账户邮件 |

消息内容（Adaptive Card 格式可选，纯文本示例）：

```
📊 Offshoring 数据校验报告已生成

✅ YearMonth：@{triggerBody()['YearMonth']}
📁 Total Rows：@{variables('varReportModel')['totalRows']}
❌ Errors：@{variables('varReportModel')['errorCount']}
⚠️ Warnings：@{variables('varReportModel')['warningCount']}

📄 Excel 报告（全量明细）：@{varOutputExcelPath}
📑 PDF 报告（Top @{triggerBody()['TopNIssuesInPDF']} Issues）：@{varOutputPDFPath}
```

---

## 常见问题排查

| 问题 | 可能原因 | 解决方法 |
|------|----------|----------|
| "无法找到表 tblIssues" | 模板中表格未命名为 `tblIssues` | 在 Excel 中确认表名，参见 [01-Excel-Template-Guide.md](01-Excel-Template-Guide.md) Step 5-4 |
| "列名不匹配" | 模板标题与 Flow 中配置不一致 | 检查列标题拼写，特别是 `Cost Center Number`（中间有空格） |
| "读取行超时" | 源文件行数过多且未开启分页 | Step 1 中开启分页（Pagination）选项 |
| "AI 返回格式错误" | Copilot Studio Agent 返回结构与期望不符 | 检查 Agent 输出 Schema，确保包含 `issues[]` 和 `report_model` |
| "PDF 转换失败" | HTML 内容过大或格式错误 | 减少 TopN 值，或检查 HTML 拼接是否有语法错误 |

---

## 后续迁移到团队 SharePoint

| 变更项 | POC（OneDrive） | 生产（SharePoint） |
|--------|----------------|-------------------|
| 连接器 | OneDrive for Business | SharePoint |
| 文件路径 | `/Documents/PowerbiTest/POC-Validator/...` | `/sites/<SiteName>/Shared Documents/POC-Validator/...` |
| 权限 | 个人账号 | 团队成员均可访问 |
| 触发方式 | 手动触发 | 定时触发（每月 N 日）或事件触发 |
| 通知方式 | Teams 个人聊天 | Teams 频道 + 邮件 |

> Flow 的核心逻辑（Steps 1-8）完全不需要修改，只需更新连接器类型和文件路径参数。
