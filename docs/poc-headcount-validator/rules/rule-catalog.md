# POC Headcount Excel Validator — Rule Catalog

**Excel Table:** `tblOffshoring`  
**Sheet:** Offshoring  
**Columns (16):**  
`Cost Center Number`, `Function`, `Team`, `Owner`, `YearMonth`,  
`Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,  
`Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,  
`Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`

---

## Row Classification

Every row is first classified before applying field rules:

| Classification | Condition |
|---|---|
| **Total Row** | `Function` = `Total (All)` **OR** `Function` = `Total (Core Operations)` |
| **Non-Total Row** | `Function` is any other value (including blank) |

This classification drives all conditional required-field rules in section 2 below.

---

## 1. Format / Range Rules (apply to ALL rows regardless of classification)

### R-CCN-001 — Cost Center Number: 7-digit numeric string
- **Applies to:** `Cost Center Number`
- **Condition:** Only validated when the cell is non-empty.
- **Rule:** After trimming whitespace, the value must consist of exactly 7 decimal digits (`^\d{7}$`).
- **Severity:** Error
- **Message:** `Cost Center Number 必须为7位纯数字。`
- **Fix Suggestion:** `请将值改为7位数字，例如 1234567。`

### R-YM-001 — YearMonth: YYYYMM format with valid month
- **Applies to:** `YearMonth`
- **Condition:** Only validated when the cell is non-empty.
- **Rule:**  
  1. After trimming, length must be exactly 6.  
  2. All characters must be decimal digits.  
  3. Last two digits (MM) must be in range `01`–`12`.
- **Severity:** Error
- **Message:** `YearMonth 必须为 YYYYMM 格式，且月份为 01-12。`
- **Fix Suggestion:** `请将 YearMonth 改为6位数字，例如 202501。`

### R-NUM-001 — Numeric columns: must be a number
- **Applies to:** `Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`, `Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`, `Target_YearEnd`, `Target_2030YearEnd`
- **Condition:** Only validated when the cell is non-empty.
- **Rule:** The value must be parseable as a number (integer or decimal).
- **Severity:** Error
- **Message:** `«列名» 必须为数字。`
- **Fix Suggestion:** `请填写数字，例如 12 或 12.5。`

### R-NUM-002 — Numeric columns: must be ≥ 0
- **Applies to:** Same 10 numeric columns as R-NUM-001.
- **Condition:** Only validated when the cell is non-empty and passes R-NUM-001.
- **Rule:** The numeric value must be ≥ 0.
- **Severity:** Error
- **Message:** `«列名» 不能为负数。`
- **Fix Suggestion:** `请填写0或正数。`

### R-SR-001 — ShoringRatio: strict percent format, range 0%–100%
- **Applies to:** `ShoringRatio`
- **Condition:**  
  - **Non-Total rows:** always validated (emptiness also caught by R-REQ-002).  
  - **Total rows:** validated **only when the cell is non-empty** (empty is allowed on Total rows).
- **Rule:**  
  1. Trim leading/trailing whitespace.  
  2. Value must match the regular expression: `^\d+(\.\d+)?%$`  
     — i.e., one or more digits, optional decimal point followed by one or more digits, then `%`.  
  3. Remove the trailing `%`, parse as a decimal number `n`.  
  4. `n` must satisfy `0 ≤ n ≤ 100`.
- **Severity:** Error
- **Allowed examples:** `0%`, `100%`, `12.5%`, ` 23% ` (spaces trimmed)
- **Rejected examples:** `23` (no `%`), `0.23` (ratio form), `101%` (out of range), `-1%` (negative), `abc%` (non-numeric)
- **Message:** `ShoringRatio 必须使用百分比格式（0%~100%），例如 23% 或 12.5%。`
- **Fix Suggestion:** `请将值改为"数字+%"形式，例如 25% 或 12.5%。不要填写 0.23 或 23。`

---

## 2. Conditional Required-Field Rules

### R-REQ-001 — Total rows: Owner and YearMonth are required
- **Applies to:** Total rows only (`Function` = `Total (All)` OR `Total (Core Operations)`)
- **Required fields:** `Owner`, `YearMonth`
- **Rule:** Each of these two fields must be non-empty after trimming.
- **Severity:** Error
- **Message:** `«列名» 不能为空（Total行仍需填写 Owner 和 YearMonth）。`
- **Fix Suggestion:** `请在 «列名» 填写有效值。`
- **Note:** All other columns (`Cost Center Number`, `Team`, all numeric columns, `Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`) may be empty on Total rows without error. If `ShoringRatio` is filled, it must still satisfy R-SR-001.

