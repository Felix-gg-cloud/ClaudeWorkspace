# 03 — Power Automate 详细流程（范围读取方式）

> **读取方式**：使用 Office Scripts（脚本）读取指定范围（命名范围 `rngOffshoring` 或固定范围 `A4:P2000`），无需将数据区域转为 Excel Table，不影响现有 Power BI 连接。

---

## 流程总览

```
触发器（手动 / 定时）
  ↓
Step 1: 读取配置文件（Function 白名单、Team 白名单、Function-Team 映射）
  ↓
Step 2: 运行 Office Script，从 SharePoint Excel 读取数据范围
  ↓
Step 3: 解析返回的行数据，转换成结构化对象数组
  ↓
Step 4: 逐行执行规则校验，生成 issues[]
  ↓
Step 5: 生成聚合摘要（Error 数、Warning 数、Top 规则）
  ↓
Step 6: 把完整 issues[] 写入 SharePoint 清单 / Excel
  ↓
Step 7: 调用 Copilot（Power Virtual Agents / Copilot Studio）发布报告
```

---

## 前置准备

1. 已有 Microsoft 365 / Power Automate 账号
2. `headcount_analysis.xlsx` 存放在 SharePoint 上
3. 在 Excel 里已完成第 4 行列名设置（见 `docs/01-excel-range-setup.md`）
4. 已在 SharePoint 上建立一个"结果存储"列表或 Excel（用于保存 issues[]）
5. 准备好 `configs/` 目录下的三个 JSON 文件（也可放在 SharePoint 方便维护）

---

## Step 1：触发器

### 选项 A — 手动触发（POC 推荐）
- 在 Power Automate 里创建新 Flow → 选"手动触发流程（Manually trigger a flow）"
- 可以加一个"月份"输入参数（让用户指定校验哪个月，留空则校验全部）

### 选项 B — 定时触发（正式上线后）
- 触发器：Recurrence（重复）
- 频率：每月 1 号 08:00 自动运行
- 设置：Interval = 1，Frequency = Month，Start time = 当前月第 1 天 08:00

---

## Step 2：从 SharePoint 读取配置（白名单 & 映射）

**为什么要读配置文件**：把规则配置存在 JSON 文件里，不用改 Flow 代码就能更新白名单。

### 子步骤 2a：读取 Function 白名单
- 动作：**SharePoint → Get file content**
- Site Address：`https://yourcompany.sharepoint.com/sites/YourSite`
- File Identifier：`/Shared Documents/OffshoringChecker/configs/function-whitelist.json`
- 保存到变量：`functionWhitelist`（JSON 数组）

### 子步骤 2b：读取 Team 白名单
- 动作：**SharePoint → Get file content**
- File Identifier：`/Shared Documents/OffshoringChecker/configs/team-whitelist.json`
- 保存到变量：`teamWhitelist`（JSON 数组）

### 子步骤 2c：读取 Function-Team 映射
- 动作：**SharePoint → Get file content**
- File Identifier：`/Shared Documents/OffshoringChecker/configs/function-team-mapping.json`
- 保存到变量：`functionTeamMapping`（JSON 对象）

> **简化方案**：如果你暂时不想在 SharePoint 存 JSON 文件，可以直接在 Flow 的"初始化变量"步骤里把白名单数组硬编码进去（后期再迁移到文件）。

---

## Step 3：用 Office Script 读取 Excel 数据范围

这是关键步骤。Office Scripts 可以读取 Excel 里的任意范围，返回一个二维数组。

### 子步骤 3a：创建 Office Script

在 Excel Online 里（先打开 headcount_analysis.xlsx）：
1. 点击"自动化（Automate）"选项卡
2. 点"新建脚本（New Script）"
3. 把以下代码粘贴进去，保存为 `ReadOffshoringRange`

