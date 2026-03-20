# Power Automate 流程步骤说明 – tblOffshoring 校验流程

> **适用版本**: Power Automate Cloud Flow（Excel Online (Business) 连接器）  
> **触发方式**: 手动触发（POC 阶段）；正式版可改为定时触发（每月 N 号自动跑）  
> **数据源**: SharePoint 上 `headcount_analysis_poc.xlsx` 中的 Excel Table `tblOffshoring`

---

## 整体流程概览

```
[触发器]
    ↓
[Step 1] 初始化变量（issues数组、统计计数器、mapping/whitelist）
    ↓
[Step 2] Excel Online: List rows present in a table (tblOffshoring)
    ↓
[Step 3] Apply to each 行 → 逐行执行规则校验 → 追加 issues
    ↓
[Step 4] 聚合统计（Error总数、Warning总数、按RuleId汇总）
    ↓
[Step 5] 完整 issues 写入 SharePoint（CSV/Excel）
    ↓
[Step 6] 构建精简 payload（摘要 + Top 50 issues）
    ↓
[Step 7] 发送到 Copilot Studio 生成式步骤 → 输出 Markdown 报告
    ↓
[Step 8] 返回报告链接（SharePoint 文件 URL）
```

---

## 详细步骤

### Step 0 – 前置准备

**在 SharePoint 上确认以下信息**（后续步骤中需要填写）：

| 配置项 | 示例值 | 说明 |
|---|---|---|
| SharePoint Site URL | `https://contoso.sharepoint.com/sites/GBS` | 你的 SharePoint 站点 |
| 文档库名称 | `Documents` | 文件所在文档库 |
| 文件路径 | `POC/headcount_analysis_poc.xlsx` | 相对于文档库根路径 |
| Table 名称 | `tblOffshoring` | Excel Table 名称 |
| 输出文件夹路径 | `POC/ValidationResults/` | 校验结果存放目录 |

---

### Step 1 – 初始化变量

新增以下变量（动作：**Initialize variable**）：

| 变量名 | 类型 | 初始值 |
|---|---|---|
| `var_issues` | Array | `[]` |
| `var_errorCount` | Integer | `0` |
| `var_warningCount` | Integer | `0` |
| `var_totalRows` | Integer | `0` |
| `var_ruleStats` | Object | `{}` |
| `var_functionWhitelist` | Array | *(见下方)* |
| `var_teamWhitelist` | Array | *(见下方)* |
| `var_functionTeamMapping` | Object | *(见下方)* |

**var_functionWhitelist 初始值**（复制自 `rules/function-whitelist.json`）：
```json
["Finance","HR","IT","Procurement","Legal","Supply Chain","Analytics","Compliance","Risk Management","Marketing","Operations","Customer Service","Tax","Treasury","Audit"]
```

**var_teamWhitelist 初始值**（复制自 `rules/team-whitelist.json`）：
```json
["GBS-Finance-AP","GBS-Finance-AR","GBS-Finance-GL","GBS-Finance-Reporting","GBS-HR-Payroll","GBS-HR-Recruitment","GBS-HR-L&D","GBS-IT-Infra","GBS-IT-AppSupport","GBS-Procurement-Sourcing","GBS-Procurement-PO","GBS-Legal-Contract","GBS-SupplyChain-Planning","GBS-SupplyChain-Logistics","GBS-Analytics-BI","GBS-Analytics-DataEng","GBS-Compliance-AML","GBS-Compliance-KYC","GBS-Risk-OpRisk","GBS-CS-L1","GBS-CS-L2","GBS-Tax-Indirect","GBS-Tax-Direct","GBS-Treasury-Cash","GBS-Audit-Internal"]
```

**var_functionTeamMapping 初始值**（复制自 `rules/function-team-mapping.json`，去掉 `_comment` 字段）：
```json
{"Finance":["GBS-Finance-AP","GBS-Finance-AR","GBS-Finance-GL","GBS-Finance-Reporting"],"HR":["GBS-HR-Payroll","GBS-HR-Recruitment","GBS-HR-L&D"],"IT":["GBS-IT-Infra","GBS-IT-AppSupport"],"Procurement":["GBS-Procurement-Sourcing","GBS-Procurement-PO"],"Legal":["GBS-Legal-Contract"],"Supply Chain":["GBS-SupplyChain-Planning","GBS-SupplyChain-Logistics"],"Analytics":["GBS-Analytics-BI","GBS-Analytics-DataEng"],"Compliance":["GBS-Compliance-AML","GBS-Compliance-KYC"],"Risk Management":["GBS-Risk-OpRisk"],"Customer Service":["GBS-CS-L1","GBS-CS-L2"],"Tax":["GBS-Tax-Indirect","GBS-Tax-Direct"],"Treasury":["GBS-Treasury-Cash"],"Audit":["GBS-Audit-Internal"],"Marketing":[],"Operations":[]}
```

