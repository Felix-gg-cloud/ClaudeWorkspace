# 02 — 规则清单（Rule Catalog）

> 所有规则均基于"范围读取"方式（命名范围 `rngOffshoring` 或固定范围 `A4:P2000`）。  
> 规则中引用的列名以第 4 行标准列名为准（见 `docs/01-excel-range-setup.md`）。

---

## 列级规则

### 维度列（Dimension Columns）

#### R-CCN-001 — Cost Center Number 格式
- **列**：`Cost Center Number`
- **规则**：必须为纯数字，且长度为 7 位
- **判断逻辑**：`/^\d{7}$/.test(value)`
- **严重级别**：🔴 Error
- **示例违规**：`123456`（6 位）、`1234567A`（含字母）、空值

---

#### R-FUNC-001 — Function 枚举白名单
- **列**：`Function`
- **规则**：值必须属于 `configs/function-whitelist.json` 中的 16 个允许值之一
- **判断逻辑**：`functionWhitelist.includes(value.trim())`
- **严重级别**：🔴 Error
- **允许值**（完整列表见 `configs/function-whitelist.json`）：
  - Chief Medical Office
  - Chief Operating Office
  - Claims Technical Excellence
  - Customer Contact Centre
  - Customer Relations and Operations Compliance
  - EB & Individual Health Claims
  - HNW Operations
  - Life & Individual Health Operations
  - Life Claims
  - Life Underwriting
  - Operations Excellence & Innovation
  - P&C Claims
  - Service Centre, Distributor Services & CPM
  - Service Excellence
  - Total (All)
  - Total (Core Operations)

---

#### R-TEAM-001 — Team 枚举白名单
- **列**：`Team`
- **规则**：值必须属于 `configs/team-whitelist.json` 中的允许值之一
- **判断逻辑**：`teamWhitelist.includes(value.trim())`
- **严重级别**：🟡 Warning（Team 变动概率高于 Function，POC 阶段设为 Warning）
- **完整列表**：见 `configs/team-whitelist.json`（43 个值）

---

#### R-OWNER-001 — Owner 非空
- **列**：`Owner`
- **规则**：不能为空（不能是空字符串、null、undefined）
- **判断逻辑**：`value !== null && value.toString().trim() !== ""`
- **严重级别**：🔴 Error

---

#### R-YM-001 — YearMonth 格式
- **列**：`YearMonth`
- **规则**：
  1. 必须匹配 `YYYYMM` 格式（6 位数字）
  2. MM 部分必须在 01–12 之间
  3. 不能为未来月份（可选，POC 阶段可关闭）
- **判断逻辑**：
  ```javascript
  const str = value.toString();
  if (!/^\d{6}$/.test(str)) return false;
  const month = parseInt(str.slice(4, 6), 10);
  return month >= 1 && month <= 12;
  ```
- **严重级别**：🔴 Error
- **示例违规**：`2024-01`（含连字符）、`202413`（月份超范围）、`24011`（位数不足）

---

### 数值列（Numeric Columns）

适用列：`Actual_GBS_TeamMember`、`Actual_GBS_TeamLeaderAM`、`Actual_EA`、`Actual_HKT`、`Planned_GBS_TeamMember`、`Planned_GBS_TeamLeaderAM`、`Planned_EA`、`Planned_HKT`、`Target_YearEnd`、`Target_2030YearEnd`

#### R-NUM-001 — 数值列必须为数字
- **规则**：必须可解析为有效数字（`!isNaN(Number(value))`）
- **严重级别**：🔴 Error
- **备注**：空值（blank）处理方式——POC 阶段设为 🟡 Warning，后续根据业务确认是否必填

---

#### R-NUM-002 — 数值列必须为非负整数
- **规则**：值必须 >= 0 且为整数（人数不能是负数或小数）
- **判断逻辑**：`Number(value) >= 0 && Number.isInteger(Number(value))`
- **严重级别**：🔴 Error

---

### 百分比列

#### R-SR-001 — Shoring Ratio 范围与格式
- **列**：`Shoring Ratio`
- **规则**：
  1. 如果是字符串格式（如 `23%`），去掉 `%` 后解析为数字
  2. 如果是小数格式（如 `0.23`），乘以 100
  3. 最终数值必须在 0~100 之间（含边界）
- **判断逻辑**：
  ```javascript
  let num = value;
  if (typeof value === "string" && value.endsWith("%")) {
    num = parseFloat(value.replace("%", ""));
  } else if (typeof value === "number" && value <= 1) {
    num = value * 100; // 兼容 0-1 小数格式
  } else {
    num = Number(value);
  }
  return !isNaN(num) && num >= 0 && num <= 100;
  ```
