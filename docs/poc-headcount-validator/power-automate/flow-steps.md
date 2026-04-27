# Power Automate Flow — POC Headcount Validator (Table-Based)

**Table:** `tblOffshoring` | **Sheet:** `Offshoring`  
**Trigger:** Manual (Instant cloud flow) — switch to scheduled after POC

---

## Step 0 — Prerequisites

| Item | Value |
|---|---|
| File location | SharePoint document library |
| File name | `headcount_analysis_poc.xlsx` (POC copy) |
| Sheet | `Offshoring` |
| Table name | `tblOffshoring` |
| Columns (16) | See rule catalog |

---

## Step 1 — Create the Flow

1. Open **Power Automate** → **+ Create** → **Instant cloud flow**
2. Name the flow: `POC-HeadcountValidator`
3. Trigger: **Manually trigger a flow** (no input parameters needed for POC)
4. Click **Create**

---

## Step 2 — Read Table Data

Add action: **Excel Online (Business)** → **List rows present in a table**

| Field | Value |
|---|---|
| Location | *(select your SharePoint site)* |
| Document Library | *(select library)* |
| File | `headcount_analysis_poc.xlsx` |
| Table | `tblOffshoring` |

**Enable Pagination** (to handle large tables):
- Click `...` → **Settings** → **Pagination** → **On**
- Threshold: `10000`

---

## Step 3 — Initialize Variables

Add the following **Initialize variable** actions (in order):

| Variable name | Type | Initial value |
|---|---|---|
| `varIssues` | Array | `[]` |
| `varErrorCount` | Integer | `0` |
| `varWarningCount` | Integer | `0` |
| `varRowCount` | Integer | `0` |

---

## Step 4 — Define Helper Constants (Compose)

Add a **Compose** action named `constTotalFunctions` to define the Total-row exemption list:

```json
["Total (All)", "Total (Core Operations)"]
```

Add a **Compose** action named `constRequiredColumnsNonTotal` to define the required non-empty columns for non-Total rows:

```json
[
  "Cost Center Number","Function","Team","Owner","YearMonth",
  "Actual_GBS_TeamMember","Actual_GBS_TeamLeaderAM","Actual_EA","Actual_HKT",
  "Planned_GBS_TeamMember","Planned_GBS_TeamLeaderAM","Planned_EA","Planned_HKT",
  "Target_YearEnd","Target_2030YearEnd","ShoringRatio"
]
```

Add a **Compose** action named `constNumericColumns`:

```json
[
  "Actual_GBS_TeamMember","Actual_GBS_TeamLeaderAM","Actual_EA","Actual_HKT",
  "Planned_GBS_TeamMember","Planned_GBS_TeamLeaderAM","Planned_EA","Planned_HKT",
  "Target_YearEnd","Target_2030YearEnd"
]
```

---

## Step 5 — Loop Over Each Row (Apply to each)

Add **Apply to each** → Input: `value` (from the List rows step output)

Inside the loop:

### 5.1 — Increment row counter

**Increment variable** → `varRowCount` → by `1`

### 5.2 — Extract key fields

Use **Compose** or direct dynamic content references:

| Variable | Expression |
|---|---|
| `fFunction` | `trim(items('Apply_to_each')?['Function'])` |
| `fYearMonth` | `trim(items('Apply_to_each')?['YearMonth'])` |
| `fCCN` | `trim(items('Apply_to_each')?['Cost Center Number'])` |
| `fTeam` | `trim(items('Apply_to_each')?['Team'])` |
| `fOwner` | `trim(items('Apply_to_each')?['Owner'])` |
| `fShoringRatio` | `trim(items('Apply_to_each')?['ShoringRatio'])` |

### 5.3 — Determine if Total row

Add a **Compose** action named `isTotalRow`:

```
Expression:
or(
  equals(trim(items('Apply_to_each')?['Function']), 'Total (All)'),
  equals(trim(items('Apply_to_each')?['Function']), 'Total (Core Operations)')
)
```

Result: `true` or `false`

---

## Step 6 — Apply Conditional Required-Field Rules

### 6.1 — R-REQ-001: Non-Total rows — all required fields must be non-empty

Add **Condition**: `outputs('isTotalRow')` is equal to `false`

**If yes (non-Total row):**

For each required column (loop or individual conditions — POC: check individually for simplicity):

Check: `empty(trim(items('Apply_to_each')?['Cost Center Number']))`  
If true → append to `varIssues`:

```json
{
  "Severity": "Error",
  "RuleId": "R-REQ-001",
  "YearMonth": "@{trim(items('Apply_to_each')?['YearMonth'])}",
  "Cost Center Number": "@{trim(items('Apply_to_each')?['Cost Center Number'])}",
  "Function": "@{trim(items('Apply_to_each')?['Function'])}",
  "Team": "@{trim(items('Apply_to_each')?['Team'])}",
  "Column": "Cost Center Number",
  "Value": "",
  "Message": "\"Cost Center Number\" must not be empty for non-Total rows.",
  "FixSuggestion": "Fill in a valid value for \"Cost Center Number\". This column is mandatory for all non-Total rows."
}
```

Repeat the same pattern for each required column:
`Function`, `Team`, `Owner`, `YearMonth`,
`Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,
`Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,
`Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`

**Power Automate expression pattern (reuse for each column):**

```
Condition: empty(trim(items('Apply_to_each')?['<COLUMN_NAME>']))
If true: Append to array variable varIssues
```

Also increment `varErrorCount` by `1` on each hit.

### 6.2 — R-REQ-002: Total rows — YearMonth must be non-empty

**If no (Total row):**

Check: `empty(trim(items('Apply_to_each')?['YearMonth']))`

If true → append Error to `varIssues`:

```json
{
  "Severity": "Error",
  "RuleId": "R-REQ-002",
  "YearMonth": "",
  "Function": "@{trim(items('Apply_to_each')?['Function'])}",
  "Column": "YearMonth",
  "Value": "",
  "Message": "\"YearMonth\" must not be empty even for Total rows.",
  "FixSuggestion": "Enter a valid YearMonth (YYYYMM) for this Total row."
}
```

---

## Step 7 — R-YM-001: YearMonth Format Validation

*(Runs for all non-blank YearMonth values, both Total and non-Total rows)*

Add **Condition**: `not(empty(fYearMonth))`

If true:

Check both conditions:
1. `not(equals(length(fYearMonth), 6))` — must be 6 characters
2. Length is 6 AND characters are all digits: use `isInt(fYearMonth)` or equivalent
3. Month range: `int(substring(fYearMonth, 4, 2))` must be between 1 and 12

**Power Automate expression to detect invalid YearMonth:**

```
Condition (invalid if true):
or(
  not(equals(length(trim(items('Apply_to_each')?['YearMonth'])), 6)),
  less(int(substring(trim(items('Apply_to_each')?['YearMonth']), 4, 2)), 1),
  greater(int(substring(trim(items('Apply_to_each')?['YearMonth']), 4, 2)), 12)
)
```

If true → append Error `R-YM-001` to `varIssues`.

---

## Step 8 — R-SR-001: ShoringRatio Format & Range Validation

*(Runs for all non-blank ShoringRatio values — Total and non-Total rows)*

Add **Condition**: `not(empty(fShoringRatio))`

If true, validate the format and range:

**Check 1 — trailing `%`:**

```
Expression: not(equals(last(fShoringRatio), '%'))
```
Or: `not(endsWith(trim(items('Apply_to_each')?['ShoringRatio']), '%'))`

If true → append Error `R-SR-001` (missing `%`).

**Check 2 — numeric part is valid and in range:**

```
Expression:
// Strip the trailing '%'
varNumPart = substring(fShoringRatio, 0, sub(length(fShoringRatio), 1))

// Validate:
// - must be parseable as a number
// - must be >= 0
// - must be <= 100
```

Power Automate expression (use nested conditions):

```
// Check numeric part
isValidRange = and(
  greaterOrEquals(float(substring(trim(items('Apply_to_each')?['ShoringRatio']), 0, sub(length(trim(items('Apply_to_each')?['ShoringRatio'])), 1))), 0),
  lessOrEquals(float(substring(trim(items('Apply_to_each')?['ShoringRatio']), 0, sub(length(trim(items('Apply_to_each')?['ShoringRatio'])), 1))), 100)
)
```

If out of range → append Error `R-SR-001`:

