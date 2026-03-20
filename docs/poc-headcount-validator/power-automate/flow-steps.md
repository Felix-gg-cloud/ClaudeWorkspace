# Power Automate Flow — Step-by-Step Build Instructions

## Overview

**Flow name**: `HC-Offshoring-Validator`  
**Trigger**: Manually triggered from Copilot Studio (HTTP request) or on file upload to SharePoint  
**Output**: Compact payload (summary + top 10 issues) returned to Copilot Studio; full CSV saved to SharePoint

---

## Prerequisites

| Item | Detail |
|------|--------|
| SharePoint site | e.g., `https://contoso.sharepoint.com/sites/KPIReview` |
| Input Excel path | `/Shared Documents/Headcount/headcount_analysis.xlsx` |
| Output folder | `/Shared Documents/Headcount/ValidationReports/` |
| Excel table name | `OffshoringData` (Excel must have the data formatted as a Table starting at row 4) |
| Connection | SharePoint (service account or delegated) |
| Copilot Studio connection | Power Automate connector in Copilot Studio topic |

---

## Step 1 — Trigger

**Action**: `When an HTTP request is received` (for Copilot Studio call)  
*OR*  
**Action**: `When a file is created or modified (properties only)` — SharePoint connector, pointing to the headcount folder

**HTTP Body Schema** (if using HTTP trigger):
```json
{
  "type": "object",
  "properties": {
    "site_url": { "type": "string" },
    "file_path": { "type": "string" }
  }
}
```

> **Tip**: For the POC demo, use the manual HTTP trigger so you can call it from Copilot Studio via the "Call an action" node.

---

## Step 2 — Get Excel Rows from SharePoint

**Action**: `List rows present in a table`  
**Connector**: Excel Online (Business)

| Parameter | Value |
|-----------|-------|
| Location | SharePoint Site (select your site) |
| Document Library | `Shared Documents` |
| File | `/Headcount/headcount_analysis.xlsx` |
| Table | `OffshoringData` |

**Output variable**: `ExcelRows` (array of objects)

> **Note**: If the Excel is not yet formatted as a Table, instruct the user to: select rows from row 4 downward → Insert → Table → "My table has headers".

---

## Step 3 — Initialize Issue Tracking Variables

Add the following **Initialize variable** actions before the loop:

| Variable Name | Type | Initial Value |
|--------------|------|---------------|
| `IssuesArray` | Array | `[]` |
| `ErrorCount` | Integer | `0` |
| `WarningCount` | Integer | `0` |
| `RowIndex` | Integer | `0` |

---

## Step 4 — Loop: Validate Each Row

**Action**: `Apply to each`  
**Input**: `ExcelRows`  

Inside the loop, add a **Compose** action to extract key fields for readability:

```
YearMonth         = items('Apply_to_each')?['YearMonth']
CostCenter        = items('Apply_to_each')?['Cost Center Number']
Function          = items('Apply_to_each')?['Function']
Team              = items('Apply_to_each')?['Team']
Owner             = items('Apply_to_each')?['Owner']
Headcount         = items('Apply_to_each')?['Headcount']
OffshoreHC        = items('Apply_to_each')?['Offshore HC']
OnshoreHC         = items('Apply_to_each')?['Onshore HC']
ShoringRatio      = items('Apply_to_each')?['Shoring Ratio']
CostUSD           = items('Apply_to_each')?['Cost (USD)']
BudgetUSD         = items('Apply_to_each')?['Budget (USD)']
```

### Step 4a — COL-001: Cost Center Number (7-digit check)

**Action**: `Condition`  
**Expression**:
```
not(and(
  greater(length(string(outputs('CostCenter'))), 0),
  equals(length(string(outputs('CostCenter'))), 7),
  equals(string(int(string(outputs('CostCenter')))), string(outputs('CostCenter')))
))
```

**If true** → Append to `IssuesArray`:
```json
{
  "Severity": "Error",
  "RuleId": "COL-001",
  "YearMonth": "@{outputs('YearMonth')}",
  "Cost Center Number": "@{outputs('CostCenter')}",
  "Function": "@{outputs('Function')}",
  "Team": "@{outputs('Team')}",
  "Column": "Cost Center Number",
  "Value": "@{outputs('CostCenter')}",
  "Message": "Cost Center Number is not a valid 7-digit number",
  "FixSuggestion": "Pad with leading zeros or correct to exactly 7 digits"
}
```
Also increment `ErrorCount` by 1.

