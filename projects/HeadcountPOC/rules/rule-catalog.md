# 规则清单 — Headcount Offshoring 数据校验

> **数据来源**：Excel 文件 `headcount_analysis.xlsx`，Sheet `Offshoring`，Table `tblOffshoring`  
> **读取方式**：Power Automate → Excel Online (Business) → `List rows present in a table` → `tblOffshoring`  
> **表头行**：第 4 行（Excel Table 的列名行）  
> **数据行**：第 5 行起

---

## 字段定义（第 4 行列名）

| 列名 | 类型 | 说明 |
|---|---|---|
| `Cost Center Number` | 字符串 | 7 位纯数字的成本中心编号 |
| `Function` | 字符串 | 职能分类，必须在白名单内 |
| `Team` | 字符串 | 团队名称，必须在白名单内 |
| `Owner` | 字符串 | 负责人，不得为空 |
| `YearMonth` | 字符串/数字 | 年月，格式 YYYYMM，月份 01-12 |
| `Shoring Ratio` | 数字/字符串 | Offshore 比例，0-100（或 0%-100%） |
| `Headcount` | 数字 | 人力总数，非负数字 |
| `Offshore HC` | 数字 | Offshore 人力数，非负数字 |

---

## 规则清单

### R001 — Cost Center Number 格式校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R001 |
| **级别** | Error |
| **校验列** | `Cost Center Number` |
| **规则描述** | 值必须为 7 位纯数字（不含空格、字母、特殊字符） |
| **合法示例** | `1234567`, `9876543` |
| **非法示例** | `123456`（6位）、`12345678`（8位）、`CC-001`（含字母）、`123 456`（含空格） |
| **修复建议** | 检查成本中心编号，确保为 7 位纯数字，去掉前缀字母和空格 |

**Power Automate 校验逻辑（伪代码）**：
```
value = string(row['Cost Center Number']).trim()
if not (length(value) == 7 AND value matches /^[0-9]{7}$/) then
    → 追加 Error issue，RuleId=R001
```

---

### R002 — YearMonth 格式校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R002 |
| **级别** | Error |
| **校验列** | `YearMonth` |
| **规则描述** | 值必须为 6 位数字，格式 YYYYMM，且月份部分（后两位）在 01-12 之间 |
| **合法示例** | `202501`, `202412` |
| **非法示例** | `2025-01`（含横线）、`20251`（5位）、`202500`（月份=00）、`202513`（月份=13） |
| **修复建议** | 确保 YearMonth 为 6 位纯数字，格式 YYYYMM，月份在 01-12 |

**Power Automate 校验逻辑（伪代码）**：
```
value = string(row['YearMonth']).trim()
month = int(substring(value, 4, 2))
if not (length(value) == 6 AND value matches /^[0-9]{6}$/ AND month >= 1 AND month <= 12) then
    → 追加 Error issue，RuleId=R002
```

---

### R003 — Function 白名单校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R003 |
| **级别** | Error |
| **校验列** | `Function` |
| **规则描述** | 值必须在 `rules/function-whitelist.json` 的白名单内（区分大小写） |
| **合法示例** | 见 `function-whitelist.json` |
| **非法示例** | 任何不在白名单内的值 |
| **修复建议** | 检查 Function 的拼写，如确需新增，请先更新白名单并走审批流程 |

---

### R004 — Team 白名单校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R004 |
| **级别** | Error |
| **校验列** | `Team` |
| **规则描述** | 值必须在 `rules/team-whitelist.json` 的白名单内（区分大小写） |
| **合法示例** | 见 `team-whitelist.json` |
| **非法示例** | 任何不在白名单内的值 |
| **修复建议** | 检查 Team 的拼写，如确需新增，请先更新白名单并走审批流程 |

---

### R005 — Owner 非空校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R005 |
| **级别** | Error |
| **校验列** | `Owner` |
| **规则描述** | Owner 不得为空、不得为纯空格 |
| **合法示例** | `Zhang San`, `李四` |
| **非法示例** | 空字符串、纯空格 |
| **修复建议** | 填写对应的业务负责人姓名 |

