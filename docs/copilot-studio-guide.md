# Copilot Studio 使用指南（新版 UI/UX 变更说明）

> 适用版本：2025 年起新版 Copilot Studio（工作流编辑器版）

---

## 目录

1. [UI/UX 变更概述](#1-uiux-变更概述)
2. [新旧界面对比](#2-新旧界面对比)
3. [新建空白代理（Blank Agent）](#3-新建空白代理blank-agent)
4. [配置系统指令（Instructions）](#4-配置系统指令instructions)
5. [主题（Topic）创建——新版工作流编辑器详解](#5-主题topic-创建新版工作流编辑器详解)
6. [Power Automate 接入：Execute Agent and wait](#6-power-automate-接入execute-agent-and-wait)
7. [JSON 输出可靠性排障与技巧](#7-json-输出可靠性排障与技巧)

---

## 1. UI/UX 变更概述

### 核心变化

微软在 2024–2025 年对 Copilot Studio 进行了重大 UI 重构，最关键的变化是：

> **触发短语（Trigger phrases）、生成式回答（Generative answers）和响应行为（Response behaviors）不再是独立的主题节点设置，而是统一在代理的"系统指令（Instructions）"中进行配置。**

主题（Topic）编辑器从"对话树"形式改为**工作流编辑器（Flow editor）**，默认只有一个触发节点（Trigger node）和一个文本编辑框。

### 影响范围

| 功能 | 旧版位置 | 新版位置 |
|------|---------|---------|
| 触发短语 | Topic → 触发短语列表 | 主要依赖 Instructions 或 Topic 触发器节点 |
| 生成式回答 | Topic → 节点属性 | Instructions（全局）或 Topic 中添加"生成式回答"步骤 |
| 回复行为 | Topic → 节点设置 | Instructions 中用自然语言描述 |
| 最终输出格式 | Topic 最后一个"发送消息"节点 | 由 Instructions 控制，Topic 输出原样传递 |

---

## 2. 新旧界面对比

### 2.1 主题创建入口

| 方面 | 旧版（~2023） | 新版（2024–2025） |
|------|-------------|-----------------|
| 编辑器类型 | 对话树/分支树 | 工作流编辑器（Flow editor） |
| 默认节点 | 触发短语列表 + 空对话节点 | 一个"触发器"节点 + 一个文本编辑框 |
| 触发短语输入 | Topic 页面顶部有独立输入框 | 点击"触发器"节点后在右侧属性面板输入 |
| 添加步骤方式 | 节点下方点击"+" | 节点间连线上点击"+"或右键 |
| 生成式回答节点 | 有独立"生成式回答"类型节点 | 同样存在，但入口变为"添加步骤 → 生成式回答" |

### 2.2 触发短语配置位置变化（关键！）

**旧版（对话树）：**
```
主题页面
├── 触发短语（Trigger phrases）
│   ├── [校验]
│   ├── [验证数据]
│   └── [validate rows]  ← 在此处直接添加
└── 对话流程
    └── ...节点...
```

**新版（工作流编辑器）：**
```
工作流画布
├── [触发器] 节点  ← 点击此节点 → 右侧属性面板 → 在"触发短语"输入
│       ↓
└── [文本框/步骤]  ← 编辑回复或添加生成式回答步骤
```

> **重要提示**：如果你在新版界面看到"只有一个编辑框，像工作流一样"，这是正常的！点击顶部的**触发器（Trigger）节点**，右侧会展开属性面板，在那里输入触发短语。

---

## 3. 新建空白代理（Blank Agent）

### 步骤

1. 打开 **Copilot Studio**，确认你在正确的环境（如 ResonaPOC）
2. 左侧菜单点击 **"代理"**（智能体/Copilot 代理）
3. 点击 **"新建代理"** 或 **"+ 新建"**
4. 选择 **"创建空白代理"**（Blank agent）
5. 填写**代理名称**，例如：`POC校验智能体`
6. 点击 **"创建"**

> ℹ️ **只看到"空白代理"选项是正常的**：微软在企业/练习租户中灰度上线模板库，许多环境只提供空白代理。空白代理功能完整，不影响使用。

---

## 4. 配置系统指令（Instructions）

系统指令是控制代理行为最核心的入口，**替代了旧版分散在 Topic 节点里的各种设置**。

### 4.1 找到入口

在代理主页，寻找以下任意一个入口（中文界面名称可能略有差异）：
- **"指令"** / **"系统指令"**
- **"行为"** / **"Behavior"**
- **"设置"→"系统指令"**

### 4.2 填写系统指令（Excel 行校验场景——可直接复制粘贴）

```text
你是一个严格的数据校验引擎，用于校验 Excel 表格的行数据。

输入格式：用户消息中包含一个 JSON 对象，结构如下：
{
  "meta": { ... 可选 ... },
  "rows": [ { "RowIndex": <整数>, 其他字段... }, ... ]
}

你的任务：
1. 按规则校验每一行数据。
2. 输出内容只能是有效的 JSON，不输出 Markdown，不输出解释说明，不返回链接。
3. 输出 JSON 必须严格符合以下结构：

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

校验规则（可继续扩展）：
- R-REQ-001（Error）：必填字段不能为空或仅为空格。必填字段：YearMonth、CostCenterNumber、Function、Team、Owner。
- R-YM-001（Error）：YearMonth 必须为 6 位数字（YYYYMM），且月份在 01–12 之间。
- R-TRIM-001（Warning）：任意文字字段如有前后空格须标记。
- R-NUM-001（Error）：应为纯数字的字段（如 CostCenterNumber）必须只包含数字。

如无任何问题，输出 "issues": []。
report_model 中的统计数量必须与 issues 列表一致。
```

### 4.3 保存

点击页面上的 **"保存"** 按钮，让系统指令生效。

---

## 5. 主题（Topic）创建——新版工作流编辑器详解

### 5.1 为什么要建主题

Power Automate 的 `Execute Agent and wait` 动作通过发送消息触发代理。为了确保代理可靠地进入校验逻辑（而不是走兜底/闲聊主题），建议创建一个专用主题。

### 5.2 新建主题

1. 左侧菜单点击 **"主题"**（Topics）
2. 点击 **"新建主题"**
3. 填写名称，例如：`验证Excel数据`

### 5.3 新版工作流编辑器操作步骤

进入主题编辑页后，你会看到一个**工作流画布**，里面只有一个默认的"触发器（Trigger）"节点和一个文本编辑区。

#### 第一步：输入触发短语

1. **单击"触发器"节点**（通常是画布最上方的紫色/蓝色节点）
2. 右侧会展开**属性面板**
3. 在属性面板中找到**"触发短语"（Trigger phrases）**输入框
4. 依次输入（每输入一条按回车或点 "+"）：
   ```
   校验
   校验行数据
   validate rows
   检查Excel
   run validation
   ```

#### 第二步：添加"生成式回答"步骤

1. 点击画布中"触发器"节点下方的 **"+"** 按钮（或在节点连线上点击）
2. 从弹出菜单选择 **"添加生成式回答"**（Add generative answer）
3. 在出现的输入框中填写（可直接复制）：
   ```
   请按系统指令校验输入的 JSON，仅返回符合 schema 的 JSON，不输出 Markdown，不输出任何解释。
   ```

#### 第三步：确认输出配置

- 最后一个输出步骤（"发送消息"）不要填写固定文本
- 确保代理的回复是"生成式回答"的原始输出（不做二次拼接/格式化）

#### 步骤示意图（文字版）

```
[触发器节点]  ← 点击 → 右侧属性面板输入触发短语
      ↓
[生成式回答]  ← 添加步骤 → 选择"生成式回答" → 填写提示词
      ↓
[输出/发送]   ← 确保输出原始 JSON，不加额外文本
```

### 5.4 保存并发布

1. 点击顶部 **"保存"**
2. 确认保存成功后点击 **"发布"**（如有此按钮）

---

## 6. Power Automate 接入：Execute Agent and wait

### 6.1 整体流程

```
[触发器] → [构造 Message] → [Execute Agent and wait] → [解析 JSON] → [写入 Excel]
```

### 6.2 Step 1：构造 Message（Compose 步骤）

新增一个 **Compose（撰写）** 步骤，Inputs 使用表达式：

```text
concat('{"rows":', string(variables('RowsForAI')), '}')
```

如果需要传 meta 信息（如批次年月）：

```text
concat('{"meta":{"batchYearMonth":"', variables('BatchYearMonth'), '"},"rows":', string(variables('RowsForAI')), '}')
```

> `RowsForAI` 是一个数组变量，存放已清洗好字段名的行对象（见 6.5 节字段映射建议）。

### 6.3 Step 2：Execute Agent and wait

| 字段 | 填写说明 |
|------|---------|
| 环境 ID | Power Platform 环境的 GUID（不是环境名）|
| 代理 | 选择你在 Copilot Studio 建好的代理 |
| 会话 ID | 留空（每次新建会话） |
| 消息（Message）| 选择上一步 Compose 的输出 |

> **如何获取环境 GUID**：打开 Power Platform 管理中心 → 选择环境 → URL 中的 GUID 即为环境 ID。

### 6.4 Step 3：提取并解析代理返回文本

在 `Execute Agent and wait` 后添加：

**Compose_AgentOutput**（调试用，可选）：
- Inputs：选择 `Execute Agent and wait` 的文本输出字段（通常叫 `Message`、`Text` 或 `Result`）

**Parse JSON（分析 JSON）**：
- Content：选择上一步 Compose 的输出
- Schema（可直接复制）：

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

### 6.5 字段映射建议（避免空格坑）

Excel 列名往往含空格（如 `Cost Center Number`），JSON 字段名建议无空格。在构造 `RowsForAI` 时做映射：

在 **Append to array variable** 的 Value 中使用如下对象模板（根据你的实际列名调整）：

```json
{
  "RowIndex": <Excel行号>,
  "YearMonth": "<YearMonth列值>",
  "CostCenterNumber": "<Cost Center Number列值>",
  "Function": "<Function列值>",
  "Team": "<Team列值>",
  "Owner": "<Owner列值>"
}
```

---

## 7. JSON 输出可靠性排障与技巧

### 7.1 常见问题与解决方案

#### 问题 1：代理只返回 `{"statusCode":"OK"}` 或空内容

**原因**：主题最后一步设置了固定回复文本（如硬编码"OK"），覆盖了生成式内容。

**解决**：
1. 进入 Copilot Studio → 找到对应主题
2. 检查最后一个"发送消息"节点，确保它输出的是"生成式回答"的结果，而不是固定字符串
3. 如仍有问题，删除主题改用"仅系统指令"模式（不建主题，直接靠 Instructions 驱动）

#### 问题 2：返回内容含 Markdown（如 ```json ... ```）导致 Parse JSON 失败

**原因**：模型默认会用代码块包裹 JSON。

**解决**（三选一）：
- **方法 A**：在系统指令中添加强调：`严禁使用 Markdown 代码块，严禁输出 ``` 字符。`
- **方法 B**：在 Power Automate 中用 `replace()` 表达式清洗：
  ```text
  replace(replace(outputs('Execute_Agent_and_wait')?['body/message'], '```json', ''), '```', '')
  ```
- **方法 C**：在 Parse JSON 的 Content 前加一个 Compose 清洗步骤

#### 问题 3：返回了 JSON 但前后有解释文字

**原因**：模型在 JSON 前后加了"以下是校验结果："等说明。

**解决**：
- 系统指令中加：`不要在 JSON 前后添加任何解释文字，直接以 { 开头，以 } 结尾。`
- Power Automate 中用正则提取纯 JSON：
  ```text
  substring(outputs('Compose'), indexOf(outputs('Compose'), '{'), sub(lastIndexOf(outputs('Compose'), '}'), indexOf(outputs('Compose'), '{')) + 1)
  ```

#### 问题 4：部分行的 issues 丢失或统计不一致

**原因**：输入 rows 数组过长，超出模型单次处理能力。

**解决**：
- 每批发送不超过 **50 行**
- 在 Power Automate 中分批处理，用 `Apply to each`（应用到每个）配合分批逻辑

### 7.2 最大化 JSON 输出可靠性的系统指令技巧

在系统指令中加入以下强化语句（追加到规则末尾）：

```text
输出约束（必须严格遵守）：
- 第一个字符必须是 {
- 最后一个字符必须是 }
- 不使用 Markdown 代码块（即不使用 ```）
- 不输出解释、标题或任何 JSON 以外的文字
- 字段名严格按照 schema，区分大小写
- issues 数组中的每个对象必须包含全部 7 个字段
```

### 7.3 在 Copilot Studio 测试面板验证

在发布到 Power Automate 之前，先在 Copilot Studio 的**测试聊天窗口**手动验证。

**标准测试输入（复制粘贴即可）**：

```json
{
  "meta": { "batchYearMonth": "202603" },
  "rows": [
    {
      "RowIndex": 1,
      "YearMonth": "202603",
      "CostCenterNumber": "12345",
      "Function": "IT ",
      "Team": "A",
      "Owner": "Felix"
    },
    {
      "RowIndex": 2,
      "YearMonth": "202613",
      "CostCenterNumber": "12A45",
      "Function": "",
      "Team": "B",
      "Owner": " "
    }
  ]
}
```

**期望输出特征**：
- 以 `{` 开头，以 `}` 结尾
- 包含 `report_model` 和 `issues` 字段
- `issues` 至少包含：
  - RowIndex=1，Function 尾部空格（Warning / R-TRIM-001）
  - RowIndex=2，YearMonth 月份非法（Error / R-YM-001）
  - RowIndex=2，CostCenterNumber 含非数字字符（Error / R-NUM-001）
  - RowIndex=2，Function / Owner 为空（Error / R-REQ-001）

### 7.4 快速排障决策树

```
代理返回异常？
│
├── 只返回 OK / statusCode
│   └── → 检查 Topic 最后节点是否为固定文本
│         → 改为输出生成式回答结果
│
├── 返回内容含 Markdown
│   └── → 系统指令加"禁止 Markdown"约束
│         → 或 Power Automate 中 replace() 清洗
│
├── 返回内容有多余解释文字
│   └── → 系统指令加"直接以 { 开头以 } 结尾"约束
│
├── Parse JSON 失败
│   └── → 先用 Compose 打印原始返回内容，检查实际格式
│         → 确认 Schema 与实际输出字段匹配
│
└── issues 数量不对
    └── → 检查 rows 数量（建议每批 ≤50）
          → 检查系统指令中 report_model 计数是否一致
```

---

## 中英文界面术语对照

| 中文 | 英文 |
|------|------|
| 代理 / 智能体 | Agent |
| 新建代理 / 空白代理 | New / Blank Agent |
| 系统指令 | System instructions / Instructions |
| 主题 | Topic |
| 触发器节点 | Trigger node |
| 触发短语 | Trigger phrases |
| 生成式回答 | Generative answer |
| 发送消息 | Send a message |
| 保存 / 发布 | Save / Publish |
| 测试面板 | Test panel / Chat preview |
| 执行代理并等待 | Execute Agent and wait |
| 分析 JSON | Parse JSON |
| 撰写 | Compose |
| 环境 ID | Environment ID (GUID) |
| 会话 ID | Conversation ID |
