# POC-Validator — Power Automate Flow 操作手册（Runbook）

**版本**：v1.0  
**适用阶段**：POC（个人 OneDrive + Teams）  
**触发方式**：手动触发（Manual trigger）

---

## 前置准备

| 项目 | 说明 |
|------|------|
| OneDrive 目录 | 已创建 `Documents/PowerbiTest/POC-Validator/` 及其子目录（见 README） |
| Excel 模板 | `Templates/ValidationReportTemplate.xlsx` 已上传至 OneDrive |
| Copilot Studio | AI 验证 Action 已发布并获得 HTTP 端点 URL |
| Teams 频道 | 已确认用于接收通知的团队和频道 |
| Power Automate | 已有 Power Automate 许可证（含 Premium 连接器权限） |

---

## Flow 总览

```
[手动触发] → [读取 Excel 数据] → [调用 AI 验证] → [复制模板]
    → [写入 Summary Sheet] → [写入 Issues Sheet]
    → [生成 HTML 报告] → [转换为 PDF]
    → [上传 OneDrive] → [发送 Teams 通知]
```

---

## Step 1 — 触发器：手动触发（Manually trigger a flow）

**连接器**：Power Automate（内置）  
**动作**：Manually trigger a flow

### 输入参数配置

| 参数名 | 类型 | 说明 |
|--------|------|------|
| `SourceFile` | File | 选择待验证的 Excel 源文件（OneDrive 中的 `tblOffshoring` 所在文件） |

> 每次手动触发时，通过文件选择器选择源文件，无需硬编码路径。

---

## Step 2 — 读取 Excel 数据（List rows present in a table）

**连接器**：Excel Online (Business)  
**动作**：List rows present in a table

### 配置

| 字段 | 值 |
|------|----|
| Location | OneDrive for Business |
| Document Library | （由触发器 SourceFile 动态传入） |
| File | `triggerBody()/file` |
| Table | `tblOffshoring` |
| Pagination | **开启**（Threshold: 5000） |

> **重要**：开启分页（Pagination）以确保数据行数多时不截断。  
> **不要**对读取到的单元格值做任何 trim/replace 处理，原样传给 AI。

### 输出变量

将此步骤结果存入变量 `varRawRows`（类型：Array）。

**动作**：Initialize variable

| 字段 | 值 |
|------|----|
| Name | `varRawRows` |
| Type | Array |
| Value | `outputs('List_rows_present_in_a_table')?['body/value']` |

---

## Step 3 — 构建带 RowIndex 的请求体（Apply to each + Append to array）

> Power Automate 表达式语言不支持 `map()` 或箭头函数，请使用以下标准动作组合来实现行数组转换。

**步骤 3a**：Initialize variable — `varRowsWithIndex`（类型：Array，初始值：`[]`）

**步骤 3b**：Apply to each（循环 `variables('varRawRows')`）

在循环体内使用 **Append to array variable** 动作：

| 字段 | 值 |
|------|----|
| Name | `varRowsWithIndex` |
| Value | `union(items('Apply_to_each'), json(concat('{"RowIndex":', add(indexOf(variables('varRawRows'), items('Apply_to_each')), 2), '}')))` |

> **简化做法**：如果 `indexOf` 性能不佳（大数组），也可使用 **Initialize variable**（`varRowIndex`，Integer，初始值 2）+ 在循环末尾 **Increment variable**（+1）来追踪行号，比 `indexOf` 更稳定。

**步骤 3c**：Compose — 构建最终请求体

```json
{
  "datasetName": "tblOffshoring",
  "rows": @{variables('varRowsWithIndex')}
}
```

将结果存入变量 `varRequestBody`。

---

## Step 4 — 调用 AI 验证（HTTP）

**连接器**：HTTP  
**动作**：HTTP

### 配置

| 字段 | 值 |
|------|----|
| Method | POST |
| URI | `<Copilot Studio Action 端点 URL>` |
| Headers | `Content-Type: application/json` |
| Body | `variables('varRequestBody')` |
| Authentication | Bearer Token（从 Entra ID 获取） |

### 输出

将响应体存入变量 `varAIResponse`：

```
body('HTTP')
```

> AI 返回格式详见 [`copilot-studio-api.md`](copilot-studio-api.md)。

---

## Step 5 — 解析 AI 响应（Parse JSON）

**动作**：Parse JSON

| 字段 | 值 |
|------|----|
| Content | `variables('varAIResponse')` |
| Schema | 参照 `copilot-studio-api.md` 中的响应 JSON Schema |

