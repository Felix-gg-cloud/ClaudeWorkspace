# Offshoring Data Quality POC — Power Automate Flow Steps

> **Design principle**: Power Automate performs only mechanical row-reading from the Excel table. All validation logic is executed by the AI model (Copilot Studio / Azure OpenAI). Power Automate then renders the AI's JSON output into an Excel workbook and/or PDF report, saves the files to SharePoint, and notifies stakeholders.

---

## 0. Prerequisites

| Item | Details |
|------|---------|
| Excel source file | Stored in SharePoint; contains the `tblOffshoring` table |
| Excel report template | Pre-built `.xlsx` file stored in SharePoint at `.../OffshoringDQ/Templates/ReportTemplate.xlsx`; contains named tables `tblSummary`, `tblIssuesDetail`, `tblReportMeta` with placeholder rows |
| Copilot Studio agent | Published with the system prompt from `prompts/copilot-studio-validator-prompt.md` |
| SharePoint output folder | `.../OffshoringDQ/Reports/<YYYY>/` (created automatically by the flow if absent) |

---

## 1. Flow Overview

```
Trigger
  └─ Manual / Scheduled / HTTP Request
       │
       ├─ [Step 1] Read source file metadata
       ├─ [Step 2] List rows from tblOffshoring (paginated)
       ├─ [Step 3] Build reference data JSON (approved_functions, approved_pairs)
       ├─ [Step 4] Batch loop — call AI validator per batch
       │     └─ [Step 4a] Compose batch payload
       │     └─ [Step 4b] Call Copilot Studio / HTTP action
       │     └─ [Step 4c] Parse and accumulate issues + table_rows
       ├─ [Step 5] Merge batch results into final report_model
       ├─ [Step 6] Render Excel report
       │     └─ [Step 6a] Populate a Microsoft Excel template  — OR —
       │     └─ [Step 6b] Create CSV + Create File (fallback)
       ├─ [Step 7] Render PDF report (optional)
       ├─ [Step 8] Save files to SharePoint
       └─ [Step 9] Notify via Teams + Email
```

---

## 2. Step-by-Step Details

### Step 1 — Read Source File Metadata

**Action**: `Get file metadata` (SharePoint connector)

- **Site**: `<SharePoint site URL>`
- **Library**: `Shared Documents`
- **File path**: dynamic — passed in as flow input variable `flow_input_file_path`

**Compose variables**:
```
source_file_name    = outputs('Get_file_metadata')?['body/Name']
period_covered      = triggerBody()?['period_covered']   // e.g., "202501"
run_timestamp       = utcNow('yyyyMMddTHHmmss')          // e.g., "20250120T143022"
output_file_prefix  = concat('OffshoringDQ_', variables('period_covered'), '_', variables('run_timestamp'), 'Z')
```

---

### Step 2 — List Rows from tblOffshoring (Paginated)

**Action**: `List rows present in a table` (Excel Online (Business) connector)

- **Location**: SharePoint  
- **Document Library**: Shared Documents  
- **File**: `@{variables('flow_input_file_path')}`  
- **Table**: `tblOffshoring`

> **Pagination**: Enable `Pagination` in Settings → set threshold to `100000` to retrieve all rows regardless of count.  
> Power Automate does NOT pre-process, trim, or normalise any cell values. Raw values are passed as-is to the AI.

**Output**: Array of row objects stored in `variables('all_rows')`.

**Record total**: `set variable 'total_rows' = length(variables('all_rows'))`

---

### Step 3 — Build Reference Data JSON

**Action**: `Compose` (or fetch from a configuration SharePoint list)

Build the two reference objects as JSON strings to inject into each AI prompt batch:

```json
{
  "approved_functions": ["GBS", "Finance", "HR", "IT", "Operations", "Total"],
  "approved_pairs": [
    { "Function": "GBS",     "Team": "EA" },
    { "Function": "GBS",     "Team": "AP" },
    { "Function": "GBS",     "Team": "EMEA" },
    { "Function": "Finance", "Team": "AP" },
    { "Function": "Finance", "Team": "EMEA" },
    { "Function": "HR",      "Team": "Asia" },
    { "Function": "IT",      "Team": "Asia" },
    { "Function": "Operations", "Team": "AP" },
    { "Function": "Total",   "Team": "" },
    { "Function": "*",       "Team": "Total" }
  ]
}
```

