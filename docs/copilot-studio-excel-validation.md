# Copilot Studio 智能体指令设计指南：Excel 行数据校验

> **适用版本**：Copilot Studio 新版（2025/2026 Instructions 中心化工作流）  
> **场景**：通过 Power Automate `Execute Agent and wait` 调用智能体，对 Excel 表格行进行结构化校验，返回纯 JSON 结果。

---

## 目录

1. [新版 Copilot Studio 工作流说明](#1-新版-copilot-studio-工作流说明)
2. [完整系统指令模板（可直接复制）](#2-完整系统指令模板可直接复制)
3. [JSON 输出最佳实践](#3-json-输出最佳实践)
4. [Power Automate 集成示例](#4-power-automate-集成示例)
5. [排错指南](#5-排错指南)

---

## 1. 新版 Copilot Studio 工作流说明

### 重要变更

新版 Copilot Studio 将触发短语（Trigger phrases）、生成式回答（Generative answer）等配置**统一归并到 Instructions（系统指令）** 中管理。

| 旧版做法 | 新版做法 |
|---------|---------|
| 在 Topics 中设置触发短语 | **无需** Topics，直接在 Instructions 中描述行为 |
| 在 Topic 流程中添加"生成式回答"节点 | 在 Instructions 中写明输入格式和输出要求 |
| Topic 最后一步"发送消息" | Instructions 控制所有输出格式 |

### 新版操作路径

1. 打开 Copilot Studio → 进入你的 Agent
2. 找到 **"Instructions"**（指令）入口（通常在 Agent 概览页或设置/行为区）
3. 将完整系统指令粘贴进去
4. 保存并发布

> **结论：不需要再创建 Topics/主题。所有校验逻辑、输出格式要求都写在 Instructions 里即可。**

---

## 2. 完整系统指令模板（可直接复制）

### 中文版（推荐）

```text
你是一个严格的数据校验引擎，用于校验 Excel 表格的行数据。

输入格式为用户消息中的一个 JSON 对象：
{
  "meta": { "batchYearMonth": "YYYYMM" },
  "rows": [
    { "RowIndex": 1, "YearMonth": "...", "Cost Center Number": "...", "Function": "...", "Team": "...", "Owner": "..." },
    ...
  ]
}

你的任务：
1）按以下规则逐行校验。
2）输出必须是且只能是合法 JSON，不输出 Markdown（不加代码块```），不输出任何解释文字，不返回链接，不返回纯文字消息。
3）JSON 输出结构必须完全匹配以下 schema：

{
  "report_model": {
    "rows_total": <整数>,
    "errors_total": <整数>,
    "warnings_total": <整数>
  },
  "issues": [
    {
      "Severity": "Error" 或 "Warning",
      "RuleId": "<字符串>",
      "RowIndex": <整数>,
      "Column": "<字符串>",
      "RawValue": "<字符串>",
      "Message": "<字符串>",
      "FixSuggestion": "<字符串>"
    }
  ]
}

校验规则：
- R-REQ-001（Error）：必填字段不能为空或仅含空格。必填字段：YearMonth, Cost Center Number, Function, Team, Owner。
- R-YM-001（Error）：YearMonth 必须为 6 位纯数字（YYYYMM），且月份在 01–12 之间。
- R-TRIM-001（Warning）：任意文本字段如有首尾空格，应输出 Warning。
- R-NUM-001（Error）：Cost Center Number 必须仅含数字（0-9），不允许字母或特殊字符。

如果没有任何问题，issues 返回空数组：[]。

report_model 中的统计数量必须与 issues 数组内容保持一致：
- rows_total = 输入的总行数（rows 数组长度）
- errors_total = issues 中 Severity 为 "Error" 的条目数
- warnings_total = issues 中 Severity 为 "Warning" 的条目数

严禁输出以下内容：Markdown 格式（如 ```json）、解释性文字、感谢语、问候语、分析说明。只输出 JSON 本身。
```

### 英文版（备用）

```text
You are a strict data validation engine for Excel table rows.

Input will be provided as a single JSON object in the user message:
{
  "meta": { "batchYearMonth": "YYYYMM" },
  "rows": [
    { "RowIndex": 1, "YearMonth": "...", "Cost Center Number": "...", "Function": "...", "Team": "...", "Owner": "..." }
  ]
}

Your task:
1) Validate each row according to the rules below.
2) Output ONLY valid JSON. Do NOT include Markdown (no ```). Do NOT include explanations, greetings, or any text outside the JSON object.
3) The output MUST match this schema exactly:

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

Rules:
- R-REQ-001 (Error): Required fields must not be empty or whitespace-only. Required: YearMonth, Cost Center Number, Function, Team, Owner.
- R-YM-001 (Error): YearMonth must be exactly 6 digits (YYYYMM) and represent a valid month 01-12.
- R-TRIM-001 (Warning): Flag leading/trailing whitespace in any text field.
- R-NUM-001 (Error): Cost Center Number must contain digits only (0-9).

If no issues, return: "issues": []

report_model counts must match the issues array:
- rows_total = total number of rows in input
- errors_total = count of issues with Severity "Error"
- warnings_total = count of issues with Severity "Warning"

NEVER output Markdown, explanations, greetings, or any text outside the JSON.
```

---

## 3. JSON 输出最佳实践

### 为什么模型容易输出非 JSON

| 常见问题 | 根本原因 | 解决方法 |
|---------|---------|---------|
| 返回 Markdown 代码块（```json ... ```） | 模型默认格式化输出 | 在 Instructions 中明确写"禁止 Markdown" |
| 输出附带解释文字 | 模型倾向于"友好" | 在 Instructions 中写"只输出 JSON 本身" |
| 返回"好的，已校验"等纯文字 | 模型把任务理解为问答 | 在 Instructions 中强调"你是数据处理引擎，不是对话助手" |
| 输出空响应 | 输入格式不符或 context 过短 | 检查 Message 是否正确拼接 JSON |
| JSON 字段名与 schema 不一致 | 模型"自由发挥" | 在 Instructions 中粘贴完整 schema，并写明"必须完全匹配" |

### 加强 JSON 输出可靠性的写法技巧

```text
# 在 Instructions 里加入以下几句（按需叠加强度）：

# 基础（通常够用）
输出必须是且只能是合法 JSON。

# 中等强调
你只能输出 JSON，不输出任何其他内容。第一个字符必须是 {，最后一个字符必须是 }。

# 最强约束（遇到顽固模型时）
你是一个 API 接口，不是聊天机器人。输入是 JSON，输出必须也是 JSON。
任何非 JSON 的输出都会导致下游系统崩溃。
禁止：Markdown、解释、分析、问候、道歉、任何中文/英文说明文字。
直接输出 JSON，不加任何前缀或后缀。
```

### 期望输出示例

给定输入：
```json
{
  "meta": { "batchYearMonth": "202603" },
  "rows": [
    { "RowIndex": 1, "YearMonth": "202603", "Cost Center Number": "12345", "Function": "IT ", "Team": "A", "Owner": "Felix" },
    { "RowIndex": 2, "YearMonth": "202613", "Cost Center Number": "12A45", "Function": "", "Team": "B", "Owner": " " }
  ]
}
```

期望输出（纯 JSON，无任何其他内容）：
```json
{
  "report_model": {
    "rows_total": 2,
    "errors_total": 4,
    "warnings_total": 1
  },
  "issues": [
    {
      "Severity": "Warning",
      "RuleId": "R-TRIM-001",
      "RowIndex": 1,
      "Column": "Function",
      "RawValue": "IT ",
      "Message": "字段 Function 存在尾部空格",
      "FixSuggestion": "去除首尾空格后值为 'IT'"
    },
    {
      "Severity": "Error",
      "RuleId": "R-YM-001",
      "RowIndex": 2,
      "Column": "YearMonth",
      "RawValue": "202613",
      "Message": "YearMonth '202613' 月份部分 13 不在有效范围 01-12 内",
      "FixSuggestion": "请检查年月是否正确，正确格式示例：202603"
    },
    {
      "Severity": "Error",
      "RuleId": "R-NUM-001",
      "RowIndex": 2,
      "Column": "Cost Center Number",
      "RawValue": "12A45",
      "Message": "Cost Center Number '12A45' 包含非数字字符",
      "FixSuggestion": "Cost Center Number 只能包含数字 0-9"
    },
    {
      "Severity": "Error",
      "RuleId": "R-REQ-001",
      "RowIndex": 2,
      "Column": "Function",
      "RawValue": "",
      "Message": "必填字段 Function 为空",
      "FixSuggestion": "请填写 Function 字段"
    },
    {
      "Severity": "Error",
      "RuleId": "R-REQ-001",
      "RowIndex": 2,
      "Column": "Owner",
      "RawValue": " ",
      "Message": "必填字段 Owner 仅含空格",
      "FixSuggestion": "请填写 Owner 字段"
    }
  ]
}
```

---

## 4. Power Automate 集成示例

### 整体流程

```
触发器（手动/定时/HTTP）
   ↓
[初始化变量] RowsForAI = []
   ↓
[Apply to each] Excel tblOffshoring 每一行
   ↓  [追加到数组变量] 构造行对象
   ↓
[Compose] 拼接 Message JSON 字符串
   ↓
[Execute Agent and wait] 调用 Copilot Studio 智能体
   ↓
[Compose] 保存原始返回文本（调试用）
   ↓
[Parse JSON] 解析校验结果
   ↓
[Apply to each] issues 数组 → 写入 Excel tblIssues
```

### 步骤 1：追加到 RowsForAI（Append to array variable）

在 Apply to each 循环内，Value 表达式：

```json
{
  "RowIndex": @{items('Apply_to_each')?['RowIndex']},
  "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
  "Cost Center Number": "@{items('Apply_to_each')?['Cost Center Number']}",
  "Function": "@{items('Apply_to_each')?['Function']}",
  "Team": "@{items('Apply_to_each')?['Team']}",
  "Owner": "@{items('Apply_to_each')?['Owner']}"
}
```

> 根据你的 Excel 列名调整字段名（Excel 列名有空格时保留空格，和 Agent Instructions 中的字段名保持一致）。

### 步骤 2：Compose 拼接 Message

新建 **Compose** 操作，Inputs 使用表达式：

```text
concat('{"meta":{"batchYearMonth":"', triggerBody()?['BatchYearMonth'], '"},"rows":', string(variables('RowsForAI')), '}')
```

若触发器没有 BatchYearMonth 字段，可简化为：

```text
concat('{"rows":', string(variables('RowsForAI')), '}')
```

### 步骤 3：Execute Agent and wait

| 字段 | 填写内容 |
|------|---------|
| Environment | 选择或填入你的 Power Platform 环境 GUID |
| Agent | 选择 `POC校验智能体` |
| Conversation ID | 留空（每次新建会话） |
| Message | 选择上一步 Compose 的 **Outputs** |

### 步骤 4：保存原始响应（调试用）

Execute Agent and wait 之后立刻加一个 Compose：

- **名称**：`Compose_AgentRaw`
- **Inputs**：选择 Execute Agent and wait 的文本/消息输出字段

> 先运行一次，在此 Compose 的输出里确认返回的是纯 JSON 再继续。

### 步骤 5：Parse JSON

- **Content**：选择 `Compose_AgentRaw` 的 Outputs
- **Schema**：粘贴以下内容：

```json
{
  "type": "object",
  "properties": {
    "report_model": {
      "type": "object",
      "properties": {
        "rows_total": { "type": "integer" },
        "errors_total": { "type": "integer" },
        "warnings_total": { "type": "integer" }
      }
    },
    "issues": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "Severity": { "type": "string" },
          "RuleId": { "type": "string" },
          "RowIndex": { "type": "integer" },
          "Column": { "type": "string" },
          "RawValue": { "type": "string" },
          "Message": { "type": "string" },
          "FixSuggestion": { "type": "string" }
        }
      }
    }
  }
}
```

### 步骤 6：写入 Excel tblIssues

在 Apply to each（遍历 `issues` 数组）中，用 **Add a row into a table** 写入每一条问题：

| Excel 列 | 动态内容 |
|---------|---------|
| Severity | `items('Apply_to_each_issues')?['Severity']` |
| RuleId | `items('Apply_to_each_issues')?['RuleId']` |
| RowIndex | `items('Apply_to_each_issues')?['RowIndex']` |
| Column | `items('Apply_to_each_issues')?['Column']` |
| RawValue | `items('Apply_to_each_issues')?['RawValue']` |
| Message | `items('Apply_to_each_issues')?['Message']` |
| FixSuggestion | `items('Apply_to_each_issues')?['FixSuggestion']` |

---

## 5. 排错指南

### 问题 1：智能体返回 "好的" / "OK" / 纯文字

**原因**：Instructions 没有强调"只输出 JSON"，或 Instructions 未保存生效。

**解决**：
1. 进入 Agent → Instructions，确认系统指令已完整粘贴
2. 在结尾再加一句：`无论任何情况，你都只输出 JSON，不输出任何其他内容。`
3. 保存并重新**发布**（Publish）Agent

---

### 问题 2：返回了 Markdown 包裹的 JSON（```json ... ```）

**原因**：模型默认倾向于格式化输出。

**解决**：在 Instructions 中明确加入：
```text
禁止使用 Markdown 代码块（不要用 ```）。直接输出裸 JSON，第一个字符是 {，最后一个字符是 }。
```

