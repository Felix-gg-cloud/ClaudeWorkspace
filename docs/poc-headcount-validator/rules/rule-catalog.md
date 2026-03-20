# POC Headcount Excel Validator — Rule Catalog

**Table:** `tblOffshoring`  
**Version:** 2.0 (YearMonth universally required for all rows)  
**Last Updated:** 2026-03-20

---

## 0) 行类型定义（Row Type Definitions）

| 行类型 | 判定条件 |
|--------|----------|
| **Total 行** | `Function` ∈ `{Total (All), Total (Core Operations)}` |
| **Non-Total 行** | 除上述以外的所有 `Function` 值 |

> 判定前对 `Function` 值做 trim（去除首尾空格）。

---

## 1) 必填规则（Requiredness Rules）—— Option A

### 1.1 所有行（All Rows）必填

| RuleId | Severity | Column | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|---------|---------------|
| R-REQ-ALL-YM | Error | YearMonth | `trim(YearMonth) != ""` | `YearMonth 不允许为空，所有行（包括 Total 行）均须填写。` | `填写 YearMonth，格式为 YYYYMM（例如 202501）。` |

> **说明**：YearMonth 不论行类型，均为必填项。

### 1.2 Total 行专属必填

| RuleId | Severity | Column | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|---------|---------------|
| R-REQ-TOTAL-OWNER | Error | Owner | isTotal = true 且 `trim(Owner) == ""` | `Total 行必须填写 Owner。` | `补充 Owner 后重新提交。` |

> Total 行其他列（Cost Center Number、Team、所有 Actual/Planned 数值列、Target\_YearEnd、Target\_2030YearEnd、ShoringRatio）允许为空。  
> 若 ShoringRatio 不为空，仍需满足 R-SR-001。

### 1.3 Non-Total 行专属必填

| RuleId | Severity | Column | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|---------|---------------|
| R-REQ-NT-TGT1 | Error | Target\_YearEnd | isTotal = false 且 `trim(Target_YearEnd) == ""` | `非 Total 行 Target_YearEnd 不允许为空。` | `填写 Target_YearEnd（≥ 0 的数字）。` |
| R-REQ-NT-TGT2 | Error | Target\_2030YearEnd | isTotal = false 且 `trim(Target_2030YearEnd) == ""` | `非 Total 行 Target_2030YearEnd 不允许为空。` | `填写 Target_2030YearEnd（≥ 0 的数字）。` |
| R-REQ-NT-SR | Error | ShoringRatio | isTotal = false 且 `trim(ShoringRatio) == ""` | `非 Total 行 ShoringRatio 不允许为空。` | `填写 ShoringRatio（例如 23%）。` |

---

## 2) 格式 / 枚举规则（Format & Enumeration Rules）

### 2.1 YearMonth 格式

| RuleId | Severity | Column | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|----------|---------|---------------|
| R-YM-001 | Error | YearMonth | YearMonth 非空（经 R-REQ-ALL-YM 保证，故所有行均校验） | 正则 `^\d{6}$` 且月份部分（第 5-6 位）在 `01`–`12` 之间 | `YearMonth 格式错误，必须为 YYYYMM（例如 202501），月份须为 01-12。` | `将 YearMonth 改为 6 位数字年月格式。` |

> 执行顺序：先 R-REQ-ALL-YM（空检查），若为空则跳过 R-YM-001（避免重复报错）。

### 2.2 Cost Center Number 格式

| RuleId | Severity | Column | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|----------|---------|---------------|
| R-CCN-001 | Error | Cost Center Number | 非空 | trim 后匹配 `^\d{7}$` | `Cost Center Number 必须为 7 位纯数字（例如 1234567）。` | `检查 Cost Center Number 是否填写正确。` |

### 2.3 Function 白名单

| RuleId | Severity | Column | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|----------|---------|---------------|
| R-FUNC-001 | Error | Function | 非空 | trim 后必须在 Function 白名单内 | `Function 值不在允许列表中，请检查填写是否正确。` | `从下拉列表中选择有效的 Function 值。` |

**Function 白名单**（含 Total 行）：
- `Total (All)`
- `Total (Core Operations)`
- 以及所有业务 Function 值（按实际数据字典填入，共约 16 个）

### 2.4 Team 白名单

| RuleId | Severity | Column | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|----------|---------|---------------|
| R-TEAM-001 | Warning | Team | 非空 | trim 后必须在 Team 白名单内 | `Team 值不在已知列表中，请确认拼写是否正确。` | `从下拉列表中选择有效的 Team 值，或联系管理员添加新 Team。` |

> 设为 Warning 是因为 Team 名称变化较频繁，POC 阶段避免误报导致刷屏。

---

## 3) Function-Team 组合映射规则

| RuleId | Severity | Column | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|--------|----------|----------|---------|---------------|
| R-MAP-001 | Error | Team | Function 和 Team 均已通过白名单校验（或已确定值） | `(trim(Function), trim(Team))` 组合必须在映射表中 | `Function 与 Team 组合不合法，请检查两者的对应关系。` | `参考 Function-Team 映射表，确保 Function 与 Team 配对正确。` |

