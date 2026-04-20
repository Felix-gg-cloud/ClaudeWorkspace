# POC Headcount Validator — 完整规则清单

**表名**：`tblOffshoring`  
**版本**：v1.3（最终确认版）  
**最后更新**：2026-03-20

---

## 一、字段定义（16 列）

| # | 列名 | 类型 | 说明 |
|---|------|------|------|
| 1 | `Cost Center Number` | 文本 | 成本中心编号，7 位纯数字 |
| 2 | `Function` | 文本 | 职能部门；固定白名单 |
| 3 | `Team` | 文本 | 团队；固定白名单 |
| 4 | `Owner` | 文本 | 负责人 |
| 5 | `YearMonth` | 文本 | 年月，格式 YYYYMM |
| 6 | `ShoringRatio` | 文本 | 离岸比例，格式 "数字%"，0%~100% |
| 7 | `Actual_HC` | 数值 | 实际总人力 |
| 8 | `Actual_Offshore_HC` | 数值 | 实际离岸人力 |
| 9 | `Actual_Cost` | 数值 | 实际总成本 |
| 10 | `Actual_Offshore_Cost` | 数值 | 实际离岸成本 |
| 11 | `Planned_HC` | 数值 | 计划总人力 |
| 12 | `Planned_Offshore_HC` | 数值 | 计划离岸人力 |
| 13 | `Planned_Cost` | 数值 | 计划总成本 |
| 14 | `Planned_Offshore_Cost` | 数值 | 计划离岸成本 |
| 15 | `Target_YearEnd` | 数值 | 年底目标 |
| 16 | `Target_2030YearEnd` | 数值 | 2030 年底目标 |

---

## 二、行类型判定

| 行类型 | 判定条件 |
|--------|----------|
| **Total 行** | `Function` ∈ `{'Total (All)', 'Total (Core Operations)'}` |
| **Non-Total 行** | `Function` 不在上述集合内（包括所有明细职能行） |

> 判定时对 `Function` 值先做 `trim()`，再精确匹配（区分大小写）。

---

## 三、必填规则（Requiredness）

### 3.1 Total 行强制必填（Option A）

| RuleId | 列 | 说明 | Severity |
|--------|----|------|----------|
| R-REQ-TOTAL-001 | `Owner` | Total 行 Owner 不得为空 | Error |
| R-REQ-TOTAL-002 | `YearMonth` | Total 行 YearMonth 不得为空，且须符合 YYYYMM 格式 | Error |

> Total 行其余列（包括 Team、Cost Center Number、所有数值列、ShoringRatio）**允许为空**。  
> 但若 ShoringRatio 不为空，仍须满足 R-SR-001 格式校验。

### 3.2 Non-Total 行强制必填（Option A）

| RuleId | 列 | 说明 | Severity |
|--------|----|------|----------|
| R-REQ-NT-001 | `Target_YearEnd` | 非 Total 行 Target_YearEnd 不得为空 | Error |
| R-REQ-NT-002 | `Target_2030YearEnd` | 非 Total 行 Target_2030YearEnd 不得为空 | Error |
| R-REQ-NT-003 | `ShoringRatio` | 非 Total 行 ShoringRatio 不得为空，且须符合 R-SR-001 格式 | Error |

> Non-Total 行其余列（Owner、YearMonth、Team、Cost Center Number、Actual_*、Planned_*）  
> **不强制必填**，但若填写则须满足对应格式/范围规则。

---

## 四、格式与范围规则（非空时校验）

### R-CCN-001 — Cost Center Number 格式

| 属性 | 值 |
|------|----|
| **RuleId** | R-CCN-001 |
| **Severity** | Error |
| **触发条件** | `trim(Cost Center Number) != ""` |
| **校验逻辑** | 必须精确匹配 `^\d{7}$`（7 位纯数字，无空格/字母/符号） |
| **Message** | `Cost Center Number 必须为 7 位纯数字（例：1234567）。` |
| **FixSuggestion** | `请确认成本中心编号为 7 位数字，不含前导零以外的非数字字符。` |

