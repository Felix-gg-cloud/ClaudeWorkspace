## 校验规则 Validation Rules

每一行都必须执行下列所有规则（规则编号，便于扩展）：  
Every row MUST be validated by **all of the following rules** (reference codes for extension).

---

### R-REQ-001（Error）

- **中文**：YearMonth 不得为空白 / null / 仅空格。
- **English**: YearMonth must not be blank, null, or consist of whitespace only.

---

### R-YM-001（Error）

- **中文**：YearMonth 必须为 6 位字符串，格式固定为 YYYYMM。
- **English**: YearMonth must be a 6-digit string in the fixed format YYYYMM.

---

### R-CCN-001（Error）

- **中文**：Cost Center Number 必须为 7 位纯数字。
- **English**: Cost Center Number must be exactly 7 digits, numbers only.

---

### R-TXT-001（Error）

- **中文**：以下字段必须为非数字文字列类型：Function, Team, Owner.
- **English**: The following fields must be non-numeric text strings: Function, Team, Owner.

---

### R-NUM-001（Error）

- **中文**：以下字段（如有值）必须为纯数字：  
  Actual_GBS_TeamMember, Actual_GBS_TeamLeaderAM, Actual_EA, Actual_HKT,  
  Planned_GBS_TeamMember, Planned_GBS_TeamLeaderAM, Planned_EA, Planned_HKT,  
  Target_YearEnd, Target_2030YearEnd.
- **English**: The following fields (if present) must be numeric only:  
  Actual_GBS_TeamMember, Actual_GBS_TeamLeaderAM, Actual_EA, Actual_HKT,  
  Planned_GBS_TeamMember, Planned_GBS_TeamLeaderAM, Planned_EA, Planned_HKT,  
  Target_YearEnd, Target_2030YearEnd.

---

### R-SHORE-001（Error）

- **中文**：ShoringRatio 必须为数值，且数值必须小于或等于 1。
- **English**: ShoringRatio must be numeric and less than or equal to 1.

---

### R-TOTAL-001（Error）

- **中文**：Function 为 "Total (All)" 或 "Total (Core Operations)" 的行，"Owner" 和 "YearMonth" 字段以外的列可以为空。
- **English**: For rows where Function is "Total (All)" or "Total (Core Operations)", columns other than "Owner" and "YearMonth" may be left empty.

---

### R-FUNC-001（Error）

- **中文**：Function 必须与已加载至 Knowledge 模块中的"月份数据模板（Monthly Reference Template）"保持一致。任何未在该模板中出现过的 Function，均视为非法。
- **English**: Function must exactly match those in Knowledge's Monthly Reference Template. Any Function not present in this template is considered invalid.

---

### R-TEAM-001（Error）

- **中文**：Team 必须与已加载至 Knowledge 模块中的"月份数据模板（Monthly Reference Template）"保持一致。任何未在该模板中出现过的 Team，均视为非法。
- **English**: Team must exactly match those in Knowledge's Monthly Reference Template. Any Team not present in this template is considered invalid.

---

### R-MAP-001（Error）

- **中文**：Function 与 Team 的组合必须严格匹配 Knowledge 模块中"月份数据模板（Monthly Reference Template）"里实际存在过的固定组合关系。任何模板中未出现过的 Function–Team 组合均视为错误。
- **English**: The Function and Team combination must strictly match one of the valid combinations present in Knowledge's Monthly Reference Template. Any Function–Team pairing not in the template is considered an error.
