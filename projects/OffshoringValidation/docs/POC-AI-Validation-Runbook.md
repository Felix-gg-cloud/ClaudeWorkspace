# POC AI Validation Runbook — Power Automate Flow Steps

本文档描述 Power Automate 在 **AI-Only 校验模式**下的完整流程步骤。  
Flow 不再包含任何确定性规则判断逻辑；所有校验均由 Copilot Studio / LLM 执行。

---

## 一、Flow 触发条件

| 触发方式       | 配置                                                   |
|--------------|------------------------------------------------------|
| 定时触发       | Recurrence：每月 N 日 08:00（业务月末提交后）               |
| 手动触发       | Manually trigger a flow（测试阶段使用）                  |
| 文件变更触发   | SharePoint → When a file is modified（可选，监听特定文件） |

---

## 二、Flow 步骤详解

### Step 1：初始化变量

| 变量名              | 类型    | 初始值 | 说明                                  |
|--------------------|---------|--------|-------------------------------------|
| `varAllIssues`     | Array   | `[]`   | 收集所有批次返回的 issues               |
| `varAllReports`    | Array   | `[]`   | 收集所有批次返回的 Markdown 报告段落     |
| `varPageSize`      | Integer | `50`   | 每批最大行数（根据 token 限制调整）      |
| `varSkipToken`     | String  | `""`   | Excel 分页游标                         |
| `varHasMore`       | Boolean | `true` | 是否还有未读行                         |

---

### Step 2：读取 Excel 表格（分页循环）

使用 **Do Until**（条件：`varHasMore = false`）循环分页读取。

#### 2a. List rows present in a table

- **Action**：Excel Online (Business) → List rows present in a table  
- **Location**：SharePoint Site URL  
- **Document Library**：Documents（或实际库名）  
- **File**：`Offshoring_Data.xlsx`（实际文件路径）  
- **Table**：`tblOffshoring`  
- **Top Count**：`@{variables('varPageSize')}`  
- **Skip Token**：`@{variables('varSkipToken')}`（首次为空）  

**输出**：`body/value`（行数组），`body/@odata.nextLink`（分页链接，无则表示最后一页）

#### 2b. 更新分页状态

```
Set variable: varSkipToken = body/@odata.nextLink（若为空则设为 ""）
Set variable: varHasMore   = if(empty(body/@odata.nextLink), false, true)
```

---

### Step 3：构造请求 Payload

将当前批次的行数组打包为 JSON payload：

```json
{
  "rows": <当前批次的 body/value>,
  "batchInfo": {
    "batchIndex": <当前批次序号，从 1 开始>,
    "rowCount": <length(body/value)>,
    "sourceFile": "Offshoring_Data.xlsx",
    "sourceTable": "tblOffshoring",
    "requestedAt": "<utcNow()>"
  }
}
```

> **注意**：`rows` 数组中每个元素保持 Excel List rows 原始字段名（列名即字段名），不做额外转换。

---

### Step 4：调用 Copilot Studio

- **Action**：HTTP  
- **Method**：POST  
- **URI**：`<Copilot Studio Direct Line / Power Virtual Agents HTTP endpoint>`  
- **Headers**：
  ```
  Content-Type: application/json
  Authorization: Bearer <connector token>
  ```
- **Body**：Step 3 构造的 payload（JSON 字符串）  
- **Timeout**：120 秒（大批量时 LLM 推理可能耗时较长）

**输出**（期望）：
```json
{
  "issues": [ ... ],
  "report": "## Validation Report\n..."
}
```

---

### Step 5：累积结果

```
Append to array variable: varAllIssues  ← body/issues（Union 合并，或 Append each）
Append to array variable: varAllReports ← body/report（每批一段 Markdown）
```

---

### Step 6：所有批次完成后——汇总与存储

#### 6a. 合并报告

将 `varAllReports` 中各批次报告用分隔线拼接：

```
join(varAllReports, '\n\n---\n\n')
```

#### 6b. 写入 SharePoint（Issues JSON 文件）

- **Action**：SharePoint → Create file  
- **Folder Path**：`/ValidationOutputs/`  
- **File Name**：`issues_@{formatDateTime(utcNow(),'yyyyMM')}.json`  
- **File Content**：`@{string(varAllIssues)}`

#### 6c. 写入 SharePoint（Markdown 报告文件）

- **Action**：SharePoint → Create file  
- **Folder Path**：`/ValidationOutputs/`  
- **File Name**：`report_@{formatDateTime(utcNow(),'yyyyMM')}.md`  
- **File Content**：`@{join(varAllReports, '\n\n---\n\n')}`

#### 6d. 发送 Teams 通知（可选）

- **Action**：Microsoft Teams → Post a message in a chat or channel  
- **Message**：
  ```
  ✅ Offshoring 数据校验完成
  - 校验时间：@{utcNow()}
  - 发现问题：@{length(varAllIssues)} 条
  - 报告链接：<SharePoint 文件链接>
  ```

---

## 三、Flow 完整示意图

```
[Trigger]
    │
    ▼
[Init Variables]
    │
    ▼
┌──[Do Until: varHasMore = false]──────────────────────────────────────┐
│                                                                      │
│  [List rows present in tblOffshoring] ──→ rows (≤ varPageSize)      │
│           │                                                          │
│           ▼                                                          │
│  [Update varSkipToken / varHasMore]                                  │
│           │                                                          │
│           ▼                                                          │
│  [Compose Payload: { rows, batchInfo }]                              │
│           │                                                          │
│           ▼                                                          │
│  [HTTP POST → Copilot Studio]                                        │
│           │                                                          │
│           ▼                                                          │
│  [Append issues → varAllIssues]                                      │
│  [Append report → varAllReports]                                     │
│           │                                                          │
└───────────┘
    │
    ▼
[Compose final report = join(varAllReports)]
    │
    ▼
[SharePoint: Create issues JSON file]
    │
    ▼
[SharePoint: Create Markdown report file]
    │
    ▼
[Teams: Post notification]
```

---

## 四、分批策略（按 YearMonth 分批的替代方案）

当数据量极大（超过数千行）时，推荐在 Step 2 中先用 Filter Query 按 `YearMonth` 筛选，每个月份单独作为一批：

1. 首先读取所有不重复的 `YearMonth` 值（可用 Select + Unique 处理）  
2. 对每个 `YearMonth` 值循环执行 Step 2–5  
3. 每个月份的 issues 和 report 单独存储，也可汇总

**Filter Query 示例**：
```
YearMonth eq '202501'
```

---

## 五、注意事项

- 每批行数（`varPageSize`）默认 50，可根据每行字段数和平均字符数动态调整。  
  经验值：列数 ≈ 18、平均行长 ≈ 300 字符，50 行 ≈ 15 000 字符，在 16k token 模型的安全范围内。  
- Copilot Studio HTTP endpoint 需在 Power Platform 管理中心开启 **Direct Line API**。  
- Flow 运行记录和 AI 返回的原始 JSON 建议保存到 SharePoint 的独立审计目录，以便追溯。