### Step 4b — COL-002: Function whitelist check

**Action**: `Condition`  
**Expression**: Use `contains` with a hardcoded array of allowed values (copy from `function_whitelist.txt`):
```
not(contains(
  createArray('Finance','Actuarial','IT','Operations','Claims','Underwriting','Risk Management','HR','Legal & Compliance','Customer Service'),
  outputs('Function')
))
```

**If true** → Append Error issue (RuleId: `COL-002`) + increment `ErrorCount`.

### Step 4c — COL-003: Team whitelist check

Same pattern as Step 4b but use the Team whitelist array and RuleId `COL-003`.

### Step 4d — COL-004: Owner non-empty check

**Expression**:
```
or(
  empty(outputs('Owner')),
  equals(trim(string(outputs('Owner'))), ''),
  equals(trim(string(outputs('Owner'))), 'N/A')
)
```

**If true** → Append Error issue (RuleId: `COL-004`) + increment `ErrorCount`.

### Step 4e — COL-005: YearMonth format check

**Expression** (check 6-digit numeric, month 01-12):
```
not(and(
  equals(length(string(outputs('YearMonth'))), 6),
  greater(int(string(outputs('YearMonth'))), 190000),
  less(int(string(outputs('YearMonth'))), 210000),
  lessOrEquals(int(substring(string(outputs('YearMonth')), 4, 2)), 12),
  greaterOrEquals(int(substring(string(outputs('YearMonth')), 4, 2)), 1)
))
```

**If true** → Append Error issue (RuleId: `COL-005`) + increment `ErrorCount`.

### Step 4f — COL-006: Numeric columns non-negative

For each numeric column (`Headcount`, `Offshore HC`, `Onshore HC`, `Cost (USD)`, `Budget (USD)`), add a Condition:
```
or(
  empty(outputs('Headcount')),
  less(float(string(outputs('Headcount'))), 0)
)
```

**If true** → Append Error issue (RuleId: `COL-006`, Column = `Headcount`) + increment `ErrorCount`.

Repeat for each numeric column.

### Step 4g — COL-007: Shoring Ratio normalization and range check

**Action**: Use a **Compose** to normalize:
```
if(
  endsWith(string(outputs('ShoringRatio')), '%'),
  float(replace(string(outputs('ShoringRatio')), '%', '')),
  if(
    lessOrEquals(float(string(outputs('ShoringRatio'))), 1),
    mul(float(string(outputs('ShoringRatio'))), 100),
    float(string(outputs('ShoringRatio')))
  )
)
```

**Condition**: Normalized value must be ≥ 0 AND ≤ 100.  
**If false** → Append Error issue (RuleId: `COL-007`) + increment `ErrorCount`.

### Step 4h — XF-001: Function-Team mapping check

**Action**: `Condition`  
Build the key as `"Function|Team"` and check against a hardcoded array:
```
not(contains(
  createArray(
    'Finance|Finance Reporting','Finance|Finance Analytics',
    'Actuarial|Actuarial Modeling','Actuarial|Actuarial Pricing',
    'IT|IT Development','IT|IT Infrastructure','IT|IT Testing',
    'Operations|Operations Processing','Operations|Operations Quality',
    'Claims|Claims Assessment','Claims|Claims Settlement',
    'Underwriting|Underwriting Support',
    'Risk Management|Risk Analytics',
    'HR|HR Business Partner','HR|HR Shared Services',
    'Legal & Compliance|Legal Support','Legal & Compliance|Compliance Monitoring',
    'Customer Service|Customer Contact'
  ),
  concat(outputs('Function'), '|', outputs('Team'))
))
```

**If true** → Append Error issue (RuleId: `XF-001`) + increment `ErrorCount`.

### Step 4i — XF-002: Offshore HC ≤ Headcount (Warning)

**Expression**:
```
and(
  not(empty(outputs('OffshoreHC'))),
  not(empty(outputs('Headcount'))),
  greater(float(string(outputs('OffshoreHC'))), float(string(outputs('Headcount'))))
)
```

**If true** → Append Warning issue (RuleId: `XF-002`) + increment `WarningCount`.

### Step 4j — Increment Row Index

**Action**: `Increment variable` — `RowIndex` by 1.

---

## Step 5 — Cross-Row: OVR-004 Month Coverage Check (Warning)

After the loop, add cross-row logic:

**Action**: `Select` — extract distinct YearMonth values from ExcelRows.  
**Action**: For each YearMonth, check all expected (Function, Team) pairs from the mapping exist in that month's rows.

