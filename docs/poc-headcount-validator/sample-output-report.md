# 样本校验报告 — tblOffshoring 数据质量检查

**文件名**：Offshoring_Headcount_202501.xlsx  
**校验日期**：2026-03-20  
**总数据行数**：12  
**错误总数**：8  
**校验结果**：❌ 未通过（发现 8 处错误，需修复后重新提交）

---

## 一、总结概览

| 类别 | 错误数 |
|------|--------|
| 必填项缺失 | 3 |
| 格式错误 | 3 |
| 白名单 / 映射错误 | 1 |
| 数值范围错误 | 1 |
| **合计** | **8** |

---

## 二、错误明细

### 【必填项缺失】

#### 错误 1 — R-REQ-NT-001
| 字段 | 值 |
|------|----|
| **规则** | R-REQ-NT-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=1234567 / Function=Finance / Team=Finance - Accounting |
| **错误列** | `Target_YearEnd` |
| **当前值** | （空） |
| **错误原因** | 非 Total 行的 Target_YearEnd 不允许为空。 |
| **修复建议** | 填写 Target_YearEnd（>= 0 的数字）。 |

---

#### 错误 2 — R-REQ-NT-002
| 字段 | 值 |
|------|----|
| **规则** | R-REQ-NT-002 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=1234567 / Function=Finance / Team=Finance - Accounting |
| **错误列** | `Target_2030YearEnd` |
| **当前值** | （空） |
| **错误原因** | 非 Total 行的 Target_2030YearEnd 不允许为空。 |
| **修复建议** | 填写 Target_2030YearEnd（>= 0 的数字）。 |

---

#### 错误 3 — R-REQ-TOTAL-001
| 字段 | 值 |
|------|----|
| **规则** | R-REQ-TOTAL-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=（空）/ Function=Total (All) / Team=（空） |
| **错误列** | `Owner` |
| **当前值** | （空） |
| **错误原因** | Total 行必须填写 Owner。 |
| **修复建议** | 补充 Owner 后重新提交。 |

---

### 【格式错误】

#### 错误 4 — R-SR-001
| 字段 | 值 |
|------|----|
| **规则** | R-SR-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=7654321 / Function=IT / Team=IT - Development |
| **错误列** | `ShoringRatio` |
| **当前值** | `0.23` |
| **错误原因** | ShoringRatio 必须使用百分比格式，例如 23%（范围 0%~100%，允许小数如 12.5%）。 |
| **修复建议** | 将值改为"数字+%"形式（如 23%），不要填写 0.23。 |

---

#### 错误 5 — R-YM-001
| 字段 | 值 |
|------|----|
| **规则** | R-YM-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202513 / Cost Center=2222222 / Function=HR / Team=HR - Payroll |
| **错误列** | `YearMonth` |
| **当前值** | `202513` |
| **错误原因** | YearMonth 月份部分为 13，不在 01–12 的有效范围内。 |
| **修复建议** | 将 YearMonth 改为合法年月，如 202501、202412。 |

---

#### 错误 6 — R-CCN-001
| 字段 | 值 |
|------|----|
| **规则** | R-CCN-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=12345 / Function=Operations / Team=Operations - Core |
| **错误列** | `Cost Center Number` |
| **当前值** | `12345` |
| **错误原因** | Cost Center Number 必须为 7 位纯数字（例：1234567）。当前值仅 5 位。 |
| **修复建议** | 请确认成本中心编号为 7 位数字。 |

---

### 【白名单 / 映射错误】

#### 错误 7 — R-FM-001
| 字段 | 值 |
|------|----|
| **规则** | R-FM-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=3333333 / Function=Finance / Team=IT - Development |
| **错误列** | `Team` |
| **当前值** | `IT - Development` |
| **错误原因** | Team "IT - Development" 不属于 Function "Finance" 的有效下属团队。 |
| **修复建议** | Finance 的有效团队为：Finance - Accounting、Finance - FP&A。请修正 Team 或 Function。 |

---

### 【数值范围错误】

#### 错误 8 — R-NUM-001
| 字段 | 值 |
|------|----|
| **规则** | R-NUM-001 |
| **严重级别** | Error |
| **行定位** | YearMonth=202501 / Cost Center=4444444 / Function=Supply Chain / Team=Supply Chain - Logistics |
| **错误列** | `Actual_HC` |
| **当前值** | `-5` |
| **错误原因** | Actual_HC 值 "-5" 无效，必须为大于等于 0 的数字。 |
| **修复建议** | 请填入 0 或正数，空值请留空而非填写文字。 |

---

## 三、合法行示例（供对比参考）

以下行通过全部校验规则：

**Total 行（合法）**：

| 列 | 值 | 说明 |
|----|----|------|
| Cost Center Number | （空） | Total 行允许为空 |
| Function | Total (All) | 合法 Total Function |
| Team | （空） | Total + 空 Team = 合法组合，R-FM-001 不触发 |
| Owner | Zhang Wei | 非空 ✓ |
| YearMonth | 202501 | YYYYMM 格式，月份合法 ✓ |
| ShoringRatio | （空） | Total 行允许为空 |
| Actual_HC | 150 | 数字且 ≥ 0 ✓ |
| Target_YearEnd | （空） | Total 行允许为空 |

**Non-Total 行（合法）**：

| 列 | 值 | 说明 |
|----|----|------|
| Cost Center Number | 1234567 | 7 位数字 ✓ |
| Function | Finance | 在白名单中 ✓ |
| Team | Finance - Accounting | 在白名单中且映射正确 ✓ |
| Owner | Li Ming | （非必填，有值则合法） |
| YearMonth | 202501 | YYYYMM，月份合法 ✓ |
| ShoringRatio | 12.5% | 百分比格式，0–100 ✓ |
| Actual_HC | 30 | 数字且 ≥ 0 ✓ |
| Actual_Offshore_HC | （空） | 数值列空值不报错 ✓ |
| Target_YearEnd | 35 | 非空 + 数字且 ≥ 0 ✓ |
| Target_2030YearEnd | 40 | 非空 + 数字且 ≥ 0 ✓ |

---

## 四、修复优先级建议

| 优先级 | 规则 | 原因 |
|--------|------|------|
| 🔴 高 | R-REQ-NT-001 / R-REQ-NT-002 / R-REQ-NT-003 | 必填项缺失会导致 Target 数据完全丢失 |
| 🔴 高 | R-REQ-TOTAL-001 / R-REQ-TOTAL-002 | Total 汇总行无 Owner/YearMonth 无法追责和归期 |
| 🟡 中 | R-SR-001 / R-YM-001 / R-CCN-001 | 格式错误影响数据计算与下游引用 |
| 🟡 中 | R-NUM-001 | 负数数值导致汇总数据失真 |
| 🟢 低 | R-FM-001 / R-FN-001 / R-TM-001 | 白名单/映射错误影响分类汇总，但数值本身可能正确 |

请按优先级顺序修复，修复完成后重新上传文件并触发校验流程。