---

### Step 2 – 读取 tblOffshoring 数据

**动作**: `Excel Online (Business)` → **List rows present in a table**

| 参数 | 值 |
|---|---|
| Location | SharePoint（选择你的租户） |
| Document Library | `Documents`（你的文档库） |
| File | `POC/headcount_analysis_poc.xlsx` |
| Table | `tblOffshoring` |

> 成功后，动作输出 `value` 为行数组，每行是一个 JSON 对象，key 为列名（与 `tblOffshoring` 表头完全一致）。  
> 示例行对象：`{"Cost Center Number":"1234567","Function":"Finance","Team":"GBS-Finance-AP","Owner":"Alice Wang","YearMonth":"202503","Actual_GBS_TeamMember":"5","ShoringRatio":"45%",...}`

---

### Step 3 – 遍历每行并执行校验

**动作**: **Apply to each** → 输入：`outputs('List_rows_present_in_a_table')?['body/value']`

在 Apply to each 内部，对每一行（`item()`）执行以下子步骤：

#### Step 3.0 – 递增总行数
```
Increment variable: var_totalRows by 1
```

#### Step 3.1 – R01: Cost Center Number 格式校验

**动作**: Condition

**条件（True = 通过, False = 失败）**：
```
and(
  not(empty(item()?['Cost Center Number'])),
  equals(length(string(item()?['Cost Center Number'])), 7)
)
```
> 注意：Power Automate 无原生正则；如需严格校验纯数字，使用 Office Script 辅助函数或按位判断每个字符。

如条件为 **False**（失败），执行：
- Append to array variable: `var_issues`
- Value:
```json
{
  "Severity": "Error",
  "RuleId": "R01_CostCenterFormat",
  "YearMonth": "@{item()?['YearMonth']}",
  "Cost Center Number": "@{item()?['Cost Center Number']}",
  "Function": "@{item()?['Function']}",
  "Team": "@{item()?['Team']}",
  "Column": "Cost Center Number",
  "Value": "@{item()?['Cost Center Number']}",
  "Message": "Cost Center Number must be exactly 7 digits",
  "FixSuggestion": "Pad with leading zeros or verify with data owner"
}
```
- Increment variable: `var_errorCount` by 1

#### Step 3.2 – R02: Function 白名单校验

**条件**：
```
contains(variables('var_functionWhitelist'), item()?['Function'])
```
如条件为 **False**，追加 issue：
```json
{
  "Severity": "Error",
  "RuleId": "R02_FunctionWhitelist",
  "YearMonth": "@{item()?['YearMonth']}",
  "Cost Center Number": "@{item()?['Cost Center Number']}",
  "Function": "@{item()?['Function']}",
  "Team": "@{item()?['Team']}",
  "Column": "Function",
  "Value": "@{item()?['Function']}",
  "Message": "Function value is not in the allowed whitelist",
  "FixSuggestion": "Use one of: Finance, HR, IT, Procurement, Legal, Supply Chain, Analytics, Compliance, Risk Management, Marketing, Operations, Customer Service, Tax, Treasury, Audit"
}
```

#### Step 3.3 – R03: Team 白名单校验

**条件**：
```
contains(variables('var_teamWhitelist'), item()?['Team'])
```
如条件为 **False**，追加 issue（`RuleId: R03_TeamWhitelist`, `Column: Team`）。

#### Step 3.4 – R04: Owner 非空校验

**条件**：
```
not(empty(trim(string(item()?['Owner']))))
```
如条件为 **False**，追加 issue（`RuleId: R04_OwnerNonEmpty`, `Column: Owner`）。

#### Step 3.5 – R05: YearMonth 格式与有效月份校验