### R-REQ-002 — Non-Total rows: Target_YearEnd, Target_2030YearEnd, ShoringRatio are required
- **Applies to:** Non-Total rows (`Function` ≠ `Total (All)` AND `Function` ≠ `Total (Core Operations)`)
- **Required fields:** `Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`
- **Rule:** Each of these three fields must be non-empty after trimming.
- **Severity:** Error
- **Message:** `«列名» 不能为空（非Total行必须填写此列）。`
- **Fix Suggestion:** `请在 «列名» 填写有效值。非 Total (All) / Total (Core Operations) 行不允许此列为空。`

### R-REQ-003 — Non-Total rows: baseline required fields
- **Applies to:** Non-Total rows
- **Required fields:** `Cost Center Number`, `Function`, `Team`, `Owner`, `YearMonth`
- **Rule:** Each of these five fields must be non-empty after trimming.
- **Severity:** Error
- **Message:** `«列名» 不能为空（非Total行必须填写此列）。`
- **Fix Suggestion:** `请在 «列名» 填写有效值。`

### R-REQ-004 — Non-Total rows: numeric columns are required
- **Applies to:** Non-Total rows
- **Required fields:** `Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`, `Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`
- **Rule:** Each of these eight fields must be non-empty after trimming.
- **Severity:** Error
- **Message:** `«列名» 不能为空（非Total行必须填写此列）。`
- **Fix Suggestion:** `请在 «列名» 填写数字。`

---

## 3. Rule Evaluation Order per Row

For each row, apply rules in this sequence:

```
1. Classify row as Total or Non-Total.

2. Apply R-REQ-001 (if Total row):
   - Check Owner non-empty → R-REQ-001
   - Check YearMonth non-empty → R-REQ-001

3. Apply R-REQ-002, R-REQ-003, R-REQ-004 (if Non-Total row):
   - Check Cost Center Number, Function, Team, Owner, YearMonth → R-REQ-003
   - Check Actual_* and Planned_* (8 columns) → R-REQ-004
   - Check Target_YearEnd, Target_2030YearEnd, ShoringRatio → R-REQ-002

4. Apply format/range rules on non-empty values (ALL rows):
   - R-CCN-001 on Cost Center Number
   - R-YM-001 on YearMonth
   - R-NUM-001, R-NUM-002 on numeric columns (when non-empty)
   - R-SR-001 on ShoringRatio:
       - Non-Total: always (empty already caught by R-REQ-002)
       - Total: only when non-empty
```

---

## 4. Rule Summary Table

| Rule ID | Severity | Column(s) | Applies To | Condition |
|---|---|---|---|---|
| R-CCN-001 | Error | Cost Center Number | All rows | When non-empty |
| R-YM-001 | Error | YearMonth | All rows | When non-empty |
| R-NUM-001 | Error | 10 numeric columns | All rows | When non-empty |
| R-NUM-002 | Error | 10 numeric columns | All rows | When non-empty & passes R-NUM-001 |
| R-SR-001 | Error | ShoringRatio | All rows | Non-Total: always; Total: when non-empty |
| R-REQ-001 | Error | Owner, YearMonth | Total rows only | Always |
| R-REQ-002 | Error | Target_YearEnd, Target_2030YearEnd, ShoringRatio | Non-Total rows only | Always |
| R-REQ-003 | Error | Cost Center Number, Function, Team, Owner, YearMonth | Non-Total rows only | Always |
| R-REQ-004 | Error | Actual_* (4), Planned_* (4) | Non-Total rows only | Always |

---

## 5. Rule Change Log

| Version | Change |
|---|---|
| v1.0 | Initial rule catalog (Table-based ingestion via `tblOffshoring`) |
| v1.1 | R-SR-001 tightened: removed ratio/bare-number acceptance; only `^\d+(\.\d+)?%$` format allowed, range 0–100 |
| v1.2 | Added R-REQ-001, R-REQ-002, R-REQ-003, R-REQ-004: conditional required-field rules based on Total / Non-Total row classification. Decimal ShoringRatio percentages (e.g. `12.5%`) explicitly allowed in R-SR-001 |
