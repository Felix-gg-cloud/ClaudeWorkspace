# Power Automate Flow — POC Headcount Validator (Option A)

## Overview

This flow reads the `tblOffshoring` table from Excel on SharePoint and validates every row according to Option A requiredness rules.  
Trigger: **Manually trigger a flow** (change to scheduled after POC).

---

## Step 0 — Prerequisites

1. Excel file uploaded to SharePoint (e.g. `headcount_analysis.xlsx`)
2. Table inside Excel named exactly: `tblOffshoring`
3. Column names match the 16 columns below (exact, case-sensitive):

   `Cost Center Number`, `Function`, `Team`, `Owner`, `YearMonth`,  
   `Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,  
   `Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,  
   `Target_YearEnd`, `Target_2030YearEnd`, `ShoringRatio`

---

## Step 1 — Trigger

- **Action**: Manually trigger a flow
- No inputs required for POC.

---

## Step 2 — Read Table Rows

- **Connector**: Excel Online (Business)
- **Action**: List rows present in a table
- **Settings**:
  - Location: SharePoint site
  - Document Library: (your library)
  - File: `headcount_analysis.xlsx`
  - Table: `tblOffshoring`
- **Enable Pagination**: Settings → Pagination → On → Threshold: `10000`

---

## Step 3 — Initialize Variables

Add the following **Initialize variable** actions (all before the loop):

| Variable Name | Type | Initial Value |
|---|---|---|
| `issues` | Array | `[]` |
| `errorCount` | Integer | `0` |
| `warningCount` | Integer | `0` |
| `rowCount` | Integer | `0` |

---

## Step 4 — Loop: Apply to each row

- **Action**: Apply to each
- **Input**: `value` (output of Step 2)

Inside the loop, add the following sub-steps in order.

> **Reusable Issue Object Template** — every time a rule fires, append an object with this shape to the `issues` array. Only the fields that differ per rule need to change:
> ```json
> {
>   "Severity": "Error",
>   "RuleId": "<RuleId>",
>   "YearMonth": "<current row YearMonth>",
>   "Cost Center Number": "<current row Cost Center Number>",
>   "Function": "<current row Function>",
>   "Team": "<current row Team>",
>   "Column": "<column that failed>",
>   "Value": "<actual cell value>",
>   "Message": "<human-readable message>",
>   "FixSuggestion": "<how to fix>"
> }
> ```
> After each append, also increment `errorCount` (or `warningCount`) by `1`.

---

### Step 4.1 — Increment rowCount

- **Action**: Increment variable
- Variable: `rowCount`
- Value: `1`

---

### Step 4.2 — Compose: isTotal (branch condition)

- **Action**: Compose
- **Name**: `isTotal`
- **Inputs** (expression):
  ```
  or(
    equals(trim(items('Apply_to_each')?['Function']), 'Total (All)'),
    equals(trim(items('Apply_to_each')?['Function']), 'Total (Core Operations)')
  )
  ```

---

### Step 4.3 — Condition: Is this a Total row?

- **Action**: Condition
- **Condition**: `outputs('isTotal')` is equal to `true`

#### Branch A — TRUE (Total rows)
> Enforce: **Owner** non-empty, **YearMonth** non-empty + YYYYMM format.  
> All other columns are optional.

**Step 4.3-A1 — Check Owner not empty**

- **Action**: Condition
  - `trim(items('Apply_to_each')?['Owner'])` is equal to `''`
- If yes → Append to `issues`:
  ```json
  {
    "Severity": "Error",
    "RuleId": "R-OWNER-TOTAL-001",
    "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
    "Cost Center Number": "@{items('Apply_to_each')?['Cost Center Number']}",
    "Function": "@{items('Apply_to_each')?['Function']}",
    "Team": "@{items('Apply_to_each')?['Team']}",
    "Column": "Owner",
    "Value": "",
    "Message": "Total 行的 Owner 不能为空。",
    "FixSuggestion": "在 Owner 列填入负责人姓名或工号。"
  }
  ```
  → Increment `errorCount` by `1`

**Step 4.3-A2 — Check YearMonth not empty**

- **Action**: Condition
  - `trim(items('Apply_to_each')?['YearMonth'])` is equal to `''`
- If yes → Append to `issues`:
  ```json
  {
    "Severity": "Error",
    "RuleId": "R-YM-TOTAL-001",
    "YearMonth": "",
    "Function": "@{items('Apply_to_each')?['Function']}",
    "Column": "YearMonth",
    "Value": "",
    "Message": "Total 行的 YearMonth 不能为空，且必须为 YYYYMM 格式。",
    "FixSuggestion": "填入六位纯数字，例如 202501。"
  }
  ```
  → Increment `errorCount` by `1`

**Step 4.3-A3 — Check YearMonth YYYYMM format (if not empty)**

- Run only when YearMonth is not empty (add inside the "If no" branch of Step 4.3-A2)
- **Action**: Condition
  - Expression:
    ```
    or(
      not(equals(length(trim(items('Apply_to_each')?['YearMonth'])), 6)),
      not(equals(string(int(trim(items('Apply_to_each')?['YearMonth']))), trim(items('Apply_to_each')?['YearMonth']))),
      less(int(substring(trim(items('Apply_to_each')?['YearMonth']), 4, 2)), 1),
      greater(int(substring(trim(items('Apply_to_each')?['YearMonth']), 4, 2)), 12)
    )
    ```
- If yes → Append Error issue with `RuleId: R-YM-001`, → Increment `errorCount`

**Step 4.3-A4 — Check ShoringRatio format (if not empty) on Total rows**

- **Action**: Condition
  - `trim(items('Apply_to_each')?['ShoringRatio'])` is NOT equal to `''`