---

### R006 — Shoring Ratio 范围校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R006 |
| **级别** | Warning |
| **校验列** | `Shoring Ratio` |
| **规则描述** | 值能被解析为数字，且在 0 到 100 的范围内（百分比符号自动剥除后解析） |
| **合法示例** | `30`, `30%`, `0`, `100`, `0.5` |
| **非法示例** | `150`（超过 100）、`-5`（负数）、`N/A`（无法解析为数字） |
| **修复建议** | Shoring Ratio 应填写 0-100 之间的数字（百分比无需带 %，也可带 %） |

**解析逻辑**：先尝试去掉 `%` 后转换为数字，若仍无法转换则报 Warning。

---

### R007 — Headcount 数值校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R007 |
| **级别** | Error |
| **校验列** | `Headcount` |
| **规则描述** | 值必须能被解析为数字，且 ≥ 0 |
| **合法示例** | `5`, `1.5`, `0` |
| **非法示例** | `-1`（负数）、`TBD`（非数字）、空 |
| **修复建议** | 填写实际人力数量，数字且非负 |

---

### R008 — Offshore HC 数值校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R008 |
| **级别** | Error |
| **校验列** | `Offshore HC` |
| **规则描述** | 值必须能被解析为数字，且 ≥ 0 |
| **合法示例** | `1.5`, `2`, `0` |
| **非法示例** | `-0.5`（负数）、`-`（非数字）、空 |
| **修复建议** | 填写实际 Offshore 人力数量，数字且非负 |

---

### R009 — Function-Team 组合映射校验

| 属性 | 值 |
|---|---|
| **规则 ID** | R009 |
| **级别** | Warning |
| **校验列** | `Function` + `Team` |
| **规则描述** | Function 与 Team 的组合必须在 `rules/function-team-mapping.json` 定义的合法组合内 |
| **合法示例** | 见 `function-team-mapping.json` |
| **非法示例** | 任何不在映射表内的 Function+Team 组合 |
| **修复建议** | 确认 Function 与 Team 的对应关系是否正确；如为新增合法组合，请更新映射表 |

**注意**：此规则仅在 R003 和 R004 均通过（即 Function 和 Team 各自合法）时才触发。若 Function 或 Team 本身不合法，优先报 R003/R004，不触发 R009。

---

## 规则触发优先级

```
R005（Owner 为空）→ 最优先，此行数据无法认责，直接 Error
R001（Cost Center Number 格式）→ Error
R002（YearMonth 格式）→ Error
R003（Function 白名单）→ Error
R004（Team 白名单）→ Error
R007（Headcount 数值）→ Error
R008（Offshore HC 数值）→ Error
R006（Shoring Ratio 范围）→ Warning（不阻断）
R009（Function-Team 映射）→ Warning（不阻断）
```

---

## 输出格式

每条 issue 包含以下字段：

| 字段 | 说明 |
|---|---|
| `Severity` | `Error` 或 `Warning` |
| `RuleId` | 规则 ID（如 `R001`） |
| `YearMonth` | 该行的 YearMonth 值 |
| `Cost Center Number` | 该行的成本中心编号 |
| `Function` | 该行的 Function 值 |
| `Team` | 该行的 Team 值 |
| `Column` | 有问题的列名 |
| `Value` | 有问题的单元格值 |
| `Message` | 错误/警告描述 |
| `FixSuggestion` | 修复建议 |

---

## 校验报告摘要格式

```
校验完成时间：2025-01-15 09:30:00
数据来源：headcount_analysis_poc.xlsx / Sheet: Offshoring / Table: tblOffshoring
总数据行：1,234
错误数量：12（阻断级，需修复后重新提交）
警告数量：5（提示级，建议确认）

Top 5 命中规则：
  R003 - Function 白名单不合法：8 条
  R001 - Cost Center Number 格式：2 条
  R009 - Function-Team 组合映射：3 条
  R006 - Shoring Ratio 范围：2 条
  R002 - YearMonth 格式：2 条
```