**允许的特殊组合（含 Total 行 + 空 Team）：**

| Function | Team |
|----------|------|
| `Total (All)` | `""` (空) |
| `Total (Core Operations)` | `""` (空) |
| 其他业务 Function | 对应 Team（按实际映射表） |

> 实现时：将空 Team 统一标准化为 `""`（trim 后为空即视为 `""`），防止空格导致匹配失败。

---

## 4) 数值列规则（Numeric Column Rules）

适用列（共 10 列）：

| 列名 |
|------|
| `Actual_GBS_TeamMember` |
| `Actual_GBS_TeamLeaderAM` |
| `Actual_EA` |
| `Actual_HKT` |
| `Planned_GBS_TeamMember` |
| `Planned_GBS_TeamLeaderAM` |
| `Planned_EA` |
| `Planned_HKT` |
| `Target_YearEnd` |
| `Target_2030YearEnd` |

| RuleId | Severity | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|----------|----------|---------|---------------|
| R-NUM-001 | Error | 列值非空 | trim 后能解析为数字（isNumber 为 true） | `{Column} 必须为数字，不能含有文字或特殊字符。` | `将 {Column} 改为有效数字（例如 10 或 0）。` |
| R-NUM-002 | Error | 列值非空且通过 R-NUM-001 | 数值 ≥ 0 | `{Column} 不能为负数。` | `将 {Column} 改为 0 或正数。` |

> 空值不触发 R-NUM-001 / R-NUM-002。  
> Non-Total 行的 `Target_YearEnd` 和 `Target_2030YearEnd` 同时受必填规则（R-REQ-NT-TGT1/TGT2）约束，非空后会自动触发数值校验。

---

## 5) ShoringRatio 格式规则

| RuleId | Severity | 适用条件 | 校验逻辑 | Message | FixSuggestion |
|--------|----------|----------|----------|---------|---------------|
| R-SR-001 | Error | ShoringRatio 非空（Total 行：若非空则校验；Non-Total 行：已必填，故必校验） | 1. `v = trim(ShoringRatio)` 2. 正则：`^\d+(\.\d+)?%$` 3. 去掉 `%` 后数值 n，校验 `0 ≤ n ≤ 100` | `ShoringRatio 必须使用百分比格式，例如 23%（范围 0%–100%，允许小数如 12.5%）。` | `改为"数字+%"格式，不要填写 0.23 或 23。` |

**合法示例：** `0%`, `100%`, `12.5%`, ` 12.5% `（有空格会被 trim）  
**非法示例：** `12.5`（无 %）、`0.23`（无 %）、`101%`（超范围）、`-1%`（负数）

---

## 6) 规则执行顺序建议（Flow Execution Order）

为避免同一单元格产生重复报错，建议在 Power Automate 按以下顺序执行：

```
For each row in tblOffshoring:
  1. 计算 isTotal（trim Function 后判断）
  2. 必填检查（Requiredness）：
     a. R-REQ-ALL-YM       ← YearMonth 必填（所有行）
     b. R-REQ-TOTAL-OWNER  ← Owner 必填（仅 Total 行）
     c. R-REQ-NT-TGT1/TGT2/SR ← 三列必填（仅 Non-Total 行）
  3. 格式/枚举检查（仅对非空列执行）：
     a. R-YM-001   ← YearMonth 格式（若步骤 2a 未报错，即非空，则执行）
     b. R-CCN-001  ← Cost Center Number 格式
     c. R-FUNC-001 ← Function 白名单
     d. R-TEAM-001 ← Team 白名单
  4. 组合映射检查：
     a. R-MAP-001  ← Function-Team 组合
  5. 数值列检查（仅对非空列执行）：
     a. R-NUM-001 → R-NUM-002（顺序执行，NUM-001 失败则跳过 NUM-002）
  6. ShoringRatio 格式检查：
     a. R-SR-001（若 ShoringRatio 非空则执行；Non-Total 行必填，故必执行）
```

> **关键原则**：若某列已触发"必填为空"报错，则跳过该列的格式校验（避免重复噪音）。  
> **YearMonth 特别说明**：R-REQ-ALL-YM（必填）→ 若空则跳过 R-YM-001（格式）。

---

## 7) Issue 记录字段结构（供报告输出）

每条校验问题记录以下字段：

| 字段 | 说明 |
|------|------|
| `Severity` | `Error` / `Warning` |
| `RuleId` | 规则编号（如 `R-REQ-ALL-YM`） |
| `YearMonth` | 该行的 YearMonth 值 |
| `CostCenterNumber` | 该行的 Cost Center Number |
| `Function` | 该行的 Function 值 |
| `Team` | 该行的 Team 值 |
| `Column` | 出问题的列名 |
| `Value` | 出问题的列值（原始值） |
| `Message` | 问题描述 |
| `FixSuggestion` | 修复建议 |