- If not empty → run R-SR-001 validation (see Step 4.3-B3 below; same logic)

---

#### Branch B — FALSE (Non-Total rows)
> Enforce: **Target_YearEnd**, **Target_2030YearEnd**, **ShoringRatio** non-empty;  
> ShoringRatio must also satisfy strict percent format.

**Step 4.3-B1 — Check Target_YearEnd not empty**

- **Action**: Condition
  - `trim(items('Apply_to_each')?['Target_YearEnd'])` is equal to `''`
- If yes → Append to `issues`:
  ```json
  {
    "Severity": "Error",
    "RuleId": "R-TGT-001",
    "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
    "Function": "@{items('Apply_to_each')?['Function']}",
    "Column": "Target_YearEnd",
    "Value": "",
    "Message": "Non-Total 行的 Target_YearEnd 不能为空。",
    "FixSuggestion": "填入 Target_YearEnd 目标值（数字）。"
  }
  ```
  → Increment `errorCount` by `1`

**Step 4.3-B2 — Check Target_2030YearEnd not empty**

- **Action**: Condition
  - `trim(items('Apply_to_each')?['Target_2030YearEnd'])` is equal to `''`
- If yes → Append to `issues`:
  ```json
  {
    "Severity": "Error",
    "RuleId": "R-TGT-002",
    "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
    "Function": "@{items('Apply_to_each')?['Function']}",
    "Column": "Target_2030YearEnd",
    "Value": "",
    "Message": "Non-Total 行的 Target_2030YearEnd 不能为空。",
    "FixSuggestion": "填入 Target_2030YearEnd 目标值（数字）。"
  }
  ```
  → Increment `errorCount` by `1`

**Step 4.3-B3 — Check ShoringRatio: non-empty + strict percent format (R-SR-001)**

Sub-step B3a: Check not empty
- **Action**: Condition
  - `trim(items('Apply_to_each')?['ShoringRatio'])` is equal to `''`
- If yes → Append Error issue `RuleId: R-SR-001`, Message: `"ShoringRatio 不能为空（Non-Total 行必填）。"` → Increment `errorCount`

Sub-step B3b: Check format (run inside the "If no" branch of B3a)
- **Action**: Condition (expression, in If-no branch):
  ```
  not(
    and(
      endsWith(trim(items('Apply_to_each')?['ShoringRatio']), '%'),
      greater(
        length(trim(items('Apply_to_each')?['ShoringRatio'])),
        1
      )
    )
  )
  ```
  > Simple check: ends with `%` and has at least one digit before it.  
  > For more precision use `isMatch()` with pattern `^\d+(\.\d+)?%$` if available in your environment.
- If format fails → Append Error `RuleId: R-SR-001` → Increment `errorCount`

Sub-step B3c: Check range 0–100 (run only if format is valid)
- **Action**: Compose
  - Expression: `float(replace(trim(items('Apply_to_each')?['ShoringRatio']), '%', ''))`
  - Name: `srValue`
  > Note: Power Automate uses `float()` to convert a numeric string to a decimal number.  
  > Ensure the value is purely numeric before calling `float()` (guaranteed if the format check in B3b passed).  
  > If your environment requires it, wrap with a `try(float(...), -1)` guard to prevent runtime errors.
- **Action**: Condition
  - `outputs('srValue')` is less than `0` OR greater than `100`
- If yes → Append Error `RuleId: R-SR-001`, Message: `"ShoringRatio 超出范围（0%~100%）。"` → Increment `errorCount`

---

## Step 5 — Compose TopIssues Payload

- **Action**: Compose
- **Name**: `topIssuesPayload`
- **Inputs** (expression):
  ```
  {
    "file": "headcount_analysis.xlsx",
    "table": "tblOffshoring",
    "rowCount": @{variables('rowCount')},
    "errorCount": @{variables('errorCount')},
    "warningCount": @{variables('warningCount')},
    "topIssues": @{take(variables('issues'), 50)}
  }
  ```

---

## Step 6 — Send to Copilot Studio / AI Builder

- **Action**: HTTP or Copilot Studio connector
- **Input**: `outputs('topIssuesPayload')` as JSON body
- Request a report in Markdown format (摘要 + 问题明细表 + 待确认事项).

---

## Step 7 — Save Issues File to SharePoint (optional)

- **Action**: Create file (SharePoint connector)
- **Folder**: `/Reports/headcount-issues/`
- **File Name**: `issues_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmm')}.json`
- **File Content**: `outputs('topIssuesPayload')`

---

## Step 8 — Teams / Email Notification (optional)

- **Action**: Post message in chat (Teams connector) or Send an email (Outlook connector)
- Include: summary counts + SharePoint link to issues file.

---

## Branching Logic Summary

```
FOR EACH row IN tblOffshoring:
  rowCount += 1
  isTotal = Function IN {'Total (All)', 'Total (Core Operations)'}

  IF isTotal:
    CHECK Owner NOT EMPTY           → Error R-OWNER-TOTAL-001
    CHECK YearMonth NOT EMPTY       → Error R-YM-TOTAL-001
    CHECK YearMonth FORMAT YYYYMM   → Error R-YM-001 (if non-empty)
    IF ShoringRatio NOT EMPTY:
      CHECK ShoringRatio FORMAT     → Error R-SR-001

  ELSE (Non-Total):
    CHECK Target_YearEnd NOT EMPTY       → Error R-TGT-001
    CHECK Target_2030YearEnd NOT EMPTY   → Error R-TGT-002
    CHECK ShoringRatio NOT EMPTY         → Error R-SR-001
    IF ShoringRatio NOT EMPTY:
      CHECK ShoringRatio FORMAT          → Error R-SR-001
      CHECK ShoringRatio RANGE 0-100     → Error R-SR-001
```
