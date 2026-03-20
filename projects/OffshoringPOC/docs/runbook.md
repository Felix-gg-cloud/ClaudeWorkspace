# Offshoring Data Quality POC — Runbook

> **Version**: 2.0  
> **Mode**: AI-only validation (no deterministic preprocessing in Power Automate)  
> **Report formats**: Excel (.xlsx) and PDF — Markdown is NOT used as an end-user output

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Validation Mode: AI-Only](#3-validation-mode-ai-only)
4. [Report Output: Excel and PDF](#4-report-output-excel-and-pdf)
5. [SharePoint Storage and Sharing](#5-sharepoint-storage-and-sharing)
6. [Running the POC End-to-End](#6-running-the-poc-end-to-end)
7. [Troubleshooting](#7-troubleshooting)
8. [Glossary](#8-glossary)

---

## 1. Overview

This POC validates Offshoring headcount planning data submitted in an Excel table (`tblOffshoring`) against a defined set of data quality rules. The validation is performed by an AI model (Copilot Studio / Azure OpenAI). The results are rendered into an **Excel workbook** and optionally a **PDF report**, both stored on SharePoint and shared via Teams and email.

**No Markdown output is produced for end users.** All user-facing artifacts are office-friendly files (Excel, PDF) that can be opened without any additional software.

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| AI-only validation | Rules are detailed enough for deterministic AI execution; avoids complex Power Automate expression logic |
| No preprocessing in Power Automate | Raw cell values (including spaces, empty strings) are passed directly to the AI so low-level errors (e.g., accidental spaces) are caught |
| Excel + PDF output | Users may not have Markdown viewers; Excel and PDF are universally accessible in a Microsoft 365 environment |
| Structured JSON as AI output | The AI produces a machine-readable JSON payload that Power Automate renders into the report files |

---

## 2. Architecture

```
┌─────────────────┐      ┌──────────────────────┐      ┌──────────────────────────┐
│  Data Owner      │      │  Power Automate Flow  │      │  AI Model                │
│  (submits Excel) │─────▶│  1. Read rows (raw)   │─────▶│  (Copilot Studio /       │
└─────────────────┘      │  2. Build batch        │      │   Azure OpenAI)          │
                         │  3. Call AI (loop)     │◀─────│  Returns JSON:           │
                         │  4. Merge results      │      │  - report_model{}        │
                         │  5. Render Excel/PDF   │      │  - issues[]              │
                         │  6. Save to SharePoint │      └──────────────────────────┘
                         │  7. Notify Teams/Email │
                         └──────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    ▼                             ▼
          SharePoint Excel report        SharePoint PDF report
          (tblSummary + tblIssuesDetail  (cover + summary +
           + tblReportMeta)               issues table)
```

**Files in this repository**:

| Path | Purpose |
|------|---------|
| `rules/validation-rules.md` | Human-readable validation rule catalogue (source of truth) |
| `prompts/copilot-studio-validator-prompt.md` | Copilot Studio system + user prompt template; defines JSON output contract |
| `flow/power-automate-flow-steps.md` | Step-by-step Power Automate flow guide |
| `templates/excel-template-schema.json` | Excel template schema: worksheet names, columns, conditional formatting |
| `samples/sample-output.json` | Sample AI JSON output (for testing and template development) |
| `samples/sample-rendered-description.md` | Description of what the rendered Excel and PDF look like |

---

## 3. Validation Mode: AI-Only

### How It Works

1. Power Automate reads all rows from `tblOffshoring` using `List rows present in a table`. **No trimming, normalisation, or transformation is applied.**
2. Rows are batched (200 rows per batch) and sent to the AI model along with the full rule catalogue and reference data (approved functions list, Function-Team mapping).
3. The AI model applies every rule deterministically, using the exact raw cell values. It returns a structured JSON object containing:
   - `issues[]` — one entry per rule violation
   - `report_model{}` — aggregated metrics and a per-row status table
4. Power Automate merges results from all batches and renders the final report.

### Why AI-Only (Not Power Automate Expressions)

- The rule catalogue (see `rules/validation-rules.md`) is comprehensive and unambiguous — it is suitable for deterministic AI execution.
- Maintaining complex validation logic in Power Automate expressions is fragile and hard to update. The AI prompt can be updated without changing the Flow structure.
- AI output variability is minimised by: using `temperature: 0`, requiring structured JSON output only, and providing explicit boundary conditions in the prompt.

### Whitespace Handling (Critical)

> **R-WS-ALL-001**: Any cell value that contains leading or trailing whitespace is an **Error**, regardless of which column it is in.

Power Automate does NOT trim values before sending them to the AI. This is intentional — the goal is to catch low-level input errors (e.g., a user accidentally typing `"GBS "` instead of `"GBS"`). The AI receives the raw value and flags it as a whitespace error. The `RawValue` field in the issue record preserves the exact problematic value for the data owner to see.

---

## 4. Report Output: Excel and PDF

### 4.1 Excel Workbook

The Excel workbook contains three worksheets:

#### ReportMeta
- Summary metrics (total rows, error count, warning count, pass rate)
- Rule counts table (how many issues per rule, sorted by frequency)

#### Summary
- One row per input data row
- Columns: `RowNum`, `YearMonth`, `CostCenterNumber`, `Function`, `Team`, `IssueCount`, `HighestSeverity`, `RuleIDs`
- Colour-coded: 🟥 Red = Error, 🟨 Yellow = Warning, 🟩 Green = Pass
- Auto-filter enabled — users can filter to show only problematic rows

#### IssuesDetail
- One row per issue (validation rule violation)
- Columns: `Severity`, `RuleId`, `RowKey`, `Column`, `RawValue`, `Message`, `FixSuggestion`, `Status`, `FixedBy`, `FixedDate`
- The `Status` column (`Open` / `Fixed` / `Accepted`) is for data owners to track corrections
- Auto-filter enabled

**Template**: The Excel template (`ReportTemplate.xlsx`) is pre-configured with:
- Named tables with the correct column headers
- Conditional formatting rules (red/yellow/green)
- Frozen header rows and auto-filter

Power Automate uses the `Populate a Microsoft Excel template` action (or `Copy file` + `Add a row into a table`) to fill the template with report data.

**File naming**: `OffshoringDQ_{YYYYMM}_{YYYYMMDDTHHmmss}Z.xlsx`  
Example: `OffshoringDQ_202501_20250120T143022Z.xlsx`

### 4.2 PDF Report

The PDF provides a printable, read-only summary for stakeholders who do not need to interact with the data.

**Layout sections**:
1. **Cover / Summary** — report title, period, timestamp, source file, key metrics (total rows, errors, warnings, pass rate)
2. **Top 5 Issues** — the five most frequent rule violations with their counts
3. **Row Status Overview** — condensed summary table (one row per input row, with colour-coded severity indicator)
4. **Issues Detail Table** — full issues list (multi-page if needed; headers repeat on each page)

**Generation method**: Power Automate composes an HTML string from the `report_model` data, saves it as an HTML file, then uses the SharePoint `Convert file` action to produce a PDF. Alternatively, a Word template can be used for higher-fidelity formatting.

**File naming**: `OffshoringDQ_{YYYYMM}_{YYYYMMDDTHHmmss}Z.pdf`

---

## 5. SharePoint Storage and Sharing

### Folder Structure

```
/sites/<SiteName>/Shared Documents/OffshoringDQ/
├── Templates/
│   └── ReportTemplate.xlsx          ← pre-configured Excel template (do not delete)
└── Reports/
    ├── 2025/
    │   ├── OffshoringDQ_202501_20250120T143022Z.xlsx
    │   ├── OffshoringDQ_202501_20250120T143022Z.pdf
    │   └── OffshoringDQ_202502_20250220T091500Z.xlsx
    └── 2026/
```

- Reports are organised by year (sub-folder auto-created by the Flow if absent).
- Files are retained indefinitely for audit purposes. Archival policy is managed by the SharePoint site administrator.

### Sharing

After saving the files, the Flow:
1. Creates shareable view-only links for both the Excel and PDF files.
2. Posts a message to the designated **Microsoft Teams channel** with the links.
3. Sends an **email** to the requestor with the same links and a summary of key metrics.

Users do not need to navigate to SharePoint manually — the link in Teams/email takes them directly to the file.

---

## 6. Running the POC End-to-End

### Prerequisites

- [ ] Excel source file uploaded to SharePoint with table named `tblOffshoring`
- [ ] `ReportTemplate.xlsx` uploaded to `.../OffshoringDQ/Templates/`
- [ ] Copilot Studio agent published with prompt from `prompts/copilot-studio-validator-prompt.md`
- [ ] Power Automate flow configured per `flow/power-automate-flow-steps.md`
- [ ] SharePoint output folder `.../OffshoringDQ/Reports/` created

### Triggering the Flow

The flow supports three trigger modes:

| Trigger | Use case |
|---------|----------|
| **Manual trigger** (Power Automate portal) | Ad-hoc validation during POC |
| **Scheduled (Recurrence)** | Automated monthly run (e.g., 1st of each month) |
| **HTTP Request** | Integration with other systems or a Power Apps button |

**Manual trigger inputs**:
- `flow_input_file_path` — SharePoint path to the source Excel file
- `period_covered` — YearMonth string (e.g., `202501`)
- `requestor_email` — email address to receive the report notification

### Expected Outputs

| Output | Location | Description |
|--------|----------|-------------|
| Excel report | SharePoint Reports folder | Workbook with Summary, IssuesDetail, ReportMeta sheets |
| PDF report | SharePoint Reports folder | Printable summary for stakeholders |
| Teams message | Designated Teams channel | Link to both report files + key metrics |
| Email | Requestor's inbox | Same as Teams message |

### Estimated Run Time

| Dataset size | Expected duration |
|-------------|-------------------|
| < 200 rows | ~2–3 minutes |
| 200–1000 rows | ~5–10 minutes |
| 1000–5000 rows | ~15–30 minutes |

---

## 7. Troubleshooting

| Symptom | Likely cause | Resolution |
|---------|-------------|------------|
| Flow fails at "List rows" | Source file path incorrect or table not named `tblOffshoring` | Verify file path in flow input; ensure table is named exactly `tblOffshoring` |
| AI returns non-JSON or malformed JSON | Prompt injection in data; model temperature too high | Check source data for unusual characters; set temperature to 0; add retry logic |
| Excel report has empty sheets | `Add a row` action failed silently | Check flow run history; ensure column names match template exactly |
| Issues count is 0 but data looks wrong | AI may have processed trimmed values if upstream step accidentally normalised data | Verify Power Automate "List rows" passes raw values; check flow variables |
| PDF generation fails | HTML too large for SharePoint convert action | Limit issues detail table to first 200 rows in HTML; full data is in Excel |
| Flow runs very slowly (> 30 min) | Too many `Add a row` calls for large issue counts | Switch IssuesDetail to CSV creation (Option B in flow steps) |

---

## 8. Glossary

| Term | Definition |
|------|-----------|
| `tblOffshoring` | Excel table name in the source file containing headcount planning data |
| `tblSummary` | Excel table in the report template for per-row validation status |
| `tblIssuesDetail` | Excel table in the report template for the full issues list |
| `tblReportMeta` | Excel table in the report template for report metadata and aggregated metrics |
| `report_model` | Top-level JSON object returned by the AI containing title, metrics, rule counts, and table rows |
| `issues[]` | JSON array in the AI output; each element represents one rule violation |
| `RawValue` | The exact cell value as read from the source Excel, before any transformation |
| `RowKey` | Composite string uniquely identifying a source row: `RowIndex=N\|YearMonth=...\|CCN=...\|Function=...\|Team=...` |
| Non-Total row | A row where neither `Function` nor `Team` equals `"Total"` |
| Total row | A row where `Function == "Total"` or `Team == "Total"` |
| R-WS-ALL-001 | Rule ID for the global whitespace check (any leading/trailing space = Error) |
