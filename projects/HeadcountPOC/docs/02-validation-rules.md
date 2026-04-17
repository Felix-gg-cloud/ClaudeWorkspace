# 02 — 校验规则完整说明

## 概述

所有规则按以下优先级执行，同一列命中多条规则时，**每条独立输出一条 issue**（不去重），但建议按"必填失败则跳过格式校验"的顺序执行，以减少噪音。

### 严重性定义

| 严重性 | 含义 |
|--------|------|
| **Error** | 硬性错误，数据不合格，必须修正后才能入库 |
| **Warning** | 软性警告，数据可能有问题，建议核实，不阻断入库 |

---

## 行类型判定（所有规则的前提）

```
isTotal = trim(Function) = "Total (All)" OR trim(Function) = "Total (Core Operations)"
```

- **Total 行**：`isTotal = true`
- **Non-Total 行**：`isTotal = false`

> 判定前请对 `Function` 进行 `trim()`，避免前后空格导致误判。

---

## 规则清单

### 一、YearMonth 全局规则

#### R-REQ-YM-001 — YearMonth 必填（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-REQ-YM-001 |
| **Severity** | Error |
| **适用行** | 所有行（Total 行 + Non-Total 行） |
| **触发条件** | `trim(YearMonth) = ""` |
| **Message** | `YearMonth 不能为空，所有行均须填写。` |
| **FixSuggestion** | `请填写 6 位数字年月，例如 202501。` |

#### R-YM-001 — YearMonth 格式（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-YM-001 |
| **Severity** | Error |
| **适用行** | 所有行 |
| **触发条件** | `trim(YearMonth) != ""` 且（不匹配 `^\d{6}$` 或月份不在 01-12） |
| **执行顺序** | 仅当 R-REQ-YM-001 未触发（即 YearMonth 非空）时才执行 |
| **Message** | `YearMonth 格式错误，必须为 6 位数字且月份为 01-12，例如 202501。` |
| **FixSuggestion** | `将 YearMonth 改为 YYYYMM 格式（例如 202501，不要用 2025-01 或 Jan-25）。` |

---

### 二、Total 行必填规则

#### R-REQ-TOTAL-OWNER — Owner 必填（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-REQ-TOTAL-OWNER |
| **Severity** | Error |
| **适用行** | Total 行（`isTotal = true`） |
| **触发条件** | `isTotal = true` 且 `trim(Owner) = ""` |
| **Message** | `Total 行必须填写 Owner。` |
| **FixSuggestion** | `请在 Owner 列填写数据负责人姓名后重新提交。` |

---

### 三、Non-Total 行必填规则

#### R-REQ-NT-TGT1 — Target_YearEnd 必填（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-REQ-NT-TGT1 |
| **Severity** | Error |
| **适用行** | Non-Total 行（`isTotal = false`） |
| **触发条件** | `isTotal = false` 且 `trim(Target_YearEnd) = ""` |
| **Message** | `非 Total 行的 Target_YearEnd 不允许为空。` |
| **FixSuggestion** | `请填写年末目标人数（≥ 0 的数字）。` |

#### R-REQ-NT-TGT2 — Target_2030YearEnd 必填（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-REQ-NT-TGT2 |
| **Severity** | Error |
| **适用行** | Non-Total 行（`isTotal = false`） |
| **触发条件** | `isTotal = false` 且 `trim(Target_2030YearEnd) = ""` |
| **Message** | `非 Total 行的 Target_2030YearEnd 不允许为空。` |
| **FixSuggestion** | `请填写 2030 年末目标人数（≥ 0 的数字）。` |

#### R-REQ-NT-SR — ShoringRatio 必填（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-REQ-NT-SR |
| **Severity** | Error |
| **适用行** | Non-Total 行（`isTotal = false`） |
| **触发条件** | `isTotal = false` 且 `trim(ShoringRatio) = ""` |
| **Message** | `非 Total 行的 ShoringRatio 不允许为空。` |
| **FixSuggestion** | `请填写 Offshoring 比率，格式为"数字+%"，例如 23% 或 12.5%。` |

