# Copilot Studio 新手入门指南（POC Validator 专用）

> 本文面向从未使用过 Microsoft Copilot Studio 的初学者，结合本仓库 **POC Validator** 工作流，说明如何进入门户、搭建 Agent、编写提示词、与 Power Automate 集成、测试，以及输出 POC 所需的结构化 JSON。

---

## 目录

1. [什么是 Copilot Studio？](#1-什么是-copilot-studio)
2. [前置条件（许可证 & 租户权限）](#2-前置条件许可证--租户权限)
3. [如何打开 Copilot Studio 门户](#3-如何打开-copilot-studio-门户)
4. [确认你有权限进入](#4-确认你有权限进入)
5. [创建或打开一个 Agent](#5-创建或打开一个-agent)
6. [编辑 Agent 指令（System Prompt）](#6-编辑-agent-指令system-prompt)
7. [添加 Action / 与 Power Automate 集成](#7-添加-action--与-power-automate-集成)
8. [在 Copilot Studio 内测试 Agent](#8-在-copilot-studio-内测试-agent)
9. [POC 输出契约：`{report_model, issues[]}`](#9-poc-输出契约report_model-issues)
10. [常见卡点与排查](#10-常见卡点与排查)

---

## 1. 什么是 Copilot Studio？

Microsoft Copilot Studio（原名 Power Virtual Agents）是一个**低代码 AI Agent 构建平台**，允许你：

- 用**自然语言提示词（System Prompt）** 定义 Agent 的行为
- 调用 **Power Automate 流**（Flow）或外部 API 作为 Action
- 通过内置聊天窗口快速测试
- 发布到 Teams、Power Automate 等渠道

在本 POC 中，Copilot Studio 扮演**纯 AI 校验引擎**：接收原始 Excel 行数据，按规则集检查，返回结构化 JSON 报告。

---

## 2. 前置条件（许可证 & 租户权限）

| 条件 | 说明 |
|------|------|
| **Microsoft 365 账号** | 需能登录 `https://make.powerapps.com` 或 `https://copilotstudio.microsoft.com` |
| **Copilot Studio 许可证** | 单独的 Copilot Studio 订阅，或通过 Microsoft 365 E3/E5 附带；也可试用 |
| **Power Platform 环境访问权** | 需在租户内至少有一个"Maker"角色的环境（通常 IT 管理员预置） |
| **Power Automate 权限** | 如需调用 Flow，你的账号需要有对应连接器的使用权（OneDrive、Teams 等） |
| **数据丢失防护（DLP）政策** | 租户 DLP 必须允许 Copilot Studio 与目标连接器共存；如有阻断请联系 IT |

> **如何判断是否有许可证**：登录 [https://admin.microsoft.com](https://admin.microsoft.com) → 账单 → 许可证，查找 "Microsoft Copilot Studio" 或 "Power Virtual Agents"。

---

## 3. 如何打开 Copilot Studio 门户

### 方式 A：直接访问门户网址（推荐）

1. 打开浏览器，访问：

   ```
   https://copilotstudio.microsoft.com
   ```

2. 使用你的**工作或学校账号**登录（与 Microsoft 365 相同）。
3. 首次登录会要求你选择**国家/地区**，选择与你租户匹配的地区（例如 Asia Pacific）。
4. 登录成功后你会看到 **Copilot Studio 主页**，顶部有"创建"、"我的代理"等导航选项。

### 方式 B：从 Power Automate 进入

1. 打开 [https://make.powerautomate.com](https://make.powerautomate.com)
2. 左侧导航 → **更多** → **Copilot Studio**（若未显示，点"发现更多"展开）

> ⚠️ **常见问题**：若页面加载后显示"你无权访问此功能"，通常是许可证或环境角色问题，见[第 10 节](#10-常见卡点与排查)。

---

## 4. 确认你有权限进入

进入门户后，依次确认：

1. **右上角账号**：确认显示的是你的工作账号（非个人 Microsoft 账号）。
2. **环境选择器**（右上角地球图标旁边的下拉框）：确认已选择正确的 **Power Platform 环境**（与你的 Power Automate 流同一个环境）。
   - 若看不到目标环境，联系管理员授权"Environment Maker"角色。
3. **主页能正常显示**："创建代理"按钮可点击，且不报权限错误。
4. **快速验证**：点击"**创建**"，能看到"空白代理"和"使用模板"选项，说明你已有完整访问权。

---

## 5. 创建或打开一个 Agent

### 新建 Agent

1. 在 Copilot Studio 主页，点击 **"创建"**（左上角或主区域按钮）。
2. 选择 **"空白代理"**（Blank agent）。
3. 填写：
   - **名称**：例如 `POC-Validator`
   - **说明**（可选）：`Excel 数据质量校验 AI，输出结构化 JSON`
   - **语言**：选择 `English`（提示词建议用英文，AI 准确率更高）
4. 点击 **"创建"**，等待约 10-30 秒。

### 打开已有 Agent

1. 主页左侧点击 **"代理"**（Agents）。
2. 在列表里找到 `POC-Validator` 并点击进入。

---

## 6. 编辑 Agent 指令（System Prompt）

进入 Agent 后，找到**"指令"（Instructions）** 区域——这是 Agent 的核心系统提示词。

### 定位步骤

1. 在 Agent 编辑页，点击顶部标签 **"概述"（Overview）** 或左侧 **"设置"（Settings）**。
2. 找到 **"代理说明"** 或 **"指令"** 文本框（有些版本叫 "Instructions"，有些叫 "System prompt"）。
3. 点击文本框，将以下提示词**直接粘贴替换**。

### POC Validator 系统提示词（可直接粘贴）

```
You are a data quality validation engine for an Excel-based offshoring strategy report.

## Input
You receive a JSON payload with:
- meta: { source_file, generated_at, batch_id, ruleset_version }
- rows: array of objects, each with RowIndex (1-based integer) plus all Excel columns as raw string values (never trimmed by the caller)

## Task
Validate every row against the ruleset below. Return ONLY a valid JSON object — no markdown, no explanation, no code fences.

## Validation Rules

### R-WS-ALL-001 (Whitespace, Error)
For every non-empty column value: if RawValue !== RawValue.trim(), report Error.
A value is considered empty if trim(RawValue) === "".
When this rule fires for a cell, skip all other rules for that same cell.

### Required columns (Error when empty)
The following columns must not be empty (treat whitespace-only as empty):
YearMonth, Cost Center Number, Function, Team, Owner

### Numeric columns (Error when non-empty but invalid)
Columns: Onshore HC, Offshore HC, Total HC, Onshore Cost, Offshore Cost, Total Cost
Rules: must be a valid number and >= 0. Empty values are allowed.

### ShoringRatio (Warning when non-empty but invalid)
Must match pattern: digits + "%" (e.g. "25%", "100%"). Value must be 0-100.
Empty is allowed.

### Function-Team mapping (Warning)
Valid Function → Team combinations are defined in the ruleset. Flag invalid pairs.

### YearMonth format (Error)
Must match YYYYMM (6 digits, valid calendar month). Example: "202603".

## Output format (strict)
{
  "report_model": {
    "meta": { ...same as input meta... },
    "metrics": {
      "rows_total": <integer>,
      "issues_total": <integer>,
      "errors_total": <integer>,
      "warnings_total": <integer>,
      "pass_rate": <float 0-1, 2 decimal places>
    },
    "top_rules": [ { "rule_id": "...", "severity": "...", "count": <int> } ],
    "top_columns": [ { "column": "...", "count": <int> } ]
  },
  "issues": [
    {
      "Severity": "Error" | "Warning",
      "RuleId": "R-WS-ALL-001",
      "RowIndex": <integer>,
      "RowKey": "<RowIndex>|<YearMonth>|<CostCenterNumber>",
      "YearMonth": "<raw value>",
      "Cost Center Number": "<raw value>",
      "Function": "<raw value>",
      "Team": "<raw value>",
      "Owner": "<raw value>",
      "Column": "<column name>",
      "RawValue": "<exact raw value from input>",
      "Message": "<short human-readable description>",
      "FixSuggestion": "<actionable fix>"
    }
  ]
}
```

> **关键要求**：
> - 只输出 JSON，不输出任何 Markdown（不要 ` ```json ` 包裹）
> - `issues[]` 里每条必须包含 `RowIndex`（整数，1-based）
> - `RawValue` 字段必须是原始值，不做 trim

---

## 7. 添加 Action / 与 Power Automate 集成

Copilot Studio 的 Action 允许 Agent 调用外部系统（包括 Power Automate Flow）。

### 方式 A：Power Automate 调用 Agent（POC 推荐）

本 POC 中，Power Automate 是**主调方**，Copilot Studio Agent 是**被调方**。

**在 Power Automate 里调用 Agent 的步骤：**

1. 在 Power Automate 编辑 Flow，点击 **"+ 新建步骤"**
2. 搜索 **"Copilot Studio"** 或 **"Microsoft Copilot Studio"**
3. 选择动作：**"运行已发布的提示（Run a prompt）"** 或 **"Generate content with Copilot Studio"**（名称因版本而异）
4. 连接使用与 Agent 同一账号
5. 在 **"Agent"** 下拉框选择 `POC-Validator`（需先发布，见第 8 节）
6. 在 **"Message"** 字段填入要发送给 Agent 的内容，例如：

   ```
   Validate the following data rows and return structured JSON only.
   Payload: @{variables('payload_json')}
   ```

7. 响应中取 `Output text` 字段，再用 JSON 解析动作（**Parse JSON**）解析出 `report_model` 和 `issues`。

### 方式 B：在 Agent 内创建 Action（Agent 主动调用外部）

如果想让 Agent 主动调用 Power Automate：

1. 在 Agent 编辑页，点击顶部 **"动作"（Actions）** 标签
2. 点击 **"添加动作"**
3. 选择 **"Power Automate 流"**
4. 选择已存在的 Flow，或新建一个
5. 配置输入输出参数映射
6. 在指令里用自然语言描述何时触发此 Action

> 本 POC 建议使用**方式 A**（Power Automate 调用 Agent），更直接，调试也更容易。

---

## 8. 在 Copilot Studio 内测试 Agent

在正式接入 Power Automate 前，可以先在 Copilot Studio 内置聊天窗口测试。

### 测试步骤

1. 在 Agent 编辑页，右侧会有一个 **"测试代理"（Test your agent）** 面板（若没有，点击右上角的 **"测试"** 按钮展开）。
2. 在输入框里粘贴一段测试 payload：

   ```json
   Validate the following data. Return JSON only.
   {"meta":{"source_file":"test.xlsx","generated_at":"2026-03-20T05:00:00Z","batch_id":"TEST-001","ruleset_version":"poc-v1"},"rows":[{"RowIndex":1,"YearMonth":"202603","Cost Center Number":"CC001","Function":"Finance","Team":"FP&A","Owner":"Alice","Onshore HC":"10","Offshore HC":"5 ","Total HC":"15","Onshore Cost":"100000","Offshore Cost":"50000","Total Cost":"150000","ShoringRatio":"33%"}]}
   ```

3. 查看 Agent 返回的文本，确认：
   - 返回纯 JSON（不含 Markdown 包裹）
   - 结构包含 `report_model` 和 `issues`
   - `"Offshore HC": "5 "` 应被 R-WS-ALL-001 标记为 Error（末尾有空格）

4. 若返回有误，调整**指令文本**并重新测试；每次修改指令后无需重新发布，测试面板实时生效。

### 发布 Agent

测试满意后，点击右上角 **"发布"（Publish）** → 确认发布。发布后，Power Automate 才能通过连接器调用此 Agent。

> **注意**：每次修改指令后，需重新发布才对 Power Automate 生效，但测试面板无需发布。

---

## 9. POC 输出契约：`{report_model, issues[]}`

本 POC 的 Power Automate Flow 依赖 Copilot Studio 返回**精确格式**的 JSON。以下是完整的输出结构说明。

### 顶层结构

```json
{
  "report_model": { ... },
  "issues": [ ... ]
}
```

### `report_model` 结构

```json
{
  "report_model": {
    "meta": {
      "source_file": "Master Excel_Strategy_OnOffshoring.xlsx",
      "generated_at": "2026-03-20T05:00:00Z",
      "batch_id": "BATCH-202603-001",
      "ruleset_version": "poc-v1"
    },
    "metrics": {
      "rows_total": 150,
      "issues_total": 23,
      "errors_total": 18,
      "warnings_total": 5,
      "pass_rate": 0.85
    },
    "top_rules": [
      { "rule_id": "R-WS-ALL-001", "severity": "Error", "count": 12 },
      { "rule_id": "R-REQUIRED", "severity": "Error", "count": 6 }
    ],
    "top_columns": [
      { "column": "ShoringRatio", "count": 8 },
      { "column": "Offshore HC", "count": 5 }
    ]
  }
}
```

### `issues[]` 单条结构

```json
{
  "Severity": "Error",
  "RuleId": "R-WS-ALL-001",
  "RowIndex": 42,
  "RowKey": "42|202603|CC001",
  "YearMonth": "202603",
  "Cost Center Number": "CC001",
  "Function": "Finance",
  "Team": "FP&A",
  "Owner": "Alice",
  "Column": "Offshore HC",
  "RawValue": "5 ",
  "Message": "Value has trailing whitespace",
  "FixSuggestion": "Remove leading/trailing spaces from 'Offshore HC' in row 42"
}
```

### Power Automate 解析示例

在 Flow 里，用 **Parse JSON** 动作解析 Agent 返回的文本：

```
Schema（主要字段）:
{
  "type": "object",
  "properties": {
    "report_model": { "type": "object" },
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

---

## 10. 常见卡点与排查

| 问题现象 | 原因 | 解决方法 |
|----------|------|----------|
| 登录后显示"你无权访问此功能" | 无 Copilot Studio 许可证 | 联系 IT 管理员分配许可证；或申请试用版 |
| 看不到目标 Power Platform 环境 | 未被授权为 Environment Maker | IT 管理员在 [admin.powerplatform.microsoft.com](https://admin.powerplatform.microsoft.com) 添加你为 Maker |
| Power Automate 连接器里找不到 Copilot Studio | Agent 未发布，或连接器未安装 | 先发布 Agent；在 Flow 里搜索"Copilot Studio"并添加连接 |
| Agent 返回的 JSON 被 Markdown 包裹（` ```json ` 包裹） | 系统提示词不够明确 | 在指令里加强说明："Return ONLY valid JSON, no markdown, no code fences" |
| Agent 返回的 `issues[]` 没有 `RowIndex` 字段 | 提示词遗漏了 RowIndex 要求 | 在输出格式说明里明确标注 `"RowIndex": <integer>` 为必填 |
| Power Automate Parse JSON 动作失败 | Agent 返回了非 JSON 文本（如解释性文字） | 在 Flow 里先用 Compose 输出原始文本调试；检查 Agent 指令是否有"only output JSON"约束 |
| DLP 政策阻断连接器 | 租户数据丢失防护规则禁止 Copilot Studio 与 OneDrive/Teams 同组 | 联系 IT 管理员调整 DLP 策略，将相关连接器归为同一组 |
| 测试时 Agent 回复很慢（>30s） | 大 payload 或模型负载高 | 分批传入 rows（每批 50-100 行）；或在 Flow 里设置超时重试 |
| 环境选错导致 Agent 不可见 | Agent 发布在 A 环境，Flow 连的是 B 环境 | 确保 Copilot Studio、Power Automate 右上角选的是**同一个环境** |

---

## 快速参考链接

- Copilot Studio 门户：[https://copilotstudio.microsoft.com](https://copilotstudio.microsoft.com)
- Power Platform 管理中心：[https://admin.powerplatform.microsoft.com](https://admin.powerplatform.microsoft.com)
- Microsoft 365 管理中心：[https://admin.microsoft.com](https://admin.microsoft.com)
- Power Automate：[https://make.powerautomate.com](https://make.powerautomate.com)
- Copilot Studio 官方文档：[https://learn.microsoft.com/microsoft-copilot-studio/](https://learn.microsoft.com/microsoft-copilot-studio/)

---

*本文档由 GitHub Copilot 生成，结合 POC Validator 项目的实际需求。如有问题，请在仓库 Issues 中反馈。*
