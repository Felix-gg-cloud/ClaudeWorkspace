# Power Automate Flow — POC Headcount Validator（逐步搭建说明）

> **数据源**：SharePoint 上的 `headcount_analysis_poc.xlsx`，Sheet: `Offshoring`，Table 名: `tblOffshoring`
> **触发方式**：手动触发（Instant cloud flow），POC 验证完毕后可改为定时触发

---

## 全流程概览

```
[手动触发]
    → Step 1: 读取 tblOffshoring 所有行
    → Step 2: 初始化变量
    → Step 3: 遍历每一行，逐条运行规则
        → R-CC-001: Cost Center Number
        → R-YM-001: YearMonth
        → R-FN-001: Function 白名单
        → R-TM-001: Team 白名单
        → R-OW-001: Owner 非空
        → R-SR-001: ShoringRatio ← 百分比格式硬校验
        → R-NV-001: 数值列非负
        → R-FT-001: Function-Team 组合
    → Step 4: 聚合统计（Errors/Warnings/Top Rules）
    → Step 5: 截取 Top 50 问题明细（Error 优先）
    → Step 6: 调用 Copilot Studio 生成 Markdown 报告
    → Step 7: 写回 SharePoint（完整问题清单 CSV + 报告 MD）
```

---

## Step 1 · 读取 tblOffshoring

**Action**: Excel Online (Business) → **List rows present in a table**

| 参数 | 值 |
|------|-----|
| Location | SharePoint 站点（选择你的站点） |
| Document Library | 文档库名称 |
| File | `headcount_analysis_poc.xlsx` |
| Table | `tblOffshoring` |

返回值：`body/value` 数组，每个元素是一行数据，字段名即第 4 行列名。

---

## Step 2 · 初始化变量

新增以下变量（Add a variable action，逐个添加）：

| 变量名 | 类型 | 初始值 |
|--------|------|--------|
| `varIssues` | Array | `[]` |
| `varErrorCount` | Integer | `0` |
| `varWarningCount` | Integer | `0` |
| `varRowCount` | Integer | `0` |

---

## Step 3 · 遍历每一行（Apply to each）

**Action**: Control → **Apply to each**
**Input**: `outputs('List_rows_present_in_a_table')?['body/value']`

在 Apply to each 内部，依次添加以下 Condition 检查：

---

### 3.0 跳过空行

**Condition**: `items('Apply_to_each')?['YearMonth']` 等于（is equal to）空字符串 `""`

- **如果是**（true 分支）：`Continue`（跳过该行，不校验）
- **如果否**（false 分支）：继续后续规则

将 `varRowCount` 递增 1：
```
add(variables('varRowCount'), 1)
```

---

### 3.1 · R-CC-001: Cost Center Number（7 位纯数字）

**表达式**（Condition）：

```
not(match(trim(string(items('Apply_to_each')?['Cost Center Number'])), '^\d{7}$'))
```

> Power Automate 使用 `match()` 函数进行正则匹配，返回 true/false。

如果条件成立（格式不合规）：

**Append to array variable** → `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-CC-001",
  "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
  "CostCenter": "@{items('Apply_to_each')?['Cost Center Number']}",
  "Column": "Cost Center Number",
  "Value": "@{items('Apply_to_each')?['Cost Center Number']}",
  "Message": "Cost Center Number 必须为 7 位纯数字，例如 1234567",
  "FixSuggestion": "请检查 Cost Center Number 是否为 7 位纯数字，无空格与特殊字符"
}
```

同时将 `varErrorCount` 递增 1。

---

### 3.2 · R-YM-001: YearMonth（YYYYMM 格式）

**表达式**（Condition）：

```
not(match(trim(string(items('Apply_to_each')?['YearMonth'])), '^20[0-9]{2}(0[1-9]|1[0-2])$'))
```

如果条件成立，追加到 `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-YM-001",
  "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
  "Column": "YearMonth",
  "Value": "@{items('Apply_to_each')?['YearMonth']}",
  "Message": "YearMonth 必须为 YYYYMM 格式，例如 202501",
  "FixSuggestion": "请将 YearMonth 改为 6 位数字，格式 YYYYMM，月份范围 01~12"
}
```

---

### 3.3 · R-OW-001: Owner 非空

**表达式**（Condition）：

```
empty(trim(string(items('Apply_to_each')?['Owner'])))
```

如果条件成立，追加到 `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-OW-001",
  "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
  "Column": "Owner",
  "Value": "",
  "Message": "Owner 不能为空",
  "FixSuggestion": "请填写 Owner 姓名或工号"
}
```

---

### 3.4 · R-SR-001: ShoringRatio（百分比格式 ← **硬规则**）

**规则**：`ShoringRatio` 列必须为整数 0–100 后跟 `%`，例如 `0%`、`23%`、`100%`。  
不允许 `0.23`（小数比率）、`23`（纯数字无 `%`）或其他任何格式。

**验证逻辑**：

首先对列值做 trim，然后用正则 `^(100|[1-9][0-9]|[0-9])%$` 匹配。

**表达式**（Condition，以下两种写法等价，选一种即可）：

**写法 A（推荐，使用 match 函数）**：
```
not(match(trim(string(items('Apply_to_each')?['ShoringRatio'])), '^(100|[1-9][0-9]|[0-9])%$'))
```