**条件**（分两步：长度检查 + 月份范围）：
```
and(
  equals(length(string(item()?['YearMonth'])), 6),
  greaterOrEquals(int(substring(string(item()?['YearMonth']), 4, 2)), 1),
  lessOrEquals(int(substring(string(item()?['YearMonth']), 4, 2)), 12)
)
```
如条件为 **False**，追加 issue（`RuleId: R05_YearMonthFormat`, `Column: YearMonth`）。

#### Step 3.6 – R06: 数值列非负校验（循环 10 列）

对以下 10 列逐一执行 Condition（可复制 10 次子流程或用 Apply to each 遍历列名数组）：

```
Actual_GBS_TeamMember | Actual_GBS_TeamLeaderAM | Actual_EA | Actual_HKT
Planned_GBS_TeamMember | Planned_GBS_TeamLeaderAM | Planned_EA | Planned_HKT
Target_YearEnd | Target_2030YearEnd
```

对每列，条件为（以 `COL` 代表当前列名）：
```
and(
  not(empty(item()?['COL'])),
  greaterOrEquals(float(string(item()?['COL'])), 0)
)
```
> **错误处理**：将 Condition 动作的"Run after"配置为"在前一步成功或失败时运行"，并捕获 `float()` 转换异常，转换失败时也产生 `R06_NumericNonNegative` issue。

如条件为 **False** 或转换失败，追加 issue：
```json
{
  "Severity": "Error",
  "RuleId": "R06_NumericNonNegative",
  "Column": "COL",
  "Message": "Column COL must be a non-negative number",
  "FixSuggestion": "Replace non-numeric or negative values with 0 or a valid positive number"
}
```

#### Step 3.7 – R07: ShoringRatio 解析归一化与范围校验

因为 Power Automate 无原生百分比解析，建议调用 **Office Script**（通过 Run Script 动作）：

**Office Script（`normalizeShoringRatio.ts`）**：
```typescript
function main(workbook: ExcelScript.Workbook, rawValue: string): { normalized: number, isValid: boolean, message: string } {
  let raw = rawValue.trim();
  let normalized: number;

  if (raw.endsWith('%')) {
    normalized = parseFloat(raw.slice(0, -1));
  } else {
    const num = parseFloat(raw);
    normalized = num <= 1.0 && num >= 0 ? num * 100 : num;
  }

  if (isNaN(normalized)) {
    return { normalized: -1, isValid: false, message: `Cannot parse ShoringRatio value: ${rawValue}` };
  }

  const isValid = normalized >= 0 && normalized <= 100;
  return {
    normalized,
    isValid,
    message: isValid ? '' : `ShoringRatio normalized to ${normalized}, must be 0-100`
  };
}
```

如果 `isValid = false`，追加 issue（`RuleId: R07_ShoringRatio`, `Column: ShoringRatio`）。

#### Step 3.8 – R08: Function-Team 组合映射校验

**条件**（需要 R02 和 R03 都通过才有意义；建议在 R02/R03 通过的分支内执行）：
```
contains(
  variables('var_functionTeamMapping')?[item()?['Function']],
  item()?['Team']
)
```
如条件为 **False**，追加 issue：
```json
{
  "Severity": "Error",
  "RuleId": "R08_FunctionTeamMapping",
  "Column": "Function+Team",
  "Message": "Team '@{item()?['Team']}' is not allowed under Function '@{item()?['Function']}'",
  "FixSuggestion": "Check function-team-mapping.json for allowed combinations"
}
```

---

### Step 4 – 聚合统计

**Apply to each 结束后**，执行以下聚合：

```
// 按 RuleId 分组统计 issue 数量
// 使用 Select + Group by（或在 Step 3 内维护 var_ruleStats 对象）

var_summary = {
  "totalRows": @{variables('var_totalRows')},
  "errorCount": @{variables('var_errorCount')},
  "warningCount": @{variables('var_warningCount')},
  "top5Rules": <取 var_issues 按 RuleId 聚合后 Top 5>
}
```

> **简化实现**：在 Step 3 的每个失败分支中，同时对 `var_ruleStats` 对象做计数（用 Set variable 覆盖写入），如 `add(if(contains(variables('var_ruleStats'), 'R01_CostCenterFormat'), variables('var_ruleStats')?['R01_CostCenterFormat'], 0), 1)`。