```json
{
  "Severity": "Error",
  "RuleId": "R-SR-001",
  "YearMonth": "@{trim(items('Apply_to_each')?['YearMonth'])}",
  "Cost Center Number": "@{trim(items('Apply_to_each')?['Cost Center Number'])}",
  "Function": "@{trim(items('Apply_to_each')?['Function'])}",
  "Team": "@{trim(items('Apply_to_each')?['Team'])}",
  "Column": "ShoringRatio",
  "Value": "@{trim(items('Apply_to_each')?['ShoringRatio'])}",
  "Message": "\"ShoringRatio\" must use percent format with a value between 0% and 100% (e.g., 23%, 12.5%). Got: \"@{trim(items('Apply_to_each')?['ShoringRatio'])}\".",
  "FixSuggestion": "Change the value to \"number%\" format, e.g., 25% or 12.5%. Do not use 0.25 or 25 without the % sign."
}
```

> **Note on Total rows:** Because R-REQ-001 only enforces non-blank on non-Total rows, `ShoringRatio` can be blank on Total rows without triggering R-SR-001. The `not(empty(fShoringRatio))` guard above ensures that blank values are skipped for R-SR-001 on Total rows.

---

## Step 9 — R-NUM-001 / R-NUM-002: Numeric Column Validation

*(Only for non-Total rows; only when the cell is non-blank — blank is already caught by R-REQ-001)*

Add **Condition**: `outputs('isTotalRow')` is equal to `false`

If true: for each numeric column (see list), check:

```
// R-NUM-001: must be parseable
Condition: not(isInt(trim(items('Apply_to_each')?['Actual_GBS_TeamMember'])))
// Use tryParse or equivalent — in Power Automate use a Try/Catch pattern or check with regex
```

**Practical approach in Power Automate:**

Use a **Try** scope:
1. In the Try block: compute `float(trim(items('Apply_to_each')?['<COLUMN>']))`
2. If the Try block **fails** → append Error `R-NUM-001`
3. If the Try block **succeeds**:
   - Check `less(float(...), 0)`
   - If true → append Error `R-NUM-002`

---

## Step 10 — R-CCN-001: Cost Center Number Validation

*(Only non-Total rows, only when non-blank)*

**Power Automate expression (check length ≠ 7 or not all digits):**

```
Condition (invalid if true):
or(
  not(equals(length(trim(items('Apply_to_each')?['Cost Center Number'])), 7)),
  not(isInt(trim(items('Apply_to_each')?['Cost Center Number'])))
)
```

If true → append Error `R-CCN-001`.

---

## Step 11 — Build Report Payload (after loop)

Add a **Compose** action named `reportPayload`:

```json
{
  "file": "headcount_analysis_poc.xlsx",
  "table": "tblOffshoring",
  "totalRows": "@{variables('varRowCount')}",
  "errorCount": "@{variables('varErrorCount')}",
  "warningCount": "@{variables('varWarningCount')}",
  "topIssues": "@{take(variables('varIssues'), 50)}"
}
```

> `take()` returns the first 50 items. For POC the issues array naturally has Errors first if you append Errors before Warnings inside the loop.

---

## Step 12 — Generate Report with Copilot Studio / AI Builder

Add action: **HTTP** or **Copilot Studio** connector → send `reportPayload` to your Copilot agent.

The Copilot prompt (see `copilot/prompt.md`) instructs the model to return a Markdown report:
- Validation summary
- Top issues table
- Questions for business sign-off

---

## Step 13 — Write Full Issues to SharePoint (Optional)

Add action: **SharePoint** → **Create file**

| Field | Value |
|---|---|
| Site | *(your SharePoint site)* |
| Folder | `POC-Validator-Output` |
| File name | `issues_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmm')}.json` |
| File content | `@{string(variables('varIssues'))}` |

---

## Key Expressions Cheat-Sheet

| Task | Power Automate Expression |
|---|---|
| Trim a field | `trim(items('Apply_to_each')?['ColumnName'])` |
| Check empty | `empty(trim(items('Apply_to_each')?['ColumnName']))` |
| String ends with `%` | `endsWith(trim(items('Apply_to_each')?['ShoringRatio']), '%')` |
| Strip trailing char | `substring(v, 0, sub(length(v), 1))` |
| Parse float | `float(substring(v, 0, sub(length(v), 1)))` |
| Check ≥ 0 | `greaterOrEquals(float(...), 0)` |
| Check ≤ 100 | `lessOrEquals(float(...), 100)` |
| Get YearMonth month part | `int(substring(trim(items('Apply_to_each')?['YearMonth']), 4, 2))` |
| Is Total row | `or(equals(fn, 'Total (All)'), equals(fn, 'Total (Core Operations)'))` |
| Take first N items | `take(variables('varIssues'), 50)` |
| Current timestamp for filename | `formatDateTime(utcNow(), 'yyyyMMdd_HHmm')` |