```typescript
function main(workbook: ExcelScript.Workbook): {
  headers: string[];
  rows: (string | number | boolean)[][];
  dataRowCount: number;
  overflowWarning: string | null;
} {
  const sheet = workbook.getWorksheet("Offshoring");
  if (!sheet) {
    throw new Error("Sheet 'Offshoring' not found");
  }

  // ==========================================
  // 方式 A：使用命名范围（推荐）
  // ==========================================
  // 如果你在 Excel 里定义了命名范围 rngOffshoring，使用以下方式：
  // const namedRange = workbook.getNamedItem("rngOffshoring");
  // const range = namedRange.getRange();

  // ==========================================
  // 方式 B：使用固定范围（备选）
  // ==========================================
  // 直接指定范围地址，第 4 行是表头，从 A4 开始
  const RANGE_ADDRESS = "A4:P2000"; // 根据你的实际列数调整（A-P = 16 列）
  const MAX_DATA_ROWS = 1996;        // 2000 - 4 + 1 - 1（去掉表头行）
  const range = sheet.getRange(RANGE_ADDRESS);

  const values = range.getValues();

  // 第一行是列名（表头，第 4 行）
  const headers = values[0].map(h => h.toString().trim());

  // 从第二行起是数据行，过滤掉空行（Cost Center Number 为空视为空行）
  const dataRows = values.slice(1).filter(row => {
    const firstCell = row[0];
    return firstCell !== null && firstCell !== "" && firstCell !== undefined;
  });

  // 溢出检测：数据行数超过最大行数的 80% 时发出警告
  const overflowWarning = dataRows.length > MAX_DATA_ROWS * 0.8
    ? `⚠️ 溢出警告：当前数据行数（${dataRows.length}）已超过读取范围上限（${MAX_DATA_ROWS} 行）的 80%。请尽快扩大 RANGE_ADDRESS 的行数上限，否则新增数据将无法被读取。`
    : null;

  return {
    headers,
    rows: dataRows,
    dataRowCount: dataRows.length,
    overflowWarning
  };
}
```

### 子步骤 3b：在 Power Automate 中调用 Office Script

- 动作：**Excel Online (Business) → Run script**
- Location：SharePoint
- Document Library：Shared Documents（你的文件所在库）
- File：`headcount_analysis.xlsx`（完整路径）
- Script：选择 `ReadOffshoringRange`

输出：Flow 会收到一个 `result` 对象，包含：
- `result.headers`：列名数组，如 `["Cost Center Number", "Function", "Team", ...]`
- `result.rows`：数据行二维数组
- `result.dataRowCount`：有效数据行数
- `result.overflowWarning`：溢出告警信息（字符串或 null）

### 子步骤 3c：处理溢出警告

在 Flow 里加一个"条件（Condition）"：
- 条件：`result.overflowWarning` is not equal to `null`
- 如果是：发送邮件给数据管理员（动作：Send an email (V2)），内容：`result.overflowWarning`

---

## Step 4：解析行数据，转换成结构化对象

Office Script 返回的是二维数组（行 × 列）。我们需要把每行转成对象。

### 方法：使用 Power Automate 的"选择（Select）"动作

- 动作：**数据操作 → 选择（Select）**
- 来源（From）：`result.rows`（上一步 Office Script 的输出）
- 映射（Map）：

  ```
  Cost Center Number:  item()[0]
  Function:            item()[1]
  Team:                item()[2]
  Owner:               item()[3]
  YearMonth:           item()[4]
  Actual_GBS_TeamMember:    item()[5]
  Actual_GBS_TeamLeaderAM:  item()[6]
  Actual_EA:                item()[7]
  Actual_HKT:               item()[8]
  Planned_GBS_TeamMember:   item()[9]
  Planned_GBS_TeamLeaderAM: item()[10]
  Planned_EA:               item()[11]
  Planned_HKT:              item()[12]
  Target_YearEnd:           item()[13]
  Target_2030YearEnd:       item()[14]
  Shoring_Ratio:            item()[15]
  ```

