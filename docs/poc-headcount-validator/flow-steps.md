# Power Automate Flow — POC Headcount Validator 逐步说明

**流程名称**：`POC-Headcount-Validator`  
**触发器**：手动触发 / SharePoint 文件上传触发  
**数据源**：Excel Online (Business) — 工作表 `tblOffshoring`（16 列）  
**版本**：v1.3（对应规则清单 v1.3）

---

## 整体架构

```
触发器
  └─ 初始化变量（issues 数组、计数器）
       └─ 获取 Excel 表格行（List rows present in a table）
            └─ Apply to each（遍历每一行）
                 ├─ 计算行类型 isTotal
                 ├─ 公共规则检查（R-CCN-001 / R-YM-001 / R-FN-001 / R-TM-001 / R-FM-001）
                 ├─ 数值列检查（R-NUM-001，逐列）
                 ├─ ShoringRatio 格式检查（R-SR-001）
                 └─ 必填检查（根据 isTotal 分支）
                      ├─ Total 行：R-REQ-TOTAL-001 / R-REQ-TOTAL-002
                      └─ Non-Total 行：R-REQ-NT-001 / R-REQ-NT-002 / R-REQ-NT-003
  └─ 条件：issues 数量 > 0
       ├─ Yes → 生成验证报告（发送邮件 / 写入 SharePoint / 调用 Copilot Studio）
       └─ No  → 发送"校验通过"通知
```

---

## 步骤详细说明

### Step 1 — 初始化变量

| 变量名 | 类型 | 初始值 | 说明 |
|--------|------|--------|------|
| `varIssues` | Array | `[]` | 存储所有校验错误 |
| `varErrorCount` | Integer | `0` | 错误计数 |
| `varFunctionWhitelist` | Array | （见下） | Function 允许值 |
| `varTeamWhitelist` | Array | （见下） | Team 允许值 |
| `varFunctionTeamMap` | Object | （见下） | Function→Teams 映射 |

**Function 白名单初始值（JSON Array）**：
```json
["Total (All)", "Total (Core Operations)", "Finance", "HR", "IT", "Operations", "Procurement", "Risk & Compliance", "Sales & Marketing", "Supply Chain"]
```

**Team 白名单初始值（JSON Array）**：
```json
["Finance - Accounting", "Finance - FP&A", "HR - Talent Acquisition", "HR - Payroll", "IT - Infrastructure", "IT - Development", "IT - Support", "Operations - Core", "Operations - Support", "Procurement - Sourcing", "Procurement - Vendor Mgmt", "Risk & Compliance - Audit", "Risk & Compliance - Legal", "Sales & Marketing - Digital", "Sales & Marketing - Field", "Supply Chain - Logistics", "Supply Chain - Warehouse"]
```

**Function-Team 映射初始值（JSON Object）**：
```json
{
  "Total (All)": [],
  "Total (Core Operations)": [],
  "Finance": ["Finance - Accounting", "Finance - FP&A"],
  "HR": ["HR - Talent Acquisition", "HR - Payroll"],
  "IT": ["IT - Infrastructure", "IT - Development", "IT - Support"],
  "Operations": ["Operations - Core", "Operations - Support"],
  "Procurement": ["Procurement - Sourcing", "Procurement - Vendor Mgmt"],
  "Risk & Compliance": ["Risk & Compliance - Audit", "Risk & Compliance - Legal"],
  "Sales & Marketing": ["Sales & Marketing - Digital", "Sales & Marketing - Field"],
  "Supply Chain": ["Supply Chain - Logistics", "Supply Chain - Warehouse"]
}
```

---

### Step 2 — 获取 Excel 表格行

**操作**：`Excel Online (Business) → List rows present in a table`

| 参数 | 值 |
|------|----|
| Location | SharePoint 站点 URL |
| Document Library | 文档库名称 |
| File | 上传的 Excel 文件路径 |
| Table | `tblOffshoring` |

---

### Step 3 — Apply to each（遍历每行）

**操作**：`Control → Apply to each`  
**输入**：`value`（来自 Step 2 的行集合）

---

### Step 3.1 — 计算行类型 isTotal

**操作**：`Data Operation → Compose`  
**表达式**：
```
or(
  equals(trim(items('Apply_to_each')?['Function']), 'Total (All)'),
  equals(trim(items('Apply_to_each')?['Function']), 'Total (Core Operations)')
)
```

将结果存为 `varIsTotal`（布尔型变量）。

---

### Step 3.2 — 辅助：appendIssue 逻辑（可封装为子流程）

每次发现错误时，执行以下操作追加到 `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "<ruleId>",
  "YearMonth": "@{trim(items('Apply_to_each')?['YearMonth'])}",
  "CostCenterNumber": "@{trim(items('Apply_to_each')?['Cost Center Number'])}",
  "Function": "@{trim(items('Apply_to_each')?['Function'])}",
  "Team": "@{trim(items('Apply_to_each')?['Team'])}",
  "Column": "<columnName>",
  "Value": "<actualValue>",
  "Message": "<messageText>",
  "FixSuggestion": "<fixText>"
}
```

