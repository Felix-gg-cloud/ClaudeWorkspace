# tblOffshoring AI-Only Validation Rules

> **Mode:** AI-Only Validation (Copilot Studio)  
> **Last Updated:** 2026-03-20  
> **Severity values:** `Error` | `Warning`  
> **Output contract:** `issues[]` + `markdown_report` (see [ai-only-validator prompt](../prompts/ai-only-validator.prompt.md))

---

## 重要说明：RawValue 与 Trim 策略

Power Automate **只做机械读取**，不对任何字段做清洗/trim。  
AI 校验器接收的每个字段值均为 **RawValue（原始值）**，包括用户误输入的前后空格。

| 概念 | 定义 |
|---|---|
| `RawValue` | 从 Excel 单元格读到的原始字符串（含前后空格） |
| `trim(RawValue)` | 去掉前后空格后的值，仅在特定规则中用于逻辑判断 |

---

## 全局规则（适用所有列）

### R-WS-ALL-001 — 禁止前后空格（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-WS-ALL-001` |
| Severity | **Error** |
| 适用列 | **所有列**：Cost Center Number、Function、Team、Owner、YearMonth、所有 Actual_*、Planned_*、Target_*、ShoringRatio |
| 触发条件 | `RawValue is not null AND RawValue != '' AND RawValue != trim(RawValue)` |
| Message | `"{Column}" 列的值 "{RawValue}" 包含前后多余空格，不允许。` |
| FixSuggestion | `去掉 "{Column}" 列值的前后空格，应为 "{trim(RawValue)}"。` |

**执行顺序：** 每列每行 **优先** 检查此规则。若触发 R-WS-ALL-001，仍可（可选）继续对 `trim(RawValue)` 进行其他规则校验以发现额外错误，但应避免重复噪音（建议：若 trim 后仍有其他违规则额外报出；若 trim 后合法则不再重复）。

**注意：**
- 空白字符组成的值（如 `"   "`）在必填性检查中视为空（即 `trim(RawValue) == ''` ⇒ 空值）。
- 此规则不覆盖纯空字符串 `''` 或 null（那属于必填规则范畴）。

---

## 必填规则

### R-REQ-YM-001 — YearMonth 全行必填（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-REQ-YM-001` |
| Severity | **Error** |
| 适用列 | `YearMonth` |
| 触发条件 | `trim(RawValue) == ''`（空字符串、纯空格或 null） |
| Message | `"YearMonth" 列不能为空，所有行均须填写。` |
| FixSuggestion | `填写格式为 YYYYMM 的年月值，例如 202601。` |
| 备注 | 若触发此规则，跳过 R-YM-001 格式校验（避免重复报错） |

### R-REQ-CCN-001 — Cost Center Number 必填（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-REQ-CCN-001` |
| Severity | **Error** |
| 适用列 | `Cost Center Number` |
| 触发条件 | `trim(RawValue) == ''` |
| Message | `"Cost Center Number" 列不能为空。` |
| FixSuggestion | `填写对应的成本中心编号。` |

### R-REQ-FUNC-001 — Function 必填（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-REQ-FUNC-001` |
| Severity | **Error** |
| 适用列 | `Function` |
| 触发条件 | `trim(RawValue) == ''` |
| Message | `"Function" 列不能为空。` |
| FixSuggestion | `填写有效的 Function 名称。` |

### R-REQ-OWN-001 — Owner 在 Total 行必填（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-REQ-OWN-001` |
| Severity | **Error** |
| 适用列 | `Owner` |
| 触发条件 | `rowType == 'Total' AND trim(RawValue) == ''` |
| Message | `Total 行的 "Owner" 列不能为空。` |
| FixSuggestion | `填写负责人姓名。` |

### R-REQ-NT-001 — Non-Total 行必填列（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-REQ-NT-001` |
| Severity | **Error** |
| 适用列 | `Target_YearEnd`、`Target_2030YearEnd`、`ShoringRatio` |
| 触发条件 | `rowType == 'Non-Total' AND trim(RawValue) == ''` |
| Message | `Non-Total 行的 "{Column}" 列不能为空。` |
| FixSuggestion | `填写 "{Column}" 的值。` |

---

## 格式规则

### R-YM-001 — YearMonth 格式校验（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-YM-001` |
| Severity | **Error** |
| 适用列 | `YearMonth` |
| 触发条件 | `trim(RawValue) != '' AND NOT matches(trim(RawValue), /^\d{6}$/) OR month(trim(RawValue)) not in [01..12]` |
| Message | `"YearMonth" 列的值 "{RawValue}" 格式不正确，必须为 YYYYMM（6位数字且月份 01-12）。` |
| FixSuggestion | `将 "YearMonth" 改为合法的 YYYYMM 格式，例如 202601。` |
| 备注 | 若 R-REQ-YM-001 已触发（值为空），则跳过此规则 |

