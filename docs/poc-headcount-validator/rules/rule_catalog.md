# POC Headcount Excel Validator — Rule Catalog

**Table:** `tblOffshoring`  
**Sheet:** `Offshoring`  
**Columns (16):**
`Cost Center Number`, `Function`, `Team`, `Owner`, `YearMonth`,
`Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,
`Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,
`Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`

---

## Column Groups

| Group | Columns |
|---|---|
| Dimension | Cost Center Number, Function, Team, Owner, YearMonth |
| Numeric (Actual/Planned) | Actual_GBS_TeamMember, Actual_GBS_TeamLeaderAM, Actual_EA, Actual_HKT, Planned_GBS_TeamMember, Planned_GBS_TeamLeaderAM, Planned_EA, Planned_HKT |
| Numeric (Target) | Target_YearEnd, Target_2030YearEnd |
| Ratio | ShoringRatio |

**Total-row Functions** (exempt rows): `Total (All)`, `Total (Core Operations)`

---

## Rules

### R-REQ-001 — Required fields non-empty (non-Total rows)

| Attribute | Value |
|---|---|
| Rule ID | R-REQ-001 |
| Severity | **Error** |
| Applies to | All rows where `Function` is **NOT** `Total (All)` AND **NOT** `Total (Core Operations)` |

**Required columns (must not be blank after trimming whitespace):**
- `Cost Center Number`
- `Function`
- `Team`
- `Owner`
- `YearMonth`
- `Actual_GBS_TeamMember`
- `Actual_GBS_TeamLeaderAM`
- `Actual_EA`
- `Actual_HKT`
- `Planned_GBS_TeamMember`
- `Planned_GBS_TeamLeaderAM`
- `Planned_EA`
- `Planned_HKT`
- `Target_YearEnd`
- `Target_2030YearEnd`
- `ShoringRatio`

**Validation logic:**
```
isTotalRow = (trim(Function) == "Total (All)") OR (trim(Function) == "Total (Core Operations)")
IF NOT isTotalRow:
  FOR EACH required column c:
    IF trim(row[c]) == "" THEN Error(R-REQ-001, column=c)
```

**Error message:** `"{Column}" must not be empty for non-Total rows.`  
**Fix suggestion:** `Fill in a valid value for "{Column}". This column is mandatory for all non-Total rows.`

---

### R-REQ-002 — Total rows: core fields still validated

| Attribute | Value |
|---|---|
| Rule ID | R-REQ-002 |
| Severity | **Error** for YearMonth; **Warning** for other fields when present but invalid |
| Applies to | All rows where `Function` is `Total (All)` OR `Total (Core Operations)` |

For Total rows the following columns **are allowed to be blank**:
- `Cost Center Number`
- `Team`
- `Owner`
- `Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`
- `Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`
- `Target_YearEnd`, `Target_2030YearEnd`
- `ShoringRatio` *(allowed blank; validated only when a value is present)*

