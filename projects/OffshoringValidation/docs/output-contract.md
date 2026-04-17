# Output Contract — AI Validator Response Schema

## 1. 响应结构

AI 校验器必须返回如下 JSON 格式（**仅此结构，不得包含其他文字**）：

```json
{
  "issues": [ /* Issue 对象数组，见下节 */ ],
  "markdown_report": "...（Markdown 格式报告字符串）..."
}
```

---

## 2. Issue 对象结构

每个 issue 对象必须包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `Severity` | `"Error"` \| `"Warning"` | 仅允许这两种值 |
| `RuleId` | string | 规则 ID，例如 `"R-REQ-YM-001"` |
| `RowKey` | string | 行唯一标识符，格式见 poc-runbook.md §3 |
| `Column` | string | 触发问题的列名，如 `"YearMonth"` |
| `RawValue` | string | 从 Excel 读取的原始值（原样保留，含空格） |
| `Message` | string | 问题描述（面向数据填写人，清晰易懂） |
| `FixSuggestion` | string | 修复建议（具体指导如何修正） |

### 2.1 Issue 示例

```json
{
  "Severity": "Error",
  "RuleId": "R-REQ-YM-001",
  "RowKey": "ROW-5|YM=   |CC=1234567|F=Finance|T=Team A",
  "Column": "YearMonth",
  "RawValue": "   ",
  "Message": "YearMonth 为必填列，当前值为纯空白字符，视为空。",
  "FixSuggestion": "请填入格式为 YYYYMM 的月份值，例如 202501。"
}
```

```json
{
  "Severity": "Error",
  "RuleId": "R-FUNC-001",
  "RowKey": "ROW-12|YM=202501|CC=1234567|F= Finance|T=Team A",
  "Column": "Function",
  "RawValue": " Finance",
  "Message": "Function 值 \" Finance\" 不在允许的白名单中（含前导空格导致不匹配）。",
  "FixSuggestion": "请删除 Function 字段值的多余空格，应为 \"Finance\"。"
}
```

```json
{
  "Severity": "Warning",
  "RuleId": "R-WS-001",
  "RowKey": "ROW-7|YM=202501|CC=2345678|F=Operations|T=Team B",
  "Column": "ShoringRatio",
  "RawValue": " 25% ",
  "Message": "ShoringRatio 值 \" 25% \" 含有前导/尾随空格，建议删除多余空格。",
  "FixSuggestion": "请将值改为 \"25%\"，删除前后多余空格。"
}
```

```json
{
  "Severity": "Error",
  "RuleId": "R-NUM-001",
  "RowKey": "ROW-9|YM=202501|CC=3456789|F=IT|T=Team C",
  "Column": "Actual_GBS_TeamMember",
  "RawValue": " 10 ",
  "Message": "Actual_GBS_TeamMember 值 \" 10 \" 含有空格，无法解析为有效数字。",
  "FixSuggestion": "请删除数字两侧的多余空格，应为 \"10\"。"
}
```

---

## 3. markdown_report 结构

`markdown_report` 字段是一段 Markdown 字符串，供人类阅读。其内容结构如下：

```markdown
# Offshoring 数据质量校验报告

**校验时间**：{ISO 8601 时间戳}  
**数据月份**：{YearMonth 或"多月"}  
**校验行数**：{N} 行  
**发现问题**：Error {n} 条，Warning {m} 条

---

## 汇总

| 规则 ID | 规则描述 | Error | Warning |
|---------|---------|-------|---------|
| R-REQ-YM-001 | YearMonth 必填（全行） | 2 | 0 |
| R-FUNC-001 | Function 白名单 | 3 | 0 |
| R-WS-001 | 前后空格警告 | 0 | 5 |
| ... | ... | ... | ... |

---

## Top Issues 明细

### Errors

1. **[R-REQ-YM-001]** ROW-5 — YearMonth 为空白字符，视为空。建议填入 202501。
2. ...

### Warnings

1. **[R-WS-001]** ROW-7 — ShoringRatio " 25% " 含空格，建议删除。
2. ...

---

## 修复优先级建议

1. 优先修复所有 Error（共 {n} 条），尤其是必填字段缺失。
2. 检查并修复 Warning 中的空格问题（共 {m} 条），以避免白名单/映射不匹配。
```

---

## 4. 样例 Payload（Power Automate → AI）

Power Automate 发送给 AI 校验器的 user message 格式如下：

```json
{
  "rows": [
    {
      "RowKey": "ROW-1|YM=202501|CC=1234567|F=Finance|T=Team A",
      "RowIndex": 1,
      "YearMonth": "202501",
      "Owner": "John Doe",
      "CostCenterNumber": "1234567",
      "Function": "Finance",
      "Team": "Team A",
      "Actual_GBS_TeamMember": "5",
      "Actual_GBS_TeamLeaderAM": "1",
      "Actual_EA": "2",
      "Actual_HKT": "0",
      "Planned_GBS_TeamMember": "6",
      "Planned_GBS_TeamLeaderAM": "1",
      "Planned_EA": "2",
      "Planned_HKT": "0",
      "Target_YearEnd": "8",
      "Target_2030YearEnd": "10",
      "ShoringRatio": "25%"
    },
    {
      "RowKey": "ROW-2|YM=   |CC=1234567|F= Finance|T=Team A",
      "RowIndex": 2,
      "YearMonth": "   ",
      "Owner": "Jane Smith",
      "CostCenterNumber": "1234567",
      "Function": " Finance",
      "Team": "Team A",
      "Actual_GBS_TeamMember": " 10 ",
      "Actual_GBS_TeamLeaderAM": "",
      "Actual_EA": "",
      "Actual_HKT": "",
      "Planned_GBS_TeamMember": "",
      "Planned_GBS_TeamLeaderAM": "",
      "Planned_EA": "",
      "Planned_HKT": "",
      "Target_YearEnd": "8",
      "Target_2030YearEnd": "10",
      "ShoringRatio": " 25% "
    }
  ]
}
```

