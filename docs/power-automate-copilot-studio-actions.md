# Power Automate × Copilot Studio：动作选择指南

> 适用场景：在 Power Automate Cloud Flow 里调用 Copilot Studio Agent 进行**同步数据校验**（例如 POC-Validator 场景）。

---

## 一、Copilot Studio 动作一览

在 Power Automate 的 **New step** 里搜索 `Copilot Studio`，会看到以下动作：

| 动作名称 | 执行方式 | 会等 Agent 回复？ | 适用场景 |
|---|---|---|---|
| **Evaluate Agent** | 异步（后台） | ❌ 不等待 | 批量触发、不关心结果、后台任务 |
| **Execute Agent** | 异步（后台） | ❌ 不等待 | 与 Evaluate Agent 类似，fire-and-forget |
| **Execute Agent and wait** | **同步** | ✅ **等待并拿回输出** | **校验、审批、需要 AI 回复才能继续的流程** |
| Get Agent Test Run Details | 查询 | — | 查询测试运行结果（配合 Test Set 使用） |
| Get Agent Test Runs | 查询 | — | 列出测试运行列表 |
| Get Agent Test Set Details | 查询 | — | 查询测试集详情 |
| Get Agent Test Sets | 查询 | — | 列出测试集列表 |

### 结论：POC-Validator 场景请使用 **Execute Agent and wait**

原因：
- 你需要拿到 Agent 返回的校验 JSON 后，才能继续写 Excel 报告、生成 PDF、发 Teams 通知。
- `Execute Agent and wait` 会让 Flow **暂停等待**，直到 Agent 运行完成并返回结果。
- 其他"Evaluate / Execute"是 fire-and-forget——Flow 不会等，你拿不到输出。

---

## 二、Execute Agent and wait — 逐步配置

### 2.1 前提条件

- 已在 **Microsoft Copilot Studio** 发布了你的 Agent（必须 Published，Draft 状态无法被 Power Automate 调用）。
- 你的 Power Automate 和 Copilot Studio 在同一个 M365 租户。
- 已完成前置步骤：读取 Excel（`List rows present in a table`）→ 初始化变量 `RowsForAI`（Array）→ 在 Apply to each 中追加行。

---

### 2.2 第一步：添加动作

在 `Compose – RowsForAI`（或你的循环结束之后）点 **+ New step**：

1. 搜索框输入：`Execute Agent`
2. 在结果列表里选：**Copilot Studio – Execute Agent and wait**

---

### 2.3 第二步：配置 Execute Agent and wait 的字段

| 字段 | 填写内容 | 说明 |
|---|---|---|
| **Environment** | 从下拉选择你的 Power Platform 环境（通常是你们公司的默认环境） | 如果没有特殊多环境，选 Default |
| **Agent** | 从下拉选择你的 Copilot Studio Agent 名称 | 必须是已发布状态 |
| **Text** | 把 RowsForAI 转成字符串（见 2.4） | Agent 的主输入 |

> 注意：有些租户版本会显示 **Message** 而不是 **Text**，含义一样。

---

### 2.4 第三步：构造发送给 Agent 的输入文本

在 `Execute Agent and wait` **之前**加一个 **Compose** 动作，用于拼 JSON 字符串：

**动作名：** `Compose – AI_Input`

**Inputs（点 Expression 标签输入表达式）：**

```
string(variables('RowsForAI'))
```

这会把你的数组变量转成 JSON 字符串，Agent 可以直接解析。

如果你想带上批次信息，也可以用：

```
string(createObject(
  'batchYearMonth', triggerBody()?['text_1'],  // text_1 = BatchYearMonth 的内部字段名
  'rows', variables('RowsForAI')
))
```

> 其中 `text_1` 是手动触发器里 `BatchYearMonth` 输入的内部名称（Power Automate 按添加顺序自动命名为 `text_1`、`number_1` 等；如果不确定，在动态内容列表里选那个字段让系统自动填入即可）。

然后在 `Execute Agent and wait` 的 **Text** 字段里，选择上面 `Compose – AI_Input` 的**输出（Outputs）**。

---

### 2.5 第四步：提取 Agent 输出

`Execute Agent and wait` 执行完成后，它的输出里有几个关键字段：

| 输出字段 | 含义 |
|---|---|
| **Text** | Agent 回复的完整文本（这就是你需要的 JSON 字符串） |
| **Status** | 运行状态（`Success` / `Failed` 等） |
| **Session Id** | 本次会话 ID |

在 `Execute Agent and wait` 后面加 **Parse JSON** 动作：

- **Content**：在动态内容里选 `Execute Agent and wait` 的 **Text**
- **Schema**：点 **Generate from sample**，粘贴你 Agent 返回的示例 JSON（见下方）

**示例 Schema 样本（粘贴后点 Done 自动生成 Schema）：**

```json
{
  "report_model": {
    "rows_total": 10,
    "errors_total": 2,
    "warnings_total": 1
  },
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "R-WS-ALL-001",
      "RowIndex": 3,
      "Column": "Function",
      "RawValue": "IT ",
      "Message": "Trailing whitespace detected",
      "FixSuggestion": "Trim leading/trailing whitespace"
    }
  ]
}
```

---

### 2.6 预期输入/输出总结

```
输入（发给 Agent）：
  JSON 字符串，包含校验批次信息和行数组
  例：'{"batchYearMonth":"202603","rows":[{"RowIndex":1,"YearMonth":"202603",...},...]}'

Agent 处理：
  按规则逐行校验，输出结构化 issues 列表

输出（从 Agent 拿回）：
  Execute Agent and wait → Text：
  '{"report_model":{"rows_total":100,"errors_total":3},"issues":[...]}'
```

