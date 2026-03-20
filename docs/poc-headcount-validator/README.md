# POC: Headcount Offshoring Validator

A ready-to-use reference package for validating `headcount_analysis.xlsx` (sheet: **Offshoring**) using **Copilot Studio + Power Automate**.

---

## 📁 Package Structure

```
docs/poc-headcount-validator/
├── README.md                          ← This file
├── rules/
│   ├── function_whitelist.txt         ← Allowed values for the Function column
│   ├── team_whitelist.txt             ← Allowed values for the Team column
│   ├── function_team_mapping.csv      ← Allowed (Function, Team) combinations
│   └── rule_catalog.md               ← Full rule definitions (RuleId, Severity, Logic, Message)
├── copilot/
│   └── prompt.md                      ← Copilot Studio Generative AI prompt template
├── power-automate/
│   └── flow-steps.md                  ← Step-by-step Power Automate build instructions
└── sample-output/
    └── poc_report_example.md          ← Example report output for demo/testing
```

---

## 🎯 Goals

This POC validates a monthly headcount offshoring Excel file **before** it reaches Power BI, catching data quality issues early so KPI reports remain accurate.

| Deliverable | Purpose |
|-------------|---------|
| Rule catalog | Define all column-level, cross-field, and cross-row validation rules with IDs |
| Whitelists & mapping | Reference data for Function, Team, and allowed (Function, Team) combinations |
| Power Automate flow | Automated validation engine; reads Excel from SharePoint, produces issues array |
| Copilot Studio prompt | Narrates structured results as a human-readable Markdown report |
| Sample output | Expected format for demo and user acceptance testing |

---

## 📋 Excel File Assumptions

| Property | Value |
|----------|-------|
| File name | `headcount_analysis.xlsx` |
| Sheet | `Offshoring` |
| Header row | **Row 4** (user normalizes multi-level header; row 4 becomes column headers) |
| Excel Table name | `OffshoringData` (must be formatted as Excel Table for Power Automate) |
| SharePoint location | `/Shared Documents/Headcount/headcount_analysis.xlsx` |

### Required Columns (as they appear in Row 4)

| Column Name | Type | Notes |
|-------------|------|-------|
| `YearMonth` | Text | Format: `YYYYMM` e.g. `202503` |
| `Cost Center Number` | Text | Exactly 7 digits |
| `Function` | Text | Must be in `function_whitelist.txt` |
| `Team` | Text | Must be in `team_whitelist.txt` |
| `Owner` | Text | Non-empty |
| `Headcount` | Number | Non-negative |
| `Offshore HC` | Number | Non-negative |
| `Onshore HC` | Number | Non-negative |
| `Shoring Ratio` | Text/Number | Accepted: `23`, `23%`, `0.23` → normalized to 0–100 |
| `Cost (USD)` | Number | Non-negative |
| `Budget (USD)` | Number | Non-negative |

> **Tip**: Before running the flow, open the Excel file and format the data range as an Excel Table (Insert → Table) starting from Row 4. Name the table `OffshoringData`.

---

## 🚀 Setup Steps

### 1. Upload Excel to SharePoint
- Upload `headcount_analysis.xlsx` to your SharePoint site under `/Shared Documents/Headcount/`.
- Open the file in Excel Online, navigate to the **Offshoring** sheet.
- Select from Row 4 downward (all data rows) → Insert → Table → "My table has headers" → Name the table `OffshoringData`.

### 2. Build the Power Automate Flow
- Follow the detailed instructions in [`power-automate/flow-steps.md`](power-automate/flow-steps.md).
- Copy the Function and Team whitelist values from `rules/function_whitelist.txt` and `rules/team_whitelist.txt` into the corresponding hardcoded arrays in the flow.
- Copy the mapping key strings from `rules/function_team_mapping.csv` into the XF-001 condition array (format: `"Function|Team"`).

