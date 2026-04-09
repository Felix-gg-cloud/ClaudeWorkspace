# Offshoring Data Validation Rules

> **Mode**: AI-only validation. Power Automate performs mechanical row-reading only.  
> The AI model receives the raw data and this rule catalogue, then produces a structured JSON payload.

---

## 0. Execution Order

Rules are applied in this order to avoid cascading noise:

1. **R-WS-ALL-001** — leading/trailing whitespace (global, per cell)
2. **R-REQ-*** — mandatory field checks
3. **R-FMT-*** — format / pattern checks
4. **R-VAL-*** — value / range checks
5. **R-REF-*** — whitelist / mapping lookup checks

If a cell already has an R-WS-ALL-001 Error, skip all subsequent rules for that same cell (do not generate duplicate errors for the same cell).

---

## 1. Global Whitespace Rule

### R-WS-ALL-001 — Leading/Trailing Whitespace Forbidden (Error)

- **Applies to**: every column of every row without exception  
- **Trigger**: `RawValue` is not null/empty AND `RawValue ≠ trim(RawValue)`  
- **Severity**: `Error`  
- **Message**: `"Column '{Column}' contains leading or trailing whitespace. Raw value: '{RawValue}'"`  
- **FixSuggestion**: `"Remove all leading and trailing spaces from the cell value."`

> Because Power Automate passes raw cell values without trimming, any accidental space (e.g., `"GBS "`, `" 202501"`, `" 12.5%"`) is caught here before any other rule runs.

---

## 2. Mandatory Field Rules

Row classification:

| Condition | Row Type |
|-----------|----------|
| `Function == "Total"` OR `Team == "Total"` | **Total row** |
| Otherwise | **Non-Total row** |

### R-REQ-001 — YearMonth Required (All Rows, Error)

- **Applies to**: all rows  
- **Trigger**: `trim(YearMonth) == ""`  
- **Severity**: `Error`  
- **Message**: `"YearMonth is mandatory for all rows."`  
- **FixSuggestion**: `"Enter the period in YYYYMM format (e.g., 202501)."`

### R-REQ-002 — Target_YearEnd Required (Non-Total Rows, Error)

- **Applies to**: Non-Total rows  
- **Trigger**: `trim(Target_YearEnd) == ""`  
- **Severity**: `Error`  
- **Message**: `"Target_YearEnd is mandatory for Non-Total rows."`  
- **FixSuggestion**: `"Enter the target year-end value."`

### R-REQ-003 — Target_2030YearEnd Required (Non-Total Rows, Error)

- **Applies to**: Non-Total rows  
- **Trigger**: `trim(Target_2030YearEnd) == ""`  
- **Severity**: `Error`  
- **Message**: `"Target_2030YearEnd is mandatory for Non-Total rows."`  
- **FixSuggestion**: `"Enter the 2030 target year-end value."`

### R-REQ-004 — ShoringRatio Required (Non-Total Rows, Error)

- **Applies to**: Non-Total rows  
- **Trigger**: `trim(ShoringRatio) == ""`  
- **Severity**: `Error`  
- **Message**: `"ShoringRatio is mandatory for Non-Total rows."`  
- **FixSuggestion**: `"Enter the shoring ratio in the format '12.5%'."`

### R-REQ-005 — Owner Required (Total Rows, Error)

- **Applies to**: Total rows  
- **Trigger**: `trim(Owner) == ""`  
- **Severity**: `Error`  
- **Message**: `"Owner is mandatory for Total rows."`  
- **FixSuggestion**: `"Enter the responsible owner name."`

---

## 3. Format Rules

### R-FMT-001 — YearMonth Format (Error)

- **Trigger**: `trim(YearMonth)` is non-empty AND does not match `^\d{4}(0[1-9]|1[0-2])$`  
- **Severity**: `Error`  
- **Message**: `"YearMonth '{RawValue}' does not match YYYYMM format with valid month (01–12)."`  
- **FixSuggestion**: `"Correct the format to YYYYMM, e.g., 202501 for January 2025."`

### R-FMT-002 — ShoringRatio Format (Error)

