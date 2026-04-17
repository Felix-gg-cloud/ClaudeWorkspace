# 02 — Power Automate 校验流程（Table 版）

> **数据入口**：Excel Online (Business) → `List rows present in a table` → `tblOffshoring`

---

## 前置准备

| 项目 | 确认内容 |
|---|---|
| Excel 文件 | 已按 `01-excel-table-setup.md` 创建 `tblOffshoring` 表 |
| SharePoint 位置 | 记录文件所在的站点、文档库、文件路径 |
| 账号权限 | Power Automate 账号对该 SharePoint 文档库有读写权限 |
| 连接器 | Excel Online (Business) 连接器可用 |

---

## 整体流程图

```
[手动触发 / 定时触发]
        ↓
[Step 1] 读取 tblOffshoring 所有行
        ↓
[Step 2] 过滤空行（YearMonth 为空则跳过）
        ↓
[Step 3] 逐行执行校验规则，生成 issues 数组
        ↓
[Step 4] 聚合统计（按 Severity、RuleId 汇总）
        ↓
[Step 5] 取 Top 50 问题 → 发给 Copilot Studio 生成报告
        ↓
[Step 6] 把完整 issues 写回 SharePoint（CSV/Excel）
        ↓
[Step 7] 发送邮件或 Teams 消息（含报告摘要）
```

---

## Step 1：新建 Cloud Flow

1. 打开 **Power Automate**（`flow.microsoft.com`）
2. 左侧菜单：**创建 → 立即云端流（Instant cloud flow）**
3. 命名：`Headcount Offshoring Validator - POC`
4. 触发器选择：**手动触发流（Manually trigger a flow）**  
   → 后续稳定后可改为 **定期（Recurrence）**，例如每月 1 日凌晨
5. 点击 **创建**

---

## Step 2：添加"列出表中的行"动作（读取 tblOffshoring）

1. 点击 **"+ 新建步骤"**
2. 搜索：**Excel Online (Business)**，选择此连接器
3. 在动作列表中选择：**列出表中的行（List rows present in a table）**
4. 填写参数：

   | 参数 | 填写内容 |
   |---|---|
   | **位置（Location）** | 选择文件所在的 SharePoint 站点（例如 `SharePoint Site - POC`） |
   | **文档库（Document Library）** | 选择对应的文档库（例如 `Documents`） |
   | **文件（File）** | 通过文件选择器找到 `headcount_analysis_poc.xlsx` |
   | **表（Table）** | 选择 `tblOffshoring`（下拉会自动列出文件中的所有 Table） |

5. **展开"显示高级选项"**，配置分页/阈值：

   | 高级参数 | 建议配置 |
   |---|---|
   | **筛选器查询（Filter Query）** | 留空（读取所有行，流程内部过滤） |
   | **排序依据（Order By）** | 留空 |
   | **顶部计数（Top Count）** | 留空（读取全部行） |

   > **⚠️ 分页与阈值说明**：Power Automate 默认每次最多返回 256 行。如果数据行超过 256 行，必须开启分页：
   > - 点击该步骤右上角的 **"..."（更多选项）→ 设置（Settings）**
   > - 开启 **"分页（Pagination）"**，将"阈值"设为 `100000`（或你的预期最大行数）
   > - 这样 Flow 会自动循环获取所有行，不会截断

---

## Step 3：初始化变量

在"列出表中的行"动作之后，添加以下变量初始化步骤（每个变量单独一个"初始化变量"动作）：

| 变量名 | 类型 | 初始值 | 用途 |
|---|---|---|---|
| `varIssues` | 数组（Array） | `[]` | 存储所有校验问题 |
| `varTotalRows` | 整数（Integer） | `0` | 统计非空行总数 |
| `varErrorCount` | 整数（Integer） | `0` | 错误数量 |
| `varWarningCount` | 整数（Integer） | `0` | 警告数量 |

**操作方法**：
1. 点击 **"+ 新建步骤" → 搜索"变量" → 初始化变量**
2. 名称：`varIssues`，类型选 `数组`，值填 `[]`
3. 重复上述步骤创建其余三个变量

---

## Step 4：循环处理每一行（Apply to each）

1. 点击 **"+ 新建步骤" → 搜索"Apply to each"（应用到每一项）**
2. **"从上一步选择输出"**：点击输入框，从动态内容选择 **`value`**（即"列出表中的行"的输出）

在 Apply to each 循环内部，添加以下子步骤：

---

### Step 4.1：过滤空行

1. 在循环内添加 **"条件（Condition）"** 动作
2. 条件：`items('Apply_to_each')['YearMonth']` **不等于** （空字符串 `""`）  
   即：只有 `YearMonth` 不为空时才继续处理
3. **"是（Yes）分支"**：后续校验步骤放在这里
4. **"否（No）分支"**：留空（跳过该行）

---

### Step 4.2：递增行计数

在"是"分支中，添加 **"递增变量（Increment variable）"** 动作：
- 名称：`varTotalRows`，值：`1`

---

### Step 4.3：逐条执行校验规则

对每一行，依次检查以下规则（每条规则用"条件"动作实现，命中则追加一条 issue 到 `varIssues`）：

> **追加 issue 的方法**：使用 **"追加到数组变量（Append to array variable）"** 动作，变量选 `varIssues`，值为以下 JSON 对象（根据规则填入对应字段）：