> Maintain this configuration in a SharePoint list (recommended) so updates do not require Flow changes.

---

### Step 4 — Batch Loop: Call AI Validator

**Batch size**: 200 rows per batch (recommended for token budget management).  
Adjust down to 50–100 rows if the dataset has many wide columns or if token limits are hit.

**Variables to initialise before the loop**:
```
batch_number      = 1
row_offset        = 0
all_issues        = []   (empty array)
all_table_rows    = []   (empty array)
```

**Control**: `Do Until` loop — `row_offset >= total_rows`

#### Step 4a — Compose Batch Payload

```json
{
  "metadata": {
    "source_file": "@{variables('source_file_name')}",
    "period_covered": "@{variables('period_covered')}",
    "batch_info": {
      "batch_number": "@{variables('batch_number')}",
      "total_batches": "@{div(add(variables('total_rows'), 199), 200)}",
      "row_offset": "@{variables('row_offset')}"
    }
  },
  "reference": "@{variables('reference_data_json')}",
  "rows": "@{slice(variables('all_rows'), variables('row_offset'), add(variables('row_offset'), 200))}"
}
```

#### Step 4b — Call AI Validator

**Option A — Copilot Studio**:  
Use the `Send a message` action to the published Copilot Studio agent.  
Set `Message` = the composed batch payload string.

**Option B — Azure OpenAI (HTTP action)**:  
```
POST https://<resource>.openai.azure.com/openai/deployments/<deployment>/chat/completions?api-version=2024-02-01
Headers:
  api-key: <from Key Vault>
  Content-Type: application/json
Body:
{
  "messages": [
    { "role": "system", "content": "<full system prompt from copilot-studio-validator-prompt.md>" },
    { "role": "user",   "content": "<batch payload from Step 4a>" }
  ],
  "temperature": 0,
  "response_format": { "type": "json_object" }
}
```

> Set `temperature: 0` (or lowest available) for maximum determinism.  
> Use `response_format: json_object` if your deployment supports it.

#### Step 4c — Parse and Accumulate

**Action**: `Parse JSON` on the AI response body.

Use the schema from `prompts/copilot-studio-validator-prompt.md` → "Output JSON Schema Reference".

Append to accumulators:
```
Append to array variable 'all_issues'     ← each item in parsed body['issues']
Append to array variable 'all_table_rows' ← each item in parsed body['report_model']['table_rows']
```

Increment counters:
```
set 'row_offset'   = add(row_offset, 200)
set 'batch_number' = add(batch_number, 1)
```

---

### Step 5 — Merge Batch Results into Final report_model

After the loop, compute aggregated metrics from `all_issues`:

```
final_error_count   = length(filter(all_issues, item['Severity'] == 'Error'))
final_warning_count = length(filter(all_issues, item['Severity'] == 'Warning'))
final_pass_count    = total_rows - length(unique(map(all_issues, item['RowKey'])))
rows_with_issues    = length(unique(map(all_issues, item['RowKey'])))
pass_rate_pct       = round(mul(div(final_pass_count, float(total_rows)), 100.0), 1)
```

Build `rule_counts` array: group `all_issues` by `RuleId` + `Severity`, count each group, sort by count descending.

Build `top_issues` array: take the top 5 from `rule_counts`, and for each find the first matching issue's `RowKey`.

Compose final `report_model` JSON variable combining the above with metadata from Step 1.

---

### Step 6 — Render Excel Report

#### Option A (Preferred) — Populate a Microsoft Excel Template

**Action**: `Copy file` (SharePoint) → copy `ReportTemplate.xlsx` to the output folder with the new filename.

```
Destination file name: @{concat(variables('output_file_prefix'), '.xlsx')}
Destination folder:    /sites/<Site>/Shared Documents/OffshoringDQ/Reports/@{formatDateTime(utcNow(), 'yyyy')}/
```

Then use three `Add a row into a table` actions (Excel Online (Business)):

**Table: tblReportMeta** — add each key-value row from `report_model` (10 rows + rule_counts rows).