### 3. Configure Copilot Studio
- Create a new topic (e.g., **"Validate Headcount Data"**).
- Add a trigger phrase: "帮我检查这个文件 (Help me check this file)" or "Validate headcount data".
- Add a **Call an action** node → select `HC-Offshoring-Validator` flow.
- Add a **Generative AI** node → paste the prompt from [`copilot/prompt.md`](copilot/prompt.md).
- Pass `CompactPayload` from the flow output as the prompt input variable.
- Display the `ValidationReport` output as a message.

### 4. Test the Flow
- Upload a test Excel with known bad rows.
- Trigger the flow from Copilot Studio or manually.
- Compare output against [`sample-output/poc_report_example.md`](sample-output/poc_report_example.md).

---

## 📏 Validation Rules Summary

See [`rules/rule_catalog.md`](rules/rule_catalog.md) for the complete rule definitions.

| Category | Rules | Severity |
|----------|-------|----------|
| Column-level | COL-001 to COL-007 | Error |
| Cross-field (same row) | XF-001 to XF-004 | Error / Warning |
| Cross-row / overall | OVR-001 to OVR-005 | Error / Warning |

**Key rules at a glance**:
- `COL-001`: Cost Center Number must be exactly 7 digits
- `COL-002`: Function must be in the whitelist
- `COL-003`: Team must be in the whitelist
- `COL-007`: Shoring Ratio normalized to 0–100% (accepts `23`, `23%`, `0.23`)
- `XF-001`: (Function, Team) combination must be in the approved mapping
- `OVR-004`: Each month should have all expected (Function, Team) combinations (Warning)

---

## 📊 Output Format

### Issues Array Fields

| Field | Description |
|-------|-------------|
| `Severity` | `Error` or `Warning` |
| `RuleId` | e.g., `COL-002`, `XF-001`, `OVR-004` |
| `YearMonth` | Affected period, e.g., `202503` |
| `Cost Center Number` | From the row |
| `Function` | From the row |
| `Team` | From the row |
| `Column` | Which column triggered the rule |
| `Value` | The actual invalid value |
| `Message` | Human-readable description |
| `FixSuggestion` | How to correct the issue |

### Compact Payload (sent to Copilot Studio)
- Summary counts (total errors, warnings, rules triggered)
- Top 10 issues only (never send full dataset to LLM)
- SharePoint link to full CSV report

---

## 🎬 Demo Script

1. **Setup** (before demo): Upload the test Excel to SharePoint; confirm the table is formatted correctly.
2. **Open Copilot Studio** in Teams or web.
3. **User says**: "帮我检查这个文件 (Help me check this file)" (or "Validate headcount data").
4. **Flow runs**: Copilot Studio calls the Power Automate flow → reads Excel → validates rows → returns compact payload.
5. **AI narration**: Generative AI node processes the payload → produces Markdown report.
6. **Show report**: Point out Error count, top issues, and Next Steps.
7. **Show SharePoint link**: Click the CSV link to show the full issues list.
8. **Q&A**: Ask the agent "Which function has the most errors?" → agent answers from the payload context.

---

## ⚠️ Constraints & Notes

| Constraint | Detail |
|-----------|--------|
| Data volume | Tested for up to ~5,000 rows. Power Automate "List rows" defaults to 256; enable pagination for larger datasets. |
| LLM token limit | Never pass more than 10 issues + summary to the AI node. Pre-filter in Power Automate. |
| Case sensitivity | Whitelist values are case-sensitive. `finance` ≠ `Finance`. Trim whitespace before comparison. |
| Shoring Ratio | Normalize in Power Automate before comparison. Accept `23`, `23%`, `0.23` → all treated as 23%. |
| OVR-004 coverage | Warning only; does not block reporting. Investigate with the data owner if combinations are intentionally absent. |
| Excel Table | The file **must** be formatted as an Excel Table (not just a range) for the Power Automate connector to list rows. |

---

## 📞 Support

For questions about this POC package, contact the project team or refer to the original design conversation.

**Related files**:
- Rule reference: `rules/rule_catalog.md`
- Flow instructions: `power-automate/flow-steps.md`
- Prompt template: `copilot/prompt.md`
- Sample output: `sample-output/poc_report_example.md`