---

### R-YM-001 — YearMonth 格式

| 属性 | 值 |
|------|----|
| **RuleId** | R-YM-001 |
| **Severity** | Error |
| **触发条件** | `trim(YearMonth) != ""` |
| **校验逻辑** | 1. 必须匹配 `^\d{6}$`；2. 月份部分（最后2位）须在 01–12 范围内 |
| **Message** | `YearMonth 必须为 YYYYMM 格式（例：202501），月份须在 01–12 之间。` |
| **FixSuggestion** | `将 YearMonth 改为 6 位数字年月，如 202501、202412。` |

---

### R-FN-001 — Function 白名单

| 属性 | 值 |
|------|----|
| **RuleId** | R-FN-001 |
| **Severity** | Error |
| **触发条件** | `trim(Function) != ""` |
| **校验逻辑** | `Function` 值（trim 后）必须在允许的 Function 白名单中（见 `function-team-mapping.json`） |
| **Message** | `Function 值 "{value}" 不在允许列表中。` |
| **FixSuggestion** | `请从标准 Function 列表中选择正确的值，注意大小写与空格。` |

---

### R-TM-001 — Team 白名单

| 属性 | 值 |
|------|----|
| **RuleId** | R-TM-001 |
| **Severity** | Error |
| **触发条件** | `trim(Team) != ""` |
| **校验逻辑** | `Team` 值（trim 后）必须在允许的 Team 白名单中（见 `function-team-mapping.json`） |
| **Message** | `Team 值 "{value}" 不在允许列表中。` |
| **FixSuggestion** | `请从标准 Team 列表中选择正确的值，注意大小写与空格。` |

---

### R-FM-001 — Function-Team 组合映射

| 属性 | 值 |
|------|----|
| **RuleId** | R-FM-001 |
| **Severity** | Error |
| **触发条件** | `trim(Function) != ""` **且** `trim(Team) != ""`（两者均非空时才检查映射） |
| **校验逻辑** | (Function, Team) 组合必须在允许的映射表中（见 `function-team-mapping.json`）。特别地：Total 行 + 空 Team 是合法组合，不触发此规则（因 Team 为空，触发条件不满足）。 |
| **Message** | `Team "{team}" 不属于 Function "{function}" 的有效下属团队。` |
| **FixSuggestion** | `请确认 Function 与 Team 的对应关系，参考映射表选择正确的组合。` |

> **重要**：R-FM-001 对 Total 行**不跳过**；但 Total 行通常 Team 为空，因触发条件（Team 非空）不满足，故自然不报错。  
> 若 Total 行填写了非空 Team，则照常校验该组合是否在映射表中。

---

### R-NUM-001 — 数值列（可选，非空则校验）

| 属性 | 值 |
|------|----|
| **RuleId** | R-NUM-001 |
| **Severity** | Error |
| **适用列** | `Actual_HC`, `Actual_Offshore_HC`, `Actual_Cost`, `Actual_Offshore_Cost`, `Planned_HC`, `Planned_Offshore_HC`, `Planned_Cost`, `Planned_Offshore_Cost`, `Target_YearEnd`, `Target_2030YearEnd` |
| **触发条件** | 对应列 `trim(值) != ""`（即**非空时**才校验；空值**不报错**） |
| **校验逻辑** | 1. 能被转换为数字；2. 数值 `>= 0` |
| **Message** | `{列名} 值 "{value}" 无效，必须为大于等于 0 的数字。` |
| **FixSuggestion** | `请填入 0 或正数（不带单位符号），空值请留空而非填写文字。` |

---

### R-SR-001 — ShoringRatio 百分比格式（硬性规则）

