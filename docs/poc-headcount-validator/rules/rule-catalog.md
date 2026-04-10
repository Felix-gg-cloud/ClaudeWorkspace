# POC Headcount Excel Validator — Rule Catalog

> **File**: `tblOffshoring` (Sheet: Offshoring, Excel Table name: `tblOffshoring`)
> **Columns**: Cost Center Number, Function, Team, Owner, YearMonth, Actual_GBS_TeamMember, Actual_GBS_TeamLeaderAM, Actual_EA, Actual_HKT, Planned_GBS_TeamMember, Planned_GBS_TeamLeaderAM, Planned_EA, Planned_HKT, Target_YearEnd, Target_2030YearEnd, ShoringRatio

---

## Severity 定义

| Severity | 含义 |
|----------|------|
| **Error** (Hard) | 必须修正，数据无效，应拒绝接受 |
| **Warning** (Soft) | 建议修正，数据存疑，可接受但需人工确认 |

---

## 规则清单

### R-SR-001 · ShoringRatio 格式校验 ⚠️ Hard Rule

| 属性 | 值 |
|------|-----|
| Rule ID | `R-SR-001` |
| Severity | **Error（硬规则）** |
| 列名 | `ShoringRatio` |
| 状态 | 激活 |

**规则说明**

`ShoringRatio` 列必须使用百分比格式，即：**整数 0～100 后跟 `%` 符号**。

- ✅ 允许：`0%`、`23%`、`100%`、`  50%  `（首尾空格可接受，校验时自动 trim）
- ❌ 拒绝：`0.23`（小数/比率格式）
- ❌ 拒绝：`23`（纯数字，缺少 `%`）
- ❌ 拒绝：`23.5%`（非整数）
- ❌ 拒绝：`101%`（超出范围）
- ❌ 拒绝：空字符串或 null

**正则表达式（trim 后匹配）**

```
^(100|[1-9][0-9]|[0-9])%$
```

等价写法（含首尾可选空白）：

```
^\s*(100|[1-9][0-9]|[0-9])%\s*$
```

**错误消息**

```
ShoringRatio 必须使用百分比格式，例如 23%（0%~100%）
```

**修正建议（FixSuggestion）**

```
请将 ShoringRatio 的值改为整数加 % 的格式，例如将 "0.23" 改为 "23%"，将 "23" 改为 "23%"。有效范围为 0%～100%。
```

---

### R-CC-001 · Cost Center Number 格式校验（Hard）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-CC-001` |
| Severity | **Error（硬规则）** |
| 列名 | `Cost Center Number` |

**规则说明**：必须为 7 位纯数字，不含空格与特殊字符。

**正则表达式**：`^\d{7}$`

**错误消息**：`Cost Center Number 必须为 7 位纯数字，例如 1234567`

---

### R-YM-001 · YearMonth 格式校验（Hard）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-YM-001` |
| Severity | **Error（硬规则）** |
| 列名 | `YearMonth` |

**规则说明**：必须为 `YYYYMM` 格式，年份合理（2000–2099），月份 01–12。

**正则表达式**：`^20[0-9]{2}(0[1-9]|1[0-2])$`

**错误消息**：`YearMonth 必须为 YYYYMM 格式，例如 202501`

---

### R-FN-001 · Function 白名单校验（Hard）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-FN-001` |
| Severity | **Error（硬规则）** |
| 列名 | `Function` |

**规则说明**：值必须在已批准的 Function 白名单中（大小写敏感）。

**错误消息**：`Function 值不在允许范围内，请核对白名单`

---

### R-TM-001 · Team 白名单校验（Hard）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-TM-001` |
| Severity | **Error（硬规则）** |
| 列名 | `Team` |

**规则说明**：值必须在已批准的 Team 白名单中。

**错误消息**：`Team 值不在允许范围内，请核对白名单`

---

### R-OW-001 · Owner 非空校验（Hard）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-OW-001` |
| Severity | **Error（硬规则）** |
| 列名 | `Owner` |

**规则说明**：`Owner` 不得为空。

**错误消息**：`Owner 不能为空`

---

### R-NV-001 · 数值列非负校验（Soft）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-NV-001` |
| Severity | **Warning（软规则）** |
| 列名 | Actual_\* / Planned_\* / Target_\* 各数值列 |

**规则说明**：所有数值列（Actual_GBS_TeamMember 等）必须为数字且 ≥ 0。

**错误消息**：`{列名} 必须为非负数字`

---

### R-FT-001 · Function-Team 组合映射校验（Soft）

| 属性 | 值 |
|------|-----|
| Rule ID | `R-FT-001` |
| Severity | **Warning（软规则）** |
| 列名 | Function + Team |

**规则说明**：Function 与 Team 的组合必须在已批准的映射表中，否则视为异常组合。

**错误消息**：`Function "{Function}" 与 Team "{Team}" 的组合不在已知映射中，请确认`

---

## 规则汇总表

| Rule ID | Column | Severity | 说明 |
|---------|--------|----------|------|
| R-SR-001 | ShoringRatio | **Error** | 必须为整数百分比格式，如 `23%`（0%–100%） |
| R-CC-001 | Cost Center Number | **Error** | 7 位纯数字 |
| R-YM-001 | YearMonth | **Error** | YYYYMM 格式，月 01–12 |
| R-FN-001 | Function | **Error** | 白名单校验 |
| R-TM-001 | Team | **Error** | 白名单校验 |
| R-OW-001 | Owner | **Error** | 不得为空 |
| R-NV-001 | Actual_*/Planned_*/Target_* | **Warning** | 非负数字 |
| R-FT-001 | Function + Team | **Warning** | 合法组合映射 |