---

### Step 3.3 — R-FN-001：Function 白名单校验

**条件（先置检查）**：
```
not(empty(trim(items('Apply_to_each')?['Function'])))
```

**若为 True，执行校验**：
```
not(contains(varFunctionWhitelist, trim(items('Apply_to_each')?['Function'])))
```

若上式为 True → appendIssue（RuleId=R-FN-001, Column=Function）

---

### Step 3.4 — R-TM-001：Team 白名单校验

**条件（先置检查）**：
```
not(empty(trim(items('Apply_to_each')?['Team'])))
```

**若为 True，执行校验**：
```
not(contains(varTeamWhitelist, trim(items('Apply_to_each')?['Team'])))
```

若上式为 True → appendIssue（RuleId=R-TM-001, Column=Team）

---

### Step 3.5 — R-FM-001：Function-Team 映射校验

**条件（先置检查，两者均非空）**：
```
and(
  not(empty(trim(items('Apply_to_each')?['Function']))),
  not(empty(trim(items('Apply_to_each')?['Team'])))
)
```

> **说明**：Total 行通常 Team 为空，因此自然跳过此检查（不需要针对 Total 行单独 skip）。若 Total 行填写了非空 Team，则照常检查。

**若为 True，执行映射校验**：
```
not(
  contains(
    getProperty(varFunctionTeamMap, trim(items('Apply_to_each')?['Function'])),
    trim(items('Apply_to_each')?['Team'])
  )
)
```

> 若 Function 不在映射表 key 中，`getProperty` 返回 null，`contains(null, ...)` 为 false → 报错（此时 R-FN-001 也已报错）。

若上式为 True → appendIssue（RuleId=R-FM-001, Column=Team）

---

### Step 3.6 — R-CCN-001：Cost Center Number 格式校验

**条件（先置检查）**：
```
not(empty(trim(items('Apply_to_each')?['Cost Center Number'])))
```

**若为 True，执行校验**（7 位纯数字）：
```
not(
  and(
    equals(length(trim(items('Apply_to_each')?['Cost Center Number'])), 7),
    isInt(int(trim(items('Apply_to_each')?['Cost Center Number'])))
  )
)
```

> 替代写法（使用 `match()`，若环境支持）：
> ```
> not(match(trim(items('Apply_to_each')?['Cost Center Number']), '^\d{7}$'))
> ```

若上式为 True → appendIssue（RuleId=R-CCN-001, Column=Cost Center Number）

---

### Step 3.7 — R-YM-001：YearMonth 格式校验

**条件（先置检查）**：
```
not(empty(trim(items('Apply_to_each')?['YearMonth'])))
```

**若为 True，执行双重校验**：

1. 长度 = 6 且全为数字（6 位数字格式）：
```
and(
  equals(length(trim(items('Apply_to_each')?['YearMonth'])), 6),
  isInt(int(trim(items('Apply_to_each')?['YearMonth'])))
)
```

2. 月份部分（最后 2 位）在 01–12 之间：
```
and(
  greaterOrEquals(int(last(split(trim(items('Apply_to_each')?['YearMonth']), '')... )),
  lessOrEquals(...)
)
```

> 简洁替代：截取月份：
> ```
> varMonth = int(substring(trim(items('Apply_to_each')?['YearMonth']), 4, 2))
> ```
> 然后判断 `varMonth >= 1 AND varMonth <= 12`

若任一条件不满足 → appendIssue（RuleId=R-YM-001, Column=YearMonth）

---

### Step 3.8 — R-NUM-001：数值列逐列校验（**空值不校验**）

对以下 10 列，每列独立执行（可用 Apply to each 循环列名数组，或展开为 10 个条件步骤）：

**列名数组**：
```json
["Actual_HC", "Actual_Offshore_HC", "Actual_Cost", "Actual_Offshore_Cost",
 "Planned_HC", "Planned_Offshore_HC", "Planned_Cost", "Planned_Offshore_Cost",
 "Target_YearEnd", "Target_2030YearEnd"]
```

对每一数值列 `colName`：

**Step A — 先置检查（非空时才校验）**：
```
not(empty(trim(items('Apply_to_each')?[colName])))
```

**Step B — 若非空，执行数值校验**：

检查能否转为数字且 >= 0：
```
or(
  not(isFloat(float(trim(items('Apply_to_each')?[colName])))),
  less(float(trim(items('Apply_to_each')?[colName])), 0)
)
```

若上式为 True（非数字或负数）→ appendIssue（RuleId=R-NUM-001, Column=colName）

> **关键点**：若列值为空/blank/null，**Step A** 条件为 False，直接跳过，**不报错**。

---

### Step 3.9 — R-SR-001：ShoringRatio 格式校验

**条件（先置检查）**：
```
not(empty(trim(items('Apply_to_each')?['ShoringRatio'])))
```

**若为 True，执行三步校验**：

**Step A** — 取 trimmed 值：
```
varSR = trim(items('Apply_to_each')?['ShoringRatio'])
```

**Step B** — 检查末位必须是 `%`：
```
equals(last(split(varSR, '')), '%')
```

