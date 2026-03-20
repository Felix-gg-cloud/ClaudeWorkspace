# POC-Validator Power Automate Runbook

> **适用对象**：Power Automate 新手；本手册按"点哪里、选什么、填什么"方式逐步描述每一个动作。  
> **最终目标**：从 SharePoint `Documents/POC-Validator/Inputs/` 读取 tblOffshoring 表 → 构建 RowsForAI → 调用 Copilot Studio 校验器 → 解析 JSON 响应 → 写入 Excel 输出模板 → 生成 PDF → 保存至 SharePoint `Documents/POC-Validator/Outputs/` → 发送 Teams 通知。

---

## 目录

1. [前置知识与已验证的关键要点](#1-前置知识与已验证的关键要点)
2. [阶段 0：准备 Excel 输入文件（tblOffshoring）](#2-阶段-0准备-excel-输入文件tbloffshoring)
3. [阶段 1：初始化变量并读取所有行](#3-阶段-1初始化变量并读取所有行)
4. [阶段 2：构建 RowsForAI 数组](#4-阶段-2构建-rowsforai-数组)
5. [阶段 3：调用 Copilot Studio 校验器](#5-阶段-3调用-copilot-studio-校验器)
6. [阶段 4：解析 JSON 响应](#6-阶段-4解析-json-响应)
7. [阶段 5：将 Issues 写入 Excel 输出模板](#7-阶段-5将-issues-写入-excel-输出模板)
8. [阶段 6：生成 PDF](#8-阶段-6生成-pdf)
9. [阶段 7：保存输出至 SharePoint](#9-阶段-7保存输出至-sharepoint)
10. [阶段 8：发送 Teams 通知](#10-阶段-8发送-teams-通知)
11. [完整验证清单](#11-完整验证清单)
12. [常见报错与排障](#12-常见报错与排障)

---

## 1 前置知识与已验证的关键要点

在搭建 Flow 前，请先理解以下已在调试过程中验证的核心规则，避免重复踩坑。

### 1.1 Excel 表头在第 5 行 → 用 Excel Table 解决

**问题**：Power Automate 的 `List rows present in a table` 动作**只读 Excel Table（命名表）**，不按行号读取工作表。  
**解法**：在 Excel 里将"从第 5 行开始的数据区"定义为一个名为 `tblOffshoring` 的 Table，第 1–4 行作为说明留在表外。  
**操作步骤（Excel 网页版或桌面版）**：

1. 打开 SharePoint `Documents/POC-Validator/Inputs/` 中的输入 Excel。
2. 点击 **A5**（或你表格起始列的第 5 行），再按 `Ctrl + Shift + End` 选到数据末尾。
3. 点菜单 **Insert（插入）→ Table（表格）**，在弹窗中勾选 **My table has headers（我的表具有标题）**，点 **OK**。
4. 点击表格内任意单元格，在顶部 **Table Design（表格设计）** 选项卡的左侧 **Table Name** 处输入 `tblOffshoring`，回车确认。
5. 保存文件。

> **验证**：在 Power Automate 的 `List rows present in a table` 动作的 **Table** 下拉框里能选到 `tblOffshoring`，即为成功。

---

### 1.2 Pagination 阈值必须是 >0 的整数

在 `List rows present in a table` 动作的 **Settings** 里开启 Pagination：

- **Pagination**：On
- **Threshold**：填 `5000`（或任意 >0 的正整数）

> ⚠️ **错误**：填 `0` 或留空会报错。**正确**：填 `5000` 即可覆盖绝大多数数据量。

---

### 1.3 Append to array variable 的 Value 必须是 Object（不能是 Array）

`Append to array variable` 动作每次只能追加**一个元素**，这个元素可以是：Float、Integer、String、Boolean、**Object（对象）**。

- **正确**（Object，没有方括号）：
  ```json
  {
    "RowIndex": 1,
    "YearMonth": "202603"
  }
  ```
- **错误**（Array，有方括号，会报错）：
  ```json
  [{"RowIndex": 1, "YearMonth": "202603"}]
  ```
- **错误**（空对象，不会报错但数据全丢）：
  ```json
  {}
  ```

---

### 1.4 RowIndex 自增步骤的放置位置

在 `Apply to each` 循环内，动作顺序必须是：

```
Apply to each
  ├─ 1. Increment variable (RowIndex, +1)   ← 先加，再追加
  └─ 2. Append to array variable (RowsForAI)
```

如果 `Increment variable` 放在循环**外面**，RowIndex 只会自增一次，导致所有行的 RowIndex 都是 1。

---

### 1.5 用 Compose 验证中间结果

在关键步骤后插入 **Compose** 动作查看实时数据，是最快的调试方法：

- 在 `Apply to each` **外面**（循环结束后）加 Compose，Inputs 选变量 `RowsForAI`。
- 运行后点开 Compose 的输出，确认每条记录都有 RowIndex 1、2、3… 以及正确的字段值。

---

## 2 阶段 0：准备 Excel 输入文件（tblOffshoring）

（已在 1.1 节详细描述，完成后进入阶段 1。）

**验证清单**：
- [ ] Excel 文件已上传至 SharePoint `Documents/POC-Validator/Inputs/`
- [ ] 第 5 行为列标题，数据从第 6 行开始
- [ ] 已定义 Excel Table 并命名为 `tblOffshoring`
- [ ] 在 Power Automate 的 Table 下拉框里能看到 `tblOffshoring`

---

## 3 阶段 1：初始化变量并读取所有行

### Step 1-1：新建 Flow

1. 打开 Power Automate → 左侧点 **Create（创建）**
2. 选 **Instant cloud flow（即时云端流）**
3. Flow 名称填：`POC-Validator-Manual`
4. 触发器选：**Manually trigger a flow（手动触发流）**
5. 点 **Create（创建）**

### Step 1-2：在触发器里添加输入参数

点开最上面的 **Manually trigger a flow** 卡片：

1. 点 **+ Add an input（添加输入）** → 选 **Text**
   - **Name**：`BatchYearMonth`（例如 `202603`）
2. 再点 **+ Add an input** → 选 **Number**
   - **Name**：`TopNIssuesInPDF`
   - **Default value**：`200`

点右上角 **Save（保存）**。

### Step 1-3：添加 List rows present in a table

点触发器下方 **+ New step**：

1. 搜索：`List rows present in a table`
2. 选：**Excel Online (Business) – List rows present in a table**

在动作卡片里填写：

| 字段 | 填写内容 |
|------|----------|
| **Location** | 选择你的 SharePoint 站点 |
| **Document Library** | `Documents` |
| **File** | 浏览选择 `POC-Validator/Inputs/<你的文件>.xlsx` |
| **Table** | 下拉选择 `tblOffshoring` |

> 如果 Table 下拉里看不到 `tblOffshoring`，请回到阶段 0 检查 Excel Table 是否正确创建。

### Step 1-4：开启 Pagination

在 **List rows present in a table** 动作右上角点 **…** → **Settings（设置）**：

- **Pagination（分页）**：打开（On）
- **Threshold（阈值）**：`5000`

点 **Done（完成）**，再点右上角 **Save**。

### Step 1-5：初始化 RowIndex 变量

点 **+ New step**：

1. 搜索：`Initialize variable`
2. 选：**Initialize variable**

填写：

| 字段 | 值 |
|------|-----|
| **Name** | `RowIndex` |
| **Type** | `Integer` |
| **Value** | `0` |

### Step 1-6：初始化 RowsForAI 变量

再点 **+ New step**：

1. 搜索：`Initialize variable`
2. 选：**Initialize variable**

填写：

| 字段 | 值 |
|------|-----|
| **Name** | `RowsForAI` |
| **Type** | `Array` |
| **Value** | `[]` |

**验证清单**：
- [ ] Flow 已创建，触发器有 BatchYearMonth 和 TopNIssuesInPDF 两个输入
- [ ] `List rows present in a table` 已选中正确的 SharePoint 站点、文件和 tblOffshoring
- [ ] Pagination 已开启，Threshold = 5000
- [ ] 变量 RowIndex（Integer，初始值 0）已创建
- [ ] 变量 RowsForAI（Array，初始值 []）已创建

---

## 4 阶段 2：构建 RowsForAI 数组

### Step 2-1：添加 Apply to each 循环

点 **+ New step**：

1. 搜索：`Apply to each`
2. 选：**Apply to each（控制流）**

在 **"Select an output from previous steps"** 里：
- 点右侧 **Dynamic content（动态内容）** 面板
- 找到 `List rows present in a table` 下的 **value**（这是行数组）
- 点击选中

### Step 2-2：在循环内 —— Increment variable

在 Apply to each 框内点 **Add an action（添加操作）**：

1. 搜索：`Increment variable`
2. 选：**Increment variable**

填写：

| 字段 | 值 |
|------|-----|
| **Name** | `RowIndex` |
| **Value** | `1` |

### Step 2-3：在循环内 —— Append to array variable

在上一步下方点 **Add an action**：

1. 搜索：`Append to array variable`
2. 选：**Append to array variable**

填写：

| 字段 | 值 |
|------|-----|
| **Name** | `RowsForAI` |
| **Value** | 见下方说明 |

**Value 应填入的内容**（复制后粘贴到 Value 输入框，或手动点选动态内容）：

```json
{
  "RowIndex": variables('RowIndex'),
  "YearMonth": items('Apply_to_each')?['YearMonth'],
  "CostCenterNumber": items('Apply_to_each')?['Cost Center Number'],
  "Function": items('Apply_to_each')?['Function'],
  "Team": items('Apply_to_each')?['Team'],
  "Owner": items('Apply_to_each')?['Owner']
}
```

> **注意**：  
> - `Apply_to_each` 必须与你画布上循环动作的实际内部名称一致（默认即为 `Apply_to_each`；如果你改过名或有多个循环，请相应调整）。  
> - 列名（如 `YearMonth`、`Cost Center Number`）必须与 tblOffshoring 表头**完全一致**（含空格和大小写）。  
> - Value 不能有外层 `[ ]`（那会变成 Array，导致报错）。

**UI 点选法（如果粘贴 JSON 不生效）**：

1. 在 Value 框里手动输入 `{`
2. 输入 `"RowIndex":` → 在右侧动态内容面板里点选变量 **RowIndex**
3. 输入 `, "YearMonth":` → 在动态内容面板里点选当前行的 **YearMonth** 列
4. 依次添加其他字段
5. 最后输入 `}`

### Step 2-4：循环外 —— Compose 验证

在 `Apply to each` **外面**（循环结束后，与循环同级）点 **+ New step**：

1. 搜索：`Compose`
2. 选：**Data Operation – Compose**
3. **Inputs**：在动态内容面板里选变量 **RowsForAI**

保存 Flow，点 **Test（测试）**，查看 Compose 的输出：

```json
[
  {"RowIndex": 1, "YearMonth": "202603", "CostCenterNumber": "CC001", ...},
  {"RowIndex": 2, "YearMonth": "202603", "CostCenterNumber": "CC002", ...},
  ...
]
```

**验证清单**：
- [ ] Apply to each 的输入是 List rows 的 `value`
- [ ] 循环内先 Increment variable（RowIndex +1），再 Append to array variable
- [ ] Append 的 Value 是 Object（`{...}`），不是 Array（`[...]`），不是空对象（`{}`）
- [ ] Compose（循环外）能输出包含所有列的对象数组
- [ ] RowIndex 值为 1, 2, 3… 依次递增，没有重复或固定为 1

---

## 5 阶段 3：调用 Copilot Studio 校验器

### Step 3-1：添加 HTTP 动作（调用 Copilot Studio）

在 Compose 验证步骤下方点 **+ New step**：

1. 搜索：`HTTP`
2. 选：**HTTP（Premium 动作）**

填写：

| 字段 | 值 |
|------|-----|
| **Method** | `POST` |
| **URI** | 你的 Copilot Studio 接口 URL（从 Copilot Studio 发布页面获取） |
| **Headers** | `Content-Type: application/json` |
| **Body** | 见下方 |

**Body**（使用 JSON 模式，从动态内容选 RowsForAI）：

```json
{
  "rows": @{variables('RowsForAI')},
  "yearMonth": "@{triggerBody()?['BatchYearMonth']}",
  "topNIssues": @{triggerBody()?['TopNIssuesInPDF']}
}
```

> 如果你的 Copilot Studio 需要 Bearer Token 认证，在 Headers 里加：  
> `Authorization: Bearer <你的 Token>`  
> 或使用 **Authentication** 字段，选择 OAuth 并配置连接。

### Step 3-2：调试小技巧

在 HTTP 动作之后插入一个 **Compose**，Inputs 填 `@{body('HTTP')}`，运行后可直接查看 Copilot Studio 的原始响应，方便排错。

**验证清单**：
- [ ] HTTP 动作的 URI、Method、Headers 已正确填写
- [ ] Body 中包含 RowsForAI、BatchYearMonth、TopNIssuesInPDF
- [ ] 运行后 HTTP 动作状态码为 200（或你的接口约定的成功码）
- [ ] Compose（响应原文）能看到有效的 JSON 内容

---

## 6 阶段 4：解析 JSON 响应

### Step 4-1：Parse JSON

在 HTTP 动作下方点 **+ New step**：

1. 搜索：`Parse JSON`
2. 选：**Data Operation – Parse JSON**

填写：

| 字段 | 值 |
|------|-----|
| **Content** | 动态内容选 HTTP 动作的 **Body** |
| **Schema** | 点 **Generate from sample（从示例生成）**，粘贴一条 Copilot Studio 的实际响应示例，系统会自动生成 Schema |

**示例响应格式**（根据你们实际接口调整）：

```json
{
  "issues": [
    {
      "RowIndex": 1,
      "RuleId": "R001",
      "Severity": "High",
      "Description": "Missing Cost Center mapping",
      "Recommendation": "Check tblMapping for CC001"
    }
  ],
  "summary": {
    "totalRows": 50,
    "issueCount": 3,
    "processedAt": "2026-03-20T08:00:00Z"
  }
}
```

### Step 4-2：初始化 Issues 变量

在 Parse JSON 下方点 **+ New step**：

1. 搜索：`Initialize variable`
2. 填写：
   - **Name**：`IssuesList`
   - **Type**：`Array`
   - **Value**：动态内容选 Parse JSON 的 **issues**（如果 Schema 里有该字段）

**验证清单**：
- [ ] Parse JSON 的 Content 选择了 HTTP Body
- [ ] Schema 已通过实际响应样本生成（不是手写）
- [ ] Parse JSON 运行后能看到结构化的 issues 数组
- [ ] IssuesList 变量包含正确数量的 issue 对象

---

## 7 阶段 5：将 Issues 写入 Excel 输出模板

### Step 5-1：准备 Excel 输出模板

在 SharePoint `Documents/POC-Validator/Outputs/` 准备一个 Excel 文件（例如 `POC-Issues-Template.xlsx`），其中包含一个 Excel Table，建议命名为 `tblIssues`，列头与 issue 字段一一对应：

| 列名 | 说明 |
|------|------|
| RowIndex | 对应输入行号 |
| RuleId | 校验规则 ID |
| Severity | 严重程度（High/Medium/Low） |
| Description | 问题描述 |
| Recommendation | 建议操作 |

### Step 5-2：Apply to each（遍历 Issues）

点 **+ New step**：

1. 搜索：`Apply to each`
2. 选：**Apply to each**
3. 输入：动态内容选变量 **IssuesList**

在循环内点 **Add an action**：

1. 搜索：`Add a row into a table`
2. 选：**Excel Online (Business) – Add a row into a table**

填写：

| 字段 | 值 |
|------|-----|
| **Location** | 你的 SharePoint 站点 |
| **Document Library** | `Documents` |
| **File** | `POC-Validator/Outputs/POC-Issues-Template.xlsx` |
| **Table** | `tblIssues` |

列映射（在 **Row** 区域里，每个列名对应选动态内容中的当前 issue 字段）：

| 列名 | 动态内容 |
|------|----------|
| RowIndex | 当前项 RowIndex |
| RuleId | 当前项 RuleId |
| Severity | 当前项 Severity |
| Description | 当前项 Description |
| Recommendation | 当前项 Recommendation |

**验证清单**：
- [ ] Excel 输出模板存在于 SharePoint 正确路径
- [ ] tblIssues Table 列名与 issue 字段完全匹配
- [ ] Add a row 动作能正确定位到模板文件和表
- [ ] 运行后 Excel 文件里已写入对应行数的 issue 数据

---

## 8 阶段 6：生成 PDF

> **方案一（推荐）**：使用 OneDrive/SharePoint 提供的 "Convert file" 功能将 Excel 转 PDF。  
> **方案二**：使用 Microsoft Graph API（HTTP 动作）调用 `convert` 端点。

### Step 6-1（方案一）：转换 Excel 为 PDF

在 Add a row 循环外面点 **+ New step**：

1. 搜索：`Convert file`（来自 OneDrive for Business 连接器）
2. 选：**OneDrive for Business – Convert file**

填写：

| 字段 | 值 |
|------|-----|
| **File** | 选择 `POC-Validator/Outputs/POC-Issues-Template.xlsx`（动态内容或浏览） |
| **Target type** | `PDF` |

动作输出的 **File Content** 即为 PDF 的二进制内容，用于下一步保存。

### Step 6-1（方案二）：通过 Graph API 转 PDF

如果方案一不可用，使用 **HTTP** 动作：

- **Method**：`GET`
- **URI**：`https://graph.microsoft.com/v1.0/sites/{siteId}/drive/items/{itemId}/content?format=pdf`
- **Authentication**：Active Directory OAuth

**验证清单**：
- [ ] Convert file 动作成功执行（状态为绿色勾）
- [ ] 动作输出的 File Content 不为空
- [ ] （可选）在后续步骤中将 PDF 保存到 SharePoint 后能在浏览器中正常打开

---

## 9 阶段 7：保存输出至 SharePoint

### Step 7-1：保存 PDF 到 SharePoint

点 **+ New step**：

1. 搜索：`Create file`
2. 选：**SharePoint – Create file**

填写：

| 字段 | 值 |
|------|-----|
| **Site Address** | 你的 SharePoint 站点 URL |
| **Folder Path** | `/Documents/POC-Validator/Outputs/` |
| **File Name** | 例如：`POC-Issues-@{triggerBody()?['BatchYearMonth']}.pdf` |
| **File Content** | 选择 Convert file 动作输出的 **File Content** |

### Step 7-2：保存填写后的 Excel 到 SharePoint

如果模板是"一次性写入"（每次运行覆盖），上述步骤已经直接写入了 `POC-Issues-Template.xlsx`。  
如果需要生成带时间戳的副本，在 Convert file 之前先 **Copy file** 重命名一份：

1. 搜索：`Copy file`
2. 选：**SharePoint – Copy file**
3. Source：`Documents/POC-Validator/Outputs/POC-Issues-Template.xlsx`
4. Destination：`Documents/POC-Validator/Outputs/POC-Issues-@{triggerBody()?['BatchYearMonth']}.xlsx`

**验证清单**：
- [ ] SharePoint `Documents/POC-Validator/Outputs/` 路径已存在
- [ ] PDF 文件已成功创建（在 SharePoint 上能看到并打开）
- [ ] Excel 输出文件已成功保存
- [ ] 文件名包含正确的 BatchYearMonth

---

## 10 阶段 8：发送 Teams 通知

### Step 8-1：发送 Teams 消息

点 **+ New step**：

1. 搜索：`Post message in a chat or channel`
2. 选：**Microsoft Teams – Post message in a chat or channel**

填写：

| 字段 | 值 |
|------|-----|
| **Post as** | `Flow bot` 或 `User` |
| **Post in** | `Channel` |
| **Team** | 选择目标 Team |
| **Channel** | 选择目标 Channel |
| **Message** | 见下方 |

**Message 示例**（支持 Adaptive Card Markdown）：

```
✅ **POC-Validator 校验完成**

- **批次**：@{triggerBody()?['BatchYearMonth']}
- **总行数**：@{body('Parse_JSON')?['summary']?['totalRows']}
- **问题数**：@{body('Parse_JSON')?['summary']?['issueCount']}
- **PDF 报告**：[点击查看](@{outputs('Create_file')?['body/Path']})
- **完成时间**：@{utcNow('yyyy-MM-dd HH:mm')} UTC
```

> **注意**：动态内容中的动作名（如 `Parse_JSON`、`Create_file`）必须与你画布上实际动作名一致。

### Step 8-2（可选）：发送失败通知

为 Flow 开启 **Error handling（错误处理）**：

1. 在可能失败的动作（如 HTTP 调用 Copilot Studio）后面添加一个并行分支（Parallel branch）
2. 将分支配置为"**仅在前一步失败时运行**"（在分支设置里选 **Configure run after** → 勾选 **has failed**）
3. 在失败分支里添加 Teams 消息动作，发送失败通知

**验证清单**：
- [ ] Teams 消息动作选择了正确的 Team 和 Channel
- [ ] 消息内容包含批次、问题数、PDF 链接
- [ ] 在 Teams 频道里能收到通知消息
- [ ] （可选）失败通知分支已配置

---

## 11 完整验证清单

按以下清单逐项验证，全部打勾后整条 Flow 即为可用状态。

### 阶段 0 — Excel 准备
- [ ] 输入 Excel 已上传至 `Documents/POC-Validator/Inputs/`
- [ ] tblOffshoring 表已正确定义（表头在第 5 行）
- [ ] Power Automate Table 下拉能看到 `tblOffshoring`

### 阶段 1 — 读取数据
- [ ] Pagination Threshold = 5000（>0 整数）
- [ ] RowIndex 变量初始化为 Integer 0
- [ ] RowsForAI 变量初始化为 Array []

### 阶段 2 — 构建 RowsForAI
- [ ] Apply to each 输入为 List rows 的 `value`
- [ ] 循环内：Increment variable 在 Append to array variable 之前
- [ ] Append 的 Value 是 Object（`{...}`），含 RowIndex 和所有需要校验的列
- [ ] Compose（循环外）输出正确的对象数组，RowIndex 递增

### 阶段 3 — 调用 Copilot Studio
- [ ] HTTP POST 成功（状态码 200）
- [ ] Body 包含 RowsForAI 数组

### 阶段 4 — 解析响应
- [ ] Parse JSON 成功，无错误
- [ ] IssuesList 数组包含预期数量的 issue

### 阶段 5 — 写入 Excel
- [ ] tblIssues Table 列名与 issue 字段对齐
- [ ] Add a row 动作成功写入所有 issue

### 阶段 6 — 生成 PDF
- [ ] Convert file 动作成功执行
- [ ] PDF 内容不为空

### 阶段 7 — 保存至 SharePoint
- [ ] PDF 文件出现在 `Documents/POC-Validator/Outputs/`
- [ ] 文件名包含 BatchYearMonth

### 阶段 8 — Teams 通知
- [ ] Teams 频道收到包含批次、问题数、PDF 链接的通知消息

---

## 12 常见报错与排障

### 报错 1：`The input value is of type 'Array' which cannot be appended`

**原因**：`Append to array variable` 的 Value 是 Array（数组），而非 Object。  
**修复**：检查 Value 输入框，确保：
- 没有外层 `[ ]`
- 没有选择 List rows 的整个 `value`（它是数组，不是单个对象）
- 每一列都是从"当前循环项"选取的字段（而不是 `value` 整体）

---

### 报错 2：`tblOffshoring` 在 Table 下拉里不显示

**原因**：Excel 数据区域尚未被定义为 Excel Table，或表名未保存成功。  
**修复**：
1. 回到 Excel 文件，点击数据区域内任意单元格。
2. 检查顶部是否有 **Table Design** 选项卡。若没有，说明该区域不是 Table，需要重新插入。
3. 在 **Table Name** 处确认名称为 `tblOffshoring`，按回车，保存文件。
4. 回到 Power Automate，重新打开 `List rows present in a table` 动作，刷新 Table 下拉。

---

### 报错 3：RowIndex 始终是 1

**原因 A**：`Increment variable` 在 `Apply to each` 循环**外面**，而不是里面。  
**修复**：把 Increment variable 动作拖进 Apply to each 框内。

**原因 B**：循环实际只执行了 1 次（List rows 只返回 1 行）。  
**修复**：检查 List rows 动作的 Settings 中 Pagination 是否已开启；检查 tblOffshoring 是否包含多行数据。

---

### 报错 4：Apply to each 循环看似无法停止

**原因 A**：数据量很大（几千行），加上每次迭代都有 Excel/SharePoint 连接器调用，速度慢。  
**排查**：在 Run history 里查看 Apply to each 已完成的迭代数，确认是否仍在增长。

**原因 B**：`List rows present in a table` 开了 Pagination 但数据量极大，拉取本身耗时。  
**修复（测试阶段）**：在 `List rows` 动作的 **Advanced options** 里找 **Top Count**，填 `20`，先用小批量验证逻辑正确性。

**取消运行**：在 Run history 里打开该运行，右上角点 **Cancel run（取消运行）**。

---

### 报错 5：HTTP 调用 Copilot Studio 失败（4xx/5xx）

**排查**：
1. 在 HTTP 动作后插入 Compose，Inputs = `@{body('HTTP')}`，查看错误详情。
2. 检查 URI 是否正确（从 Copilot Studio 发布页面复制）。
3. 检查认证 Token 是否过期。
4. 检查 Body 中的 JSON 格式是否正确（用 Compose 预先查看 RowsForAI 数组）。

---

### 报错 6：Parse JSON 报 Schema 错误

**原因**：实际响应结构与 Schema 不符（字段缺失或类型不匹配）。  
**修复**：重新点 **Generate from sample**，粘贴最新的实际 Copilot Studio 响应，生成新 Schema。

---

### 报错 7：Teams 消息里的动态内容显示为空

**原因**：动作名称（如 `Parse_JSON`、`Create_file`）在表达式里写错了。  
**修复**：在 Teams 消息的动态内容面板里重新点选对应字段，让系统自动填入正确的内部名称，而不要手动拼写。

---

*文档版本：v1.0 | 最后更新：2026-03-20*
