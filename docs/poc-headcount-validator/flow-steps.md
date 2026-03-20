# Power Automate Flow — POC Headcount Excel Validator 步骤说明

**表格：** `tblOffshoring`  
**版本：** 2.0（YearMonth 所有行均必填）  
**Last Updated：** 2026-03-20

---

## 概述

本 Flow 读取 Excel 表格 `tblOffshoring` 的每一行数据，逐行执行校验规则，并将发现的问题汇总输出为结构化报告，供 Copilot Studio 生成中文摘要。

---

## 流程步骤（Step-by-Step）

### Step 1：触发器（Trigger）

- **类型**：手动触发（Manually trigger a flow）或按计划触发  
- **配置**：如需上传附件触发，可改为"收到邮件附件"触发器

---

### Step 2：读取 Excel 表格（List rows present in a table）

- **连接器**：Excel Online (Business)  
- **动作**：`List rows present in a table`  
- **配置**：
  - Location：指定 SharePoint/OneDrive 位置  
  - Document Library：指定文档库  
  - File：上传的 Excel 文件  
  - Table：`tblOffshoring`

---

### Step 3：初始化 Issues 数组（Initialize variable）

- **变量名**：`varIssues`  
- **类型**：Array  
- **初始值**：`[]`

---

### Step 4：遍历每行数据（Apply to each）

**遍历**：`List rows present in a table` 的 `value` 输出

对每一行执行以下子步骤：

---

#### Step 4.1：计算行类型（Compose — isTotal）

```
isTotal = or(
  equals(trim(items('Apply_to_each')?['Function']), 'Total (All)'),
  equals(trim(items('Apply_to_each')?['Function']), 'Total (Core Operations)')
)
```

---

#### Step 4.2：必填检查（Requiredness Checks）

执行顺序严格按以下步骤，**避免重复报错**。

---

##### Step 4.2.1：YearMonth 必填检查（所有行 — R-REQ-ALL-YM）

```
ymValue = trim(items('Apply_to_each')?['YearMonth'])
ymIsEmpty = equals(ymValue, '')
```

**条件（If `ymIsEmpty` = true）：**
- Append to array variable `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-REQ-ALL-YM",
  "YearMonth": "",
  "CostCenterNumber": "<本行 Cost Center Number>",
  "Function": "<本行 Function>",
  "Team": "<本行 Team>",
  "Column": "YearMonth",
  "Value": "",
  "Message": "YearMonth 不允许为空，所有行（包括 Total 行）均须填写。",
  "FixSuggestion": "填写 YearMonth，格式为 YYYYMM（例如 202501）。"
}
```

> ⚠️ **关键**：若 `ymIsEmpty = true`，则 **跳过 Step 4.3.1（YearMonth 格式检查 R-YM-001）**，避免重复报错。  
> 使用一个布尔变量 `varYmSkipFormat` 标记是否跳过格式检查：  
> - 若 `ymIsEmpty = true`，则 `varYmSkipFormat = true`  
> - 否则 `varYmSkipFormat = false`

---

##### Step 4.2.2：Owner 必填检查（仅 Total 行 — R-REQ-TOTAL-OWNER）

**条件（If `isTotal` = true）：**

```
ownerValue = trim(items('Apply_to_each')?['Owner'])
```

若 `ownerValue == ''`，Append to `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-REQ-TOTAL-OWNER",
  "YearMonth": "<本行 YearMonth>",
  "CostCenterNumber": "<本行 Cost Center Number>",
  "Function": "<本行 Function>",
  "Team": "<本行 Team>",
  "Column": "Owner",
  "Value": "",
  "Message": "Total 行必须填写 Owner。",
  "FixSuggestion": "补充 Owner 后重新提交。"
}
```

---

##### Step 4.2.3：Non-Total 行三列必填检查（R-REQ-NT-TGT1 / TGT2 / SR）

**条件（If `isTotal` = false）：**

对以下三列分别检查，逻辑相同：

| 列名 | RuleId | Message |
|------|--------|---------|
| `Target_YearEnd` | `R-REQ-NT-TGT1` | `非 Total 行 Target_YearEnd 不允许为空。` |
| `Target_2030YearEnd` | `R-REQ-NT-TGT2` | `非 Total 行 Target_2030YearEnd 不允许为空。` |
| `ShoringRatio` | `R-REQ-NT-SR` | `非 Total 行 ShoringRatio 不允许为空。` |

