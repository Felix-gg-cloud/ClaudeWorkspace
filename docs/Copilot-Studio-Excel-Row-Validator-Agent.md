# Copilot Studio Excel 行校验 Agent — 从零创建完整指南

> **目标**：在 Copilot Studio 新建一个专用 Agent，由 Power Automate 通过 **Execute Agent and wait** 动作调用，传入 Excel 行数据后，Agent 返回**纯 JSON** 校验报告（`report_model` + `issues` 数组），Power Automate 再解析并写入输出 Excel / PDF。

---

## 目录

1. [在 Copilot Studio 创建新 Agent](#1-在-copilot-studio-创建新-agent)
2. [配置知识库与动作（Knowledge / Actions）](#2-配置知识库与动作knowledge--actions)
3. [创建 Topic / 触发器（Power Automate 专用入口）](#3-创建-topic--触发器power-automate-专用入口)
4. [定义输入载荷 Schema](#4-定义输入载荷-schema)
5. [强制 Agent 只返回有效 JSON](#5-强制-agent-只返回有效-json)
6. [在 Power Automate 中用 Execute Agent and wait 调用](#6-在-power-automate-中用-execute-agent-and-wait-调用)
7. [样例：JSON 请求与响应](#7-样例json-请求与响应)
8. [常见问题排障](#8-常见问题排障)

---

## 1. 在 Copilot Studio 创建新 Agent

### 1.1 进入 Copilot Studio

1. 打开浏览器，导航到：  
   <https://copilotstudio.microsoft.com/>
2. 右上角确认当前环境（例如 **ResonaPOC**）。  
   > ⚠️ 环境一定要选对，后续 Power Automate 需要填写该环境的 **Environment ID（GUID）**。

### 1.2 新建 Agent

1. 左侧菜单点 **Agents（智能体）** → 右上角点 **+ New agent**。
2. 选择 **Skip to configure**（跳过向导，手动配置）。
3. 填写基本信息：

   | 字段 | 建议值 |
   |---|---|
   | Name | `ExcelRowValidator` |
   | Description | `Validates Excel rows for data quality issues and returns a structured JSON report.` |
   | Language | 按需选择（如 English / 中文） |

4. 点 **Create**。

---

## 2. 配置知识库与动作（Knowledge / Actions）

> 本 Agent 是**规则引擎型**，不需要外部知识库；它依靠系统指令（System Instructions）内置校验逻辑。若将来需要连接自定义 API，可在此添加 Action。

### 2.1 系统指令（System Instructions）

在 Agent 详情页，找到 **Instructions** 或 **System Prompt** 区域，**清空默认内容**，粘贴以下内容（可直接复制）：

```
You are an Excel data quality validator. When invoked, you receive a JSON payload containing an array of rows from an Excel file. Each row has a "RowIndex" (integer) and a "columns" object where keys are column names and values are cell values.

Your job is to validate each row against the following rules:

1. [R-WS-ALL-001] Trailing/leading whitespace: any column value must not have leading or trailing spaces.
2. [R-NULL-REQ-002] Required columns must not be empty or null. Required columns: Function, CostCenter, EmployeeID, StartDate.
3. [R-DATE-FMT-003] Date columns (StartDate, EndDate) must match the format YYYY-MM-DD.
4. [R-NUM-FMT-004] Numeric columns (Amount, Headcount) must be valid numbers (no text, no special characters).
5. [R-ENUM-005] The "Status" column, if present, must be one of: Active, Inactive, Pending.

After validation, return ONLY a valid JSON object with exactly this structure. Do not add any explanation, Markdown formatting, or extra text outside the JSON:

{
  "report_model": {
    "rows_total": <integer>,
    "errors_total": <integer>,
    "warnings_total": <integer>
  },
  "issues": [
    {
      "Severity": "Error" | "Warning",
      "RuleId": "<string>",
      "RowIndex": <integer>,
      "Column": "<string>",
      "RawValue": "<string>",
      "Message": "<string>",
      "FixSuggestion": "<string>"
    }
  ]
}

If there are no issues, return an empty issues array. Always output pure JSON. Never wrap the JSON in Markdown code fences.
```

### 2.2 开发者说明（Developer Instructions / Notes）

在 Agent 的 **Notes for developers** 或备注区域（如有），粘贴以下内容：

```
INPUT CONTRACT:
  The caller (Power Automate) sends a plain-text message with the following JSON structure:
  {
    "meta": { "batchYearMonth": "YYYY-MM", "generatedAt": "<ISO8601 datetime>" },
    "rows": [
      { "RowIndex": 1, "columns": { "Function": "Finance", "CostCenter": "CC001", ... } },
      ...
    ]
  }

OUTPUT CONTRACT:
  The agent MUST reply with exactly one message containing a valid JSON object:
  {
    "report_model": { "rows_total": N, "errors_total": N, "warnings_total": N },
    "issues": [ { "Severity": "...", "RuleId": "...", "RowIndex": N, "Column": "...", "RawValue": "...", "Message": "...", "FixSuggestion": "..." } ]
  }
  No other text, no Markdown, no links.

IMPORTANT:
  - If the AI model adds explanation text, the downstream Parse JSON step in Power Automate will fail.
  - Always test by calling Execute Agent and wait and checking that the raw output starts with '{' and ends with '}'.
```

### 2.3 关闭不需要的功能

在 Agent 设置里，建议关闭以下选项（避免 Agent 产生非 JSON 回复）：

- **Web search / Bing** → 关闭
- **Generative answers from knowledge base** → 关闭（除非你手动添加了知识库）
- **Fallback topic** → 保留，但将回复改成 JSON 格式（见第 3 节）

---

## 3. 创建 Topic / 触发器（Power Automate 专用入口）

### 3.1 新建 Topic

1. 在 Agent 页左侧点 **Topics** → **+ Add topic** → **From blank**。
2. 命名：`ValidateExcelRows`
3. 描述：`Triggered by Power Automate to validate Excel row data and return JSON report.`

### 3.2 配置触发器（Trigger）

在 Topic 编辑器里：

1. 点 **Trigger（触发器）** 节点。
2. 触发类型选 **When a message is received**（或等效选项）。
3. 在 **Phrases（示例短语）** 里添加以下触发短语（全部复制粘贴）：

```
{"rows":
validate rows
{"meta":
run validation
excel row validation
```

> 这些短语用于让 Agent 识别来自 Power Automate 的 JSON 消息。

### 3.3 添加 Generative AI 节点（核心校验节点）

在 Trigger 节点后面，添加一个 **Generative answers** 或 **Send a message** 节点：

#### 方法 A：使用 Generative AI 节点（推荐）

1. 点 **+** → **Ask with generative AI**（或 **Generative AI node**）
2. 在 **Input** 里选择 `Activity.Text`（即 Power Automate 传入的 Message 内容）
3. 在 **Instructions** 里填写：

```
You are an Excel row validator. The user has sent a JSON payload. Parse the "rows" array and validate each row. Return ONLY a pure JSON object with "report_model" and "issues" array. No Markdown. No extra text.
```

4. 输出变量命名为 `ValidationResult`（String 类型）

#### 方法 B：直接用 Send a message 节点（简化版）

如果找不到 Generative AI 节点：

1. 点 **+** → **Send a message**
2. 消息内容用变量引用：`{ValidationResult}`
3. 这需要你在前一个节点（例如 Power Automate Custom Connector）生成好 `ValidationResult`

### 3.4 最终回复节点

在 Topic 末尾，确保最后一个节点是 **Message（发送消息）**，内容是：

```
{ValidationResult}
```

> 这样 Execute Agent and wait 在 Power Automate 里收到的文本就是纯 JSON 字符串。

---

## 4. 定义输入载荷 Schema

### 4.1 Power Automate 发送给 Agent 的 JSON 格式

下面是 Power Automate 应发送给 Agent 的完整 JSON 格式（在 **Execute Agent and wait** 的 **Message** 字段里传入）：

```json
{
  "meta": {
    "batchYearMonth": "2025-06",
    "source": "SharePoint/Documents/POC-Validator/Inputs",
    "generatedAt": "2025-06-01T08:00:00Z"
  },
  "rows": [
    {
      "RowIndex": 1,
      "columns": {
        "Function": "Finance",
        "CostCenter": "CC001",
        "EmployeeID": "E12345",
        "StartDate": "2025-01-15",
        "EndDate": "2025-12-31",
        "Amount": "5000",
        "Headcount": "3",
        "Status": "Active"
      }
    },
    {
      "RowIndex": 2,
      "columns": {
        "Function": "IT ",
        "CostCenter": "",
        "EmployeeID": "E99999",
        "StartDate": "2025/06/01",
        "EndDate": "",
        "Amount": "N/A",
        "Headcount": "1",
        "Status": "Unknown"
      }
    }
  ]
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|---|---|---|
| `meta.batchYearMonth` | String | 批次年月，格式 YYYY-MM |
| `meta.source` | String | 来源路径（可选） |
| `meta.generatedAt` | String | UTC 时间戳（ISO 8601） |
| `rows` | Array | 行数组，每个元素代表 Excel 的一行 |
| `rows[].RowIndex` | Integer | Excel 行号（从 1 开始，对应 tblData 里的顺序） |
| `rows[].columns` | Object | 键值对：列名 → 单元格原始值（均为字符串） |

### 4.2 在 Power Automate 里生成这个 Message

在 **Execute Agent and wait** 之前加一个 **Compose** 步骤（命名 `Compose_AgentMessage`）：

**Compose 的 Inputs（Expression 模式）填写：**

```
concat(
  '{"meta":{"batchYearMonth":"',
  triggerBody()?['BatchYearMonth'],
  '","source":"SharePoint/Documents/POC-Validator/Inputs","generatedAt":"',
  utcNow(),
  '"},"rows":',
  string(variables('RowsForAI')),
  '}'
)
```

然后在 **Execute Agent and wait** 的 **Message** 字段里选择 `Outputs of Compose_AgentMessage`。

---

## 5. 强制 Agent 只返回有效 JSON

### 5.1 关键系统指令（必须包含）

在第 2.1 节的 System Instructions 末尾，确保包含以下语句（已含在上方模板里）：

```
Return ONLY a valid JSON object. Do not add any explanation, Markdown formatting, or extra text outside the JSON. Never wrap the JSON in Markdown code fences (```). Your entire response must be a single valid JSON object starting with { and ending with }.
```

### 5.2 测试 Agent 的样例 Prompt（用于手动测试）

在 Copilot Studio 的 **Test your agent** 面板里，复制以下内容发送：

```json
{"meta":{"batchYearMonth":"2025-06","generatedAt":"2025-06-01T08:00:00Z"},"rows":[{"RowIndex":1,"columns":{"Function":"Finance","CostCenter":"CC001","EmployeeID":"E12345","StartDate":"2025-01-15","Status":"Active"}},{"RowIndex":2,"columns":{"Function":"IT ","CostCenter":"","EmployeeID":"E99999","StartDate":"2025/06/01","Status":"Unknown"}}]}
```

**预期返回（纯 JSON，无其他文字）：**

```json
{
  "report_model": {
    "rows_total": 2,
    "errors_total": 4,
    "warnings_total": 0
  },
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "R-WS-ALL-001",
      "RowIndex": 2,
      "Column": "Function",
      "RawValue": "IT ",
      "Message": "Trailing whitespace detected",
      "FixSuggestion": "Trim leading/trailing whitespace from the value"
    },
    {
      "Severity": "Error",
      "RuleId": "R-NULL-REQ-002",
      "RowIndex": 2,
      "Column": "CostCenter",
      "RawValue": "",
      "Message": "Required column is empty",
      "FixSuggestion": "Provide a valid CostCenter value"
    },
    {
      "Severity": "Error",
      "RuleId": "R-DATE-FMT-003",
      "RowIndex": 2,
      "Column": "StartDate",
      "RawValue": "2025/06/01",
      "Message": "Date format must be YYYY-MM-DD",
      "FixSuggestion": "Change to 2025-06-01"
    },
    {
      "Severity": "Error",
      "RuleId": "R-ENUM-005",
      "RowIndex": 2,
      "Column": "Status",
      "RawValue": "Unknown",
      "Message": "Status must be one of: Active, Inactive, Pending",
      "FixSuggestion": "Replace with a valid status value"
    }
  ]
}
```

---

## 6. 在 Power Automate 中用 Execute Agent and wait 调用

### 6.1 为什么选 Execute Agent and wait

| 动作名 | 适合场景 |
|---|---|
| **Execute Agent and wait** | ✅ 同步调用，立刻拿回结果（本方案使用） |
| Execute Agent | 异步触发，不等待返回（适合通知类场景） |
| Evaluate Agent | 测试评估用，非生产调用 |
| Get Agent Test Runs 等 | 查询测试记录，非生产调用 |

### 6.2 获取 Environment ID（必填项）

**Environment ID 不是环境名称**（如 `ResonaPOC`），而是一个 **GUID**，格式如：  
`3f2f2b1a-1234-5678-9abc-def012345678`

**获取方式：**

1. 打开：<https://admin.powerplatform.microsoft.com/>
2. 左侧点 **Environments**
3. 找到你的环境（如 ResonaPOC），点击进入详情
4. 复制 **Environment ID** 字段的值（一串 GUID）
5. 粘贴到 Power Automate 的 `Execute Agent and wait` → **Environment ID** 字段

> ⚠️ 如果填入环境名称（如 `ResonaPOC`），会导致 DNS 解析错误：  
> `The remote name could not be resolved: 'resonap.oc.environment.api.powerplatform.com'`

### 6.3 配置 Execute Agent and wait 的各字段

| 字段 | 填写内容 |
|---|---|
| **Environment ID** | Power Platform 环境的 GUID（见 6.2） |
| **Agent** | 选择 `ExcelRowValidator`（下拉选择） |
| **Message** | 选择 `Outputs of Compose_AgentMessage`（见 4.2） |
| **Conversation ID** | 留空（单次无状态调用） |
| **Locale** (Advanced) | 留空或填 `en-US` |
| **Attachments** (Advanced) | 留空 |

### 6.4 解析返回的 JSON（Parse JSON）

在 **Execute Agent and wait** 后面加：

**动作：Parse JSON**

- **Content**：选择 Execute Agent and wait 的输出字段（通常是 `body/text` 或 `Message`）  
  > 若不确定，先加一个 **Compose**，把 Execute Agent and wait 的所有输出打印出来，再从中找包含 `{...}` 的那个字段。

- **Schema**：点 **Generate from sample**，粘贴以下样例：

```json
{
  "report_model": {
    "rows_total": 2,
    "errors_total": 1,
    "warnings_total": 0
  },
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "R-WS-ALL-001",
      "RowIndex": 1,
      "Column": "Function",
      "RawValue": "IT ",
      "Message": "Trailing whitespace detected",
      "FixSuggestion": "Trim whitespace"
    }
  ]
}
```

点 **Done**，系统自动生成 Schema。

### 6.5 写入 tblIssues（Apply to each）

Parse JSON 之后：

1. **Apply to each**：选择 `issues`（来自 Parse JSON 输出）
2. 循环里加：**Excel Online (Business) → Add a row into a table**
   - Location：SharePoint site
   - Document Library：Documents
   - File：输出报告 Excel 的 Identifier（来自 Create file 步骤）
   - Table：`tblIssues`
   - 列映射：

   | Excel 列名 | 动态内容 |
   |---|---|
   | Severity | `Severity` |
   | RuleId | `RuleId` |
   | RowIndex | `RowIndex` |
   | Column | `Column` |
   | RawValue | `RawValue` |
   | Message | `Message` |
   | FixSuggestion | `FixSuggestion` |

---

## 7. 样例：JSON 请求与响应

### 7.1 请求（Power Automate → Agent Message）

```json
{
  "meta": {
    "batchYearMonth": "2025-06",
    "source": "SharePoint/Documents/POC-Validator/Inputs",
    "generatedAt": "2025-06-01T08:00:00Z"
  },
  "rows": [
    {
      "RowIndex": 1,
      "columns": {
        "Function": "Finance",
        "CostCenter": "CC001",
        "EmployeeID": "E12345",
        "StartDate": "2025-01-15",
        "EndDate": "2025-12-31",
        "Amount": "5000",
        "Headcount": "3",
        "Status": "Active"
      }
    },
    {
      "RowIndex": 2,
      "columns": {
        "Function": "IT ",
        "CostCenter": "",
        "EmployeeID": "E99999",
        "StartDate": "2025/06/01",
        "EndDate": "",
        "Amount": "N/A",
        "Headcount": "1",
        "Status": "Unknown"
      }
    }
  ]
}
```

### 7.2 响应（Agent → Power Automate）

```json
{
  "report_model": {
    "rows_total": 2,
    "errors_total": 5,
    "warnings_total": 0
  },
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "R-WS-ALL-001",
      "RowIndex": 2,
      "Column": "Function",
      "RawValue": "IT ",
      "Message": "Trailing whitespace detected",
      "FixSuggestion": "Trim leading/trailing whitespace from the value"
    },
    {
      "Severity": "Error",
      "RuleId": "R-NULL-REQ-002",
      "RowIndex": 2,
      "Column": "CostCenter",
      "RawValue": "",
      "Message": "Required column CostCenter is empty",
      "FixSuggestion": "Provide a valid CostCenter value"
    },
    {
      "Severity": "Error",
      "RuleId": "R-DATE-FMT-003",
      "RowIndex": 2,
      "Column": "StartDate",
      "RawValue": "2025/06/01",
      "Message": "Date format must be YYYY-MM-DD",
      "FixSuggestion": "Change to 2025-06-01"
    },
    {
      "Severity": "Error",
      "RuleId": "R-NUM-FMT-004",
      "RowIndex": 2,
      "Column": "Amount",
      "RawValue": "N/A",
      "Message": "Amount must be a valid number",
      "FixSuggestion": "Replace N/A with a numeric value or leave empty"
    },
    {
      "Severity": "Error",
      "RuleId": "R-ENUM-005",
      "RowIndex": 2,
      "Column": "Status",
      "RawValue": "Unknown",
      "Message": "Status must be one of: Active, Inactive, Pending",
      "FixSuggestion": "Replace with a valid status value"
    }
  ]
}
```

---

## 8. 常见问题排障

### ❌ 问题 1：Agent 只返回 `{"statusCode": "OK"}`，没有校验结果

**原因：** Topic 末尾没有正确配置"返回消息"节点，或 Generative AI 节点输出没有被引用到最终回复。

**解决方案：**

1. 打开 Copilot Studio → 找到 `ValidateExcelRows` Topic
2. 确认 Topic 最后一个节点是 **Send a message**
3. 该节点内容必须是 `{ValidationResult}`（或你存校验结果的变量）
4. 重新发布 Agent（点 **Publish**）
5. 等待 2–5 分钟后重新测试

---

### ❌ 问题 2：Agent 返回的是一个链接，打开是空白网页

**原因：** Agent 把结果放在了 attachments/transcript 链接里，而不是直接在 Message 文本里返回。

**解决方案：**

1. 在 System Instructions 里明确加上：  
   `Return the result directly as message text. Do not use attachments or links.`
2. 在 Topic 里确保最终回复节点是 **Send a message**（文本节点），不是附件节点
3. 关闭 Agent 设置里的 **Adaptive Cards** 或 **Rich Content** 相关选项（若有）

---

### ❌ 问题 3：Agent 返回了 JSON，但包裹在 Markdown 代码块里（如 ` ```json ... ``` `）

**原因：** LLM 默认用 Markdown 格式化输出，导致 Parse JSON 失败（内容不以 `{` 开头）。

**解决方案：**

在 System Instructions 最后加上（已含在第 2.1 模板里）：

```
Never wrap your response in Markdown code fences (```). Your entire response must start with { and end with }. Output only raw JSON text.
```

在 Power Automate 里也可以加一个 **Compose** 来清洗（备用方案）：

```
trim(replace(replace(outputs('Execute_Agent_and_wait')?['body/text'], '```json', ''), '```', ''))
```

---

### ❌ 问题 4：调用 Agent 报错 502 / DNS 无法解析

**错误信息：**
```
Action 'Execute_Agent_and_wait' failed: The request failed. Error code: '502'. 
Error Message: 'The remote name could not be resolved: 'xxxxx.oc.environment.api.powerplatform.com''
```

**原因：** **Environment ID** 填写的是环境名称（如 `ResonaPOC`）而不是 GUID，导致系统拼出无效的域名。

**解决方案：**

1. 获取正确的 Environment ID（见 [6.2 节](#62-获取-environment-id必填项)）
2. 在 `Execute Agent and wait` 里，把 **Environment ID** 字段的值替换为 GUID  
   格式示例：`3f2f2b1a-1234-5678-9abc-def012345678`
3. 保存并重新运行

---

### ❌ 问题 5：Parse JSON 报 Schema 不匹配

**错误信息：**
```
InvalidTemplate. Unable to process template language expressions in action 'Parse_JSON': 
The template language function 'json' parameter is not valid.
```

**原因：** Agent 返回了额外的文字（解释、Markdown 等），导致 JSON 格式无效。

**解决方案（按顺序尝试）：**

**方案 A（最推荐）：** 强化 System Instructions，确保 Agent 只输出 JSON（见第 5 节）

**方案 B：** 在 Parse JSON 之前加一个 **Compose** 做清洗：

```
json(trim(replace(replace(outputs('Execute_Agent_and_wait')?['body/text'], '```json', ''), '```', '')))
```

**方案 C：** 在 Power Automate 加条件判断，检查输出是否以 `{` 开头：

```
startsWith(trim(outputs('Execute_Agent_and_wait')?['body/text']), '{')
```

若为 `false`，记录错误日志并跳过该批次。

---

### ❌ 问题 6：Agent 测试时正常，但 Power Automate 调用时失败

**常见原因及检查项：**

| 检查项 | 解决方案 |
|---|---|
| 连接（Connection）账号不对 | 确认 Power Automate 里 Copilot Studio 连接使用的是有权限的账号 |
| Agent 没有重新发布 | 每次修改 Agent 后必须点 **Publish** |
| Message 字段是空值 | 检查 `Compose_AgentMessage` 输出是否正确（Run history 里查看） |
| Topic 没有被触发 | 检查触发短语是否包含 JSON 关键字（见第 3.2 节） |
| Agent 在不同环境 | 确认 Power Automate 和 Copilot Studio 使用同一个环境 |

---

## 附录：完整 Power Automate Flow 结构示意

```
[Trigger] Manual / Scheduled
    ↓
[Get Excel rows] → RowsForAI (Array)
    ↓
[Compose_AgentMessage] → JSON string
    ↓
[Execute Agent and wait]
    Message = Compose_AgentMessage output
    Environment ID = <GUID>
    Agent = ExcelRowValidator
    ↓
[Parse JSON]
    Content = Execute Agent and wait output text
    Schema = (generated from sample)
    ↓
[Apply to each: issues]
    → [Add a row into table: tblIssues]
    ↓
[Create report file]
    ↓
[Teams notification]
```

---

*文档版本：v1.0 | 最后更新：2025-06*