**Table: tblSummary** — loop over `report_model.table_rows` (Apply to each):
```json
{
  "RowNum":           "@{item()['row_num']}",
  "YearMonth":        "@{item()['year_month']}",
  "CostCenterNumber": "@{item()['cost_center_number']}",
  "Function":         "@{item()['function']}",
  "Team":             "@{item()['team']}",
  "IssueCount":       "@{item()['issue_count']}",
  "HighestSeverity":  "@{item()['highest_severity']}",
  "RuleIDs":          "@{item()['rule_ids']}"
}
```

**Table: tblIssuesDetail** — loop over `all_issues` (Apply to each):
```json
{
  "Severity":      "@{item()['Severity']}",
  "RuleId":        "@{item()['RuleId']}",
  "RowKey":        "@{item()['RowKey']}",
  "Column":        "@{item()['Column']}",
  "RawValue":      "@{item()['RawValue']}",
  "Message":       "@{item()['Message']}",
  "FixSuggestion": "@{item()['FixSuggestion']}",
  "Status":        "Open"
}
```

> **Performance note**: For datasets with >500 issues, use `Create CSV table` (see Option B) for the IssuesDetail sheet to avoid per-row API call overhead.

#### Option B (Fallback) — Create CSV + Upload

When the issues count is large (>500 rows) or the `Populate Excel template` connector is unavailable:

1. **Action**: `Create CSV table` from `all_issues` array.  
   Select columns: `Severity, RuleId, RowKey, Column, RawValue, Message, FixSuggestion`; add static `Status` = `"Open"`.

2. **Action**: `Create file` (SharePoint) — save as `OffshoringDQ_202501_20250120T143022Z_Issues.csv`.

3. Open in Excel by the user (Excel handles CSV with full auto-filter capability).

---

### Step 7 — Render PDF Report (Optional)

#### Approach A — HTML Template → Convert to PDF