- **Trigger**: `trim(ShoringRatio)` is non-empty AND does not match `^\d+(\.\d+)?%$`  
- **Severity**: `Error`  
- **Message**: `"ShoringRatio '{RawValue}' must be in the format '12.5%' (digits, optional decimal, then '%')."`  
- **FixSuggestion**: `"Re-enter the ratio with a percent sign, e.g., '25%' or '12.5%'."`

---

## 4. Value / Range Rules

### R-VAL-001 — Numeric Columns: Non-Negative (Error)

- **Applies to**: all `Actual_*`, `Planned_*`, `Target_YearEnd`, `Target_2030YearEnd` columns  
- **Trigger**: `trim(value)` is non-empty AND (value cannot be parsed as a number OR parsed number < 0)  
- **Severity**: `Error`  
- **Message**: `"Column '{Column}' value '{RawValue}' is not a valid non-negative number."`  
- **FixSuggestion**: `"Enter a non-negative numeric value, or leave blank if not applicable."`

> If `trim(value) == ""`, skip silently (no error).

### R-VAL-002 — ShoringRatio Range 0–100 (Error)

- **Trigger**: `trim(ShoringRatio)` is non-empty AND matches format AND numeric part is < 0 or > 100  
- **Severity**: `Error`  
- **Message**: `"ShoringRatio '{RawValue}' is out of valid range (0%–100%)."`  
- **FixSuggestion**: `"Enter a percentage between 0% and 100%."`

### R-VAL-003 — ShoringRatio: Total Row May Be Blank; If Non-Blank Must Pass Format + Range (Warning)

- **Applies to**: Total rows where `trim(ShoringRatio)` is non-empty  
- **Trigger**: value does not pass R-FMT-002 or R-VAL-002  
- **Severity**: `Warning`  
- **Message**: `"ShoringRatio on a Total row is optional, but if provided it must be a valid percentage."`  
- **FixSuggestion**: `"Either clear the cell or enter a valid percentage (e.g., '50%')."`

---

## 5. Whitelist / Mapping Rules

### R-REF-001 — Function Whitelist (Error)

- **Trigger**: `trim(Function)` is non-empty AND value is not in the approved Function list  
- **Severity**: `Error`  
- **Message**: `"Function '{RawValue}' is not in the approved Function list."`  
- **FixSuggestion**: `"Use one of the approved Function values. Check for typos or extra spaces."`

> Note: because R-WS-ALL-001 runs first, a value like `"GBS "` will already be an Error before this rule runs and this rule will be skipped for that cell.

### R-REF-002 — Function-Team Mapping (Error)

- **Trigger**: the `(Function, Team)` pair is not in the approved mapping table  
- **Severity**: `Error`  
- **Message**: `"The combination Function='{Function}' + Team='{Team}' is not a valid mapping."`  
- **FixSuggestion**: `"Verify the Function-Team pairing against the approved mapping table."`

> Special case: `(Total, "")` and `(*, Total)` are explicitly allowed in the mapping table.

---

## 6. Rule Summary Table

| RuleId | Severity | Category | Short Description |
|--------|----------|----------|-------------------|
| R-WS-ALL-001 | Error | Whitespace | Leading/trailing whitespace in any cell |
| R-REQ-001 | Error | Mandatory | YearMonth required for all rows |
| R-REQ-002 | Error | Mandatory | Target_YearEnd required for Non-Total rows |
| R-REQ-003 | Error | Mandatory | Target_2030YearEnd required for Non-Total rows |
| R-REQ-004 | Error | Mandatory | ShoringRatio required for Non-Total rows |
| R-REQ-005 | Error | Mandatory | Owner required for Total rows |
| R-FMT-001 | Error | Format | YearMonth must be YYYYMM with valid month |
| R-FMT-002 | Error | Format | ShoringRatio must match `^\d+(\.\d+)?%$` |
| R-VAL-001 | Error | Value | Numeric columns must be non-negative numbers |
| R-VAL-002 | Error | Value | ShoringRatio must be 0–100% |
| R-VAL-003 | Warning | Value | Total row ShoringRatio optional but must be valid if provided |
| R-REF-001 | Error | Reference | Function must be in approved whitelist |
| R-REF-002 | Error | Reference | Function-Team pair must be in approved mapping |