---

### 四、Cost Center Number 格式规则

#### R-CCN-001 — 7 位纯数字（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-CCN-001 |
| **Severity** | Error |
| **适用行** | 所有行 |
| **触发条件** | `trim(Cost Center Number) != ""` 且不匹配 `^\d{7}$` |
| **Message** | `Cost Center Number 格式错误，必须是 7 位纯数字。` |
| **FixSuggestion** | `请检查 Cost Center Number 是否恰好 7 位、仅含数字（例如 1234567）。` |

---

### 五、Function 枚举校验

#### R-FUNC-001 — Function 白名单（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-FUNC-001 |
| **Severity** | Error |
| **适用行** | 所有行 |
| **触发条件** | `trim(Function) != ""` 且不在 Function 白名单中 |
| **白名单** | 见 [01 Excel 表格搭建 - Function 白名单](./01-excel-table-setup.md#3-function-白名单) |
| **Message** | `Function 值"{{value}}"不在允许的白名单中。` |
| **FixSuggestion** | `请从允许的 Function 列表中选择，注意大小写和空格须完全一致。` |

---

### 六、Team 枚举校验

#### R-TEAM-001 — Team 白名单（Warning）

| 属性 | 值 |
|------|----|
| **RuleId** | R-TEAM-001 |
| **Severity** | Warning |
| **适用行** | 所有行 |
| **触发条件** | `trim(Team) != ""` 且不在 Team 白名单中 |
| **白名单** | 见 [01 Excel 表格搭建 - Team 白名单](./01-excel-table-setup.md#4-team-白名单) |
| **Message** | `Team 值"{{value}}"不在常见白名单中，请确认是否正确。` |
| **FixSuggestion** | `请核实 Team 名称拼写，确认是否为新增 Team（如是则需更新白名单）。` |

---

### 七、Function-Team 映射规则

#### R-MAP-001 — Function-Team 组合校验（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-MAP-001 |
| **Severity** | Error |
| **适用行** | 所有行（包括 Total 行，Total+空 Team 是合法组合） |
| **触发条件** | `(trim(Function), trim(Team))` 组合不在允许映射表中 |
| **映射表** | 见 [01 Excel 表格搭建 - Function-Team 映射表](./01-excel-table-setup.md#5-function-team-映射表允许组合) |
| **Message** | `Function"{{function}}"与 Team"{{team}}"的组合不在允许的映射关系中。` |
| **FixSuggestion** | `请检查 Function 和 Team 的对应关系，或联系管理员更新映射配置。` |

> **重要**：`Total (All)` 和 `Total (Core Operations)` 与空 Team 的组合是**合法组合**，不应报错。实现时请将空 `Team` 统一标准化为 `""`（trim 后为空字符串）。

---

### 八、数值列校验（可空，非空必须 ≥ 0）

适用列（共 10 列）：

| 列名 | 说明 |
|------|------|
| `Actual_GBS_TeamMember` | GBS 团队成员实际 |
| `Actual_GBS_TeamLeaderAM` | GBS 团队长/AM 实际 |
| `Actual_EA` | EA 实际 |
| `Actual_HKT` | HKT 实际 |
| `Planned_GBS_TeamMember` | GBS 团队成员计划 |
| `Planned_GBS_TeamLeaderAM` | GBS 团队长/AM 计划 |
| `Planned_EA` | EA 计划 |
| `Planned_HKT` | HKT 计划 |
| `Target_YearEnd` | 年末目标（Non-Total 必填） |
| `Target_2030YearEnd` | 2030 年末目标（Non-Total 必填） |

#### R-NUM-001 — 非空时必须是数字（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-NUM-001 |
| **Severity** | Error |
| **适用行** | 所有行 |
| **触发条件** | 列值 `!= ""` 且无法解析为数字 |
| **Message** | `{{column}} 的值"{{value}}"不是合法数字。` |
| **FixSuggestion** | `请填写数字，不要含有文字、逗号分隔符或其他符号（例如填 10 而非 "十"）。` |

#### R-NUM-002 — 非空时必须 ≥ 0（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-NUM-002 |
| **Severity** | Error |
| **适用行** | 所有行 |
| **触发条件** | 列值可解析为数字 且数值 `< 0` |
| **执行顺序** | 仅当 R-NUM-001 未触发（即能解析为数字）时才执行 |
| **Message** | `{{column}} 的值{{value}}不能为负数。` |
| **FixSuggestion** | `Headcount 数据不应为负数，请核实并修正。` |

---

### 九、ShoringRatio 格式规则

#### R-SR-001 — 严格百分比格式（Error）

| 属性 | 值 |
|------|----|
| **RuleId** | R-SR-001 |
| **Severity** | Error |
| **适用行** | 所有行；Non-Total 行总是校验（因为必填）；Total 行仅在非空时校验 |
| **触发条件** | `trim(ShoringRatio) != ""` 且不满足以下条件之一 |
| **校验逻辑** | 1. `v = trim(ShoringRatio)` <br> 2. 正则匹配：`^\d+(\.\d+)?%$` <br> 3. 数值范围：`0 ≤ float(replace(v, "%", "")) ≤ 100` |
| **允许示例** | `0%`、`100%`、`23%`、`12.5%`、` 23% `（trim 后合法） |
| **不允许示例** | `0.23`（无 %）、`23`（无 %）、`101%`（超范围）、`-1%`（负数） |
| **Message** | `ShoringRatio 格式错误，必须是 0%–100% 的百分比，允许小数，例如 23% 或 12.5%。` |
| **FixSuggestion** | `请改为"数字+%"格式，不要填写纯小数（如 0.23）或省略百分号。` |

---

## 校验执行顺序建议

为减少同一字段的重复报错（例如 ShoringRatio 既触发必填又触发格式），建议在 Power Automate 中按以下顺序执行：

```
1. 计算 isTotal
2. 执行 YearMonth 必填规则（R-REQ-YM-001）
   └── 若 YearMonth 非空 → 执行格式规则（R-YM-001）
3. 若 isTotal：
   └── 执行 Owner 必填规则（R-REQ-TOTAL-OWNER）
4. 若非 Total：
   └── 执行 Target_YearEnd 必填（R-REQ-NT-TGT1）
   └── 执行 Target_2030YearEnd 必填（R-REQ-NT-TGT2）
   └── 执行 ShoringRatio 必填（R-REQ-NT-SR）
5. Cost Center Number 非空时执行格式规则（R-CCN-001）
6. Function 非空时执行白名单规则（R-FUNC-001）
7. Team 非空时执行白名单规则（R-TEAM-001）
8. Function-Team 组合规则（R-MAP-001）
9. 数值列（逐列）：非空时执行 R-NUM-001，通过后执行 R-NUM-002
10. ShoringRatio 非空时执行格式规则（R-SR-001）
    └── 若 Non-Total 行 ShoringRatio 已必填失败（为空），跳过 R-SR-001
```

---

## issue 记录结构

每条规则触发后，向 issues 数组追加一条记录，字段如下：

```json
{
  "Severity": "Error",
  "RuleId": "R-SR-001",
  "YearMonth": "202501",
  "CostCenterNumber": "1234567",
  "Function": "Finance",
  "Team": "GBS",
  "Column": "ShoringRatio",
  "Value": "0.23",
  "Message": "ShoringRatio 格式错误，必须是 0%–100% 的百分比，允许小数，例如 23% 或 12.5%。",
  "FixSuggestion": "请改为"数字+%"格式，不要填写纯小数（如 0.23）或省略百分号。"
}
```