解析后可通过 `body('Parse_JSON')/issues` 引用每条问题。

---

## Step 6 — 复制 Excel 模板（Copy file）

**连接器**：OneDrive for Business  
**动作**：Copy file

### 配置

| 字段 | 值 |
|------|----|
| File to Copy | `/Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx` |
| Destination Folder | `/Documents/PowerbiTest/POC-Validator/Outputs/Excel/` |
| New Name | `ValidationReport_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmmss')}.xlsx` |

将返回的新文件 ID 存入变量 `varReportFileId`。

---

## Step 7 — 写入 Summary Sheet（更新单元格）

**连接器**：Excel Online (Business)  
**动作**：Update a row（或 Run script）

按照 [`summary-sheet-cell-mapping.md`](summary-sheet-cell-mapping.md) 中定义的固定单元格，将统计结果写入 `Summary` 工作表。

### 推荐方式：Office Script

使用 **Run script** 动作，传入以下参数，通过 Office Script 批量更新固定单元格：

```javascript
// Office Script 示例（在 Excel Online 中保存为脚本）
function main(workbook: ExcelScript.Workbook,
              validatedAt: string,
              sourceFile: string,
              totalRows: number,
              errorCount: number,
              warningCount: number,
              infoCount: number) {
  const sheet = workbook.getWorksheet("Summary");
  sheet.getRange("B2").setValue(validatedAt);     // 验证时间
  sheet.getRange("B3").setValue(sourceFile);      // 源文件名
  sheet.getRange("B4").setValue(totalRows);       // 总行数
  sheet.getRange("B5").setValue(errorCount);      // Error 数量
  sheet.getRange("B6").setValue(warningCount);    // Warning 数量
  sheet.getRange("B7").setValue(infoCount);       // Info 数量
  sheet.getRange("B8").setValue(errorCount + warningCount + infoCount); // 合计
}
```

**Power Automate 调用配置**：

> Power Automate 表达式语言不支持 `where()` 的 lambda 语法。推荐在 Step 5（Parse JSON）之后，用 **Filter Array** 动作分别过滤三个级别，再用 `length()` 统计数量。

**步骤 7 前置**：添加三个 Filter Array 动作

| 动作名称 | 输入 | 条件 |
|---------|------|------|
| `Filter_Errors` | `body('Parse_JSON')?['issues']` | `@{items('Filter_Errors')?['level']}` equals `Error` |
| `Filter_Warnings` | `body('Parse_JSON')?['issues']` | `@{items('Filter_Warnings')?['level']}` equals `Warning` |
| `Filter_Infos` | `body('Parse_JSON')?['issues']` | `@{items('Filter_Infos')?['level']}` equals `Info` |

然后设置变量：

| 变量 | 表达式 |
|------|--------|
| `varErrorCount` | `@{length(body('Filter_Errors'))}` |
| `varWarningCount` | `@{length(body('Filter_Warnings'))}` |
| `varInfoCount` | `@{length(body('Filter_Infos'))}` |

**Run script 调用配置**：

| 字段 | 值 |
|------|----|
| Location | OneDrive for Business |
| File | `variables('varReportFileId')` |
| Script | `UpdateSummary`（脚本名称） |
| validatedAt | `@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}` |
| sourceFile | `@{triggerBody()?['file']?['name']}` |
| totalRows | `@{length(variables('varRawRows'))}` |
| errorCount | `@{variables('varErrorCount')}` |
| warningCount | `@{variables('varWarningCount')}` |
| infoCount | `@{variables('varInfoCount')}` |

---

## Step 8 — 写入 Issues Sheet（添加行）

**连接器**：Excel Online (Business)  
**动作**：Add a row into a table（在 Apply to each 循环中执行）

**循环对象**：`body('Parse_JSON')/issues`

### 每行写入字段

| Excel 列名 | 表达式 |
|-----------|--------|
| RowIndex | `items('Apply_to_each')/RowIndex` |
| Field | `items('Apply_to_each')/field` |
| RuleId | `items('Apply_to_each')/ruleId` |
| Level | `items('Apply_to_each')/level` |
| Message | `items('Apply_to_each')/message` |
| ActualValue | `items('Apply_to_each')/actualValue` |

> `issues[]` 数组中每个元素必须包含 `RowIndex` 字段（由 AI 返回），以便追溯到源数据行。

---

## Step 9 — 生成 HTML 报告（Compose）

**动作**：Compose

根据 AI 返回的 `issues` 数组，构建 HTML 报告字符串：

