# POC Validator — Power Automate 搭建 Runbook

> 适用场景：使用共享账户 + SharePoint 存储的 POC 数据校验 Flow，基于 Excel Online (Business) + Copilot Studio + Teams 通知。

---

## 目录

1. [前置条件](#1-前置条件)
2. [Trigger 设计](#2-trigger-设计)
3. [读取输入 Excel：tblOffshoring](#3-读取输入-exceltbloffshoring)
   - 3.1 [Pagination 配置说明（必读）](#31-pagination-配置说明必读)
4. [构建 RowIndex 和 RowsForAI](#4-构建-rowindex-和-rowsforai)
   - 4.1 [初始化变量](#41-初始化变量)
   - 4.2 [Apply to each 循环](#42-apply-to-each-循环)
   - 4.3 [故障排除：追加对象到数组变量时的 UI 差异](#43-故障排除追加对象到数组变量时的-ui-差异)
5. [验证 RowsForAI（Compose 快速检查）](#5-验证-rowsforaicompose-快速检查)
6. [调用 Copilot Studio](#6-调用-copilot-studio)
7. [生成 Excel 报告](#7-生成-excel-报告)
8. [写入 tblIssues](#8-写入-tblissues)
9. [Teams 通知](#9-teams-通知)
10. [Summary 固定单元格映射](#10-summary-固定单元格映射)
11. [issues\[\] 必含字段](#11-issues-必含字段)

---

## 1. 前置条件

| 项目 | 要求 |
|------|------|
| Power Automate 账号 | 共享账户（非个人 OneDrive） |
| SharePoint 站点 | Team Site（非 `/personal/...`） |
| 文档库 | `Documents` |
| SharePoint 目录结构 | 见下方 |
| Environment | Power Automate 与 Copilot Studio **必须同一环境** |

**文档库目录结构（需提前在 SharePoint 创建）：**

```
Documents/
└── POC-Validator/
    ├── Inputs/          ← 输入 Excel（含 tblOffshoring）
    ├── Templates/       ← ValidationReportTemplate.xlsx
    └── Outputs/
        ├── Excel/       ← 生成的报告 Excel
        ├── PDF/         ← 生成的 PDF 报告
        └── JSON/        ← AI 原始输出（可选，用于审计）
```

**确保 Data → Connections 中以下连接均使用共享账户：**
- SharePoint
- Excel Online (Business)
- Microsoft Teams
- Copilot Studio 相关连接

---

## 2. Trigger 设计

**动作类型：** `Manually trigger a flow`（即时云端流）

添加以下 Trigger 输入：

| 名称 | 类型 | 说明 |
|------|------|------|
| `BatchYearMonth` | Text | 例：`202603`；必填 |
| `TopNIssuesInPDF` | Number | 默认 `200` |

---

## 3. 读取输入 Excel：tblOffshoring

**动作：** `Excel Online (Business) → List rows present in a table`

| 字段 | 填写内容 |
|------|----------|
| Location | 选择 SharePoint Site（团队站点，非个人站点） |
| Document Library | `Documents` |
| File | 选择 `POC-Validator/Inputs/<你的输入文件>.xlsx` |
| Table | `tblOffshoring` |

> **重要：** 不要对原始值做任何 trim/replace。原始值（含前后空格）必须原封不动传给 AI，才能正确触发 R-WS-ALL-001 规则。

### 3.1 Pagination 配置说明（必读）

Pagination 功能用于突破单次返回行数限制（默认上限 256 行），**务必开启并正确配置**。

**操作步骤：**

1. 在 `List rows present in a table` 动作右上角，点击 **`...`（更多选项）**。
2. 选择 **`Settings`**。
3. 找到 **`Pagination`** 开关，将其拨为 **`On`**。
4. 在 **`Threshold`** 字段填写一个 **大于 0 的整数**（不可为 0、负数或小数）。

**Threshold 推荐值：**

| 预估数据行数 | 推荐 Threshold |
|-------------|----------------|
| ≤ 1,000 行  | `1000` |
| ≤ 5,000 行  | `5000` |
| ≤ 20,000 行 | `20000` |

> **⚠️ 常见错误：**
> - Threshold 填 `0`：Flow 保存时报错或运行时只返回 0 行。Threshold **必须 > 0**。
> - Threshold 留空：Pagination 不生效，实际只读取默认上限行数（通常 256）。
> - Threshold 填小数（如 `5000.5`）：会被拒绝或截断，必须是整数。

---

## 4. 构建 RowIndex 和 RowsForAI

本节是核心步骤：在读取完 `tblOffshoring` 之后，为每行附加行号（`RowIndex`），组装成供 AI 校验的数组（`RowsForAI`）。

### 4.1 初始化变量

在 `List rows present in a table` 动作之后，依次添加两个 **`Initialize variable`** 动作：

**动作 1：初始化 RowIndex**

| 字段 | 填写内容 |
|------|----------|
| Name | `RowIndex` |
| Type | `Integer` |
| Value | `0` |

**动作 2：初始化 RowsForAI**

| 字段 | 填写内容 |
|------|----------|
| Name | `RowsForAI` |
| Type | `Array` |
| Value | `[]` （两个方括号，代表空数组） |

### 4.2 Apply to each 循环

添加 **`Apply to each`** 动作，在 `Select An Output From Previous Steps` 框里：

1. 点击输入框，右侧会弹出动态内容面板。
2. 选择 `List rows present in a table` 下的 **`value`**（代表所有行的数组）。

在循环**内部**按顺序添加以下动作：

---

**循环内部第 1 步：Increment variable（RowIndex +1）**

动作类型：`Increment variable`

| 字段 | 填写内容 |
|------|----------|
| Name | `RowIndex` |
| Value | `1` |

---

**循环内部第 2 步：Append to array variable（追加当前行到 RowsForAI）**

动作类型：`Append to array variable`

| 字段 | 填写内容 |
|------|----------|
| Name | `RowsForAI` |
| Value | 见下方 JSON 对象 |

在 `Value` 字段，切换到**表达式/代码视图**（Code View，详见 [4.3 节故障排除](#43-故障排除追加对象到数组变量时的-ui-差异)），粘贴以下 JSON 对象（按你实际列名调整）：

```json
{
  "RowIndex": @{variables('RowIndex')},
  "YearMonth": @{items('Apply_to_each')?['YearMonth']},
  "Cost Center Number": @{items('Apply_to_each')?['Cost Center Number']},
  "Function": @{items('Apply_to_each')?['Function']},
  "Team": @{items('Apply_to_each')?['Team']},
  "Owner": @{items('Apply_to_each')?['Owner']},
  "ShoringRatio": @{items('Apply_to_each')?['ShoringRatio']},
  "OffshoreSite": @{items('Apply_to_each')?['OffshoreSite']}
}
```

> **说明：**
> - `@{variables('RowIndex')}` 引用变量值（当前行号，从 1 开始）。
> - `@{items('Apply_to_each')?['列名']}` 引用当前循环项目中对应列的原始值。
> - `Apply_to_each` 是循环动作的名称（若你的循环已重命名，改为对应名称）。
> - 所有列值**原样传入**，不做任何 trim/replace。
> - 请将所有需要 AI 校验的列都加进来。

### 4.3 故障排除：追加对象到数组变量时的 UI 差异

在 `Append to array variable` 动作的 Value 字段中追加 JSON 对象时，不同版本的 Power Automate 界面行为不同，以下是常见问题及解决方法：

---

**问题 1：界面只显示一个普通文本框，无法输入多行 JSON**

**原因：** 默认 UI 以纯文本模式展示，看起来只有一行输入框。

**解决方法：**
1. 点击 Value 输入框右下角的 **`↗`（展开）** 图标（若有），进入大输入框。
2. 或者点击输入框后，在弹出的动态内容面板顶部找到 **`Expression`**（表达式）标签，切换到表达式模式，在里面可以用 `json(...)` 函数构造对象。

---

**问题 2：直接粘贴 JSON 对象后，Flow 保存报错"预期字符串，实际为对象"**

**原因：** Value 字段被识别为字符串类型输入，不接受原始 JSON 对象。

**解决方法（推荐）：**
1. 在动作右上角点击 **`...`** → **`Peek code`**（查看代码）或切换到 **`Code view`**。
2. 找到该动作的 `inputs.value` 字段，将值改为你的 JSON 对象（而非 JSON 字符串）。
3. 例如：
   ```json
   "inputs": {
     "name": "RowsForAI",
     "value": {
       "RowIndex": "@{variables('RowIndex')}",
       "YearMonth": "@{items('Apply_to_each')?['YearMonth']}"
     }
   }
   ```
4. 保存后切回正常视图验证字段映射是否正确。

---

**问题 3：动态内容面板里找不到当前行的列字段**

**原因：** `Apply to each` 内部有时不会自动在动态内容面板里展示上层循环项目的字段。

**解决方法：**
1. 点击 Value 输入框，在动态内容面板顶部点击 **`Expression`**。
2. 手动输入引用表达式：`items('Apply_to_each')?['列名']`（将 `列名` 替换为实际列名）。
3. 点击 **`OK`** 确认。
4. 对每个列字段重复此步骤。

---

**问题 4：追加的对象在运行后变成字符串（如 `"{\"RowIndex\":1,...}"`）**

**原因：** 输入值被当作字符串序列化，而非 JSON 对象。

**解决方法：**
1. 改用 `Expression` 模式，在表达式框里写：
   ```
   json(concat('{"RowIndex":', string(variables('RowIndex')), ',"YearMonth":"', items('Apply_to_each')?['YearMonth'], '"}'))
   ```
   > 注意：此方法适合列值为字符串类型的情况，数值列不要加引号。
2. 或者通过 Code View 直接编辑 JSON 定义（参见问题 2 解决方法）。

---

## 5. 验证 RowsForAI（Compose 快速检查）

在 `Apply to each` 循环结束后，立即添加一个 **`Compose`** 动作，用于检查 `RowsForAI` 的实际内容，确认结构正确后再交给 AI。

**动作：** `Data Operation → Compose`

| 字段 | 填写内容 |
|------|----------|
| Inputs | `@{variables('RowsForAI')}` |

**操作步骤：**
1. 在循环之后添加 `Compose` 动作。
2. 在 Inputs 框里点击，选择动态内容 → `RowsForAI` 变量；或直接在表达式框输入 `variables('RowsForAI')`。
3. 保存并手动运行 Flow（用少量测试数据，例如输入 Excel 仅保留 2-3 行）。
4. 运行完成后，进入 Flow **运行历史**，点击该次运行记录，找到这个 `Compose` 动作，展开 **`OUTPUTS`**。

**预期输出（正常情况）：**

```json
[
  {
    "RowIndex": 1,
    "YearMonth": "202603",
    "Cost Center Number": "12345",
    "Function": "IT",
    "Team": "Platform",
    "Owner": "Alice"
  },
  {
    "RowIndex": 2,
    "YearMonth": "202603 ",
    "Cost Center Number": " 67890",
    "Function": "HR",
    "Team": "Talent",
    "Owner": "Bob"
  }
]
```

> 注意：第 2 行 `YearMonth` 末尾有空格、`Cost Center Number` 开头有空格——这是预期的（原样传入，AI 会报 R-WS-ALL-001 Error）。

**排查检查点：**

| 检查项 | 正常 | 异常 |
|--------|------|------|
| 输出是数组（`[...]`） | ✅ | 输出是字符串，说明追加时被序列化，见 [4.3 节问题 4](#43-故障排除追加对象到数组变量时的-ui-差异) |
| 每个元素是对象（`{...}`） | ✅ | 每个元素是字符串（`"{...}"`），同上 |
| `RowIndex` 从 1 开始依次递增 | ✅ | `RowIndex` 全为同一值，检查 Increment variable 是否在循环**内部** |
| 所有需要校验的列都存在 | ✅ | 某列缺失，检查追加对象的 JSON key 是否拼写正确 |
| 列值含空格（原样） | ✅ | 列值已被 trim，检查是否意外加了处理表达式 |
| 总条目数 = 输入 Excel 行数 | ✅ | 条目数不足，检查 Pagination 是否正确开启（Threshold > 0） |

> 验证通过后，可将此 `Compose` 动作**禁用**（动作右上角 `...` → `Disable`）或删除，避免在生产运行时占用输出存储。

---

## 6. 调用 Copilot Studio

**动作：** 在 Power Automate 搜索 `Copilot` 或 `Agent`，选择与你们租户匹配的动作（名称因租户略有不同）。

**输入 Payload（建议）：**

```json
{
  "meta": {
    "batchYearMonth": "@{triggerBody()?['BatchYearMonth']}",
    "sourceFile": "@{<输入文件名表达式，例如固定字符串或 SharePoint 元数据>}",
    "generatedAt": "@{utcNow()}"
  },
  "ruleset_version": "poc-v1",
  "rows": "@{variables('RowsForAI')}"
}
```

**要求 Agent 输出（严格 JSON，禁止 Markdown）：**
- `report_model`：汇总（行数、Error/Warning 数量、top rules、top columns 等）
- `issues`：数组，每条至少包含 `Severity`、`RuleId`、`RowIndex`、`Column`、`RawValue`、`Message`、`FixSuggestion`、`RowKey`，以及 `YearMonth`、`Cost Center Number`、`Function`、`Team`、`Owner`

> 确保 Power Automate 与 Copilot Studio **处于同一 Environment**，否则在 Flow 动作列表里找不到你的 Agent。

---

## 7. 生成 Excel 报告

### 7.1 读取模板

**动作：** `SharePoint → Get file content using path`

| 字段 | 填写内容 |
|------|----------|
| Site Address | 选择团队站点 |
| File Path | `/Documents/POC-Validator/Templates/ValidationReportTemplate.xlsx` |

### 7.2 创建报告副本

**动作：** `SharePoint → Create file`

| 字段 | 填写内容 |
|------|----------|
| Site Address | 选择团队站点 |
| Folder Path | `/Documents/POC-Validator/Outputs/Excel` |
| File Name | `ValidationReport_@{triggerBody()?['BatchYearMonth']}_@{formatDateTime(utcNow(), 'yyyyMMdd-HHmm')}.xlsx` |
| File Content | 7.1 的 `File Content` |

### 7.3 写入 Summary 固定单元格

**动作：** `Excel Online (Business) → Update a row`（或 `Run script` 批量写入）

对每个单元格分别执行写入（Location = SharePoint Site，File = 7.2 新建文件，Worksheet = `Summary`）：

| 单元格 | 内容来源 |
|--------|----------|
| `B2` | 固定文字 `POC Data Validation Report` |
| `B3` | `utcNow()` |
| `B4` | Trigger `BatchYearMonth` |
| `B5` | 输入文件名 |
| `B7` | `report_model.rows_total` |
| `B8` | `report_model.issues_total` |
| `B9` | `report_model.errors_total` |
| `B10` | `report_model.warnings_total` |
| `B11` | `report_model.pass_rate` |

---

## 8. 写入 tblIssues

### 8.1 解析 AI 输出

**动作：** `Data Operation → Parse JSON`

- Content：AI 动作的输出（`issues` 数组部分）
- Schema：按你 Agent 输出的字段生成（点击 `Generate from sample` 粘贴示例 JSON 即可）

### 8.2 逐行写入

**动作：** `Apply to each` → 遍历 Parse JSON 的 `issues`

在循环内添加：`Excel Online (Business) → Add a row into a table`

| 字段 | 填写内容 |
|------|----------|
| Location | SharePoint Site |
| File | 7.2 新建的报告 Excel |
| Table | `tblIssues` |
| Severity | `items()?['Severity']` |
| RuleId | `items()?['RuleId']` |
| RowKey | `items()?['RowKey']` |
| RowIndex | `items()?['RowIndex']` |
| YearMonth | `items()?['YearMonth']` |
| Cost Center Number | `items()?['Cost Center Number']` |
| Function | `items()?['Function']` |
| Team | `items()?['Team']` |
| Owner | `items()?['Owner']` |
| Column | `items()?['Column']` |
| RawValue | `items()?['RawValue']` |
| Message | `items()?['Message']` |
| FixSuggestion | `items()?['FixSuggestion']` |

---

## 9. Teams 通知

**动作：** `Microsoft Teams → Post a message in a chat or channel`

建议发到固定频道（如 `poc-validator`）。消息内容示例：

```
POC Validator Result - @{triggerBody()?['BatchYearMonth']}

Rows: @{body('Parse_JSON')?['report_model']?['rows_total']}
Errors: @{body('Parse_JSON')?['report_model']?['errors_total']}
Warnings: @{body('Parse_JSON')?['report_model']?['warnings_total']}
Pass Rate: @{body('Parse_JSON')?['report_model']?['pass_rate']}

Excel 报告: @{outputs('Create_file')?['body/WebUrl']}
```

> 如果 `Create file` 动作没有直接返回 `WebUrl`，在其后添加 `SharePoint → Create sharing link for a file or folder` 动作获取分享链接。

---

## 10. Summary 固定单元格映射

模板的 `Summary` 页预留以下格子（A 列写标签，B 列写值）：

| 单元格 | 标签（A 列） | 值（B 列） |
|--------|------------|-----------|
| A2 / B2 | Report Title | `POC Data Validation Report` |
| A3 / B3 | GeneratedAt | UTC 时间戳 |
| A4 / B4 | BatchYearMonth | 触发器输入值 |
| A5 / B5 | SourceFileName | 输入 Excel 文件名 |
| A7 / B7 | RowsTotal | 行数合计 |
| A8 / B8 | IssuesTotal | 问题总数 |
| A9 / B9 | ErrorsTotal | Error 数量 |
| A10 / B10 | WarningsTotal | Warning 数量 |
| A11 / B11 | PassRate | 通过率（如 `0.875`） |

可选汇总区（从 `A13` 起）：

- `A13` 起：Top Rules 小表（列：RuleId / Severity / Count）
- `E13` 起：Top Columns 小表（列：Column / Count）

---

## 11. issues[] 必含字段

Copilot Studio Agent 返回的每条 issue 必须包含：

| 字段 | 说明 | 是否必须 |
|------|------|---------|
| `Severity` | `Error` 或 `Warning` | ✅ 必须 |
| `RuleId` | 规则编号，如 `R-WS-ALL-001` | ✅ 必须 |
| `RowIndex` | 原始行号（用于在 Excel 中精准定位） | ✅ 必须 |
| `Column` | 问题所在列名 | ✅ 必须 |
| `RawValue` | 原始值（原样，不 trim） | ✅ 必须 |
| `Message` | 问题描述 | ✅ 必须 |
| `FixSuggestion` | 修复建议 | ✅ 必须 |
| `RowKey` | 行唯一标识（建议：RowIndex + 关键业务字段） | ✅ 必须 |
| `YearMonth` | 原始行的 YearMonth 列值 | 强烈建议 |
| `Cost Center Number` | 原始行的 Cost Center Number 列值 | 强烈建议 |
| `Function` | 原始行的 Function 列值 | 强烈建议 |
| `Team` | 原始行的 Team 列值 | 强烈建议 |
| `Owner` | 原始行的 Owner 列值 | 强烈建议 |

> **关键规则提示：**
> - **R-WS-ALL-001**：所有列禁止前后空格（Severity: Error）
> - "全空格"视为空值，用于必填项判断
> - `ShoringRatio` 必须严格符合百分号格式
> - `YearMonth`、Option A 均为必填列
