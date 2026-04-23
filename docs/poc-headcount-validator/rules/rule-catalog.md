# POC Headcount Validator — Rule Catalog (Option A)

## Table: `tblOffshoring`
**Columns (16 total):**

| Category | Column |
|---|---|
| Dimension | Cost Center Number, Function, Team, Owner, YearMonth |
| Numeric | Actual_GBS_TeamMember, Actual_GBS_TeamLeaderAM, Actual_EA, Actual_HKT, Planned_GBS_TeamMember, Planned_GBS_TeamLeaderAM, Planned_EA, Planned_HKT, Target_YearEnd, Target_2030YearEnd |
| Ratio | ShoringRatio |

---

## Row-Type Classification

| Row Type | Condition | Description |
|---|---|---|
| **Total** | `Function` ∈ `{'Total (All)', 'Total (Core Operations)'}` | Aggregation / summary rows |
| **Non-Total** | `Function` NOT in the above set | Detail / operational rows |

---

## Option A — Requiredness Matrix

| Column | Non-Total Row | Total Row |
|---|---|---|
| Cost Center Number | Optional | Optional |
| Function | Optional | Required (identity determines row type) |
| Team | Optional | Optional |
| Owner | Optional | **Required non-empty** |
| YearMonth | Optional | **Required non-empty** |
| Actual_GBS_TeamMember | Optional | Optional |
| Actual_GBS_TeamLeaderAM | Optional | Optional |
| Actual_EA | Optional | Optional |
| Actual_HKT | Optional | Optional |
| Planned_GBS_TeamMember | Optional | Optional |
| Planned_GBS_TeamLeaderAM | Optional | Optional |
| Planned_EA | Optional | Optional |
| Planned_HKT | Optional | Optional |
| **Target_YearEnd** | **Required non-empty** | Optional |
| **Target_2030YearEnd** | **Required non-empty** | Optional |
| **ShoringRatio** | **Required non-empty + R-SR-001** | Optional (if present, R-SR-001 applies) |

---

## Rule Catalog

### R-YM-001 — YearMonth must be YYYYMM (Error)

| Attribute | Value |
|---|---|
| RuleId | R-YM-001 |
| Severity | **Error** |
| Applies To | All rows where `YearMonth` is not empty |
| Condition | Length = 6, all digits, month part (last 2) ∈ 01–12 |
| Message | `YearMonth 必须为 YYYYMM 格式，且月份在 01-12。` |
| Fix Suggestion | 将 YearMonth 改为纯数字六位，例如 `202501`。 |

---

### R-OWNER-TOTAL-001 — Owner required on Total rows (Error)

| Attribute | Value |
|---|---|
| RuleId | R-OWNER-TOTAL-001 |
| Severity | **Error** |
| Applies To | Total rows only (`Function` ∈ `{'Total (All)', 'Total (Core Operations)'}`) |
| Condition | `trim(Owner)` must not be empty |
| Message | `Total 行的 Owner 不能为空。` |
| Fix Suggestion | 在 Owner 列填入负责人姓名或工号。 |

---

### R-YM-TOTAL-001 — YearMonth required on Total rows (Error)

| Attribute | Value |
|---|---|
| RuleId | R-YM-TOTAL-001 |
| Severity | **Error** |
| Applies To | Total rows only |
| Condition | `trim(YearMonth)` must not be empty, AND must satisfy R-YM-001 format |
| Message | `Total 行的 YearMonth 不能为空，且必须为 YYYYMM 格式。` |
| Fix Suggestion | 在 YearMonth 列填入六位纯数字，例如 `202501`。 |

---

### R-TGT-001 — Target_YearEnd required on Non-Total rows (Error)

| Attribute | Value |
|---|---|
| RuleId | R-TGT-001 |
| Severity | **Error** |
| Applies To | Non-Total rows only |
| Condition | `trim(Target_YearEnd)` must not be empty |
| Message | `Non-Total 行的 Target_YearEnd 不能为空。` |
| Fix Suggestion | 填入 Target_YearEnd 目标值（数字）。 |

---

### R-TGT-002 — Target_2030YearEnd required on Non-Total rows (Error)

| Attribute | Value |
|---|---|
| RuleId | R-TGT-002 |
| Severity | **Error** |
| Applies To | Non-Total rows only |
| Condition | `trim(Target_2030YearEnd)` must not be empty |
| Message | `Non-Total 行的 Target_2030YearEnd 不能为空。` |
| Fix Suggestion | 填入 Target_2030YearEnd 目标值（数字）。 |

---

### R-SR-001 — ShoringRatio strict percent format (Error)

| Attribute | Value |
|---|---|
| RuleId | R-SR-001 |
| Severity | **Error** |
| Applies To | Non-Total rows (required); Total rows (only when the field is not empty) |
| Allowed format | `^\d+(\.\d+)?%$` after trimming whitespace |
| Range | Numeric value (after removing `%`) must satisfy `0 ≤ n ≤ 100` |
| Examples (valid) | `0%`, `100%`, `12.5%`, `" 12.5% "` (leading/trailing whitespace OK) |
| Examples (invalid) | `12.5` (no `%`), `0.23` (no `%`), `101%` (out of range), `-1%` (negative), `abc%` (non-numeric) |
| Message | `ShoringRatio 必须使用百分比格式（数字+%），范围 0%~100%，允许小数，例如 12.5%。` |
| Fix Suggestion | 将值改为"数字+%"形式，例如 `0%`、`25%`、`12.5%`、`100%`。 |

#### Validation Logic
```
v = trim(ShoringRatio)
if v is empty:
    → For Non-Total rows: Error (R-SR-001, "ShoringRatio 不能为空")
    → For Total rows: pass (skip R-SR-001)
else:
    if NOT matches(v, /^\d+(\.\d+)?%$/) → Error (R-SR-001)
    n = toNumber(replace(v, "%", ""))
    if n < 0 OR n > 100 → Error (R-SR-001)
```

---

## Rule Summary Table

| RuleId | Severity | Applies To | Column | Trigger |
|---|---|---|---|---|
| R-YM-001 | Error | All rows (non-empty YearMonth) | YearMonth | Not YYYYMM or invalid month |
| R-OWNER-TOTAL-001 | Error | Total rows | Owner | Empty |
| R-YM-TOTAL-001 | Error | Total rows | YearMonth | Empty or invalid format |
| R-TGT-001 | Error | Non-Total rows | Target_YearEnd | Empty |
| R-TGT-002 | Error | Non-Total rows | Target_2030YearEnd | Empty |
| R-SR-001 | Error | Non-Total rows (always); Total rows (if non-empty) | ShoringRatio | Not `^\d+(\.\d+)?%$` or out of range 0–100 |

---

## Out-of-Scope for Option A (not enforced)

The following are intentionally **not** required under Option A, keeping the rule set minimal for POC:

- On **Non-Total rows**: `Cost Center Number`, `Team`, `Owner`, `YearMonth` are all **optional** (not enforced for emptiness). Note that `Owner` and `YearMonth` are only mandatory on **Total rows**.
- Actual_* and Planned_* numeric columns (not validated for emptiness on either row type)
- Function / Team whitelist and Function-Team mapping (separate rule set, to be added in a later iteration)