> 列索引（0-based）对应第 4 行的列顺序。如果你的 Excel 列顺序不同，请相应调整。

结果保存为变量 `structuredRows`（对象数组）。

---

## Step 5：逐行执行规则校验

### 方法：对每行调用一个子流程（Child Flow）或者使用 Apply to each

**推荐（对 Automate 不熟悉的用户）**：使用 Apply to each + 条件判断

1. 初始化变量：`issues`（数组类型，初始值 `[]`）

2. 动作：**Apply to each**
   - 输入：`structuredRows`
   - 在每次循环中执行以下检查：

#### 检查 R-CCN-001（Cost Center Number 格式）

```
条件：
  AND(
    not(empty(items('Apply_to_each')?['Cost Center Number'])),
    not(equals(length(string(items('Apply_to_each')?['Cost Center Number'])), 7))
  )
如果是：
  Append to array variable: issues
  值：{
    "severity": "Error",
    "ruleId": "R-CCN-001",
    "yearMonth": "@{items('Apply_to_each')?['YearMonth']}",
    "costCenterNumber": "@{items('Apply_to_each')?['Cost Center Number']}",
    "function": "@{items('Apply_to_each')?['Function']}",
    "team": "@{items('Apply_to_each')?['Team']}",
    "column": "Cost Center Number",
    "value": "@{items('Apply_to_each')?['Cost Center Number']}",
    "message": "Cost Center Number 必须为 7 位纯数字",
    "suggestion": "请检查是否有多余字符或位数不足"
  }
```

#### 检查 R-FUNC-001（Function 白名单）

```
条件：
  not(contains(variables('functionWhitelist'), items('Apply_to_each')?['Function']))
如果是：
  Append to array variable: issues
  值：{
    "severity": "Error",
    "ruleId": "R-FUNC-001",
    ...
    "message": "Function 值不在允许白名单中：@{items('Apply_to_each')?['Function']}",
    "suggestion": "请从 function-whitelist.json 中选择正确的 Function"
  }
```

#### 检查 R-TEAM-001（Team 白名单）

```
条件：
  not(contains(variables('teamWhitelist'), items('Apply_to_each')?['Team']))
如果是：
  Append to array variable: issues
  值：{ "severity": "Warning", "ruleId": "R-TEAM-001", ... }
```

#### 检查 R-OWNER-001（Owner 非空）

```
条件：
  empty(items('Apply_to_each')?['Owner'])
如果是：
  Append to array variable: issues
  值：{ "severity": "Error", "ruleId": "R-OWNER-001", ... }
```

#### 检查 R-YM-001（YearMonth 格式）

```
条件：
  OR(
    not(equals(length(string(items('Apply_to_each')?['YearMonth'])), 6)),
    less(int(substring(string(items('Apply_to_each')?['YearMonth']), 4, 2)), 1),
    greater(int(substring(string(items('Apply_to_each')?['YearMonth']), 4, 2)), 12)
  )
如果是：
  Append to array variable: issues
  值：{ "severity": "Error", "ruleId": "R-YM-001", ... }
```

#### 检查 R-MAP-001（Function-Team 映射）

```
条件（表达式）：
  not(contains(variables('functionTeamMapping')[items('Apply_to_each')?['Function']], items('Apply_to_each')?['Team']))
如果是：
  Append to array variable: issues
  值：{ "severity": "Error", "ruleId": "R-MAP-001", ... }
```

> **注意**：Total (All) 和 Total (Core Operations) 的 Team 映射为空数组，在 mapping 里对应 `[]`，条件中需要额外处理：
> ```
> 如果 Function 不是 "Total (All)" 且不是 "Total (Core Operations)"：
>   再做 R-MAP-001 检查
> ```

---

## Step 6：生成聚合摘要

在所有行处理完成后，统计 issues 数组：