---

### Step 5 – 写入完整 issues 到 SharePoint

**完整 issues（所有问题行）持久化方案（二选一）**：

#### 方案 A：写入 CSV 文件

**动作**: `SharePoint` → **Create file**

| 参数 | 值 |
|---|---|
| Site Address | `https://contoso.sharepoint.com/sites/GBS` |
| Folder Path | `/Documents/POC/ValidationResults/` |
| File Name | `validation_@{formatDateTime(utcNow(),'yyyyMMddHHmm')}.csv` |
| File Content | CSV 内容（使用 `join` + `select` 构建 CSV 字符串） |

**CSV 构建表达式**：
```
concat(
  'Severity,RuleId,YearMonth,Cost Center Number,Function,Team,Column,Value,Message,FixSuggestion',
  decodeUriComponent('%0A'),
  join(
    select(variables('var_issues'), item(), 
      concat(item()?['Severity'],',',item()?['RuleId'],',',item()?['YearMonth'],',',item()?['Cost Center Number'],',',item()?['Function'],',',item()?['Team'],',',item()?['Column'],',',item()?['Value'],',',item()?['Message'],',',item()?['FixSuggestion'])
    ),
    decodeUriComponent('%0A')
  )
)
```

#### 方案 B：写入 Excel 表格（需预建模板文件）

**动作**: `Excel Online (Business)` → **Add a row into a table**  
将 issues 逐行写入预先在 SharePoint 上创建好的 `validation_results_template.xlsx` 中的 `tblIssues` 表。

---

### Step 6 – 构建精简 Payload 发送给 Copilot Studio

从 `var_issues` 中取前 50 条（优先取 Severity=Error）：

**动作**: Compose（组合动作）

```json
{
  "summary": {
    "totalRows": @{variables('var_totalRows')},
    "errorCount": @{variables('var_errorCount')},
    "warningCount": @{variables('var_warningCount')},
    "runTimestamp": "@{utcNow()}",
    "sourceFile": "headcount_analysis_poc.xlsx",
    "sourceTable": "tblOffshoring"
  },
  "topIssues": @{take(variables('var_issues'), 50)},
  "fullReportUrl": "@{outputs('Create_file')?['body/webUrl']}"
}
```

---

### Step 7 – 调用 Copilot Studio 生成式步骤

**动作**: `HTTP` 或 Power Automate 的 **Send an HTTP request to SharePoint** / 自定义连接器（取决于你的 Copilot Studio 配置）

| 参数 | 值 |
|---|---|
| Method | POST |
| URI | Copilot Studio Power Automate 触发 URL |
| Body | `outputs('Compose_payload')` |
| Headers | `Content-Type: application/json` |

> Copilot Studio 接收 payload 后，使用 `copilot-prompt-template.md` 中的 Prompt 生成 Markdown 报告并返回。

---

### Step 8 – 返回报告链接

**动作**: **Respond to a PowerApp or flow**（或 HTTP Response）

```json
{
  "status": "completed",
  "errorCount": @{variables('var_errorCount')},
  "warningCount": @{variables('var_warningCount')},
  "fullReportUrl": "@{outputs('Create_file')?['body/webUrl']}",
  "markdownReport": "@{body('Call_Copilot_Studio')}"
}
```

---

## 注意事项与最佳实践

1. **并发控制**：Apply to each 默认并发（Degree of Parallelism 可调为 1 串行，避免变量竞争）。若并发写 `var_issues`，需用串行模式。

2. **超时处理**：若 `tblOffshoring` 数据量超过 5000 行，`List rows present in a table` 会自动分页（返回 `@odata.nextLink`）；需要循环读取所有页。  
   配置：动作内勾选 "Pagination" 并设置阈值（如 100000）。

3. **Office Script 限制**：Office Script 仅在 Excel Online (Business) 中可用，且文件需在 OneDrive/SharePoint 上打开。POC 阶段可先用 Expression 替代简单场景的 Script。

4. **错误处理**：对可能抛出异常的 Expression（如 `float()`、`int()`），将动作的"Configure run after"设为"has failed"也触发，用 Condition 区分正常数据与异常数据。

5. **每月数据新增后**：确认数据追加在 `tblOffshoring` 表末尾，然后手动触发 Flow 一次，验证新月份行被正确读取。