1. **Action**: `Compose` — build an HTML string using the `report_model` variables:

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  body { font-family: Calibri, Arial, sans-serif; margin: 40px; }
  h1 { color: #2c3e50; }
  .metric { display: inline-block; margin: 8px 20px; text-align: center; }
  .metric .value { font-size: 2em; font-weight: bold; color: #c0392b; }
  .metric.pass .value { color: #27ae60; }
  table { border-collapse: collapse; width: 100%; font-size: 12px; margin-top: 16px; }
  th { background: #2c3e50; color: white; padding: 6px 8px; text-align: left; }
  td { border: 1px solid #ddd; padding: 5px 8px; }
  tr.error { background: #ffdddd; }
  tr.warning { background: #fff8dd; }
</style>
</head>
<body>
<h1>Offshoring Data Quality Validation Report</h1>
<p>Period: <b>@{variables('period_covered')}</b> &nbsp;|&nbsp;
   Generated: <b>@{variables('run_timestamp')}</b> UTC &nbsp;|&nbsp;
   Source: <b>@{variables('source_file_name')}</b></p>

<h2>Summary</h2>
<div class="metric"><div class="value">@{variables('total_rows')}</div>Rows Checked</div>
<div class="metric"><div class="value" style="color:#c0392b">@{variables('final_error_count')}</div>Errors</div>
<div class="metric"><div class="value" style="color:#e67e22">@{variables('final_warning_count')}</div>Warnings</div>
<div class="metric pass"><div class="value">@{variables('pass_rate_pct')}%</div>Pass Rate</div>

<h2>Top Issues</h2>
<table>
  <tr><th>Rule ID</th><th>Severity</th><th>Count</th><th>Example Row</th></tr>
  <!-- Power Automate: loop over top_issues array here -->
</table>

<h2>Issues Detail (first 200 shown)</h2>
<table>
  <tr><th>Severity</th><th>Rule</th><th>Row</th><th>Column</th><th>Raw Value</th><th>Message</th><th>Fix</th></tr>
  <!-- Power Automate: loop over all_issues (first 200) here -->
</table>
</body>
</html>
```

2. **Action**: `Create file` (SharePoint) — save as `.html`.

3. **Action**: `Convert file` (SharePoint connector) — select the HTML file, convert to PDF.

4. **Action**: `Create file` — save the PDF as `OffshoringDQ_202501_20250120T143022Z.pdf`.

#### Approach B — Word Template → PDF

1. Pre-build a Word `.docx` template with content controls mapped to report fields.
2. **Action**: `Populate a Microsoft Word template` (Word Online connector) — fill in `report_model` fields.
3. **Action**: `Convert file` — Word → PDF.

> Approach A is recommended for POC due to simpler setup. Approach B produces higher-fidelity formatting.

---

### Step 8 — Save Files to SharePoint

**Action**: `Create sharing link for a file or folder` (SharePoint) — create `View` links for the Excel and PDF files.

Store the URLs in variables: `excel_share_link`, `pdf_share_link`.

---

### Step 9 — Notify via Teams and Email

#### Teams Notification

**Action**: `Post message in a chat or channel` (Microsoft Teams connector)

**Message** (adaptive card or plain text):
```
✅ Offshoring DQ Report Ready
Period: @{variables('period_covered')}
Pass Rate: @{variables('pass_rate_pct')}% | Errors: @{variables('final_error_count')} | Warnings: @{variables('final_warning_count')}
📊 Open Excel: @{variables('excel_share_link')}
📄 Open PDF: @{variables('pdf_share_link')}
```

#### Email Notification

**Action**: `Send an email (V2)` (Office 365 Outlook connector)

- **To**: `@{triggerBody()?['requestor_email']}`
- **Subject**: `[OffshoringDQ] Report Ready — @{variables('period_covered')}`
- **Body** (HTML):
```html
<p>Your Offshoring Data Quality report for period <b>@{variables('period_covered')}</b> is ready.</p>
<ul>
  <li>Total rows checked: @{variables('total_rows')}</li>
  <li>Errors: @{variables('final_error_count')}</li>
  <li>Warnings: @{variables('final_warning_count')}</li>
  <li>Pass rate: @{variables('pass_rate_pct')}%</li>
</ul>
<p>
  <a href="@{variables('excel_share_link')}">Open Excel Report</a> &nbsp;|&nbsp;
  <a href="@{variables('pdf_share_link')}">Open PDF Report</a>
</p>
<p>Files are stored at: <code>SharePoint → OffshoringDQ/Reports/@{formatDateTime(utcNow(), 'yyyy')}/</code></p>
```

---

## 3. Large Dataset Handling

| Scenario | Recommendation |
|----------|---------------|
| < 200 rows | Single AI batch call; no loop needed |
| 200–2000 rows | Batch size 200 rows; loop with Do Until |
| > 2000 rows | Batch by `YearMonth` group first (natural partition); then 200-row sub-batches within each group |
| > 500 issues in output | Use CSV option (Option B in Step 6) for IssuesDetail; Excel row-by-row add actions will be slow |
| AI timeout | Reduce batch size to 50–100 rows; enable retry policy on the HTTP action |

---

## 4. File Naming Convention

| File type | Pattern | Example |
|-----------|---------|---------|
| Excel report | `OffshoringDQ_{YYYYMM}_{YYYYMMDDTHHmmss}Z.xlsx` | `OffshoringDQ_202501_20250120T143022Z.xlsx` |
| PDF report | `OffshoringDQ_{YYYYMM}_{YYYYMMDDTHHmmss}Z.pdf` | `OffshoringDQ_202501_20250120T143022Z.pdf` |
| Issues CSV (fallback) | `OffshoringDQ_{YYYYMM}_{YYYYMMDDTHHmmss}Z_Issues.csv` | `OffshoringDQ_202501_20250120T143022Z_Issues.csv` |
| Excel template | `ReportTemplate.xlsx` | (static; not renamed) |

**SharePoint folder structure**:
```
/sites/<Site>/Shared Documents/OffshoringDQ/
├── Templates/
│   └── ReportTemplate.xlsx
└── Reports/
    ├── 2025/
    │   ├── OffshoringDQ_202501_20250120T143022Z.xlsx
    │   ├── OffshoringDQ_202501_20250120T143022Z.pdf
    │   └── OffshoringDQ_202502_20250220T091500Z.xlsx
    └── 2026/
        └── ...
```

---

## 5. Error Handling

Add the following to the flow:

| Step | Error handling |
|------|---------------|
| Step 2 (List rows) | Configure Run After: failed → send email "Source file read failed" |
| Step 4b (AI call) | Set retry policy: 3 retries, 30-second intervals; on final failure, log error and skip batch |
| Step 6 (Excel render) | On failure, fall back to CSV creation |
| Any step | Scope action wrapping each major section; on failure → `Send an email` with error details |
