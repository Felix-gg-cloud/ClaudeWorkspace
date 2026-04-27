# Sample Validation Report — POC Headcount Validator

**File:** headcount_analysis_poc.xlsx  
**Table:** tblOffshoring  
**Run date:** 2025-01-15 09:30 (UTC+8)

---

## Validation Summary

- **File:** headcount_analysis_poc.xlsx
- **Table:** tblOffshoring
- **Total rows processed:** 248
- **Errors:** 9
- **Warnings:** 3

### Top Rules Hit

| Rule ID | Description | Count | Severity |
|---|---|---|---|
| R-REQ-001 | Required fields non-empty (non-Total rows) | 4 | Error |
| R-SR-001 | ShoringRatio strict percent format | 3 | Error |
| R-REQ-002 | Total rows — YearMonth must be present | 2 | Error |
| R-TEAM-001 | Team not in whitelist | 3 | Warning |

---

## Top Issues

| Severity | Rule ID | YearMonth | Cost Center Number | Function | Team | Column | Value | Message | Fix Suggestion |
|---|---|---|---|---|---|---|---|---|---|
| Error | R-REQ-001 | 202501 | 1234567 | Life Claims | Life Claims (High End Medical) | ShoringRatio | *(blank)* | "ShoringRatio" must not be empty for non-Total rows. | Fill in a valid percent value, e.g., 25%. |
| Error | R-REQ-001 | 202501 | 1234567 | Life Claims | Life Claims (High End Medical) | Owner | *(blank)* | "Owner" must not be empty for non-Total rows. | Enter the responsible owner name. |
| Error | R-REQ-001 | 202502 | 7654321 | GBS Ops | Data Operations | Target_2030YearEnd | *(blank)* | "Target_2030YearEnd" must not be empty for non-Total rows. | Fill in the 2030 year-end target headcount. |
| Error | R-REQ-001 | 202502 | 7654321 | GBS Ops | Data Operations | Actual_HKT | *(blank)* | "Actual_HKT" must not be empty for non-Total rows. | Fill in the actual HKT headcount. |
| Error | R-SR-001 | 202501 | 2345678 | Finance Operations | AP Processing | ShoringRatio | 0.25 | "ShoringRatio" must use percent format (e.g., 25%). Got: "0.25". | Change "0.25" to "25%". |
| Error | R-SR-001 | 202501 | 3456789 | HR Services | Payroll | ShoringRatio | 30 | "ShoringRatio" must use percent format (e.g., 30%). Got: "30". | Change "30" to "30%". |
| Error | R-SR-001 | 202502 | 4567890 | Life Claims | Life Claims (Standard) | ShoringRatio | 105% | "ShoringRatio" must be between 0% and 100%. Got: "105%". | Correct to a value ≤ 100%, e.g., 100%. |
| Error | R-REQ-002 | *(blank)* | *(blank)* | Total (All) | *(blank)* | YearMonth | *(blank)* | "YearMonth" must not be empty even for Total rows. | Enter a valid YearMonth (YYYYMM), e.g., 202501. |
| Error | R-REQ-002 | *(blank)* | *(blank)* | Total (Core Operations) | *(blank)* | YearMonth | *(blank)* | "YearMonth" must not be empty even for Total rows. | Enter a valid YearMonth (YYYYMM), e.g., 202501. |
| Warning | R-TEAM-001 | 202501 | 5678901 | GBS Ops | AI Automation Team | Team | AI Automation Team | "Team" value "AI Automation Team" is not in the allowed list. | Confirm if this is a new team and request whitelist addition. |
| Warning | R-TEAM-001 | 202501 | 6789012 | Finance Operations | Invoice Scanning (New) | Team | Invoice Scanning (New) | "Team" value "Invoice Scanning (New)" is not in the allowed list. | Check team name spelling or request whitelist addition. |
| Warning | R-TEAM-001 | 202502 | 7890123 | HR Services | Benefits Admin v2 | Team | Benefits Admin v2 | "Team" value "Benefits Admin v2" is not in the allowed list. | Confirm if this team was renamed and update accordingly. |

---

## Notes on Total-Row Handling

The following rows have `Function = Total (All)` or `Function = Total (Core Operations)` and were treated as **summary rows** under rule **R-REQ-002**:

- These rows are **exempt** from the R-REQ-001 required-fields check.
- The following columns were found blank on Total rows and were **not flagged as errors** (blank is allowed):
  - `Cost Center Number`, `Team`, `Owner`
  - All 10 numeric columns (`Actual_*`, `Planned_*`, `Target_*`)
  - `ShoringRatio` *(allowed blank; validated only when a value is present)*
- The only column flagged as Error on Total rows: `YearMonth` was blank (2 rows → R-REQ-002).

---

## ShoringRatio Rule Summary (R-SR-001)

| Value found | Result | Reason |
|---|---|---|
| `25%` | ✅ Valid | Correct format, in range |
| `12.5%` | ✅ Valid | Decimal percent, correct format |
| `0%` | ✅ Valid | Lower bound |
| `100%` | ✅ Valid | Upper bound |
| `0.25` | ❌ Error | Missing `%` |
| `30` | ❌ Error | Missing `%` |
| `105%` | ❌ Error | Exceeds 100% |
| *(blank)* on non-Total row | ❌ Error | R-REQ-001: required field |
| *(blank)* on Total row | ✅ Allowed | R-REQ-002: blank permitted |

---

## Questions for Business Sign-off

1. **ShoringRatio format confirmed:** All non-blank values must be `number%` (e.g., `23%`, `12.5%`). Values like `0.25` or `30` (without `%`) will be flagged as Errors. Please confirm data owners are aligned.

2. **Total-row blank columns confirmed:** For `Total (All)` and `Total (Core Operations)` rows, the following are allowed blank: Cost Center Number, Team, Owner, all Actual/Planned/Target columns, and ShoringRatio. Only `YearMonth` is mandatory. Please confirm.

3. **New teams flagged (Warning):** The following Teams were not found in the whitelist: `AI Automation Team`, `Invoice Scanning (New)`, `Benefits Admin v2`. Should these be added to the approved list, or are they data entry errors?

4. **Blank YearMonth on Total rows (Error):** Two Total rows had no `YearMonth` value. These represent aggregate rows that cannot be associated with any reporting period. Please ensure all Total rows have a valid `YearMonth`.