> **Implementation tip**: Use a nested `Filter array` for each expected pair:
```
Filter ExcelRows where YearMonth = currentYM AND Function = expectedF AND Team = expectedT
→ if result is empty → append OVR-004 warning
```

Increment `WarningCount` for each missing combination.

---

## Step 6 — Build Compact Payload

**Action**: `Compose` — build the compact JSON payload:

```json
{
  "filename": "headcount_analysis.xlsx",
  "timestamp": "@{utcNow()}",
  "total_rows": "@{length(variables('ExcelRows'))}",
  "summary": {
    "total_errors": "@{variables('ErrorCount')}",
    "total_warnings": "@{variables('WarningCount')}",
    "rules_triggered": "@{union(...)}"
  },
  "top_issues": "@{take(variables('IssuesArray'), 10)}",
  "sharepoint_link": "https://contoso.sharepoint.com/sites/KPIReview/ValidationReports/issues_@{formatDateTime(utcNow(),'yyyyMMdd')}.csv"
}
```

**Variable**: `CompactPayload` (string — use `string(outputs('Compose'))`)

---

## Step 7 — Save Full Issues as CSV to SharePoint

**Action**: `Create CSV table`  
**Input**: `IssuesArray`  
**Columns**: `Severity, RuleId, YearMonth, Cost Center Number, Function, Team, Column, Value, Message, FixSuggestion`

**Action**: `Create file`  
**Connector**: SharePoint  

| Parameter | Value |
|-----------|-------|
| Site address | `https://contoso.sharepoint.com/sites/KPIReview` |
| Folder path | `/Shared Documents/Headcount/ValidationReports` |
| File name | `issues_@{formatDateTime(utcNow(),'yyyyMMdd_HHmmss')}.csv` |
| File content | Output of "Create CSV table" |

---

## Step 8 — Return Result to Copilot Studio

**Action**: `Respond to a Power Virtual Agents or Copilot Studio` (if triggered from Copilot Studio)  

Return the following outputs:

| Output Name | Type | Value |
|-------------|------|-------|
| `CompactPayload` | String | `variables('CompactPayload')` |
| `SharePointLink` | String | Full SharePoint file URL |
| `ErrorCount` | Integer | `variables('ErrorCount')` |
| `WarningCount` | Integer | `variables('WarningCount')` |

---

## Step 9 — Copilot Studio: Generative AI Narration

In Copilot Studio, after calling the Power Automate action:

1. **Get variable**: `CompactPayload` from flow output.
2. **Add a Generative AI node** (or AI Builder "Create text with GPT"):
   - **Input**: `CompactPayload`
   - **Prompt**: Use the template from `copilot/prompt.md`
   - **Output variable**: `ValidationReport`
3. **Send message**: Display `ValidationReport` to the user (Markdown card).
4. **Optional**: Send a Teams adaptive card with `ErrorCount`, `WarningCount`, and the SharePoint link.

---

## Error Handling Tips

| Scenario | Handling |
|----------|----------|
| Excel file not found | Add `Configure run after` on the "List rows" step; return friendly message |
| Excel not formatted as Table | Pre-check with `Get file metadata`; guide user to format as Table |
| Empty file (0 rows) | Condition after Step 2; return "No data rows found" |
| Shoring Ratio non-numeric | Wrap float conversion in a try/catch using `if(equals(...), ...)` |
| Flow timeout (>30 min) | Limit rows processed with `top` parameter in "List rows" action (max 5000); use pagination for larger sets |

---

## Aggregating Counts per Rule and Severity

After the loop (Step 4), add a **Select + Group By** step to produce the rule summary:

**Action**: `Select` — extract `RuleId` from each issue  
**Action**: Custom aggregation using `reduce` expression (or use a Dictionary variable inside the loop to count per RuleId)

> **Shortcut for POC**: Count using filter expressions after the loop:
```
length(filter(variables('IssuesArray'), item()?['RuleId'] == 'COL-001'))
```
Repeat per RuleId and add to summary.

---

## Testing the Flow

1. Upload a test Excel with known bad rows to SharePoint.
2. Run the flow manually (trigger: "Test" button in Power Automate).
3. Inspect `IssuesArray` in the run history to verify each rule fires correctly.
4. Call from Copilot Studio → confirm `ValidationReport` is returned and displayed.
5. Open the SharePoint CSV link and verify the full issues list is there.