The following columns **must still be present (non-blank)** on Total rows:
- `YearMonth` → **Error** if blank
- `Function` → **Error** if blank (the row's Function value drives the Total-row classification itself)

Additional validation that still applies when a value **is present**:
- `YearMonth` format and range (R-YM-001)
- `ShoringRatio` format and range (R-SR-001) — only when non-blank

**Error message (YearMonth blank):** `"YearMonth" must not be empty even for Total rows.`  
**Warning message (other field invalid when present):** `"{Column}" value "{Value}" is present but invalid on a Total row.`

---

### R-CCN-001 — Cost Center Number: 7-digit numeric

| Attribute | Value |
|---|---|
| Rule ID | R-CCN-001 |
| Severity | **Error** |
| Applies to | Non-Total rows (R-REQ-001 already ensures non-blank) |

**Validation logic:**
```
v = trim(Cost Center Number)
isValid = matches(v, /^\d{7}$/)
```

**Error message:** `"Cost Center Number" must be exactly 7 digits (e.g., 1234567).`  
**Fix suggestion:** `Check the Cost Center Number. It must be a 7-digit number with no spaces or letters.`

---

### R-YM-001 — YearMonth: YYYYMM format with valid month

| Attribute | Value |
|---|---|
| Rule ID | R-YM-001 |
| Severity | **Error** |
| Applies to | All rows (non-blank rows only; blank is caught by R-REQ-001 / R-REQ-002) |

**Validation logic:**
```
v = trim(YearMonth)
isValidFormat = matches(v, /^\d{6}$/)
month = int(right(v, 2))   // last two characters
isValidMonth = month >= 1 AND month <= 12
isValid = isValidFormat AND isValidMonth
```

**Error message:** `"YearMonth" must be in YYYYMM format with a valid month (01–12). Got: "{Value}".`  
**Fix suggestion:** `Change to a 6-digit string like 202501 (year=2025, month=01).`

---

### R-OWNER-001 — Owner: non-empty

| Attribute | Value |
|---|---|
| Rule ID | R-OWNER-001 |
| Severity | **Error** |
| Applies to | Non-Total rows (superseded by R-REQ-001 for those rows; kept as explicit rule for clarity) |

> **Note:** R-REQ-001 already covers the non-empty check for `Owner` on non-Total rows. R-OWNER-001 is retained as a standalone rule so it can appear in rule-level reporting distinctly.

---

### R-NUM-001 — Numeric columns: must be a number

| Attribute | Value |
|---|---|
| Rule ID | R-NUM-001 |
| Severity | **Error** |
| Applies to | Non-Total rows where the numeric column is non-blank |

**Applies to columns:**
`Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,
`Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,
`Target_YearEnd`, `Target_2030YearEnd`

**Validation logic:**
```
v = trim(column_value)
isValid = isNumeric(v)
```

**Error message:** `"{Column}" must be a number. Got: "{Value}".`

---

### R-NUM-002 — Numeric columns: must be ≥ 0

| Attribute | Value |
|---|---|
| Rule ID | R-NUM-002 |
| Severity | **Error** |
| Applies to | Non-Total rows where R-NUM-001 passed |

**Validation logic:**
```
n = toNumber(trim(column_value))
isValid = n >= 0
```

**Error message:** `"{Column}" must be ≥ 0. Got: {Value}.`

---

### R-SR-001 — ShoringRatio: strict percent format, 0%–100%

| Attribute | Value |
|---|---|
| Rule ID | R-SR-001 |
| Severity | **Error** |
| Applies to | Any row (non-Total: must be non-blank per R-REQ-001; Total: validated only when non-blank) |

**Format specification:**
- Must end with a literal `%` character.
- The numeric part before `%` must be an integer or decimal number.
- Regex (after trimming whitespace): **`^\d+(\.\d+)?%$`**
- The numeric value must satisfy `0 ≤ n ≤ 100`.

**Examples:**

| Value (after trim) | Result |
|---|---|
| `0%` | ✅ Valid |
| `100%` | ✅ Valid |
| `23%` | ✅ Valid |
| `12.5%` | ✅ Valid |
| `0.5%` | ✅ Valid |
| `23` | ❌ Missing `%` |
| `0.23` | ❌ Missing `%` |
| `101%` | ❌ Out of range |
| `-1%` | ❌ Negative / not matching regex |
| `abc%` | ❌ Non-numeric |
| ` 23% ` | ✅ Valid (whitespace trimmed before check) |

**Validation logic:**
```
v = trim(ShoringRatio)
if v == "": skip (blank — R-REQ-001 handles non-blank requirement for non-Total rows)
isFormatValid = matches(v, /^\d+(\.\d+)?%$/)
if isFormatValid:
  n = toNumber(left(v, len(v)-1))   // strip trailing '%' and convert
  isRangeValid = n >= 0 AND n <= 100
isValid = isFormatValid AND isRangeValid
```

**Error message:** `"ShoringRatio" must use percent format with a value between 0% and 100% (e.g., 23%, 12.5%). Got: "{Value}".`  
**Fix suggestion:** `Change the value to "number%" format, e.g., 25% or 12.5%. Do not use 0.25 or 25 without the % sign.`

---

### R-FUNC-001 — Function: must be in whitelist

| Attribute | Value |
|---|---|
| Rule ID | R-FUNC-001 |
| Severity | **Error** |
| Applies to | All non-blank rows |

**Error message:** `"Function" value "{Value}" is not in the allowed list.`

---

### R-TEAM-001 — Team: must be in whitelist

| Attribute | Value |
|---|---|
| Rule ID | R-TEAM-001 |
| Severity | **Warning** |
| Applies to | Non-Total rows where Team is non-blank |

**Error message:** `"Team" value "{Value}" is not in the allowed list.`

---

### R-MAP-001 — (Function, Team) combination must be allowed

| Attribute | Value |
|---|---|
| Rule ID | R-MAP-001 |
| Severity | **Error** |
| Applies to | Non-Total rows where both Function and Team are non-blank |

**Error message:** `Function "{Function}" + Team "{Team}" is not an allowed combination.`

---

### R-COV-001 — Monthly coverage: expected (Function, Team) combinations present

| Attribute | Value |
|---|---|
| Rule ID | R-COV-001 |
| Severity | **Warning** |
| Applies to | Whole-file aggregation per YearMonth |

**Error message:** `YearMonth {YearMonth}: expected combination Function="{Function}" / Team="{Team}" is missing.`

---

## Rule Summary Table

| Rule ID | Description | Severity | Scope |
|---|---|---|---|
| R-REQ-001 | Required fields non-empty — non-Total rows | Error | Non-Total rows |
| R-REQ-002 | Total rows: YearMonth required; others allowed blank | Error / Warning | Total rows |
| R-CCN-001 | Cost Center Number must be 7-digit numeric | Error | Non-Total rows |
| R-YM-001 | YearMonth must be YYYYMM with valid month | Error | All non-blank |
| R-OWNER-001 | Owner must not be empty | Error | Non-Total rows |
| R-NUM-001 | Numeric columns must be numeric | Error | Non-Total, non-blank |
| R-NUM-002 | Numeric columns must be ≥ 0 | Error | Non-Total, non-blank |
| R-SR-001 | ShoringRatio strict `^\d+(\.\d+)?%$`, 0–100 | Error | All non-blank |
| R-FUNC-001 | Function must be in whitelist | Error | All non-blank |
| R-TEAM-001 | Team must be in whitelist | Warning | Non-Total non-blank |
| R-MAP-001 | (Function, Team) must be allowed combination | Error | Non-Total non-blank |
| R-COV-001 | Monthly coverage completeness | Warning | Whole file |
