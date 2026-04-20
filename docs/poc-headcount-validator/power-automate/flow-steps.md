# POC Headcount Excel Validator — Power Automate Flow Steps

**Flow type:** Instant cloud flow (manual trigger for POC; switch to scheduled trigger for production)  
**Excel Table:** `tblOffshoring`

---

## Prerequisites

1. Excel file (`headcount_analysis_poc.xlsx`) is stored in a SharePoint document library.
2. The file contains a named Table `tblOffshoring` on the `Offshoring` sheet.
3. Table headers exactly match (16 columns):  
   `Cost Center Number`, `Function`, `Team`, `Owner`, `YearMonth`,  
   `Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,  
   `Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,  
   `Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`

---

## Step 0 — Create the Flow

1. Go to **Power Automate** → **My flows** → **+ New flow** → **Instant cloud flow**.
2. Name: `POC Headcount Validator`.
3. Trigger: **Manually trigger a flow** → Create.

---

## Step 1 — Read Table Data from Excel

Add action: **Excel Online (Business)** → **List rows present in a table**

| Setting | Value |
|---|---|
| Location | SharePoint site URL |
| Document Library | Library name (e.g. `Documents`) |
| File | `headcount_analysis_poc.xlsx` |
| Table | `tblOffshoring` |

**Enable pagination** (Settings gear on the action → Pagination → On → Threshold: `10000`)  
This ensures all rows are fetched even if the table grows beyond the default page size.

Output reference: `body/value` (array of row objects)

---

## Step 2 — Initialize Variables

Add the following **Initialize variable** actions (in order):

| Variable Name | Type | Initial Value | Purpose |
|---|---|---|---|
| `issues` | Array | `[]` | Collects all validation issue objects |
| `errorCount` | Integer | `0` | Counts Error-severity issues |
| `warningCount` | Integer | `0` | Counts Warning-severity issues |
| `rowCount` | Integer | `0` | Counts total rows processed |

---

## Step 3 — Loop Over Each Row

Add action: **Control** → **Apply to each**  
- **Select an output from previous steps:** `value` (from Step 1 List rows output)

All validation logic in Steps 4–9 is inside this loop.

---

## Step 4 — Increment Row Counter

Inside the loop, add: **Variables** → **Increment variable**  
- Variable: `rowCount`, Value: `1`

---

## Step 5 — Extract Key Fields

Add a **Compose** action named `row_fields` to extract all 16 columns for easy reference:

```json
{
  "CostCenterNumber": "@{trim(items('Apply_to_each')?['Cost Center Number'])}",
  "Function": "@{trim(items('Apply_to_each')?['Function'])}",
  "Team": "@{trim(items('Apply_to_each')?['Team'])}",
  "Owner": "@{trim(items('Apply_to_each')?['Owner'])}",
  "YearMonth": "@{trim(items('Apply_to_each')?['YearMonth'])}",
  "Actual_GBS_TeamMember": "@{trim(items('Apply_to_each')?['Actual_GBS_TeamMember'])}",
  "Actual_GBS_TeamLeaderAM": "@{trim(items('Apply_to_each')?['Actual_GBS_TeamLeaderAM'])}",
  "Actual_EA": "@{trim(items('Apply_to_each')?['Actual_EA'])}",
  "Actual_HKT": "@{trim(items('Apply_to_each')?['Actual_HKT'])}",
  "Planned_GBS_TeamMember": "@{trim(items('Apply_to_each')?['Planned_GBS_TeamMember'])}",
  "Planned_GBS_TeamLeaderAM": "@{trim(items('Apply_to_each')?['Planned_GBS_TeamLeaderAM'])}",
  "Planned_EA": "@{trim(items('Apply_to_each')?['Planned_EA'])}",
  "Planned_HKT": "@{trim(items('Apply_to_each')?['Planned_HKT'])}",
  "Target_YearEnd": "@{trim(items('Apply_to_each')?['Target_YearEnd'])}",
  "Target_2030YearEnd": "@{trim(items('Apply_to_each')?['Target_2030YearEnd'])}",
  "ShoringRatio": "@{trim(items('Apply_to_each')?['ShoringRatio'])}"
}
```

---

## Step 6 — Classify Row (Total vs. Non-Total)

Add a **Compose** action named `is_total_row`:

```
@{or(
  equals(outputs('row_fields')?['Function'], 'Total (All)'),
  equals(outputs('row_fields')?['Function'], 'Total (Core Operations)')
)}
```

This produces `true` for Total rows, `false` for Non-Total rows.

---

## Step 7 — Branch: Total Row vs. Non-Total Row

Add a **Condition** action:

- **Condition:** `outputs('is_total_row')` **is equal to** `true`

### Branch A — True (Total Row): R-REQ-001

**Check Owner is non-empty:**

```
Condition: empty(outputs('row_fields')?['Owner']) is equal to true
→ Yes: Append to array variable `issues`:
{
  "Severity": "Error",
  "RuleId": "R-REQ-001",
  "YearMonth": "@{outputs('row_fields')?['YearMonth']}",
  "Function": "@{outputs('row_fields')?['Function']}",
  "Team": "@{outputs('row_fields')?['Team']}",
  "Column": "Owner",
  "Value": "",
  "Message": "Owner 不能为空（Total行仍需填写 Owner 和 YearMonth）。",
  "FixSuggestion": "请在 Owner 列填写有效的负责人姓名。"
}
Then: Increment variable errorCount by 1
```

**Check YearMonth is non-empty (same branch, add second Condition):**

```
Condition: empty(outputs('row_fields')?['YearMonth']) is equal to true
→ Yes: Append to array variable `issues`:
{
  "Severity": "Error",
  "RuleId": "R-REQ-001",
  "YearMonth": "",
  "Function": "@{outputs('row_fields')?['Function']}",
  "Team": "@{outputs('row_fields')?['Team']}",
  "Column": "YearMonth",
  "Value": "",
  "Message": "YearMonth 不能为空（Total行仍需填写 Owner 和 YearMonth）。",
  "FixSuggestion": "请在 YearMonth 列填写 YYYYMM 格式的年月，例如 202501。"
}
Then: Increment variable errorCount by 1
```

**Check ShoringRatio format if non-empty (Total row — conditional R-SR-001):**

```
Condition: not(empty(outputs('row_fields')?['ShoringRatio'])) is equal to true
→ Yes:
  Sub-condition: not(isMatch(outputs('row_fields')?['ShoringRatio'], '^\d+(\.\d+)?%$'))
  → Yes: Append to array variable `issues`:
  {
    "Severity": "Error",
    "RuleId": "R-SR-001",
    "YearMonth": "@{outputs('row_fields')?['YearMonth']}",
    "Function": "@{outputs('row_fields')?['Function']}",
    "Team": "@{outputs('row_fields')?['Team']}",
    "Column": "ShoringRatio",
    "Value": "@{outputs('row_fields')?['ShoringRatio']}",
    "Message": "ShoringRatio 必须使用百分比格式（0%~100%），例如 23% 或 12.5%。",
    "FixSuggestion": "请将值改为「数字+%」形式，例如 25% 或 12.5%。不要填写 0.23 或 23。"
  }
  Then: Increment variable errorCount by 1
```

> Note: On Total rows, ShoringRatio is **optional**. The format rule R-SR-001 is only triggered if a value is present.

---

### Branch B — False (Non-Total Row): R-REQ-002, R-REQ-003, R-REQ-004

#### B-1: Required baseline fields — R-REQ-003

For each of the following columns, add a **Condition** checking emptiness:  
`Cost Center Number` → `CostCenterNumber`, `Function`, `Team`, `Owner`, `YearMonth`

Template (repeat for each column):

```
Condition: empty(outputs('row_fields')?['«field»']) is equal to true
→ Yes: Append to array variable `issues`:
{
  "Severity": "Error",
  "RuleId": "R-REQ-003",
  "YearMonth": "@{outputs('row_fields')?['YearMonth']}",
  "Function": "@{outputs('row_fields')?['Function']}",
  "Team": "@{outputs('row_fields')?['Team']}",
  "Column": "«ColumnName»",
  "Value": "",
  "Message": "«ColumnName» 不能为空（非Total行必须填写此列）。",
  "FixSuggestion": "请在 «ColumnName» 列填写有效值。"
}
Then: Increment variable errorCount by 1
```

#### B-2: Numeric Actual/Planned columns — R-REQ-004