若对应列 trim 后为空，Append to `varIssues`（格式同上，Column 和 RuleId 对应替换）。

> 若 `ShoringRatio` 触发了 R-REQ-NT-SR（为空），则 **跳过该行的 R-SR-001**（格式检查）。  
> 使用布尔变量 `varSrSkipFormat` 记录：若 R-REQ-NT-SR 触发，则 `varSrSkipFormat = true`；否则 `false`。

---

#### Step 4.3：格式 / 枚举检查（Format & Enumeration Checks）

仅对非空列执行（若已在 4.2 报必填空，则跳过对应格式检查）。

---

##### Step 4.3.1：YearMonth 格式检查（R-YM-001）

**条件：** `varYmSkipFormat = false`（即 YearMonth 非空）

```
ymValue = trim(items('Apply_to_each')?['YearMonth'])
```

格式校验（Power Automate 表达式）：

```
and(
  equals(length(ymValue), 6),
  equals(string(int(ymValue)), ymValue),   // 全为数字
  greaterOrEquals(int(substring(ymValue, 4, 2)), 1),
  lessOrEquals(int(substring(ymValue, 4, 2)), 12)
)
```

若不满足，Append to `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-YM-001",
  "Column": "YearMonth",
  "Value": "<ymValue>",
  "Message": "YearMonth 格式错误，必须为 YYYYMM（例如 202501），月份须为 01-12。",
  "FixSuggestion": "将 YearMonth 改为 6 位数字年月格式。"
}
```

---

##### Step 4.3.2：Cost Center Number 格式检查（R-CCN-001）

**条件：** `trim(Cost Center Number) != ''`

格式校验：

```
and(
  equals(length(trim(ccnValue)), 7),
  equals(string(int(trim(ccnValue))), trim(ccnValue))  // 全为数字
)
```

若不满足，Append to `varIssues`（RuleId: `R-CCN-001`）。

---

##### Step 4.3.3：Function 白名单检查（R-FUNC-001）

**条件：** `trim(Function) != ''`

使用 `contains()` 检查是否在白名单数组中：

```
contains(variables('varFunctionWhitelist'), trim(funcValue))
```

若不在白名单，Append to `varIssues`（RuleId: `R-FUNC-001`，Severity: `Error`）。

---

##### Step 4.3.4：Team 白名单检查（R-TEAM-001）

**条件：** `trim(Team) != ''`

```
contains(variables('varTeamWhitelist'), trim(teamValue))
```

若不在白名单，Append to `varIssues`（RuleId: `R-TEAM-001`，Severity: `Warning`）。

---

#### Step 4.4：Function-Team 组合映射检查（R-MAP-001）

**检查方式**：构造组合 Key = `trim(Function) + "|" + trim(Team)`，查找是否在映射表（对象数组）中。

```
combinationKey = concat(trim(funcValue), '|', trim(teamValue))
contains(variables('varFunctionTeamMap'), combinationKey)
```

**映射表初始化示例**（Step 3 之前初始化变量 `varFunctionTeamMap`）：

```json
[
  "Total (All)|",
  "Total (Core Operations)|",
  "Function_A|Team_X",
  "Function_A|Team_Y",
  "Function_B|Team_Z"
]
```

> 包含 `Total (All)|`（空 Team）和 `Total (Core Operations)|`（空 Team）两个特殊组合。

若不在映射表，Append to `varIssues`（RuleId: `R-MAP-001`，Severity: `Error`）。

---

#### Step 4.5：数值列检查（Numeric Column Rules）

对以下 10 列逐列执行（仅非空时校验）：

```
[Actual_GBS_TeamMember, Actual_GBS_TeamLeaderAM, Actual_EA, Actual_HKT,
 Planned_GBS_TeamMember, Planned_GBS_TeamLeaderAM, Planned_EA, Planned_HKT,
 Target_YearEnd, Target_2030YearEnd]
```

**对每列：**

```
colValue = trim(items('Apply_to_each')?['<列名>'])
```

