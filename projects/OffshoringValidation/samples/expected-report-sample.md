# Validation Report

**Batch**: 1 / Source: Offshoring_Data.xlsx  
**Rows Checked**: 5  
**Issues Found**: 7 Error(s), 1 Warning(s)

### Summary by Rule

| RuleId | Severity | Count | Affected Column(s) |
|--------|----------|-------|---------------------|
| R-SR-001 | Error | 2 | ShoringRatio |
| R-REQ-NT-TGT1 | Error | 1 | Target_YearEnd |
| R-NUM-002 | Error | 1 | Actual_GBS_TeamMember |
| R-REQ-YM-001 | Error | 1 | YearMonth |
| R-CCN-001 | Error | 1 | Cost Center Number |
| R-NUM-001 | Error | 1 | Actual_GBS_TeamMember |
| R-TEAM-001 | Warning | 1 | Team |

### Issue Details

| # | Row | Column | Value | Message |
|---|-----|--------|-------|---------|
| 1 | YearMonth=202501\|Function=HR\|Team=Talent Acquisition | ShoringRatio | 125% | ShoringRatio must be a percentage with value 0-100. |
| 2 | YearMonth=202501\|Function=IT\|Team=Infrastructure | Target_YearEnd | (empty) | Target_YearEnd is required for Non-Total rows. |
| 3 | YearMonth=202501\|Function=IT\|Team=Infrastructure | Actual_GBS_TeamMember | -3 | Must be >= 0. |
| 4 | YearMonth=(empty)\|Function=Marketing\|Team=UnknownTeam | YearMonth | (empty) | YearMonth is required for all rows. |
| 5 | YearMonth=(empty)\|Function=Marketing\|Team=UnknownTeam | Cost Center Number | 12345 | Must be exactly 7 digits. |
| 6 | YearMonth=(empty)\|Function=Marketing\|Team=UnknownTeam | Team | UnknownTeam | Not in allowed team list. |
| 7 | YearMonth=(empty)\|Function=Marketing\|Team=UnknownTeam | Actual_GBS_TeamMember | abc | Must be a valid number. |
| 8 | YearMonth=(empty)\|Function=Marketing\|Team=UnknownTeam | ShoringRatio | 30 % | Invalid format (space before %). |

### Fix Recommendations

1. **ShoringRatio (R-SR-001, 2 rows)**: Ensure all ShoringRatio values are in format `\d+(\.\d+)?%` with value 0-100, e.g. `25%`, `12.5%`.
2. **Target_YearEnd (R-REQ-NT-TGT1, 1 row)**: Fill in Target_YearEnd for all Non-Total rows.
3. **Actual_GBS_TeamMember (R-NUM-001 / R-NUM-002, 2 rows)**: Correct invalid or negative numeric values.
4. **YearMonth (R-REQ-YM-001, 1 row)**: Fill in YearMonth (YYYYMM format) — required for all rows.
5. **Cost Center Number (R-CCN-001, 1 row)**: Correct to exactly 7 digits.
6. **Team (R-TEAM-001, 1 row, Warning)**: Verify team name spelling against the approved list.

### Rows Passed

- `YearMonth=202501|Function=Finance|Team=Finance Operations` — ✅ All checks passed.
- `YearMonth=202501|Function=Total (All)|Team=` — ✅ All checks passed (Total row, optional fields skipped).
