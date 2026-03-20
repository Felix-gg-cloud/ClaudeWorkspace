# AI 输入/输出契约 — Copilot Studio（POC-Validator）

本文档定义 Power Automate 与 Copilot Studio 之间的数据交换格式。  
**PA 负责机械读取，AI 负责所有校验逻辑。**

---

## 1. AI 输入 Payload（PA → Copilot Studio）

### 结构

```json
{
  "meta": {
    "source_file_name": "Master Excel_Strategy_OnOffshoring.xlsx",
    "table_name": "tblOffshoring",
    "generated_at": "2026-03-20T10:00:00Z",
    "batch_id": "202603_001"
  },
  "ruleset_version": "poc-v1",
  "rows": [
    {
      "RowIndex": 1,
      "Cost Center Number": "0123456",
      "Function": "GBS",
      "Team": "EA",
      "Owner": "Felix",
      "YearMonth": "202603",
      "Actual_GBS_TeamMember": "1",
      "ShoringRatio": "12.5%"
    }
  ]
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `meta.source_file_name` | string | ✅ | 输入文件名，用于报告展示 |
| `meta.table_name` | string | ✅ | 固定为 `tblOffshoring` |
| `meta.generated_at` | string (ISO8601) | ✅ | 触发时间（UTC） |
| `meta.batch_id` | string | ✅ | 批次唯一标识，建议 `<YearMonth>_<序号>` |
| `ruleset_version` | string | ✅ | 规则集版本，POC 阶段固定 `poc-v1` |
| `rows[]` | array | ✅ | 从 Excel 读出的原始行，**不做任何清洗** |
| `rows[].RowIndex` | integer | ✅ | 从 1 开始的行号，PA 在组装时加入 |

> ⚠️ **关键原则**：`rows[]` 中每个单元格的值必须是 Excel 原始值，PA 不得做任何 trim、toLower、replace 等操作。前后空格等异常由 AI 识别为 Error。

---

## 2. AI 输出（Copilot Studio → PA）

### 结构

```json
{
  "report_model": {
    "title": "POC Data Validation Report",
    "generated_at": "2026-03-20T10:01:00Z",
    "source_file_name": "Master Excel_Strategy_OnOffshoring.xlsx",
    "table_name": "tblOffshoring",
    "batch_id": "202603_001",
    "metrics": {
      "rows_total": 120,
      "issues_total": 18,
      "errors_total": 15,
      "warnings_total": 3,
      "pass_rate": 0.875
    },
    "top_rules": [
      { "RuleId": "R-WS-ALL-001", "Severity": "Error", "Count": 10 },
      { "RuleId": "R-YM-001",     "Severity": "Error", "Count": 3  }
    ],
    "top_columns": [
      { "Column": "Team",      "Count": 6 },
      { "Column": "YearMonth", "Count": 4 }
    ]
  },
  "issues": [
    {
      "Severity":      "Error",
      "RuleId":        "R-WS-ALL-001",
      "RowKey":        "RowIndex=7|YearMonth=202603|CCN=0123456|Function=GBS|Team=EA ",
      "RowIndex":      7,
      "YearMonth":     "202603",
      "Cost Center Number": "0123456",
      "Function":      "GBS",
      "Team":          "EA ",
      "Owner":         "Felix",
      "Column":        "Team",
      "RawValue":      "EA ",
      "Message":       "该单元格包含前后空格，不允许。",
      "FixSuggestion": "删除该值开头/结尾的空格后重新提交。"
    }
  ]
}
```

### 字段说明

#### `report_model`

| 字段 | 说明 |
|------|------|
| `metrics.rows_total` | 读取的总行数 |
| `metrics.issues_total` | 全部问题总数（Error + Warning） |
| `metrics.errors_total` | Error 级别总数 |
| `metrics.warnings_total` | Warning 级别总数 |
| `metrics.pass_rate` | 无 Error 行占比（0–1） |
| `top_rules[]` | 按命中次数降序，最多 5 条 |
| `top_columns[]` | 按命中次数降序，最多 5 列 |

#### `issues[]`（每条记录）

| 字段 | 说明 |
|------|------|
| `Severity` | `Error` 或 `Warning` |
| `RuleId` | 规则编号（见校验规则清单） |
| `RowKey` | 定位锚点：`RowIndex=N\|YearMonth=...\|CCN=...\|Function=...\|Team=...` |
| `RowIndex` | 行号（与 RowKey 一致，便于 Excel 筛选） |
| `YearMonth` | 该行原始值（raw，不 trim） |
| `Cost Center Number` | 该行原始值 |
| `Function` | 该行原始值 |
| `Team` | 该行原始值 |
| `Owner` | 该行原始值 |
| `Column` | 命中问题的列名 |
| `RawValue` | 命中列的原始值 |
| `Message` | 问题说明（中文，面向业务用户） |
| `FixSuggestion` | 修复建议 |

---

## 3. 校验规则概览（poc-v1）

| RuleId | Severity | 规则描述 |
|--------|----------|----------|
| `R-WS-ALL-001` | Error | **任意列**：单元格值包含前导或尾随空格 |
| `R-YM-001` | Error | `YearMonth` 为空（或仅含空格） |
| `R-YM-002` | Error | `YearMonth` 格式不符合 `YYYYMM`（6 位数字） |
| `R-CCN-001` | Warning | `Cost Center Number` 为空 |
| `R-FN-001` | Warning | `Function` 不在白名单内 |
| `R-NUM-001` | Error | 数值列包含非数字字符（排除 `%` 后缀） |

### 去重策略

若某单元格命中 `R-WS-ALL-001`（空格错误），**不再对该单元格报其他规则错误**，避免同一格多次报错噪音。

---

## 4. Copilot Studio 提示词要点

Copilot Studio System Prompt 中必须明确：

1. **绝对不修改输入值**：接收到的 `RawValue` 是什么就是什么，不做 trim 判断是否合格；判断空格错误用 `value != value.strip()` 逻辑。
2. **输出必须是纯 JSON**：不输出 Markdown、注释或其他文字。
3. **全列空格规则最优先**：先扫全部列的空格问题，再扫其他规则；空格命中后跳过该单元格的其他规则检查。
4. **严格遵循输出 schema**：字段名、类型与本文档定义完全一致。