```
errorCount   = length(filter(variables('issues'), item()?['severity'] == 'Error'))
warningCount = length(filter(variables('issues'), item()?['severity'] == 'Warning'))
```

> Power Automate 的 filter 表达式：
> ```
> @{length(filter(variables('issues'), equals(item()?['severity'], 'Error')))}
> ```

生成 `summary` 对象：
```json
{
  "checkFile": "headcount_analysis.xlsx",
  "checkedAt": "@{utcNow()}",
  "dataRowCount": "@{result.dataRowCount}",
  "errorCount": "@{errorCount}",
  "warningCount": "@{warningCount}",
  "overflowWarning": "@{result.overflowWarning}"
}
```

---

## Step 7：把完整 issues[] 写入 SharePoint

**选项 A：写入 SharePoint 列表**
- 对每个 issue，动作：**SharePoint → Create item**（在预先建好的列表里）
- 字段一一对应 issue 对象的字段

**选项 B：写入 SharePoint Excel**
- 使用 Office Script 把 issues 数组写回一个"结果记录"Excel（每次运行追加）

**选项 C：发送到 OneDrive 生成报告 Excel（最简化）**
- 动作：**OneDrive for Business → Create file**，把 issues 数组序列化为 CSV 保存

---

## Step 8：调用 Copilot Studio 生成自然语言报告

- 动作：**HTTP** 或 **Power Virtual Agents → Send a message**（取决于你的 Copilot Studio 集成方式）
- 发送的内容：`summary` + `issues[]`（Top 50，按 severity 排序）
- 见 `docs/04-copilot-prompt-template.md` 了解 Prompt 格式

---

## 完整 Flow 结构参考

```
[Trigger] 手动触发
  ↓
[初始化变量] issues = []
  ↓
[HTTP / SharePoint Get File] 读取 function-whitelist.json → functionWhitelist
[HTTP / SharePoint Get File] 读取 team-whitelist.json    → teamWhitelist
[HTTP / SharePoint Get File] 读取 function-team-mapping.json → functionTeamMapping
  ↓
[Excel Online - Run Script] ReadOffshoringRange → result
  ↓
[Condition] result.overflowWarning != null
  → 是：发邮件告警
  ↓
[Select] result.rows → structuredRows（结构化对象数组）
  ↓
[Apply to each] structuredRows
  → [Condition] R-CCN-001 → Append to issues
  → [Condition] R-FUNC-001 → Append to issues
  → [Condition] R-TEAM-001 → Append to issues
  → [Condition] R-OWNER-001 → Append to issues
  → [Condition] R-YM-001 → Append to issues
  → [Condition] R-NUM-001 (x10 columns) → Append to issues
  → [Condition] R-NUM-002 (x10 columns) → Append to issues
  → [Condition] R-SR-001 → Append to issues
  → [Condition] R-MAP-001 → Append to issues
  ↓
[统计] errorCount, warningCount
  ↓
[Write to SharePoint] issues[]
  ↓
[Copilot Studio / Send message] summary + topIssues[]
```

---

## 常见问题

**Q：Apply to each 太慢，几百行数据要等很久怎么办？**  
A：可以把规则校验逻辑全部放进 Office Script，在脚本里完成，把 issues 数组直接从脚本返回给 Flow。这样 Flow 里就不需要 Apply to each，速度大幅提升。POC 阶段用 Apply to each 先跑通逻辑，后期优化移入 Office Script 即可。

**Q：Office Script 不可用怎么办？**  
A：需要 Microsoft 365 E3/E5 或含 Office Scripts 的许可证。如果没有，可以改用 Azure Function 或 Power Automate 的 HTTP 动作调用一个自定义 API 来完成读取与校验。

**Q：每月追加新数据后，Flow 会自动读到吗？**  
A：会。只要新数据追加在命名范围内（或固定范围内的行内），Office Script 的过滤空行逻辑会自动纳入新行。如果行数超过固定范围上限，溢出检测会提前告警。