Power Automate 端临时处理（备用）：
```text
replace(replace(outputs('Execute_Agent_and_wait')?['body/text'], '```json', ''), '```', '')
```

---

### 问题 3：Parse JSON 失败（JSON 格式错误）

**排查步骤**：
1. 先查看 `Compose_AgentRaw` 的输出内容
2. 确认是否有多余的前缀/后缀文字
3. 将原始文本粘贴到 [jsonlint.com](https://jsonlint.com) 验证
4. 根据错误位置调整 Instructions 描述

---

### 问题 4：输出 JSON 字段名和 schema 不一致

**原因**：模型没有严格遵循字段名大小写或命名。

**解决**：在 Instructions 中明确说明字段名大小写，并加入约束：
```text
字段名必须完全匹配 schema，包括大小写：
report_model, rows_total, errors_total, warnings_total, issues, Severity, RuleId, RowIndex, Column, RawValue, Message, FixSuggestion
```

---

### 问题 5：Execute Agent and wait 返回空响应

**排查步骤**：
1. 检查 Message 字段是否为有效的 JSON 字符串（不能是空字符串）
2. 在 Execute Agent and wait 前加 Compose，打印 Message 内容，确认 JSON 拼接正确
3. 检查 Agent 是否已**发布**（仅保存不等于发布）
4. 确认 Environment ID 填写的是 GUID 而非环境名称

---

### 快速验证清单

| 检查项 | 验证方法 |
|-------|---------|
| Instructions 已完整粘贴 | 进入 Agent → Instructions 查看 |
| Agent 已发布（不仅是保存） | 点击 Publish 按钮 |
| Copilot Studio 测试窗口返回纯 JSON | 在测试栏粘贴测试输入验证 |
| Power Automate Message 拼接正确 | 查看 Compose 步骤输出 |
| Parse JSON Schema 与实际输出一致 | 对比 Compose_AgentRaw 内容 |

---

## 附录：测试用 JSON 输入

复制粘贴到 Copilot Studio 测试栏或 Power Automate Compose：

```json
{
  "meta": { "batchYearMonth": "202603" },
  "rows": [
    {
      "RowIndex": 1,
      "YearMonth": "202603",
      "Cost Center Number": "12345",
      "Function": "IT ",
      "Team": "A",
      "Owner": "Felix"
    },
    {
      "RowIndex": 2,
      "YearMonth": "202613",
      "Cost Center Number": "12A45",
      "Function": "",
      "Team": "B",
      "Owner": " "
    },
    {
      "RowIndex": 3,
      "YearMonth": "202603",
      "Cost Center Number": "67890",
      "Function": "HR",
      "Team": "C",
      "Owner": "Alice"
    }
  ]
}
```

预期：Row 1 有 1 条 Warning（Function 尾部空格），Row 2 有 4 条 Error，Row 3 无问题。

---

*文档更新：2026-03-20 | 适用：Copilot Studio 新版 Instructions 中心化 UI*