**Step C** — 去掉 `%`，检查数值部分是否为数字：
```
varSRNum = float(replace(varSR, '%', ''))
```

**Step D** — 范围检查 0 ≤ n ≤ 100：
```
and(
  greaterOrEquals(varSRNum, 0),
  lessOrEquals(varSRNum, 100)
)
```

若 B、C、D 任一失败 → appendIssue（RuleId=R-SR-001, Column=ShoringRatio）

> **使用 match() 的环境（推荐更简洁）**：
> ```
> if(
>   and(
>     match(varSR, '^\d+(\.\d+)?%$'),
>     greaterOrEquals(float(replace(varSR, '%', '')), 0),
>     lessOrEquals(float(replace(varSR, '%', '')), 100)
>   ),
>   'pass',
>   'fail'
> )
> ```

---

### Step 3.10 — 必填规则（按 isTotal 分支）

#### 分支 A：Total 行（isTotal = true）

**R-REQ-TOTAL-001 — Owner 必填**：
```
empty(trim(items('Apply_to_each')?['Owner']))
```
若为 True → appendIssue（RuleId=R-REQ-TOTAL-001, Column=Owner）

**R-REQ-TOTAL-002 — YearMonth 必填（空值检查，格式已在 Step 3.7 校验）**：
```
empty(trim(items('Apply_to_each')?['YearMonth']))
```
若为 True → appendIssue（RuleId=R-REQ-TOTAL-002, Column=YearMonth）

> 注：若 YearMonth 填了但格式不对，Step 3.7（R-YM-001）已报错；此处仅检查是否为空。

---

#### 分支 B：Non-Total 行（isTotal = false）

**R-REQ-NT-001 — Target_YearEnd 必填**：
```
empty(trim(items('Apply_to_each')?['Target_YearEnd']))
```
若为 True → appendIssue（RuleId=R-REQ-NT-001, Column=Target_YearEnd）

**R-REQ-NT-002 — Target_2030YearEnd 必填**：
```
empty(trim(items('Apply_to_each')?['Target_2030YearEnd']))
```
若为 True → appendIssue（RuleId=R-REQ-NT-002, Column=Target_2030YearEnd）

**R-REQ-NT-003 — ShoringRatio 必填**（格式已在 Step 3.9 校验）：
```
empty(trim(items('Apply_to_each')?['ShoringRatio']))
```
若为 True → appendIssue（RuleId=R-REQ-NT-003, Column=ShoringRatio）

---

### Step 4 — 结果输出

#### 条件判断：有无错误

```
greater(length(varIssues), 0)
```

#### Yes — 有错误：发送报告

- 可选操作 1：发送邮件（Office 365 Outlook → Send an email）
  - 邮件正文：将 `varIssues` 序列化为 HTML 表格
- 可选操作 2：写入 SharePoint 列表（Create item per issue）
- 可选操作 3：调用 Power Virtual Agents / Copilot Studio webhook，传入 issues JSON

**传入 Copilot Studio 的 JSON 格式**：
```json
{
  "validationDate": "@{utcNow()}",
  "fileName": "@{triggerBody()?['fileName']}",
  "errorCount": "@{length(varIssues)}",
  "issues": "@{varIssues}"
}
```

#### No — 无错误：通知成功

```
Send notification: "校验通过，共 {rowCount} 行，无错误。"
```

---

## 关键表达式速查

| 场景 | Power Automate 表达式 |
|------|-----------------------|
| trim 字段值 | `trim(items('Apply_to_each')?['FieldName'])` |
| 判断非空 | `not(empty(trim(...)))` |
| 判断空 | `empty(trim(...))` |
| 转浮点数 | `float(trim(...))` |
| 转整数 | `int(trim(...))` |
| 字符串长度 | `length(trim(...))` |
| 字符串替换 | `replace(value, '%', '')` |
| 正则匹配（如支持）| `match(value, '^\d{7}$')` |
| 数组包含 | `contains(arrayVar, value)` |
| JSON 对象属性 | `getProperty(objectVar, key)` |
| 追加数组 | `Append to array variable` |

---

## 注意事项

1. **Empty vs Null**：Excel 连接器读出空单元格可能是 `null` 或 `""` 或 `" "`；统一用 `trim()` 后再 `empty()` 判断。
2. **数值列类型**：Excel 读出数值可能为字符串或数值类型；建议先 `trim()` 再 `string()` 转字符串，再 `float()` 转数字。
3. **R-FM-001 与 Total 行**：不需要对 Total 行单独跳过映射检查；触发条件（Team 非空）已自然处理。Total + 空 Team = 合法组合。
4. **R-NUM-001 空值处理**：空值 **不报错**，只有非空且不合法才报错。这是与 R-REQ-* 规则配合的关键。
5. **R-SR-001 与 R-REQ-NT-003 联动**：Non-Total 行的 ShoringRatio 先由 R-REQ-NT-003 检查必填（空报必填错），再由 R-SR-001 检查格式（非空且格式错才报格式错）。两个规则互不冲突。