- **严重级别**：🔴 Error（范围违规）；🟡 Warning（格式不一致，如同一文件混用 `%` 和小数）

---

## 跨字段规则（Cross-field Rules）

#### R-MAP-001 — Function → Team 允许组合
- **列**：`Function` + `Team`
- **规则**：每一行的 `(Function, Team)` 组合必须出现在 `configs/function-team-mapping.json` 中
- **判断逻辑**：
  ```javascript
  const allowedTeams = mapping[row.Function] || [];
  // Total (All) 和 Total (Core Operations) 的 Team 可以为空
  if (row.Function === "Total (All)" || row.Function === "Total (Core Operations)") {
    return true; // 不做 Team 校验
  }
  return allowedTeams.includes(row.Team.trim());
  ```
- **严重级别**：🔴 Error
- **示例违规**：`Function=Life Claims, Team=Underwriting`（Underwriting 属于 Life Underwriting，不属于 Life Claims）

---

#### R-COV-001 — 每月组合完整性（月度覆盖检查）
- **适用范围**：所有 `(YearMonth, Function, Team)` 组合
- **规则**：对每个 `YearMonth`，检查 `(Function, Team)` 组合集合与期望的全量组合集之间的差集
  - **缺失组合**：某月应有但没有的 Function/Team 行（Warning）
  - **新增组合**：某月出现了不在历史中存在的 Function/Team 行（Warning）
- **严重级别**：🟡 Warning
- **备注**：POC 阶段可选做；"期望全量组合"来自 `configs/function-team-mapping.json` 展开

---

#### R-NUM-003 — Actual 合理性（可选）
- **规则**：`Actual_GBS_TeamMember + Actual_GBS_TeamLeaderAM + Actual_EA + Actual_HKT` 的总和，不应远大于同行 `Planned` 的总和（例如不超过 Planned 总和的 200%）
- **严重级别**：🟡 Warning
- **备注**：POC 可暂不实现，待业务确认合理阈值后加入

---

## 规则汇总表

| Rule ID | 列/范围 | 规则描述 | 严重级别 | POC 实现 |
|---------|---------|---------|---------|---------|
| R-CCN-001 | Cost Center Number | 7 位纯数字 | 🔴 Error | ✅ 必做 |
| R-FUNC-001 | Function | 枚举白名单（16 个值） | 🔴 Error | ✅ 必做 |
| R-TEAM-001 | Team | 枚举白名单（43 个值） | 🟡 Warning | ✅ 必做 |
| R-OWNER-001 | Owner | 非空 | 🔴 Error | ✅ 必做 |
| R-YM-001 | YearMonth | YYYYMM 格式 + 月份合法 | 🔴 Error | ✅ 必做 |
| R-NUM-001 | 所有数值列 | 必须为数字 | 🔴 Error | ✅ 必做 |
| R-NUM-002 | 所有数值列 | 非负整数 | 🔴 Error | ✅ 必做 |
| R-SR-001 | Shoring Ratio | 0~100 范围 + 格式兼容 | 🔴 Error | ✅ 必做 |
| R-MAP-001 | Function + Team | 允许组合映射 | 🔴 Error | ✅ 必做 |
| R-COV-001 | YearMonth + Function + Team | 每月组合完整性 | 🟡 Warning | 🔲 可选 |
| R-NUM-003 | Actual vs Planned | 合理性对比 | 🟡 Warning | 🔲 可选 |

---

## 输出格式（issues 数组对象结构）

每条发现的问题，生成一个 issue 对象：

```json
{
  "severity": "Error",
  "ruleId": "R-FUNC-001",
  "yearMonth": "202401",
  "costCenterNumber": "1234567",
  "function": "Unknown Function",
  "team": "Some Team",
  "column": "Function",
  "value": "Unknown Function",
  "message": "Function 值不在白名单中：Unknown Function",
  "suggestion": "请从允许的 Function 列表中选择正确的值"
}
```

字段说明：

| 字段 | 说明 |
|-----|------|
| `severity` | `"Error"` 或 `"Warning"` |
| `ruleId` | 规则 ID，如 `R-FUNC-001` |
| `yearMonth` | 该行的 YearMonth 值（用于定位） |
| `costCenterNumber` | 该行的 Cost Center Number（用于定位） |
| `function` | 该行的 Function 值（用于定位） |
| `team` | 该行的 Team 值（用于定位） |
| `column` | 违规的列名 |
| `value` | 违规的原始值 |
| `message` | 违规说明（人类可读） |
| `suggestion` | 修复建议（可选） |
