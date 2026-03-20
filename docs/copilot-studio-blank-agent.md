# Copilot Studio：只有"空白代理"选项时的完整配置指南

> 适用场景：练习租户 / 公司沙盒环境下，Copilot Studio 新建代理页面只出现 **"空白代理（Blank agent）"**，没有模板可选。

---

## 目录

1. [为什么看不到模板？](#1-为什么看不到模板)
2. [前提：Maker 权限与连接检查清单](#2-前提-maker-权限与连接检查清单)
3. [用空白代理从零搭建校验智能体](#3-用空白代理从零搭建校验智能体)
   - 3.1 新建代理
   - 3.2 粘贴系统指令
   - 3.3 创建 ValidateRows Topic
   - 3.4 用示例 JSON 测试
4. [从 Power Automate 调用代理](#4-从-power-automate-调用代理)
   - 4.1 生成 Message（Compose）
   - 4.2 Execute Agent and wait 配置
   - 4.3 解析返回值
5. [故障排除](#5-故障排除)

---

## 1 为什么看不到模板？

Copilot Studio 的"根据模板创建代理"功能受以下因素影响，任意一项不满足都可能导致只显示"空白代理"：

| 原因 | 说明 |
|------|------|
| **租户灰度 / UI 差异** | Microsoft 采用逐步发布策略（Staged Rollout），练习租户通常比生产租户晚收到新 UI，模板库可能尚未推送到该租户。 |
| **许可证类型** | 完整的模板功能通常需要 Copilot Studio 独立许可或 Power Platform Premium 许可；试用/开发者/沙盒许可可能不包含模板目录。 |
| **环境类型** | Developer 环境和 Sandbox 环境默认功能集少于 Production 环境；部分模板仅在 Production 环境可用。 |
| **Maker 权限不足** | 如果当前账号在该环境中没有 Environment Maker 角色，部分创建入口会被隐藏或禁用。 |
| **数据驻留 / 合规区域** | 政府云（GCC/GCC-High）或特定合规区域的租户会限制部分功能，包括模板预览。 |
| **DLP 策略限制** | 管理员设置的数据丢失防护（DLP）策略有时会屏蔽与模板关联的连接器，从而导致模板入口不可用。 |

> **结论：只有空白代理完全正常**，能实现和模板相同的功能，只是需要手动完成模板默认帮你做好的部分。本文档给你一步步的可复制粘贴配置。

---

## 2 前提：Maker 权限与连接检查清单

在开始前，请确认以下各项：

### 2.1 Maker 权限检查

- [ ] 在 [Power Platform 管理中心](https://admin.powerplatform.microsoft.com/) → **Environments** → 选择你的环境 → **Settings → Users + permissions → Environment users**，确认你的账号有 **Environment Maker** 角色。
- [ ] 能在该环境中创建 Cloud Flow（Power Automate → My flows → New flow）。
- [ ] 能在该环境中打开 Copilot Studio 并看到 **Create** 按钮。
- [ ] 在 Copilot Studio 里能看到 **Flows** 和 **Tools** 选项卡（说明基础权限没问题）。

### 2.2 连接（Connection）检查

- [ ] Power Automate 中已建立 **Microsoft Copilot Studio** 连接（Data → Connections），且显示为绿色/已连接状态。
- [ ] 连接所用账号与 Copilot Studio 环境的账号**一致**（同一 Microsoft 365 账号）。
- [ ] 如果连接显示警告或过期：删除旧连接，在 Flow 动作中重新授权创建新连接。

### 2.3 Environment ID 确认

- [ ] 已从 Power Platform 管理中心复制了目标环境的 **Environment ID（GUID 格式）**，例如 `3f2f2b1a-1234-5678-9abc-def012345678`。
- [ ] **不要**把环境显示名称（如 `ResonaPOC`）当作 Environment ID——这会导致 DNS 解析失败（502 错误）。

**获取 Environment ID 的方法：**
1. 打开 https://admin.powerplatform.microsoft.com/
2. 左侧点 **Environments**
3. 点击目标环境名称进入详情
4. 复制页面中的 **Environment ID**（一串 GUID）

---

## 3 用空白代理从零搭建校验智能体

### 3.1 新建代理

1. 打开 Copilot Studio，确认右上角环境是你的目标环境（如 ResonaPOC）。
2. 点击 **Create → Blank agent（空白代理）**。
3. 填写基本信息：
   - **Name（名称）**：`POC-Validator-Agent`
   - **Language（语言）**：按需选择（`Chinese Simplified` 或 `English`）
4. 点击 **Create** 完成创建，进入代理配置页。

---

### 3.2 粘贴系统指令（System Instructions）

进入代理配置页后，找到以下任意一个入口：

- **Instructions** 标签页
- **Configure → Instructions**
- **Settings → Agent instructions / Behavior**

将下面的内容**完整复制粘贴**进去（不需要改动任何内容即可开始测试）：

```text
You are a strict data validation engine for Excel table rows.

Input will be provided as a single JSON object in the user message:
{
  "meta": { ... optional ... },
  "rows": [ { "RowIndex": <int>, ...columns... }, ... ]
}

Your task:
1) Validate each row according to the rules below.
2) Produce ONLY valid JSON as output. Do not include Markdown. Do not include any explanation text. Do not return links.
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

Rules to apply:
- R-REQ-001 (Error): Required fields must not be empty or whitespace-only. Required fields: YearMonth, CostCenterNumber, Function, Team, Owner.
- R-YM-001 (Error): YearMonth must be exactly 6 digits (YYYYMM) and represent a valid month 01-12.
- R-TRIM-001 (Warning): Leading/trailing whitespace in any text field should be flagged.
- R-NUM-001 (Error): If a field is supposed to be numeric (e.g., CostCenterNumber), it must contain digits only.

If there are no issues, return issues as an empty array: "issues": [].
Always compute report_model counts consistently with issues.

IMPORTANT: Return ONLY the JSON object. No Markdown code blocks. No explanations. No links.
```

---

### 3.3 创建 ValidateRows Topic

Topic 相当于代理的"技能入口"，用于接收 Power Automate 发来的 JSON 并触发校验。

**步骤：**

1. 在代理配置页，点击顶部 **Topics** 选项卡。
2. 点击 **New topic（新建主题）**。
3. 填写 **Name（名称）**：`ValidateRows`
4. 在 **Trigger phrases（触发短语）** 中添加以下短语（每行一条，复制粘贴）：

   ```
   validate rows
   validate excel rows
   run validation
   校验
   校验rows
   {"rows"
   ```

   > 最后一条 `{"rows"` 确保 Power Automate 发来的 JSON 消息能匹配到此 Topic。

5. 在 Topic 的对话流中，确保**最后一步**使用"生成式回复（Generative answer）"或直接将模型输出返回，**不要**写固定文本（例如写死"OK"会导致只返回 OK）。

   - 如果有"**Send a message / 发送消息**"节点：删除或替换为"**Generative answer**"节点。
   - 如果有"**Generative answer**"节点：保留，输入提示词：

     ```
     Validate input JSON and return ONLY JSON output according to the required schema.
     ```

6. 点击 **Save** 保存 Topic。

---

### 3.4 用示例 JSON 测试

在代理配置页右侧的**测试聊天窗口**（Test your agent）中，粘贴以下内容并发送：

```json
{
  "meta": { "batchYearMonth": "202603" },
  "rows": [
    { "RowIndex": 1, "YearMonth": "202603", "CostCenterNumber": "12345", "Function": "IT ", "Team": "A", "Owner": "Felix" },
    { "RowIndex": 2, "YearMonth": "202613", "CostCenterNumber": "12A45", "Function": "", "Team": "B", "Owner": " " }
  ]
}
```

**预期返回结果**（纯 JSON，无 Markdown，无解释文字）：

```json
{
  "report_model": {
    "rows_total": 2,
    "errors_total": 4,
    "warnings_total": 1
  },
  "issues": [
    { "Severity": "Warning", "RuleId": "R-TRIM-001", "RowIndex": 1, "Column": "Function", "RawValue": "IT ", "Message": "Trailing whitespace detected.", "FixSuggestion": "Trim the value to 'IT'." },
    { "Severity": "Error", "RuleId": "R-YM-001", "RowIndex": 2, "Column": "YearMonth", "RawValue": "202613", "Message": "Month '13' is not valid (must be 01-12).", "FixSuggestion": "Correct the month to a value between 01 and 12." },
    { "Severity": "Error", "RuleId": "R-NUM-001", "RowIndex": 2, "Column": "CostCenterNumber", "RawValue": "12A45", "Message": "Non-numeric characters found.", "FixSuggestion": "Remove non-digit characters." },
    { "Severity": "Error", "RuleId": "R-REQ-001", "RowIndex": 2, "Column": "Function", "RawValue": "", "Message": "Required field is empty.", "FixSuggestion": "Provide a non-empty value." },
    { "Severity": "Error", "RuleId": "R-REQ-001", "RowIndex": 2, "Column": "Owner", "RawValue": " ", "Message": "Required field contains only whitespace.", "FixSuggestion": "Provide a non-empty value." }
  ]
}
```

**如果返回了 Markdown 或解释文字：**
- 检查系统指令末尾是否有 `IMPORTANT: Return ONLY the JSON object.`
- 检查 Topic 最后一步是否写了固定回复文本（删除它）

---

## 4 从 Power Automate 调用代理

### 4.1 生成 Message（Compose 步骤）

在 Power Automate Flow 中，`Execute Agent and wait` 之前添加一个 **Compose** 步骤（建议命名为 `Compose_Message`）。

在 **Inputs** 中选择 **Expression（表达式）** 模式，填入：

```
concat('{"rows":', string(variables('RowsForAI')), '}')
```

如果需要带 meta 信息（批次年月等），使用：

```
concat('{"meta":{"batchYearMonth":"', triggerBody()?['BatchYearMonth'], '"},"rows":', string(variables('RowsForAI')), '}')
```

> ⚠️ 把 `BatchYearMonth` 替换为你触发器中实际的字段名；不确定时先用第一个简化版本。

---

### 4.2 Execute Agent and wait 配置

添加动作：搜索 **"Copilot Studio"** → 选择 **Execute Agent and wait**。

| 字段 | 填写内容 |
|------|---------|
| **Environment ID** | 粘贴从管理中心复制的 Environment GUID（如 `3f2f2b1a-...`），**不要**填环境名称 |
| **Agent** | 从下拉选择 `POC-Validator-Agent` |
| **Message** | 选择动态内容：**Outputs of Compose_Message** |
| **Conversation ID** | 留空（单次调用不需要维持会话） |
| **Advanced parameters** | 全部留空（Locale、Attachments 均不需要） |

---

### 4.3 解析返回值

`Execute Agent and wait` 完成后：

1. **立即加一个 Compose（Compose_AgentRawOutput）** 把原始返回打印出来：
   - Inputs：选择 `Execute Agent and wait` 的文本输出字段（通常是 `Message` 或 `Result`）

2. 在 Run history 中确认 `Compose_AgentRawOutput` 的输出是纯 JSON（以 `{` 开头 `}` 结尾）。

3. 然后加 **Parse JSON**：
   - **Content**：选 `Compose_AgentRawOutput` 的输出
   - **Schema**：使用以下 JSON Schema（复制粘贴）：

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

4. Parse JSON 之后可以用 **Apply to each** 遍历 `issues` 数组，将每个 issue 写入 Excel 的 `tblIssues` 表。

---

## 5 故障排除

### 5.1 Copilot Studio 只有空白代理选项

| 现象 | 最可能原因 | 处理建议 |
|------|-----------|---------|
| 新建代理页面没有任何模板 | 租户灰度/许可限制 | 直接使用空白代理，功能完全等价，按本文档操作 |
| 某些账号能看到模板，某些不能 | Maker 权限或许可证差异 | 联系管理员确认许可和角色分配 |
| 之前能看到，现在不能 | UI 版本回滚或缓存问题 | 清除浏览器缓存，或换浏览器/隐私窗口重试 |

### 5.2 Execute Agent and wait 报 502 + DNS 无法解析

**典型错误信息：**
```
The remote name could not be resolved: 'xxxxx.oc.environment.api.powerplatform.com'
```

**根本原因：** Environment ID 字段填写了环境**名称**（如 `ResonaPOC`）而非 GUID，导致拼出了不存在的域名。

**修复步骤：**
1. 从 Power Platform Admin Center 取正确的 Environment GUID（见 [2.3 节](#23-environment-id-确认)）
2. 在动作中把 Environment ID 字段替换为 GUID
3. 重新保存并测试 Flow

### 5.3 代理只返回 `{"statusCode":"OK"}` 或链接

| 现象 | 原因 | 修复 |
|------|------|------|
| 返回 `{"statusCode":"OK"}` | Topic 最后一步写了固定回复 "OK" | 删除固定回复节点，改为"Generative answer" |
| 返回一个 URL/链接 | 代理把会话记录或附件作为响应 | 同上，确保系统指令里有"Return ONLY JSON, no links" |
| 返回 Markdown 格式的 JSON | 模型加了代码块标记 | 系统指令末尾加：`Do not wrap JSON in Markdown code blocks.` |

### 5.4 Power Automate 连接问题

| 现象 | 处理建议 |
|------|---------|
| 连接显示为警告/错误状态 | 删除旧连接，重新在 Flow 动作中授权创建新连接 |
| 连接账号和 Copilot Studio 环境账号不一致 | 确保使用同一 Microsoft 365 账号创建连接和代理 |
| Flow 报"连接器未授权"之类的错误 | 检查管理员的 DLP 策略是否允许 Copilot Studio 连接器 |

### 5.5 Parse JSON 失败

- 用 `Compose_AgentRawOutput` 把原始返回打印出来，检查是否真的是纯 JSON。
- 如果 JSON 前后有额外文字，在 Parse JSON 的 Content 里套一层 `trim()` 或正则提取，或者回到系统指令加强"只返回 JSON"的约束。
- 如果字段名和 Schema 对不上：以实际返回的字段名为准，修改 Schema。