1. **若 colValue == ''**：跳过（不报错）
2. **R-NUM-001**：检查能否解析为数字
   - Power Automate：`equals(string(float(colValue)), colValue)` 或使用 `tryConvertToNumber`
   - 若非数字，Append Error（R-NUM-001），并跳过 R-NUM-002
3. **R-NUM-002**：检查 `float(colValue) >= 0`
   - 若为负数，Append Error（R-NUM-002）

---

#### Step 4.6：ShoringRatio 格式检查（R-SR-001）

**条件：** `varSrSkipFormat = false`（即 ShoringRatio 非空）

```
srValue = trim(items('Apply_to_each')?['ShoringRatio'])
```

**若 srValue != ''**（Total 行 ShoringRatio 可空，空时跳过）：

1. 正则检查（使用 `startsWith` + `endsWith` 模拟，或 Copilot Studio 后处理）：
   - 必须以 `%` 结尾
   - 去掉 `%` 后（`replace(srValue, '%', '')`）能解析为数字 n
   - `0 ≤ n ≤ 100`
2. 若不满足，Append to `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-SR-001",
  "Column": "ShoringRatio",
  "Value": "<srValue>",
  "Message": "ShoringRatio 必须使用百分比格式，例如 23%（范围 0%–100%，允许小数如 12.5%）。",
  "FixSuggestion": "改为"数字+%"格式，不要填写 0.23 或 23。"
}
```

---

### Step 5：生成报告 JSON（Compose — Final Report）

```json
{
  "ReportGeneratedAt": "<utcNow()>",
  "TotalRows": <length(body('List_rows_present_in_a_table')?['value'])>,
  "TotalIssues": <length(variables('varIssues'))>,
  "ErrorCount": <count of Severity=Error in varIssues>,
  "WarningCount": <count of Severity=Warning in varIssues>,
  "Issues": <variables('varIssues')>
}
```

---

### Step 6：输出报告（发送给 Copilot Studio / 写入 SharePoint）

- **选项 A**：以 HTTP 响应返回 JSON（供 Copilot Studio 解析）
- **选项 B**：将报告 JSON 写入 SharePoint 文档库供人工审查

---

## 变量初始化清单（Step 3 之前）

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `varIssues` | Array | 收集所有 Issue 记录 |
| `varFunctionWhitelist` | Array | Function 白名单（字符串数组） |
| `varTeamWhitelist` | Array | Team 白名单（字符串数组） |
| `varFunctionTeamMap` | Array | Function\|Team 组合允许列表 |
| `varYmSkipFormat` | Boolean | 标记当前行是否跳过 YearMonth 格式检查 |
| `varSrSkipFormat` | Boolean | 标记当前行是否跳过 ShoringRatio 格式检查 |

> `varYmSkipFormat` 和 `varSrSkipFormat` 在每行循环开始时重置为 `false`。

---

## 执行顺序流程图（简版）

```
┌─────────────────────────────────────────────────────────┐
│ For each row                                             │
│                                                          │
│  ① 计算 isTotal                                          │
│                                                          │
│  ② 必填检查                                              │
│     ├─ R-REQ-ALL-YM (YearMonth，所有行)                  │
│     │   └─ 若空 → 报错 + varYmSkipFormat=true            │
│     ├─ R-REQ-TOTAL-OWNER (Owner，仅 Total)               │
│     └─ R-REQ-NT-TGT1/TGT2/SR (仅 Non-Total)             │
│         └─ SR 若空 → 报错 + varSrSkipFormat=true         │
│                                                          │
│  ③ 格式/枚举检查（跳过已空的列）                           │
│     ├─ R-YM-001  (若 varYmSkipFormat=false)             │
│     ├─ R-CCN-001 (非空)                                  │
│     ├─ R-FUNC-001 (非空)                                 │
│     └─ R-TEAM-001 (非空)                                 │
│                                                          │
│  ④ R-MAP-001 (Function-Team 组合)                        │
│                                                          │
│  ⑤ R-NUM-001/002 (10 个数值列，非空)                     │
│                                                          │
│  ⑥ R-SR-001 (若 varSrSkipFormat=false 且非空)            │
└─────────────────────────────────────────────────────────┘
```