```json
{
  "Severity": "Error",
  "RuleId": "R001",
  "YearMonth": "<当前行 YearMonth 的动态内容>",
  "Cost Center Number": "<当前行 Cost Center Number 的动态内容>",
  "Function": "<当前行 Function 的动态内容>",
  "Team": "<当前行 Team 的动态内容>",
  "Column": "Cost Center Number",
  "Value": "<当前行 Cost Center Number 的动态内容>",
  "Message": "Cost Center Number 必须为 7 位纯数字",
  "FixSuggestion": "检查成本中心编号是否填写正确"
}
```

**规则列表（按优先级排序）：**

| 规则 ID | 级别 | 校验列 | 条件（Power Automate 表达式） | 错误消息 |
|---|---|---|---|---|
| R001 | Error | `Cost Center Number` | `not(and(greater(length(string(items('Apply_to_each')['Cost Center Number'])), 6), less(length(string(items('Apply_to_each')['Cost Center Number'])), 8)))` | Cost Center Number 必须为 7 位纯数字 |
| R002 | Error | `YearMonth` | 值长度 ≠ 6，或月份（后两位）不在 01-12 | YearMonth 格式必须为 YYYYMM，月份在 01-12 |
| R003 | Error | `Function` | 值不在 Function 白名单中 | Function 值不在允许列表，请检查 |
| R004 | Error | `Team` | 值不在 Team 白名单中 | Team 值不在允许列表，请检查 |
| R005 | Error | `Owner` | 值为空 | Owner 不得为空 |
| R006 | Warning | `Shoring Ratio` | 值无法转为数字，或转换后不在 0-100 范围 | Shoring Ratio 应为 0-100 的数字（或百分比） |
| R007 | Error | `Headcount` | 值无法转为数字，或转换后 < 0 | Headcount 必须为非负数字 |
| R008 | Error | `Offshore HC` | 值无法转为数字，或转换后 < 0 | Offshore HC 必须为非负数字 |
| R009 | Warning | `Function` + `Team` | Function-Team 组合不在映射表中 | Function 与 Team 的组合不合法，请参考映射表 |

> 规则详情请参考 `rules/rule-catalog.md`，白名单参考 `rules/function-whitelist.json`、`rules/team-whitelist.json`，组合映射参考 `rules/function-team-mapping.json`。

---

### Step 4.4：命中规则时递增计数器

当规则命中（追加 issue 之后）：
- Severity = Error → **递增** `varErrorCount`，值 `1`
- Severity = Warning → **递增** `varWarningCount`，值 `1`

---

## Step 5：聚合统计

循环结束后，构建统计摘要。添加 **"撰写（Compose）"** 动作，名称为 `summaryObject`，内容如下：

```json
{
  "TotalRows": @{variables('varTotalRows')},
  "ErrorCount": @{variables('varErrorCount')},
  "WarningCount": @{variables('varWarningCount')},
  "IssueCount": @{length(variables('varIssues'))}
}
```

---

## Step 6：取 Top 50 问题并调用 Copilot Studio

1. 添加 **"撰写（Compose）"** 动作，取前 50 条 issue：
   ```
   take(variables('varIssues'), 50)
   ```
2. 添加 **"HTTP"** 动作或 **"Copilot Studio"** 连接器，将以下内容发送给 Copilot：
   - 摘要：`outputs('summaryObject')`
   - Top50 问题：`outputs('Compose_top50')`

---

## Step 7：把完整 issues 写回 SharePoint

1. 添加 **"创建 CSV 表（Create CSV table）"** 动作：
   - 从：`variables('varIssues')`
2. 添加 **"创建文件（Create file）"** 或 **"更新文件内容（Update file content）"** 动作：
   - 位置：SharePoint POC 文件夹
   - 文件名：`headcount_issues_@{formatDateTime(utcNow(), 'yyyyMMdd')}.csv`
   - 内容：上一步 CSV 的输出

---

## Step 8：发送通知

添加 **"发送电子邮件（Send an email V2）"** 或 **"Teams - 发布消息"** 动作：

**邮件主题**：`[Headcount POC] Offshoring 数据校验完成 - @{formatDateTime(utcNow(), 'yyyy-MM-dd')}`

**邮件正文示例**：
```
本次校验完成：
- 总数据行：@{variables('varTotalRows')}
- 错误数量：@{variables('varErrorCount')}
- 警告数量：@{variables('varWarningCount')}

详细问题列表已写入 SharePoint：
headcount_issues_@{formatDateTime(utcNow(), 'yyyyMMdd')}.csv

如需查看完整报告，请登录 Copilot Studio 查看生成内容。
```

---

## 分页与大数据量注意事项

| 数据规模 | 建议配置 |
|---|---|
| ≤ 256 行 | 默认即可，无需开启分页 |
| 257 ~ 5,000 行 | 开启分页，阈值设为 `5000` |
| > 5,000 行 | 开启分页，阈值设为 `100000`；同时建议在 Flow 外部分批处理或用 Office Script 预处理 |
| > 50,000 行 | 考虑改用 Dataflow 或 Azure Data Factory 处理，Power Automate 不适合超大量数据校验 |

> **如何开启分页**：在"列出表中的行"步骤右上角 → `...` → 设置 → 开启分页 → 填入阈值数值。

---

## 测试与调试

1. **先用小数据集测试**：在 POC 副本文件的 Table 中只保留 10~20 行数据，手动触发 Flow，确认能正常读取并输出 issues。
2. **检查动态内容**：Flow 运行后点击每个步骤查看输入/输出，确认 `List rows present in a table` 步骤的 `value` 中包含正确的行数据。
3. **逐步启用规则**：先只启用 R001、R002，验证通过后再启用其他规则，避免一次性调试多条规则。
4. **分页验证**：先用 5 行数据，再用 300 行数据（超过 256 行默认限制），确认分页后数据行数一致。
