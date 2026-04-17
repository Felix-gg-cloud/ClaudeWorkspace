# Rule Catalog — Headcount Offshoring Validator

> **Sheet**: `Offshoring` | **Header row**: Row 4 (after user normalization)  
> **Severity levels**: `Error` (blocks reporting) · `Warning` (flag for review)

---

## Column-Level Rules

| RuleId | Severity | AppliesTo | Logic | Message | FixSuggestion |
|--------|----------|-----------|-------|---------|---------------|
| COL-001 | Error | Cost Center Number | Must be exactly 7 digits (numeric string). Regex: `^\d{7}$` | Cost Center Number `{value}` is not a valid 7-digit number. | Pad with leading zeros or correct the value to 7 digits. |
| COL-002 | Error | Function | Value must exist in `function_whitelist.txt` (exact match, case-sensitive). | Function `{value}` is not in the allowed list. | Use one of the approved Function values; check for typos or extra spaces. |
| COL-003 | Error | Team | Value must exist in `team_whitelist.txt` (exact match, case-sensitive). | Team `{value}` is not in the allowed list. | Use one of the approved Team values. |
| COL-004 | Error | Owner | Must be non-empty (not null, not blank, not `N/A`). | Owner is missing or blank. | Enter the responsible person's name or employee ID. |
| COL-005 | Error | YearMonth | Must match format `YYYYMM` (6-digit numeric). Valid range: 200001–209912. Regex: `^(19|20)\d{4}$` with month 01–12. | YearMonth `{value}` is not in YYYYMM format. | Example: `202503` for March 2025. |
| COL-006 | Error | Headcount (all numeric columns) | Must be numeric and ≥ 0. Applies to: `Headcount`, `Offshore HC`, `Onshore HC`, `Cost (USD)`, `Budget (USD)`. | Column `{column}` value `{value}` is not a valid non-negative number. | Remove text, currency symbols, or commas; enter a plain number. |
| COL-007 | Error | Shoring Ratio | Must be numeric; accepted formats: integer `23`, percentage `23%`, decimal `0.23`. Normalize to 0–100 range: if value ≤ 1 treat as fraction (×100); strip `%`. Result must be in [0, 100]. | Shoring Ratio `{value}` cannot be normalized to a valid 0–100 percentage. | Enter as `23`, `23%`, or `0.23` (all equivalent to 23%). Value must be between 0 and 100 %. |

---

## Cross-Field Rules

| RuleId | Severity | AppliesTo | Logic | Message | FixSuggestion |
|--------|----------|-----------|-------|---------|---------------|
| XF-001 | Error | Function + Team | The (Function, Team) combination must exist in `function_team_mapping.csv`. | Function `{function}` with Team `{team}` is not an allowed combination. | Check `function_team_mapping.csv` for valid (Function, Team) pairs. |
| XF-002 | Warning | Offshore HC + Headcount | `Offshore HC` must be ≤ `Headcount` when both are present. | Offshore HC (`{offshore}`) exceeds total Headcount (`{total}`). | Verify headcount split; offshore portion cannot exceed total headcount. |
| XF-003 | Warning | Onshore HC + Offshore HC + Headcount | `Onshore HC + Offshore HC` should equal `Headcount` (tolerance ±1). | Onshore HC + Offshore HC (`{sum}`) does not match Headcount (`{total}`). | Reconcile the split figures so they add up to total headcount. |
| XF-004 | Warning | Shoring Ratio + Offshore HC + Headcount | If Shoring Ratio and HC are both present: `Shoring Ratio ≈ (Offshore HC / Headcount) × 100` (tolerance ±2 pp). | Shoring Ratio `{ratio}%` is inconsistent with HC split ({offshore}/{total} = {computed}%). | Recalculate Shoring Ratio from HC figures or correct the HC values. |

---

## Cross-Row / Overall Rules

| RuleId | Severity | AppliesTo | Logic | Message | FixSuggestion |
|--------|----------|-----------|-------|---------|---------------|
| OVR-001 | Error | Function (all rows) | All Function values must be from `function_whitelist.txt`. Reported as a summary count per invalid value. | `{count}` rows contain invalid Function value `{value}`. | Replace with an approved Function value. |
| OVR-002 | Error | Team (all rows) | All Team values must be from `team_whitelist.txt`. | `{count}` rows contain invalid Team value `{value}`. | Replace with an approved Team value. |
| OVR-003 | Error | Function + Team (all rows) | All (Function, Team) combinations must exist in mapping. | `{count}` rows have disallowed combination (Function=`{f}`, Team=`{t}`). | Correct either Function or Team so the pair matches the mapping table. |
| OVR-004 | Warning | YearMonth coverage | For each YearMonth present in the file, every expected (Function, Team) combination from `function_team_mapping.csv` should appear at least once. Missing combinations are flagged as Warning. | YearMonth `{ym}`: missing expected combination (Function=`{f}`, Team=`{t}`). | Add the missing row for this period, or confirm that the combination is intentionally absent this month. |
| OVR-005 | Warning | Cost Center Number (all rows) | Duplicate (YearMonth, Cost Center Number, Function, Team) composite key detected. | Duplicate composite key found: YearMonth=`{ym}`, Cost Center=`{cc}`, Function=`{f}`, Team=`{t}` (`{count}` occurrences). | Remove or reconcile duplicate rows before uploading. |

---

## Severity Summary

| Severity | Action Required | Blocks Reporting? |
|----------|----------------|-------------------|
| Error | Must be corrected before data is considered valid | Yes |
| Warning | Should be reviewed; data may still be used with caution | No |

---

## Rule ID Naming Convention

| Prefix | Scope |
|--------|-------|
| `COL-` | Single-column validation |
| `XF-` | Cross-field validation (same row) |
| `OVR-` | Overall / cross-row validation |