Repeat the emptiness check template above (RuleId `R-REQ-004`) for each of:  
`Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,  
`Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`

#### B-3: Target and ShoringRatio columns — R-REQ-002

Repeat the emptiness check template above (RuleId `R-REQ-002`) for each of:  
`Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`

> These three columns are **strictly required** on all Non-Total rows.  
> If ShoringRatio passes the emptiness check, its format is validated below in Step 8 (R-SR-001).

---

## Step 8 — Format / Range Rules (All Rows)

These checks run **after** the branch, for all rows, on non-empty values.

### R-CCN-001 — Cost Center Number 7-digit

```
Condition: not(empty(outputs('row_fields')?['CostCenterNumber'])) is equal to true
→ Yes:
  Sub-condition: not(isMatch(outputs('row_fields')?['CostCenterNumber'], '^\d{7}$'))
  → Yes: Append Error issue for R-CCN-001, increment errorCount
```

Issue object:

```json
{
  "Severity": "Error",
  "RuleId": "R-CCN-001",
  "YearMonth": "@{outputs('row_fields')?['YearMonth']}",
  "Function": "@{outputs('row_fields')?['Function']}",
  "Team": "@{outputs('row_fields')?['Team']}",
  "Column": "Cost Center Number",
  "Value": "@{outputs('row_fields')?['CostCenterNumber']}",
  "Message": "Cost Center Number 必须为7位纯数字。",
  "FixSuggestion": "请将值改为7位数字，例如 1234567。"
}
```

### R-YM-001 — YearMonth YYYYMM

```
Condition: not(empty(outputs('row_fields')?['YearMonth'])) is equal to true
→ Yes:
  Sub-condition A: not(isMatch(outputs('row_fields')?['YearMonth'], '^\d{6}$'))
  → Yes (A): Append Error issue for R-YM-001 (not 6 digits), increment errorCount
  → No (A — is 6 digits): extract month = substring(YearMonth, 4, 2)
    Sub-condition B: less(month, '01') OR greater(month, '12')
    → Yes (B): Append Error issue for R-YM-001 (month out of 01-12 range), increment errorCount
```

> **Implementation note:** After confirming the value is exactly 6 digits, extract `substring(YearMonth, 4, 2)` to get the month portion (e.g. `"03"` from `"202503"`). Use string comparison — `less(month, '01')` or `greater(month, '12')` — to validate the range `01`–`12`. This correctly rejects values like `'00'`, `'13'`, and `'99'`.

Issue object:

```json
{
  "Severity": "Error",
  "RuleId": "R-YM-001",
  "YearMonth": "@{outputs('row_fields')?['YearMonth']}",
  "Function": "@{outputs('row_fields')?['Function']}",
  "Team": "@{outputs('row_fields')?['Team']}",
  "Column": "YearMonth",
  "Value": "@{outputs('row_fields')?['YearMonth']}",
  "Message": "YearMonth 必须为 YYYYMM 格式，且月份为 01-12。",
  "FixSuggestion": "请将 YearMonth 改为6位数字，例如 202501。"
}
```

### R-NUM-001 / R-NUM-002 — Numeric columns

For each numeric column (10 total: Actual_*, Planned_*, Target_*):

```
Condition: not(empty(outputs('row_fields')?['«col»'])) is equal to true
→ Yes:
  Sub-condition: not(isMatch(outputs('row_fields')?['«col»'], '^\d+(\.\d+)?$'))
  → Yes (R-NUM-001): Append Error "«col» 必须为数字（不允许负数）。", increment errorCount
  → No (is a non-negative number): value is valid, no further check needed for R-NUM-002
```

> **Note:** The regex `^\d+(\.\d+)?$` requires the value to start with one or more digits, so negative numbers (starting with `-`) are rejected immediately by R-NUM-001. R-NUM-002 (separate ≥ 0 check) is effectively covered by this pattern and does not need a separate condition step.

### R-SR-001 — ShoringRatio format (Non-Total rows only in this step)

For Non-Total rows, ShoringRatio was already checked for emptiness (R-REQ-002). Now validate format:

```
Condition: not(empty(outputs('row_fields')?['ShoringRatio'])) is equal to true
           AND outputs('is_total_row') is equal to false