```
@{concat(
  '<html><head><meta charset="UTF-8"><style>',
  'body{font-family:Calibri,sans-serif;font-size:13px;}',
  'table{border-collapse:collapse;width:100%;}',
  'th{background:#366092;color:#fff;padding:6px 10px;text-align:left;}',
  'td{border:1px solid #ccc;padding:5px 10px;}',
  '.Error{color:#C00000;font-weight:bold;}',
  '.Warning{color:#ED7D31;}',
  '.Info{color:#4472C4;}',
  '</style></head><body>',
  '<h2>Validation Report — ', formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm'), '</h2>',
  '<p>Source File: <b>', triggerBody()/file/name, '</b></p>',
  '<p>Total Rows: ', length(variables('varRawRows')),
  ' | Errors: <span class="Error">', variables('varErrorCount'), '</span>',
  ' | Warnings: <span class="Warning">', variables('varWarningCount'), '</span>',
  ' | Info: <span class="Info">', variables('varInfoCount'), '</span></p>',
  '<table><tr><th>RowIndex</th><th>Field</th><th>Rule</th><th>Level</th><th>Message</th><th>Actual Value</th></tr>',
  variables('varIssuesHtmlRows'),
  '</table></body></html>'
)}
```

> `varIssuesHtmlRows` 通过前置循环（Apply to each）拼接每条 issue 的 `<tr>` 行。

将结果存入变量 `varHtmlContent`。

---

## Step 10 — 上传 HTML 文件（Create file）

**连接器**：OneDrive for Business  
**动作**：Create file

| 字段 | 值 |
|------|----|
| Folder Path | `/Documents/PowerbiTest/POC-Validator/Outputs/PDF/` |
| File Name | `ValidationReport_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmmss')}.html` |
| File Content | `variables('varHtmlContent')` |

将返回的文件路径存入 `varHtmlFilePath`，将文件 ID 存入 `varHtmlFileId`。

---

## Step 11 — HTML 转 PDF（Convert file）

**连接器**：OneDrive for Business  
**动作**：Convert file

| 字段 | 值 |
|------|----|
| File | `variables('varHtmlFileId')` |
| Target type | PDF |

将 PDF 内容存入 `varPdfContent`。

**随后**：Create file（保存 PDF）

| 字段 | 值 |
|------|----|
| Folder Path | `/Documents/PowerbiTest/POC-Validator/Outputs/PDF/` |
| File Name | `ValidationReport_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmmss')}.pdf` |
| File Content | `variables('varPdfContent')` |

将返回的文件 Web URL 存入 `varPdfUrl`，Excel 报告的 Web URL 存入 `varExcelUrl`。

---

## Step 12 — 发送 Teams 通知（Post adaptive card）

**连接器**：Microsoft Teams  
**动作**：Post an Adaptive Card to a Teams channel (V2)

配置方式详见 [`teams-message-template.md`](teams-message-template.md)。

| 字段 | 值 |
|------|----|
| Team | 选择目标团队 |
| Channel | 选择目标频道 |
| Adaptive Card | 参照模板填入动态值 |

---

## 错误处理建议

| 场景 | 处理方式 |
|------|---------|
| AI 调用失败（HTTP 非 200） | 用 Configure run after 捕获，发送 Teams 错误通知 |
| Excel 写入失败 | 记录失败信息，附加到 Teams 通知 |
| 模板文件不存在 | 在 Step 6 前加 Get file metadata 校验步骤 |

---

## 变量汇总

| 变量名 | 类型 | 用途 |
|--------|------|------|
| `varRawRows` | Array | Excel 原始数据行 |
| `varRequestBody` | Object | 发送给 AI 的请求体 |
| `varAIResponse` | String | AI 返回的原始响应 |
| `varReportFileId` | String | 复制后的 Excel 报告文件 ID |
| `varHtmlContent` | String | 生成的 HTML 报告内容 |
| `varHtmlFileId` | String | 上传的 HTML 文件 ID |
| `varHtmlFilePath` | String | HTML 文件路径 |
| `varPdfContent` | String | PDF 文件内容（Base64） |
| `varPdfUrl` | String | PDF 文件的 OneDrive 分享链接 |
| `varExcelUrl` | String | Excel 报告的 OneDrive 分享链接 |
| `varErrorCount` | Integer | Error 级别问题数量 |
| `varWarningCount` | Integer | Warning 级别问题数量 |
| `varInfoCount` | Integer | Info 级别问题数量 |
| `varIssuesHtmlRows` | String | Issues 的 HTML 表格行拼接 |
