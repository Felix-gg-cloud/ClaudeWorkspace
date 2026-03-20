# POC Headcount Excel Validator — Sample Output

This document shows example Copilot Studio report output for a mixed dataset containing both Total rows and Non-Total rows with various validation errors.

---

## Sample Input Scenario

The table `tblOffshoring` has 6 rows for `YearMonth = 202503`:

| Row | Function | Team | Owner | YearMonth | Target_YearEnd | Target_2030YearEnd | ShoringRatio | Notes |
|---|---|---|---|---|---|---|---|---|
| 1 | Life Claims | Life Claims (High End Medical) | Alice Wang | 202503 | *(empty)* | *(empty)* | *(empty)* | Non-Total: all 3 targets missing |
| 2 | Operations | Customer Service | Bob Li | 202503 | 150 | 200 | `abc%` | Non-Total: ShoringRatio bad format |
| 3 | Finance | AP Processing | *(empty)* | 202503 | 120 | 160 | `45%` | Non-Total: Owner missing |
| 4 | Total (All) | *(empty)* | Carol Chen | 202503 | *(empty)* | *(empty)* | *(empty)* | Total row: empty targets OK |
| 5 | Total (Core Operations) | *(empty)* | *(empty)* | *(empty)* | *(empty)* | *(empty)* | `110%` | Total row: Owner + YearMonth missing; ShoringRatio out of range |
| 6 | IT | Infrastructure | David Kim | 202503 | 80 | 100 | `30%` | Non-Total: all valid |

---

## Sample Report Output (Copilot Studio)

---

### 一、摘要

| 项目 | 结果 |
|---|---|
| 文件 | `headcount_analysis_poc.xlsx` |
| 表格 | `tblOffshoring` |
| 总行数 | 6 |
| ❌ Error | 7 条 |
| ⚠️ Warning | 0 条 |
| 是否需要立即处理 | **是**——发现 7 条 Error，请数据负责人在下次月度汇总前完成修正。 |

---

### 二、问题明细

| # | 严重级别 | 规则 | YearMonth | Function | Team | 列名 | 问题值 | 问题描述 | 修复建议 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | ❌ Error | R-REQ-002 | 202503 | Life Claims | Life Claims (High End Medical) | Target_YearEnd | *(空)* | Target_YearEnd 不能为空（非Total行必须填写此列）。 | 请在 Target_YearEnd 列填写有效值。非 Total (All) / Total (Core Operations) 行不允许此列为空。 |
| 2 | ❌ Error | R-REQ-002 | 202503 | Life Claims | Life Claims (High End Medical) | Target_2030YearEnd | *(空)* | Target_2030YearEnd 不能为空（非Total行必须填写此列）。 | 请在 Target_2030YearEnd 列填写有效值。非 Total (All) / Total (Core Operations) 行不允许此列为空。 |
| 3 | ❌ Error | R-REQ-002 | 202503 | Life Claims | Life Claims (High End Medical) | ShoringRatio | *(空)* | ShoringRatio 不能为空（非Total行必须填写此列）。 | 请在 ShoringRatio 列填写有效值。非 Total (All) / Total (Core Operations) 行不允许此列为空。 |
| 4 | ❌ Error | R-SR-001 | 202503 | Operations | Customer Service | ShoringRatio | `abc%` | ShoringRatio 必须使用百分比格式（0%~100%），例如 23% 或 12.5%。 | 请将值改为「数字+%」形式，例如 25% 或 12.5%。不要填写 0.23 或 23。 |
| 5 | ❌ Error | R-REQ-003 | 202503 | Finance | AP Processing | Owner | *(空)* | Owner 不能为空（非Total行必须填写此列）。 | 请在 Owner 列填写有效值。 |
| 6 | ❌ Error | R-REQ-001 | *(空)* | Total (Core Operations) | *(空)* | Owner | *(空)* | Owner 不能为空（Total行仍需填写 Owner 和 YearMonth）。 | 请在 Owner 列填写有效的负责人姓名。 |
| 7 | ❌ Error | R-REQ-001 | *(空)* | Total (Core Operations) | *(空)* | YearMonth | *(空)* | YearMonth 不能为空（Total行仍需填写 Owner 和 YearMonth）。 | 请在 YearMonth 列填写 YYYYMM 格式的年月，例如 202501。 |
| 8 | ❌ Error | R-SR-001 | *(空)* | Total (Core Operations) | *(空)* | ShoringRatio | `110%` | ShoringRatio 必须使用百分比格式（0%~100%），例如 23% 或 12.5%。 | 请将值改为「数字+%」形式，范围必须在 0%~100%。当前值 110% 超出允许范围。 |