---

## 三、Apply to each 循环常见问题排查

### 问题 1：RowIndex 始终是 1

**原因 A：`Increment variable` 不在循环内部**

检查：`Increment variable RowIndex` 必须在 `Apply to each` **里面**（缩进显示在循环框内）。

修复：把 `Increment variable` 拖进 Apply to each 框内，放在 `Append to array variable` 之前。

**原因 B：循环实际只跑了 1 次（数据只有 1 行）**

检查：打开 Run history → 展开 Apply to each → 看 Iterations 计数。

修复：检查 Excel 的 tblOffshoring 表是否真的有多行数据；确认 `List rows present in a table` 的 Table 选的是正确表名。

**原因 C：循环名称与 `items()` 里的名称不一致**

检查：你的对象里写了 `items('Apply_to_each')?['YearMonth']`，但循环动作实际名为 `Apply to each 2`。Power Automate 会把动作名中的**空格自动转为下划线**，所以 `Apply to each 2` 在表达式里需要写成 `Apply_to_each_2`。

修复：在动态内容里重新插入 `items()`，让系统自动填正确名称；或者在循环动作卡片上点 `…` → Rename，改成不含空格的名字（如 `Apply_to_each_rows`），之后表达式就用 `items('Apply_to_each_rows')`。

---

### 问题 2：Compose 没有输出（或输出为空数组）

**原因 A：Compose 放在循环内部**

修复：把 `Compose – RowsForAI` 拖到 Apply to each **外面**（和循环同级，在循环下方）。

**原因 B：前面某步骤失败，Flow 提前终止**

检查：Run history 里看所有步骤颜色——红色 = Failed，灰色 = Skipped。找到第一个红色，修复它。

**原因 C：`Append to array variable` 的 Value 写错**

正确写法（Object，不是 Array）：

```
{
  "RowIndex": variables('RowIndex'),
  "YearMonth": items('Apply_to_each')?['YearMonth'],
  "Function": items('Apply_to_each')?['Function']
}
```

错误写法（Array——会报错）：

```
[{"RowIndex": 1}]
```

错误写法（空对象——不报错但无用）：

```
{}
```

---

### 问题 3：循环看似跑不完

**快速排查步骤：**

1. 在 `List rows present in a table` 里找 `Top Count`，先填 `20`，跑一次小批量确认逻辑正确。
2. 打开 Apply to each 的 Settings → **Concurrency control**，把 Degree 设为 `1`（关闭并发），避免 Excel/SharePoint 限流。
3. 在 Apply to each 之前加一个 `Compose`，输入选 List rows 的 `value`，看数组有多少条——如果有几千条，循环确实要跑很久。
4. 如果要取消当前运行：在 **Run history** 里打开那次运行，右上角点 **Cancel run**。

---

### 问题 4：tblOffshoring 表在 Power Automate 下拉里找不到

**原因：** Excel 区域没有被设置成"Table（表）"，或表名没有正确保存。

**修复（在 Excel 里一次性设置）：**

1. 打开 Excel 文件（SharePoint 上直接点击即可用网页版）
2. 选中从第 5 行（表头行）开始的所有数据区域（`A5` 到最后一列最后一行）
3. **Insert → Table**，勾选 **My table has headers**，点 **OK**
4. 点表格内任意格 → 顶部出现 **Table Design** → 左上角 **Table Name** 改为 `tblOffshoring`，回车
5. 保存文件
6. 回 Power Automate，在 `List rows present in a table` 的 Table 字段里刷新下拉，应该能看到 `tblOffshoring`

---

## 四、完整 Flow 结构速查

```
[手动触发器]
  输入：BatchYearMonth (Text)、TopNIssuesInPDF (Number)

[List rows present in a table]
  Location: <你的 SharePoint 站点>
  Library: Documents
  File: POC-Validator/Inputs/<输入文件>.xlsx
  Table: tblOffshoring
  Settings → Pagination: On, Threshold: 5000

[Initialize variable – RowIndex]
  Type: Integer, Value: 0

[Initialize variable – RowsForAI]
  Type: Array, Value: []

[Apply to each] ← 输入选 List rows 的 value
  ├─ [Increment variable – RowIndex] +1
  └─ [Append to array variable – RowsForAI]
       Value: {"RowIndex": variables('RowIndex'), "YearMonth": items('Apply_to_each')?['YearMonth'], ...}

[Compose – AI_Input]
  Inputs: string(variables('RowsForAI'))

[Execute Agent and wait]  ← Copilot Studio
  Agent: <你的 Agent 名称>
  Text: Outputs of Compose – AI_Input

[Parse JSON]
  Content: Text (from Execute Agent and wait)
  Schema: (根据 Agent 输出样例生成)

[Apply to each] ← 输入选 Parse JSON 的 issues
  └─ [Excel Online – Add a row into a table – tblIssues]
       Severity / RuleId / RowIndex / Column / ...

[SharePoint – Create file]  ← 保存输出报告

[Teams – Post message]  ← 通知
```

---

## 五、参考资源

- [Microsoft 官方：Copilot Studio 与 Power Automate 集成](https://learn.microsoft.com/en-us/microsoft-copilot-studio/publication-connect-bot-to-power-automate)
- [Power Automate Apply to each 官方文档](https://learn.microsoft.com/en-us/power-automate/apply-to-each)
- [Excel Online (Business) 连接器文档](https://learn.microsoft.com/en-us/connectors/excelonlinebusiness/)