### R-SR-001 — ShoringRatio 格式与范围（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-SR-001` |
| Severity | **Error** |
| 适用列 | `ShoringRatio` |
| 触发条件 | `trim(RawValue) != '' AND NOT matches(trim(RawValue), /^\d+(\.\d+)?%$/) OR numeric_value > 100 OR numeric_value < 0` |
| Message | `"ShoringRatio" 列的值 "{RawValue}" 格式不正确，必须为百分比格式（如 25% 或 25.5%）且在 0-100 之间。` |
| FixSuggestion | `将 "ShoringRatio" 改为合法百分比，例如 25% 或 25.50%，去掉多余空格。` |
| 备注 | 格式判断始终基于 `trim(RawValue)`（R-WS-ALL-001 已单独报告空格问题）；Total 行此列可空 |

---

## 白名单/映射规则

### R-FUNC-001 — Function 白名单（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-FUNC-001` |
| Severity | **Error** |
| 适用列 | `Function` |
| 触发条件 | `trim(RawValue) != '' AND trim(RawValue) not in allowed_functions` |
| Message | `"Function" 列的值 "{RawValue}" 不在允许列表中。` |
| FixSuggestion | `从允许的 Function 列表中选择正确值；注意大小写和空格。` |
| 备注 | 使用 `trim(RawValue)` 匹配白名单；若 RawValue 有空格则 R-WS-ALL-001 已单独报告 |

### R-TEAM-001 — Team 白名单（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-TEAM-001` |
| Severity | **Error** |
| 适用列 | `Team` |
| 触发条件 | `trim(RawValue) != '' AND trim(RawValue) not in allowed_teams` |
| Message | `"Team" 列的值 "{RawValue}" 不在允许列表中。` |
| FixSuggestion | `从允许的 Team 列表中选择正确值；注意大小写和空格。` |

### R-FT-MAP-001 — Function-Team 映射（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-FT-MAP-001` |
| Severity | **Error** |
| 适用列 | `Function` + `Team`（组合） |
| 触发条件 | `trim(Function) != '' AND NOT is_valid_function_team_pair(trim(Function), trim(Team))` |
| Message | `Function "{Function}" 与 Team "{Team}" 的组合不在允许的映射关系中。` |
| FixSuggestion | `检查 Function 与 Team 的对应关系；Total 行的 Team 允许为空，即 (Function, '') 是合法组合。` |
| 备注 | Total 行：`(Function, '')` 是合法组合，必须在映射表中显式允许 |

---

## 数值列规则

### R-NUM-001 — 数值列非负校验（Error）

| 属性 | 值 |
|---|---|
| RuleId | `R-NUM-001` |
| Severity | **Error** |
| 适用列 | 所有 `Actual_*`、`Planned_*`、`Target_*` 列（ShoringRatio 除外） |
| 触发条件 | `trim(RawValue) != '' AND (NOT is_numeric(trim(RawValue)) OR to_number(trim(RawValue)) < 0)` |
| Message | `"{Column}" 列的值 "{RawValue}" 不是有效的非负数字。` |
| FixSuggestion | `"{Column}" 必须填写数字且 ≥ 0，或留空。` |
| 备注 | `trim(RawValue) == ''` 时跳过此规则（空值允许）；若有前后空格则 R-WS-ALL-001 已报告 |

---

## 规则执行顺序（每行每列）

```
对于每一列：
  1. 检查 R-WS-ALL-001（前后空格）
     → 若触发：报 Error；继续下一步（仅对 trim(RawValue) 额外校验，但避免重复噪音）
  2. 检查必填规则（R-REQ-*）
     → 若触发：报 Error；跳过该列后续格式规则
  3. 检查格式规则（R-YM-001 / R-SR-001 等）
  4. 检查白名单/映射规则（R-FUNC-001 / R-TEAM-001 / R-FT-MAP-001）
  5. 检查数值规则（R-NUM-001）
```

---

## 完整 RuleId 清单

| RuleId | 列 | Severity | 说明 |
|---|---|---|---|
| R-WS-ALL-001 | 所有列 | Error | 前后不允许空格 |
| R-REQ-YM-001 | YearMonth | Error | 全行必填 |
| R-REQ-CCN-001 | Cost Center Number | Error | 必填 |
| R-REQ-FUNC-001 | Function | Error | 必填 |
| R-REQ-OWN-001 | Owner | Error | Total 行必填 |
| R-REQ-NT-001 | Target_YearEnd / Target_2030YearEnd / ShoringRatio | Error | Non-Total 行必填 |
| R-YM-001 | YearMonth | Error | YYYYMM 格式 + 月份 01-12 |
| R-SR-001 | ShoringRatio | Error | 百分比格式 + 0-100 范围 |
| R-FUNC-001 | Function | Error | 白名单校验 |
| R-TEAM-001 | Team | Error | 白名单校验 |
| R-FT-MAP-001 | Function + Team | Error | 映射关系校验 |
| R-NUM-001 | Actual_* / Planned_* / Target_* | Error | 非负数字 |