> ⚠️ **注意**：所有字段值均为原始值（raw values），Power Automate 不做任何 trim 或清洗。

---

## 5. 样例响应（AI → Power Automate）

```json
{
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "R-REQ-YM-001",
      "RowKey": "ROW-2|YM=   |CC=1234567|F= Finance|T=Team A",
      "Column": "YearMonth",
      "RawValue": "   ",
      "Message": "YearMonth 为必填列，当前值为纯空白字符，视为空。",
      "FixSuggestion": "请填入格式为 YYYYMM 的月份值，例如 202501。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-FUNC-001",
      "RowKey": "ROW-2|YM=   |CC=1234567|F= Finance|T=Team A",
      "Column": "Function",
      "RawValue": " Finance",
      "Message": "Function 值 \" Finance\" 不在允许的白名单中（含前导空格导致不匹配）。",
      "FixSuggestion": "请删除 Function 字段值的多余空格，应为 \"Finance\"。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-MAP-001",
      "RowKey": "ROW-2|YM=   |CC=1234567|F= Finance|T=Team A",
      "Column": "Function+Team",
      "RawValue": "Function=\" Finance\", Team=\"Team A\"",
      "Message": "Function-Team 组合 (\" Finance\", \"Team A\") 不在允许的映射表中。",
      "FixSuggestion": "请先修正 Function 字段的多余空格（应为 \"Finance\"），再确认 (Finance, Team A) 是否在允许的映射中。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-NUM-001",
      "RowKey": "ROW-2|YM=   |CC=1234567|F= Finance|T=Team A",
      "Column": "Actual_GBS_TeamMember",
      "RawValue": " 10 ",
      "Message": "Actual_GBS_TeamMember 值 \" 10 \" 含有空格，无法解析为有效数字。",
      "FixSuggestion": "请删除数字两侧的多余空格，应为 \"10\"。"
    },
    {
      "Severity": "Warning",
      "RuleId": "R-WS-001",
      "RowKey": "ROW-2|YM=   |CC=1234567|F= Finance|T=Team A",
      "Column": "ShoringRatio",
      "RawValue": " 25% ",
      "Message": "ShoringRatio 值 \" 25% \" 含有前导/尾随空格；格式本身有效（25%，在 0-100 范围内），但建议删除多余空格。",
      "FixSuggestion": "请将值改为 \"25%\"，删除前后多余空格。"
    }
  ],
  "markdown_report": "# Offshoring 数据质量校验报告\n\n**校验行数**：2 行  \n**发现问题**：Error 4 条，Warning 1 条\n\n## Errors\n1. **[R-REQ-YM-001]** ROW-2 — YearMonth 为空白字符，视为空。\n2. **[R-FUNC-001]** ROW-2 — Function \" Finance\" 含前导空格，不匹配白名单。\n3. **[R-MAP-001]** ROW-2 — Function-Team 组合因空格问题无法匹配映射。\n4. **[R-NUM-001]** ROW-2 — Actual_GBS_TeamMember \" 10 \" 含空格，非有效数字。\n\n## Warnings\n1. **[R-WS-001]** ROW-2 — ShoringRatio \" 25% \" 有前后空格，建议删除。\n"
}
```

---

## 6. 规则 ID 速查表

| RuleId | Severity | 触发条件 | 列 |
|--------|----------|---------|-----|
| R-REQ-YM-001 | Error | YearMonth 为空或纯空白（所有行） | YearMonth |
| R-YM-001 | Error | YearMonth 非空但不符合 YYYYMM 格式或月份非 01-12 | YearMonth |
| R-REQ-TOTAL-OWNER | Error | Total 行 Owner 为空或纯空白 | Owner |
| R-REQ-NT-TGT1 | Error | Non-Total 行 Target_YearEnd 为空或纯空白 | Target_YearEnd |
| R-REQ-NT-TGT2 | Error | Non-Total 行 Target_2030YearEnd 为空或纯空白 | Target_2030YearEnd |
| R-REQ-NT-SR | Error | Non-Total 行 ShoringRatio 为空或纯空白 | ShoringRatio |
| R-CCN-001 | Error | CostCenterNumber 非空但不是 7 位纯数字 | CostCenterNumber |
| R-FUNC-001 | Error | Function 非空但不在白名单（精确原始字符串比对） | Function |
| R-TEAM-001 | Warning | Team 非空但不在 Team 白名单（精确原始字符串比对） | Team |
| R-MAP-001 | Error | Function-Team 组合不在允许映射表（精确原始字符串比对） | Function+Team |
| R-NUM-001 | Error | 数值列非空但无法解析为数字（含含空格的数字字符串） | 10 个数值列 |
| R-NUM-002 | Error | 数值列非空、可解析为数字，但值 < 0 | 10 个数值列 |
| R-SR-001 | Error | ShoringRatio 非空，trim 后不匹配 `^\d+(\.\d+)?%$` 或数值不在 0-100 | ShoringRatio |
| R-WS-001 | Warning | ShoringRatio 格式有效但整体值含前导或尾随空格 | ShoringRatio |
