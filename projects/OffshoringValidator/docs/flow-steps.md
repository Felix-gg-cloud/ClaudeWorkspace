# Power Automate 流程步骤详细配置

> **流程名称**：`Offshoring Validator — POC`  
> **触发方式**：手动触发（Manually trigger a flow）  
> **对应 Runbook**：[poc-runbook.md](./poc-runbook.md)

---

## 目录

1. [触发器配置](#1-触发器配置)
2. [初始化变量](#2-初始化变量)
3. [读取 Excel 表格数据](#3-读取-excel-表格数据)
4. [构造 AI 输入 Payload](#4-构造-ai-输入-payload)
5. [调用 AI 模型](#5-调用-ai-模型)
6. [解析 AI JSON 输出](#6-解析-ai-json-输出)
7. [创建 OneDrive 输出文件夹](#7-创建-onedrive-输出文件夹)
8. [生成 Excel 报告](#8-生成-excel-报告)
9. [生成 PDF 报告](#9-生成-pdf-报告)
10. [获取文件分享链接](#10-获取文件分享链接)
11. [发送 Teams 通知](#11-发送-teams-通知)
12. [错误处理](#12-错误处理)
13. [变量汇总](#变量汇总)

---

## 1. 触发器配置

**Action**：`Manually trigger a flow`

### 触发器输入字段

在手动触发时，要求用户输入以下字段：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `excel_file_url` | Text | ✅ | OneDrive 中 Excel 文件的完整 URL 或相对路径（如 `/OffshoringValidator/InputData/tblOffshoring_input.xlsx`） |
| `notify_teams_chat_id` | Text | ❌ | Teams 聊天 ID（可选；留空则使用流程内置默认值） |

> **提示**：`excel_file_url` 可以是 OneDrive 文件的相对路径（从根目录开始），Power Automate 的 Excel Online 连接器接受此格式。

---

## 2. 初始化变量

使用多个 **Initialize variable** actions 在流程开始时设置所有配置值。

### 变量列表

```
// 时间戳（用于文件命名）
varTimestamp           = formatDateTime(utcNow(), 'yyyyMMdd_HHmm')
varYear                = formatDateTime(utcNow(), 'yyyy')
varMonth               = formatDateTime(utcNow(), 'MM')

// 输入配置
varExcelFileUrl        = triggerBody()?['text']   ← 从触发器获取
varTableName           = 'tblOffshoring'           ← 固定值

// 输出路径
varOutputFolder        = concat('/OffshoringValidator/Reports/', varYear, '-', varMonth)
varOutputFileBaseName  = concat('ValidationReport_', varTimestamp)
varExcelOutputPath     = concat(varOutputFolder, '/', varOutputFileBaseName, '.xlsx')
varPdfOutputPath       = concat(varOutputFolder, '/', varOutputFileBaseName, '.pdf')

// 结果存储
varAiRawResponse       = ''    (String)
varParsedReport        = null  (Object，由 Parse JSON 填充)
varExcelShareLink      = ''    (String)
varPdfShareLink        = ''    (String)
```

**配置要点**：

- `varTimestamp`：使用 `formatDateTime(utcNow(), 'yyyyMMdd_HHmm')` 生成时间戳
- `varOutputFolder`：按年月分组，格式 `/OffshoringValidator/Reports/2025-01`
- 所有路径变量在此处集中定义，方便后续迁移至 SharePoint

---

## 3. 读取 Excel 表格数据

**Action**：`List rows present in a table` (Excel Online - Business)

### 配置

| 参数 | 值 |
|------|----|
| Location | `OneDrive for Business` |
| Document Library | `OneDrive` |
| File | `varExcelFileUrl`（动态内容） |
| Table | `tblOffshoring` |
| Filter Query | （留空，读取所有行） |
| Top Count | （留空，或设置最大值如 `5000`） |

> ⚠️ **重要**：不设置任何过滤或转换，数据原样传递，包含所有前/后空格。

### 输出

- `body/value`：行数组，每行是键值对 `{列名: 值}`
- 后续步骤通过 `outputs('List_rows')?['body/value']` 引用

---

## 4. 构造 AI 输入 Payload

**Action**：`Compose`（或 `Initialize variable` + `Set variable`）

### 目的

将 Excel 行数组序列化为 AI 可处理的文本格式，减少 Token 消耗。

### 表达式

```
// 将行数组转换为 JSON 字符串
string(outputs('List_rows')?['body/value'])
```

存入变量 `varTableDataJson`。

### 行数过多时的分批处理

若数据量超过 200 行，建议使用 **Apply to each（分批）** 模式：

```
// 使用 chunk() 或手动分页
// 每批 50 行调用一次 AI，合并 issues[]
```

> POC 阶段若数据量 ≤ 200 行可跳过分批处理。

---

## 5. 调用 AI 模型

**Action**：根据许可选择以下之一：

| 选项 | Action | 适用场景 |
|------|--------|----------|
| A | `Create text with GPT using a prompt` (AI Builder) | AI Builder 许可 |
| B | `Send a prompt` (Copilot Studio / AI 插件) | Copilot Studio 许可 |
| C | `HTTP` → Azure OpenAI REST API | 自定义 API Key |

### 方案 A（AI Builder）配置

| 参数 | 值 |
|------|----|
| Prompt | 参见 [copilot-studio-prompts.md](./copilot-studio-prompts.md) → `系统提示词` |
| Input variable `table_data` | `varTableDataJson` |
| Input variable `table_name` | `varTableName` |

### 方案 C（Azure OpenAI HTTP 调用）配置

```
Method: POST
URI: https://{your-resource}.openai.azure.com/openai/deployments/{deployment-id}/chat/completions?api-version=2024-02-01
Headers:
  api-key: {your-api-key}
  Content-Type: application/json
Body:
{
  "messages": [
    {
      "role": "system",
      "content": "<系统提示词内容>"
    },
    {
      "role": "user",
      "content": "请验证以下数据：\n\n表格名称：@{varTableName}\n\n数据（JSON）：\n@{varTableDataJson}"
    }
  ],
  "temperature": 0,
  "response_format": { "type": "json_object" }
}
```

> 详细提示词见 [copilot-studio-prompts.md](./copilot-studio-prompts.md)。

---

## 6. 解析 AI JSON 输出

**Action**：`Parse JSON`

### 配置

| 参数 | 值 |
|------|----|
| Content | AI 模型输出的文本（动态内容） |
| Schema | 见下方 |

### JSON Schema

```json
{
  "type": "object",
  "properties": {
    "report_model": {
      "type": "object",
      "properties": {
        "generated_at": { "type": "string" },
        "source_file": { "type": "string" },
        "table_name": { "type": "string" },
        "total_rows": { "type": "integer" },
        "error_count": { "type": "integer" },
        "warning_count": { "type": "integer" }
      }
    },
    "issues": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "row_number": { "type": "integer" },
          "column": { "type": "string" },
          "value_preview": { "type": "string" },
          "severity": { "type": "string" },
          "rule": { "type": "string" },
          "message": { "type": "string" }
        }
      }
    }
  }
}
```

### 输出引用方式

```
// report_model 字段
body('Parse_JSON')?['report_model']?['error_count']
body('Parse_JSON')?['report_model']?['warning_count']
body('Parse_JSON')?['report_model']?['total_rows']

// issues 数组
body('Parse_JSON')?['issues']
```

---

## 7. 创建 OneDrive 输出文件夹

**Action**：`Create folder` (OneDrive for Business)

| 参数 | 值 |
|------|----|
| Root Folder | `root` |
| Folder Path | `varOutputFolder`（如 `/OffshoringValidator/Reports/2025-01`） |

> 若文件夹已存在，此 Action 会返回错误；建议配置 **Run after: has failed, is skipped** 继续后续步骤，或在 `Configure run after` 中勾选 `is successful` 和 `has failed`（文件夹已存在视为正常）。

---

## 8. 生成 Excel 报告

### 步骤 8a：创建空白 Excel 文件

**Action**：`Create file` (OneDrive for Business)

| 参数 | 值 |
|------|----|
| Folder Path | `varOutputFolder` |
| File Name | `concat(varOutputFileBaseName, '.xlsx')` |
| File Content | ← 使用预置模板（见下方说明） |

**模板方案**：

推荐使用预存在 OneDrive 中的 Excel 模板文件（含 `Summary` 和 `Issues` 两个工作表、格式化表格）：

```
模板路径（OneDrive）：/OffshoringValidator/Templates/ValidationReport_Template.xlsx
```

步骤：
1. **Get file content** (OneDrive) → 读取模板文件内容
2. **Create file** (OneDrive) → 将模板内容写入新路径
3. 后续步骤往新文件写入数据

### 步骤 8b：写入 Summary Sheet

**Action**：`Update a row` 或 `Add a row into a table` (Excel Online - Business)

目标：写入 `Summary` Sheet 的表格 `tblSummary`：

| 列 | 值 |
|----|----|
| GeneratedAt | `body('Parse_JSON')?['report_model']?['generated_at']` |
| SourceFile | `body('Parse_JSON')?['report_model']?['source_file']` |
| TableName | `body('Parse_JSON')?['report_model']?['table_name']` |
| TotalRows | `body('Parse_JSON')?['report_model']?['total_rows']` |
| ErrorCount | `body('Parse_JSON')?['report_model']?['error_count']` |
| WarningCount | `body('Parse_JSON')?['report_model']?['warning_count']` |

### 步骤 8c：写入 Issues Sheet（Apply to each）

**Action**：`Apply to each` → 遍历 `body('Parse_JSON')?['issues']`

每次迭代执行 **`Add a row into a table`** (Excel Online - Business)，目标为 `Issues` Sheet 的 `tblIssues`：

| 列 | 值 |
|----|----|
| Row | `items('Apply_to_each')?['row_number']` |
| Column | `items('Apply_to_each')?['column']` |
| Severity | `items('Apply_to_each')?['severity']` |
| Rule | `items('Apply_to_each')?['rule']` |
| ValuePreview | `items('Apply_to_each')?['value_preview']` |
| Message | `items('Apply_to_each')?['message']` |

> **性能提示**：若 issues 数量超过 50 条，单次 Apply to each 可能较慢。可考虑使用 Office Scripts 批量写入。

---

## 9. 生成 PDF 报告

### 方案 A：OneDrive 原生转换（推荐）

**Action**：`Convert file` (OneDrive for Business)

| 参数 | 值 |
|------|----|
| File | Excel 文件的 ID（来自步骤 8a 的输出） |
| Target Type | `PDF` |

然后保存转换后的内容：

**Action**：`Create file` (OneDrive for Business)

| 参数 | 值 |
|------|----|
| Folder Path | `varOutputFolder` |
| File Name | `concat(varOutputFileBaseName, '.pdf')` |
| File Content | 上一步的转换结果 |

### 方案 B：使用 Encodian 连接器（更多控制）

若需要自定义 PDF 样式或水印：

**Action**：`Convert Excel to PDF` (Encodian)

| 参数 | 值 |
|------|----|
| File Content | Excel 文件内容 |
| File Name | `concat(varOutputFileBaseName, '.xlsx')` |

---

## 10. 获取文件分享链接

### Excel 文件链接

**Action**：`Create share link` (OneDrive for Business)

| 参数 | 值 |
|------|----|
| File | Excel 文件的路径（`varExcelOutputPath`） 或 ID |
| Link Type | `View`（只读分享） |
| Link Scope | `Organization`（组织内可访问） |

**Action**：`Set variable` → `varExcelShareLink` = `outputs('Create_share_link_Excel')?['body/link/webUrl']`

### PDF 文件链接

重复上述步骤，目标改为 PDF 文件：

**Action**：`Set variable` → `varPdfShareLink` = `outputs('Create_share_link_PDF')?['body/link/webUrl']`

---

## 11. 发送 Teams 通知

**Action**：`Post message in a chat or channel` (Microsoft Teams)

### 配置

| 参数 | 值 |
|------|----|
| Post as | `Flow bot` |
| Post in | `Chat with Flow bot`（POC 阶段）或 `Channel`（正式） |
| Recipient / Team | 当前用户或指定频道 |
| Message | 见下方模板 |

### 消息体（HTML 格式）

```html
<p>📊 <strong>Offshoring 数据验证完成</strong></p>
<p>
  🕐 生成时间：@{body('Parse_JSON')?['report_model']?['generated_at']}<br>
  📁 源文件：@{body('Parse_JSON')?['report_model']?['source_file']}<br>
  📋 总行数：@{body('Parse_JSON')?['report_model']?['total_rows']}
</p>
<p>
  ✅ 验证结果：<br>
  &nbsp;&nbsp;🔴 Error：<strong>@{body('Parse_JSON')?['report_model']?['error_count']}</strong> 条<br>
  &nbsp;&nbsp;🟡 Warning：<strong>@{body('Parse_JSON')?['report_model']?['warning_count']}</strong> 条
</p>
<p>
  📂 报告文件（点击打开）：<br>
  &nbsp;&nbsp;📗 <a href="@{varExcelShareLink}">Excel 报告</a><br>
  &nbsp;&nbsp;📕 <a href="@{varPdfShareLink}">PDF 报告</a>
</p>
<p>
  @{if(equals(body('Parse_JSON')?['report_model']?['error_count'], 0),
     if(equals(body('Parse_JSON')?['report_model']?['warning_count'], 0),
        '✅ 数据完全通过验证，无需修正。',
        concat('⚠️ 存在 ', string(body(''Parse_JSON'')?[''report_model'']?[''warning_count'']), ' 条警告，建议核查。')),
     concat('❌ 存在 ', string(body(''Parse_JSON'')?[''report_model'']?[''error_count'']), ' 条错误，请修正数据后重新提交。'))}
</p>
```

> **注意**：Power Automate Teams Action 支持有限的 HTML；`<a href>` 链接可用。

---

## 12. 错误处理

### 全局错误处理步骤

在流程末尾（Scope 外）添加：

**Action**：`Terminate`（失败路径）  
**条件**：`Configure run after` → `has failed`

发送错误通知至 Teams：

```
❌ Offshoring 验证流程异常

时间：@{utcNow()}
错误信息：@{result('Scope_MainFlow')?[0]?['error']?['message']}

请联系管理员查看 Power Automate 运行历史。
```

### 各步骤错误配置建议

| 步骤 | 错误处理策略 |
|------|------------|
| 读取 Excel | 失败时终止并发 Teams 告警 |
| 调用 AI | 失败时重试 2 次（配置 Retry policy），仍失败则终止 |
| 创建文件夹 | 忽略"已存在"错误，继续 |
| 写入 Excel | 失败时终止并发 Teams 告警 |
| 生成 PDF | 失败时仅发 Teams 告警（可选，不阻断主流程） |
| 发送 Teams | 失败时记录日志，不终止流程 |

---

## 变量汇总

| 变量名 | 类型 | 初始值/来源 | 说明 |
|--------|------|------------|------|
| `varTimestamp` | String | `formatDateTime(utcNow(), 'yyyyMMdd_HHmm')` | 文件名时间戳 |
| `varYear` | String | `formatDateTime(utcNow(), 'yyyy')` | 年份（文件夹分组） |
| `varMonth` | String | `formatDateTime(utcNow(), 'MM')` | 月份（文件夹分组） |
| `varExcelFileUrl` | String | 触发器输入 | 输入 Excel 文件路径 |
| `varTableName` | String | `tblOffshoring` | 固定表格名称 |
| `varOutputFolder` | String | 拼接 | 输出文件夹路径 |
| `varOutputFileBaseName` | String | 拼接 | 输出文件基础名称 |
| `varExcelOutputPath` | String | 拼接 | Excel 报告完整路径 |
| `varPdfOutputPath` | String | 拼接 | PDF 报告完整路径 |
| `varTableDataJson` | String | Compose 输出 | 序列化的表格数据 |
| `varAiRawResponse` | String | AI 调用输出 | AI 原始响应文本 |
| `varExcelShareLink` | String | OneDrive 分享链接 | Excel 文件分享 URL |
| `varPdfShareLink` | String | OneDrive 分享链接 | PDF 文件分享 URL |