| 属性 | 值 |
|------|----|
| **RuleId** | R-SR-001 |
| **Severity** | Error |
| **触发条件** | `trim(ShoringRatio) != ""`（Total 行非空时也校验；Non-Total 行因必填所以总是校验） |
| **校验逻辑** | 1. `v = trim(ShoringRatio)`；2. 必须匹配正则 `^\d+(\.\d+)?%$`；3. 去掉 `%` 转数字 `n`；4. 校验 `0 <= n <= 100` |
| **允许示例** | `0%`, `100%`, `12.5%`, `" 23% "` (trim后合法) |
| **不允许示例** | `23`（无%）、`0.23`（无%）、`101%`（超范围）、`-1%`（负数）、`abc%`（非数字） |
| **Message** | `ShoringRatio 必须使用百分比格式，例如 23%（范围 0%~100%，允许小数如 12.5%）。` |
| **FixSuggestion** | `将值改为"数字+%"形式（如 0%、25.5%、100%），不要填写 0.23 或 23。` |

---

## 五、规则优先级与执行顺序

每行按如下顺序执行规则（发现错误继续执行，全部规则都检查完才汇总）：

1. **R-FN-001** — Function 白名单（有值才检查）
2. **R-TM-001** — Team 白名单（有值才检查）
3. **R-FM-001** — Function-Team 映射（Function 和 Team 都非空才检查）
4. **R-CCN-001** — Cost Center Number 格式（有值才检查）
5. **R-YM-001** — YearMonth 格式（有值才检查）
6. **必填规则** — 根据行类型：
   - Total 行：R-REQ-TOTAL-001（Owner）、R-REQ-TOTAL-002（YearMonth）
   - Non-Total 行：R-REQ-NT-001（Target_YearEnd）、R-REQ-NT-002（Target_2030YearEnd）、R-REQ-NT-003（ShoringRatio）
7. **R-SR-001** — ShoringRatio 格式/范围（有值才检查）
8. **R-NUM-001** — 各数值列（逐列，有值才检查）

---

## 六、Issue 记录格式

每条校验失败记录以下字段，追加到 issues 数组：

| 字段 | 说明 |
|------|------|
| `Severity` | `Error` / `Warning` |
| `RuleId` | 规则编号（如 `R-SR-001`） |
| `YearMonth` | 行的 YearMonth 值 |
| `CostCenterNumber` | 行的 Cost Center Number 值 |
| `Function` | 行的 Function 值 |
| `Team` | 行的 Team 值 |
| `Column` | 发生错误的列名 |
| `Value` | 错误的原始值 |
| `Message` | 错误描述文案 |
| `FixSuggestion` | 修复建议文案 |

---

## 七、规则汇总表

| RuleId | 适用行 | 触发条件 | Severity | 说明 |
|--------|--------|----------|----------|------|
| R-CCN-001 | All | Cost Center Number 非空 | Error | 7 位纯数字 |
| R-YM-001 | All | YearMonth 非空 | Error | YYYYMM，月份 01–12 |
| R-FN-001 | All | Function 非空 | Error | Function 白名单 |
| R-TM-001 | All | Team 非空 | Error | Team 白名单 |
| R-FM-001 | All | Function 且 Team 均非空 | Error | Function-Team 映射 |
| R-NUM-001 | All | 各数值列非空 | Error | 数字且 >=0 |
| R-SR-001 | All | ShoringRatio 非空 | Error | 百分比格式，0%–100% |
| R-REQ-TOTAL-001 | Total 行 | 始终 | Error | Owner 必填 |
| R-REQ-TOTAL-002 | Total 行 | 始终 | Error | YearMonth 必填+格式 |
| R-REQ-NT-001 | Non-Total 行 | 始终 | Error | Target_YearEnd 必填 |
| R-REQ-NT-002 | Non-Total 行 | 始终 | Error | Target_2030YearEnd 必填 |
| R-REQ-NT-003 | Non-Total 行 | 始终 | Error | ShoringRatio 必填+格式 |
