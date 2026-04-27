# POC Headcount Validator — 校验规则汇总

> 本文档定义 AI（Copilot Studio）在校验 `tblOffshoring` 数据时必须强制执行的所有规则。
>
> **规则执行原则**：所有列的原始值（RawValue）原样传入，AI **不做任何 trim/清洗**后再校验。
>
> **输出原则**：只输出 `Error` 和 `Warning`，不输出 `Info`。

---

## 目录

1. [全局规则](#1-全局规则)
2. [必填规则](#2-必填规则)
3. [格式与枚举规则](#3-格式与枚举规则)
4. [数值规则](#4-数值规则)
5. [Function-Team 映射规则](#5-function-team-映射规则)
6. [ShoringRatio 规则](#6-shoringratio-规则)
7. [规则执行顺序与去重策略](#7-规则执行顺序与去重策略)
8. [规则 ID 速查表](#8-规则-id-速查表)

---

## 1. 全局规则

### R-WS-ALL-001 — 前后空格禁止（Error）

**适用列**：`tblOffshoring` 的**所有列**，包括但不限于：
`YearMonth`、`Cost Center Number`、`Function`、`Team`、`Owner`、`ShoringRatio`、
`Actual Onshore`、`Actual Offshore`、`Actual Total`、
`Planned Onshore`、`Planned Offshore`、`Planned Total`、
`Target Onshore`、`Target Offshore`、`Target Total`

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-WS-ALL-001` |
| **触发条件** | `RawValue` 非空（非 null、非空字符串）且 `RawValue ≠ trim(RawValue)` |
| **Message** | `列 {Column} 的值包含前后空格，不允许。` |
| **FixSuggestion** | `删除该值开头/结尾的空格后重新提交。` |

**空白字符定义**：前后空格包括普通空格（U+0020）、Tab（U+0009）、全角空格（U+3000）等所有 Unicode 空白字符。

**去重策略**：若某列已触发 `R-WS-ALL-001`，则该列的其它规则（格式、枚举、数值解析等）**不再叠加报错**，只保留此条 Error，以减少噪音。

**"空白字符组成"与空值的关系**：

- 若 `RawValue` 仅由空白字符组成（即 `trim(RawValue) == ""`），则：
  - **不触发** `R-WS-ALL-001`（不视为"有前后空格的非空值"）
  - **视为空值**，按必填规则判断是否触发 Error

---

## 2. 必填规则

### R-REQ-001 — YearMonth 必填（Error）

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-REQ-001` |
| **适用列** | `YearMonth` |
| **触发条件** | `trim(RawValue) == ""`（null、空字符串、纯空白字符均视为空） |
| **Message** | `YearMonth 为必填列，不能为空或纯空格。` |
| **FixSuggestion** | `填写格式为 YYYYMM 的年月值，例如 202501。` |

> **所有行均需填写 YearMonth**，包括 Total 行和非 Total 行。

### R-REQ-002 — Option A 必填列（Error）

Option A 指以下三列：`Function`、`Team`、`Cost Center Number`

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-REQ-002` |
| **适用列** | `Function`、`Team`、`Cost Center Number` |
| **适用行** | 非 Total 行（即 `Function ≠ "Total"`） |
| **触发条件** | `trim(RawValue) == ""` |
| **Message** | `列 {Column} 为非 Total 行的必填列，不能为空或纯空格。` |
| **FixSuggestion** | `请填写 {Column} 的值。` |

### R-REQ-003 — Total 行 Owner 必填（Error）

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-REQ-003` |
| **适用列** | `Owner` |
| **适用行** | Total 行（`Function == "Total"`） |
| **触发条件** | `trim(RawValue) == ""` |
| **Message** | `Total 行的 Owner 为必填，不能为空或纯空格。` |
| **FixSuggestion** | `请填写 Total 行的 Owner 姓名或工号。` |

---

## 3. 格式与枚举规则

### R-FMT-001 — YearMonth 格式校验（Error）

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-FMT-001` |
| **适用列** | `YearMonth` |
| **前置条件** | `trim(RawValue) ≠ ""`（非空）且未触发 `R-WS-ALL-001` |
| **触发条件** | `RawValue` 不匹配 `^\d{6}$`（即不是 6 位纯数字），或月份部分不在 01-12 范围内 |
| **Message** | `YearMonth 格式错误，当前值为 "{RawValue}"，应为 YYYYMM（6 位数字，月份 01-12）。` |
| **FixSuggestion** | `将值改为 YYYYMM 格式，例如 202501。` |

### R-ENUM-001 — Function 枚举校验（Error）

**Function 白名单**（精确匹配，**区分大小写**，**不做 trim**）：

```
GBS, IT, Finance, HR, Legal, Compliance, Operations, Risk, Total
```

> 若需完整白名单，请联系业务方更新此文档。

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-ENUM-001` |
| **适用列** | `Function` |
| **前置条件** | `trim(RawValue) ≠ ""`（非空）且未触发 `R-WS-ALL-001` |
| **触发条件** | `RawValue` 不在 Function 白名单中 |
| **Message** | `Function 值 "{RawValue}" 不在允许列表中。` |
| **FixSuggestion** | `请使用允许的 Function 值之一（精确匹配，区分大小写）。` |

---

## 4. 数值规则

**适用列**：`Actual Onshore`、`Actual Offshore`、`Actual Total`、`Planned Onshore`、`Planned Offshore`、`Planned Total`、`Target Onshore`、`Target Offshore`、`Target Total`

### R-NUM-001 — 数值列：空则跳过（无规则触发）

- 若 `trim(RawValue) == ""`（null、空字符串、纯空白），则**跳过所有数值校验**，不报错。

### R-NUM-002 — 数值列：非空必须为有效数字（Error）

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-NUM-002` |
| **前置条件** | `trim(RawValue) ≠ ""`（非空）且**未触发** `R-WS-ALL-001` |
| **触发条件** | `RawValue` 无法解析为有效数字（含整数和小数） |
| **Message** | `列 {Column} 的值 "{RawValue}" 不是有效数字。` |
| **FixSuggestion** | `请填写数字（整数或小数），或留空跳过校验。` |

> **注意**：若已触发 `R-WS-ALL-001`（存在前后空格），则不再叠加报 `R-NUM-002`。

### R-NUM-003 — 数值列：必须 ≥ 0（Error）

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-NUM-003` |
| **前置条件** | `RawValue` 已通过 `R-NUM-002`（成功解析为数字） |
| **触发条件** | 解析后的数值 `< 0` |
| **Message** | `列 {Column} 的值 "{RawValue}" 不能为负数。` |
| **FixSuggestion** | `请填写 0 或正数。` |

---

## 5. Function-Team 映射规则

### R-MAP-001 — Function-Team 合法性校验（Error）

**精确匹配**（区分大小写，不做 trim），下表定义每个 Function 允许的 Team 列表：

| Function | 允许的 Team 值（精确匹配） |
|----------|--------------------------|
| `Total` | `Total`、`""`（空字符串，即 Total 行的 Team 可为空） |
| `GBS` | `EA`、`APAC`、`EMEA`、`Americas`、`Total` |
| `IT` | `Infrastructure`、`Application`、`Security`、`Total` |
| `Finance` | `Controllership`、`FP&A`、`Treasury`、`Total` |
| `HR` | `HRBP`、`C&B`、`Talent`、`Total` |
| _(其余 Function)_ | 由业务方补充白名单后更新此表 |

> **Total 行特殊处理**：`Function = "Total"` 时，`Team` 允许为空或为 `"Total"`（精确匹配）。

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-MAP-001` |
| **适用列** | `Team` |
| **前置条件** | `Function` 未触发 `R-WS-ALL-001`；`Team` 未触发 `R-WS-ALL-001` |
| **触发条件** | `Team` 值不在当前行 `Function` 对应的允许列表中 |
| **Message** | `Function="{Function}" 下，Team="{RawValue}" 不是允许的组合。` |
| **FixSuggestion** | `请检查 Function-Team 映射表，使用允许的 Team 值（精确匹配）。` |

---

## 6. ShoringRatio 规则

### R-SR-001 — ShoringRatio 格式与范围校验（Error）

| 属性 | 值 |
|------|----|
| **Severity** | `Error` |
| **RuleId** | `R-SR-001` |
| **适用列** | `ShoringRatio` |
| **前置条件** | `trim(RawValue) ≠ ""`（非空）且未触发 `R-WS-ALL-001` |
| **触发条件（任一）** | 1. `RawValue` 不匹配 `^\d+(\.\d+)?%$`（必须以 `%` 结尾，小数点前至少一位数字，例如 `0.5%` / `12%` / `100%` 均合法，`.5%` 不合法） / 2. 数值部分（去掉 `%`）不在 `[0, 100]` 范围内 |
| **Message** | `ShoringRatio 值 "{RawValue}" 格式错误或超出 0%~100% 范围。` |
| **FixSuggestion** | `请使用 "12.5%" 格式，数值在 0%~100% 之间。` |

> **注意**：`ShoringRatio` **同样受全局规则 R-WS-ALL-001 约束**，前后空格即 Error。早期版本曾允许 `" 12.5% "` 格式，现已**废止**。

---

## 7. 规则执行顺序与去重策略

### 执行顺序（按优先级）

```
1. R-WS-ALL-001  ← 最优先；触发后，同列其它规则不再执行
2. R-REQ-001 / R-REQ-002 / R-REQ-003  ← 必填检查（在空白检查通过后）
3. R-FMT-001 / R-ENUM-001  ← 格式与枚举（在必填通过后）
4. R-MAP-001  ← Function-Team 映射（在枚举通过后）
5. R-NUM-001 / R-NUM-002 / R-NUM-003  ← 数值（在空白检查通过后）
6. R-SR-001  ← ShoringRatio（在空白检查通过后）
```

### 去重策略

- 同一 `(RowIndex, Column)` 组合，**只保留优先级最高的一条规则**报错。
- 同一行的不同列可以各自独立报错（不跨列去重）。

---

## 8. 规则 ID 速查表

| 规则 ID | 类别 | Severity | 说明 |
|---------|------|----------|------|
| `R-WS-ALL-001` | 全局 | Error | 所有列前后空格禁止 |
| `R-REQ-001` | 必填 | Error | YearMonth 必填 |
| `R-REQ-002` | 必填 | Error | 非 Total 行 Function/Team/CCN 必填 |
| `R-REQ-003` | 必填 | Error | Total 行 Owner 必填 |
| `R-FMT-001` | 格式 | Error | YearMonth 格式（YYYYMM） |
| `R-ENUM-001` | 枚举 | Error | Function 白名单校验 |
| `R-MAP-001` | 映射 | Error | Function-Team 组合合法性 |
| `R-NUM-001` | 数值 | —（跳过） | 数值列空值跳过 |
| `R-NUM-002` | 数值 | Error | 数值列非空必须为有效数字 |
| `R-NUM-003` | 数值 | Error | 数值列必须 ≥ 0 |
| `R-SR-001` | 格式 | Error | ShoringRatio 格式与范围 |