**写法 B（手动分步，适合不支持 match 的环境）**：
1. `Compose`（命名为 `trimmedShoringRatio`）：  
   ```
   trim(string(items('Apply_to_each')?['ShoringRatio']))
   ```
2. Condition 1：检查末尾是否为 `%`  
   ```
   not(endsWith(outputs('trimmedShoringRatio'), '%'))
   ```
3. Condition 2（在 Condition 1 为 false 时继续）：提取 `%` 前的部分，检查是否为 0–100 的整数  
   ```
   @{
     or(
       not(equals(string(int(substring(outputs('trimmedShoringRatio'), 0, sub(length(outputs('trimmedShoringRatio')), 1)))), substring(outputs('trimmedShoringRatio'), 0, sub(length(outputs('trimmedShoringRatio')), 1)))),
       greater(int(substring(outputs('trimmedShoringRatio'), 0, sub(length(outputs('trimmedShoringRatio')), 1))), 100),
       less(int(substring(outputs('trimmedShoringRatio'), 0, sub(length(outputs('trimmedShoringRatio')), 1))), 0)
     )
   }
   ```

**推荐使用写法 A**（match 函数在 Power Automate 标准环境中可用）。

如果条件成立（格式不合规），追加到 `varIssues`：

```json
{
  "Severity": "Error",
  "RuleId": "R-SR-001",
  "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
  "CostCenter": "@{items('Apply_to_each')?['Cost Center Number']}",
  "Column": "ShoringRatio",
  "Value": "@{items('Apply_to_each')?['ShoringRatio']}",
  "Message": "ShoringRatio 必须使用百分比格式，例如 23%（0%~100%）",
  "FixSuggestion": "请将 ShoringRatio 改为整数加 % 的格式，例如将 \"0.23\" 改为 \"23%\"，将 \"23\" 改为 \"23%\"。有效范围为 0%～100%。"
}
```

同时将 `varErrorCount` 递增 1。

> **注意**：R-SR-001 是 Hard Rule（Error 级别）。以下均为不合规示例：
> - `0.23` → 应改为 `23%`
> - `23` → 应改为 `23%`
> - `23.5%` → 非整数，不允许
> - `101%` → 超出范围，不允许
> - 空值 → 视同格式错误

---

### 3.5 · R-NV-001: 数值列非负（Soft）

对以下各列逐一检查（可复制相同逻辑）：  
`Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`,  
`Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`,  
`Target_YearEnd`, `Target_2030YearEnd`

**表达式**（以 `Actual_GBS_TeamMember` 为例）：

```
or(
  not(isFloat(items('Apply_to_each')?['Actual_GBS_TeamMember'])),
  less(float(items('Apply_to_each')?['Actual_GBS_TeamMember']), 0)
)
```

如果条件成立，追加到 `varIssues`（Severity: Warning）：

```json
{
  "Severity": "Warning",
  "RuleId": "R-NV-001",
  "YearMonth": "@{items('Apply_to_each')?['YearMonth']}",
  "Column": "Actual_GBS_TeamMember",
  "Value": "@{items('Apply_to_each')?['Actual_GBS_TeamMember']}",
  "Message": "Actual_GBS_TeamMember 必须为非负数字",
  "FixSuggestion": "请检查该列是否含有负数或非数字字符"
}
```

将 `varWarningCount` 递增 1。

---

## Step 4 · 聚合统计

使用 `Compose` 计算摘要：

```json
{
  "TotalRows": "@{variables('varRowCount')}",
  "ErrorCount": "@{variables('varErrorCount')}",
  "WarningCount": "@{variables('varWarningCount')}",
  "IssueCount": "@{length(variables('varIssues'))}"
}
```

---

## Step 5 · 截取 Top 50 问题

**Action**: `Select` + `Filter array`（先筛 Error，再 append Warning，合并取前 50）

```
first(skip(sort(variables('varIssues'), 'Severity'), 0), 50)
```

或使用简化写法（POC 阶段直接取 varIssues 前 50 条即可）：

```
take(variables('varIssues'), 50)
```

---

## Step 6 · 调用 Copilot Studio 生成报告

将 Step 4 的摘要 + Step 5 的 Top 50 问题明细传入 Copilot Studio Agent（或 HTTP action）：

**输入 body（JSON）**：

```json
{
  "summary": "@{outputs('Compose_Summary')}",
  "topIssues": "@{outputs('Take_Top50')}"
}
```

Copilot Studio 收到后按 `prompt.md` 中的模板生成 Markdown 报告。

---

## Step 7 · 写回 SharePoint

**Action**: SharePoint → **Create file**

| 参数 | 值 |
|------|-----|
| Site Address | 你的 SharePoint 站点 |
| Folder Path | `/POC-Reports/` |
| File Name | `report_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmmss')}.md` |
| File Content | Copilot Studio 返回的 Markdown 文本 |

---

## 附：完整 issues 条目字段说明

| 字段 | 含义 |
|------|------|
| Severity | `Error` / `Warning` |
| RuleId | 规则 ID，如 `R-SR-001` |
| YearMonth | 所在行的 YearMonth 值 |
| CostCenter | 所在行的 Cost Center Number |
| Column | 问题列名 |
| Value | 问题列的原始值 |
| Message | 错误消息（中文） |
| FixSuggestion | 修正建议 |