→ Yes:
  Sub-condition: not(isMatch(outputs('row_fields')?['ShoringRatio'], '^\d+(\.\d+)?%$'))
  → Yes: Append Error R-SR-001, increment errorCount
  → No (format OK):
    Sub-condition: float(replace(outputs('row_fields')?['ShoringRatio'], '%', '')) > 100
    → Yes: Append Error R-SR-001 (out of range), increment errorCount
```

> Note: The Total-row ShoringRatio format check (when non-empty) is already handled in Step 7 Branch A.

---

## Step 9 — Build Payload for Copilot Studio

After the Apply to each loop, add a **Compose** action named `report_payload`:

```json
{
  "file": "headcount_analysis_poc.xlsx",
  "table": "tblOffshoring",
  "rowCount": "@{variables('rowCount')}",
  "errorCount": "@{variables('errorCount')}",
  "warningCount": "@{variables('warningCount')}",
  "topIssues": "@{take(variables('issues'), 50)}"
}
```

> `take()` limits the array to 50 items. For the POC, this is sufficient for demonstration. If `issues` has fewer than 50 items, all are included.

---

## Step 10 — Send to Copilot Studio / AI Builder

Add action: **HTTP** (or use the Copilot Studio connector if available):

- **Method:** POST
- **URI:** Copilot Studio custom topic webhook URL
- **Body:** `outputs('report_payload')`
- **Headers:** `Content-Type: application/json`

Alternatively, use **AI Builder** → **Create text with GPT** with the payload as prompt context (see `copilot/prompt.md` for the exact system prompt).

---

## Step 11 — Save Issues to SharePoint (Optional but recommended for demo)

Add action: **SharePoint** → **Create file**

| Setting | Value |
|---|---|
| Site Address | SharePoint site URL |
| Folder Path | `/Documents/headcount-validator-output` |
| File Name | `issues_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmm')}.json` |
| File Content | `@{string(variables('issues'))}` |

---

## Step 12 — Send Teams / Email Notification (Optional)

Add action: **Microsoft Teams** → **Post message in a chat or channel**  
Or: **Office 365 Outlook** → **Send an email**

Message body (example):

```
📊 POC Headcount Validator 结果

文件：headcount_analysis_poc.xlsx（tblOffshoring）
总行数：@{variables('rowCount')}
❌ Error：@{variables('errorCount')} 条
⚠️ Warning：@{variables('warningCount')} 条

详细 issues 文件已保存至 SharePoint。
Copilot 报告请查看下方输出。
```

---

## Flow Diagram Summary

```
[Trigger: Manual]
    │
    ▼
[Step 1] List rows from tblOffshoring (pagination on)
    │
    ▼
[Step 2] Initialize: issues=[], errorCount=0, warningCount=0, rowCount=0
    │
    ▼
[Step 3] Apply to each row
    ├─ [Step 4] rowCount + 1
    ├─ [Step 5] Extract & trim all 16 fields
    ├─ [Step 6] Compose is_total_row (true/false)
    │
    ├─ [Step 7] Branch on is_total_row
    │    ├─ TRUE (Total row)
    │    │    ├─ R-REQ-001: Owner non-empty?
    │    │    ├─ R-REQ-001: YearMonth non-empty?
    │    │    └─ R-SR-001: ShoringRatio format (only if non-empty)
    │    │
    │    └─ FALSE (Non-Total row)
    │         ├─ R-REQ-003: Cost Center Number, Function, Team, Owner, YearMonth non-empty?
    │         ├─ R-REQ-004: Actual_* (4) and Planned_* (4) non-empty?
    │         └─ R-REQ-002: Target_YearEnd, Target_2030YearEnd, ShoringRatio non-empty?
    │
    └─ [Step 8] Format/range rules (all rows, non-empty values only)
         ├─ R-CCN-001: Cost Center Number = 7 digits?
         ├─ R-YM-001: YearMonth = YYYYMM?
         ├─ R-NUM-001/002: numeric columns parseable & ≥ 0?
         └─ R-SR-001: ShoringRatio format & range?
                       (Non-Total: always; Total: already handled in Step 7)
    │
    ▼
[Step 9] Build report_payload (top 50 issues)
    │
    ▼
[Step 10] Send payload to Copilot Studio / AI Builder
    │
    ▼
[Step 11] Save full issues JSON to SharePoint
    │
    ▼
[Step 12] Send Teams/Email notification
```