> 行 4（Total (All)）和行 6（IT/Infrastructure）无问题，未列入明细。

---

### 三、例外逻辑说明

本次校验对"Total 行"和"非 Total 行"采用了不同的必填规则：

- **Total 行**（`Function` = `Total (All)` 或 `Total (Core Operations)`）属于汇总性行，通常不填写明细数据。因此，校验对 Total 行仅强制要求 `Owner` 和 `YearMonth` 非空，其余所有列（包括 `Target_YearEnd`、`Target_2030YearEnd`、`ShoringRatio`）均**允许为空**。
- **非 Total 行**代表实际业务单元的人力数据，`Target_YearEnd`、`Target_2030YearEnd`、`ShoringRatio` 三列是**严格必填项**——这三列的缺失意味着没有目标规划数据，会直接影响 Shoring 策略分析。
- `ShoringRatio` 的格式要求：必须是"数字+%"格式（如 `25%` 或 `12.5%`），数值范围 0~100。不接受小数比例（`0.25`）或不带百分号的纯数字（`25`）。若 Total 行填写了 ShoringRatio，同样需要满足此格式要求。

---

### 四、关键缺失字段高亮

以下为涉及核心必填字段的 Error 汇总：

**Target_YearEnd / Target_2030YearEnd / ShoringRatio（非 Total 行严格必填）：**

| 行类型 | YearMonth | Function | Team | 缺失列 | 规则 |
|---|---|---|---|---|---|
| 非 Total 行 | 202503 | Life Claims | Life Claims (High End Medical) | Target_YearEnd | R-REQ-002 |
| 非 Total 行 | 202503 | Life Claims | Life Claims (High End Medical) | Target_2030YearEnd | R-REQ-002 |
| 非 Total 行 | 202503 | Life Claims | Life Claims (High End Medical) | ShoringRatio | R-REQ-002 |
| 非 Total 行 | 202503 | Operations | Customer Service | ShoringRatio（格式错误） | R-SR-001 |

**Owner / YearMonth（所有行必填；Total 行规则 R-REQ-001）：**

| 行类型 | YearMonth | Function | 缺失列 | 规则 |
|---|---|---|---|---|
| Total 行 | *(空)* | Total (Core Operations) | Owner | R-REQ-001 |
| Total 行 | *(空)* | Total (Core Operations) | YearMonth | R-REQ-001 |
| 非 Total 行 | 202503 | Finance | Owner | R-REQ-003 |

---

### 五、待业务确认的问题

1. **ShoringRatio 填写规范：** 当前校验要求格式为"数字+%"（如 `25%` 或 `12.5%`）。如业务方有其他填写习惯（如 `0.25`），请告知以便调整规则。
2. **数值列是否允许小数：** 当前允许小数（如 `0.5`）。如 Headcount 应为整数，请告知以便收紧校验。
3. **Team 白名单：** 当前 POC 未启用 Team 白名单校验。如需启用，请提供允许的 Team 列表。
4. **新增 Function-Team 组合：** 如业务扩张导致出现新的 Function/Team 组合，需提前告知以避免误报。

---

## Notes on This Sample

- Row 4 (`Total (All)`, Owner=Carol Chen, YearMonth=202503): **passes all checks** — empty Target_*, Target_2030*, ShoringRatio are all allowed on Total rows.
- Row 5 (`Total (Core Operations)`): **fails R-REQ-001** (missing Owner and YearMonth), and **fails R-SR-001** (ShoringRatio=`110%` is out of 0–100 range even though the format string is parseable).
- Row 6 (IT/Infrastructure): **passes all checks** — a clean Non-Total row with all required fields filled correctly.
