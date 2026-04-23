# POC-Validator：Power Automate 手动触发流 Runbook

> **适用版本**：POC 阶段（个人 OneDrive + Teams 通知）  
> **前置条件**（在执行本 Runbook 前必须完成）：
> 1. 已在 OneDrive 创建 `ValidationReportTemplate.xlsx`，路径：  
>    `/Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx`  
>    - 工作表：`Summary`（含固定标头行）、`Issues`（含 Excel 表格 `tblIssues`）
> 2. `tblIssues` 表格已建立，列结构见 [§ tblIssues 字段映射表](#tblissues-字段映射表)
> 3. Power Automate 已开通以下连接器权限：
>    - **OneDrive for Business**（读/写）
>    - **Excel Online (Business)**（读/写）
>    - **HTTP**（或 Copilot Studio 自定义连接器）
>    - **Convert to PDF**（或 OneDrive 文件转换）
>    - **Microsoft Teams**

---

## OneDrive 目录结构

```
Documents/PowerbiTest/POC-Validator/
├── Inputs/                          （可选，存放输入文件）
│   └── Master Excel_Strategy_OnOffshoring.xlsx
├── Templates/
│   └── ValidationReportTemplate.xlsx   ← 本 Runbook 的前置条件
├── Outputs/
│   ├── Excel/                       ← Step 3 生成的报告
│   ├── PDF/                         ← Step 5 生成的摘要
│   └── JSON/                        （可选，AI 原始返回用于审计）
```

---

## 流程概览

```
[手动触发]
    │
    ▼
Step 1: 读取 tblOffshoring（不修剪空白）
    │
    ▼
Step 2: 调用 Copilot Studio AI 校验器 → { report_model, issues[] }
    │
    ▼
Step 3: 复制模板 → 生成新 Excel 报告文件
    │
    ▼
Step 4: 写入 Summary 工作表 + 插入 tblIssues 行
    │
    ▼
Step 5: 将 Excel 转 PDF（Top N 摘要）→ 存 /Outputs/PDF/
    │
    ▼
Step 6: Teams 发送通知（附 Excel + PDF 链接）
```

---

## Step 1：读取 tblOffshoring（不修剪空白）

### 目的
从输入工作簿中原样读取 `tblOffshoring` 所有行和列，**不对任何字段执行 trim/strip 操作**。校验规则 R-WS-ALL-001 要求 AI 模型自行检测首尾空白，PA 侧不得预处理。

### 操作步骤

| # | 操作 | 说明 |
|---|------|------|
| 1.1 | 在流中添加 **"获取文件内容（OneDrive）"** 操作 | 选择文件：`/Documents/PowerbiTest/POC-Validator/Inputs/Master Excel_Strategy_OnOffshoring.xlsx` |
| 1.2 | 添加 **"列出表中存在的行（Excel Online）"** 操作 | 文件：上一步 ID；表：`tblOffshoring` |
| 1.3 | ⚠️ **不添加**任何 `trim()`、`replace()` 或字符串清洗表达式 | PA 层零修剪，原始值透传给 AI |

### 连接器配置

```
操作名称: 列出表中存在的行
├── 位置: OneDrive for Business
├── 文档库: OneDrive
├── 文件: /Documents/PowerbiTest/POC-Validator/Inputs/Master Excel_Strategy_OnOffshoring.xlsx
├── 表: tblOffshoring
└── 高级选项:
    ├── 过滤器查询: (留空)
    ├── 排序依据: (留空)
    └── 限制数量: (留空，读取全部)
```

### 输出变量
- `body/value`：数组，每个元素为一行，键名与 Excel 列标题一致（含原始空白）

---

## Step 2：调用 Copilot Studio AI 校验器

### 目的
将 Step 1 读取的原始数据发送给 Copilot Studio Agent，由 AI 执行所有校验规则，返回：
- `report_model`：汇总指标对象
- `issues[]`：问题明细数组

### 操作步骤

| # | 操作 | 说明 |
|---|------|------|
| 2.1 | 初始化变量 `varRows`（类型：数组） | 值：`body/value`（来自 Step 1） |
| 2.2 | 添加 **"HTTP"** 操作（或 Copilot Studio 自定义连接器） | POST 到 Copilot Studio Agent endpoint |
| 2.3 | 初始化变量 `varReportModel`（类型：对象） | 值：`body/report_model`（来自 HTTP 响应） |
| 2.4 | 初始化变量 `varIssues`（类型：数组） | 值：`body/issues`（来自 HTTP 响应） |

### HTTP 请求配置

```
方法: POST
URI: https://<your-copilot-studio-endpoint>/validate
标头:
  Content-Type: application/json
  Authorization: Bearer <token>
正文:
{
  "table_name": "tblOffshoring",
  "rows": @{variables('varRows')},
  "validation_mode": "ai_only",
  "trim_input": false
}
```

### AI 返回结构

```jsonc
{
  "report_model": {
    "run_id": "RUN-20240315-001",
    "generated_at": "2024-03-15T09:30:00Z",
    "total_rows": 120,
    "valid_rows": 98,
    "invalid_rows": 22,
    "error_count": 35,
    "warning_count": 12,
    "rules_applied": ["R-WS-ALL-001", "R-REQ-*", "..."],
    "top_issues": [
      { "rule_id": "R-WS-ALL-001", "count": 15, "affected_fields": ["Employee Name", "Cost Center"] }
    ]
  },
  "issues": [
    {
      "row_index": 3,
      "field_name": "Employee Name",
      "rule_id": "R-WS-ALL-001",
      "severity": "Error",
      "message": "Field contains leading/trailing whitespace",
      "raw_value": " John Doe "
    }
  ]
}
```

---

## Step 3：复制模板，创建新 Excel 报告

### 目的
将 `/Templates/ValidationReportTemplate.xlsx` 复制到 `/Outputs/Excel/`，生成带时间戳的新文件，作为本次校验报告的载体。

### 操作步骤

| # | 操作 | 说明 |
|---|------|------|
| 3.1 | 初始化变量 `varTimestamp` | 见 [§ 表达式示例](#表达式示例timestamp) |
| 3.2 | 初始化变量 `varExcelFileName` | 见 [§ 表达式示例](#表达式示例文件名) |
| 3.3 | 添加 **"复制文件（OneDrive）"** 操作 | 源 → 目标（见下） |

### 复制文件配置

```
操作名称: 复制文件
├── 来源文件路径: /Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx
├── 目标文件夹路径: /Documents/PowerbiTest/POC-Validator/Outputs/Excel/
├── 目标文件名: @{variables('varExcelFileName')}
└── 遇到冲突时: 重命名（Rename）
```

### 输出变量
- `body/id`：新复制文件的 OneDrive ID（后续步骤写入数据时使用）
- `body/webUrl`：新文件的分享链接（Step 6 使用）

---

## Step 4：写入 Summary 工作表 + 插入 tblIssues 行

本步骤分为两个并行块，建议使用 **作用域（Scope）** 容器包裹以便错误捕获。

### 4-A：写入 Summary 工作表指标

| # | 操作 | 说明 |
|---|------|------|
| 4-A.1 | 添加 **"更新行（Excel Online）"** 操作 | 目标：Step 3 生成的新文件，工作表：`Summary` |
| 4-A.2 | 将 `varReportModel` 各字段映射到 Summary 表格行 | 见 [§ Summary 字段映射表](#summary-字段映射表) |

> **注意**：如果 Summary 工作表不使用 Excel 表格格式，则改用 **"更新单元格"** 操作，按具体单元格地址写入。

#### Summary 字段映射表

| Summary 单元格 / 列 | report_model 字段 | 示例表达式 |
|---------------------|-------------------|-----------|
| `B2`（Run ID） | `run_id` | `@{body('HTTP')?['report_model']?['run_id']}` |
| `B3`（生成时间） | `generated_at` | `@{body('HTTP')?['report_model']?['generated_at']}` |
| `B4`（总行数） | `total_rows` | `@{body('HTTP')?['report_model']?['total_rows']}` |
| `B5`（有效行数） | `valid_rows` | `@{body('HTTP')?['report_model']?['valid_rows']}` |
| `B6`（无效行数） | `invalid_rows` | `@{body('HTTP')?['report_model']?['invalid_rows']}` |
| `B7`（Error 数） | `error_count` | `@{body('HTTP')?['report_model']?['error_count']}` |
| `B8`（Warning 数） | `warning_count` | `@{body('HTTP')?['report_model']?['warning_count']}` |
| `B9`（应用规则列表） | `rules_applied` | `@{join(body('HTTP')?['report_model']?['rules_applied'], ', ')}` |

### 4-B：插入 issues 到 tblIssues

| # | 操作 | 说明 |
|---|------|------|
| 4-B.1 | 添加 **"Apply to each"** 循环 | 遍历 `varIssues` |
| 4-B.2 | 在循环内添加 **"将行添加到表（Excel Online）"** 操作 | 目标：新 Excel 文件，表：`tblIssues` |
| 4-B.3 | 按 [§ tblIssues 字段映射表](#tblissues-字段映射表) 填写各列 | — |

#### tblIssues 字段映射表

| tblIssues 列名 | 来源 | 示例表达式 |
|----------------|------|-----------|
| `RowKey` | 生成唯一键 | `@{concat(variables('varTimestamp'), '-', string(items('Apply_to_each')?['row_index']))}` |
| `RunID` | report_model | `@{body('HTTP')?['report_model']?['run_id']}` |
| `RowIndex` | issues 项 | `@{items('Apply_to_each')?['row_index']}` |
| `FieldName` | issues 项 | `@{items('Apply_to_each')?['field_name']}` |
| `RuleID` | issues 项 | `@{items('Apply_to_each')?['rule_id']}` |
| `Severity` | issues 项 | `@{items('Apply_to_each')?['severity']}` |
| `Message` | issues 项 | `@{items('Apply_to_each')?['message']}` |
| `RawValue` | issues 项 | `@{items('Apply_to_each')?['raw_value']}` |
| `CreatedAt` | 运行时间 | `@{variables('varTimestamp')}` |

---

## Step 5：生成 PDF 摘要

### 目的
将 Step 3 生成的 Excel 报告转换为 PDF（仅含 Summary 工作表和 Top N Issues），存入 `/Outputs/PDF/`。

### 操作步骤

| # | 操作 | 说明 |
|---|------|------|
| 5.1 | 初始化变量 `varPdfFileName` | 见 [§ 表达式示例](#表达式示例文件名) |
| 5.2 | 添加 **"获取文件内容（OneDrive）"** | 获取 Step 3 生成的 Excel 文件内容 |
| 5.3 | 添加 **"转换文件（OneDrive）"** 或 **"HTTP"** 操作 | 将 Excel 转为 PDF |
| 5.4 | 添加 **"创建文件（OneDrive）"** 操作 | 将 PDF 内容存入 `/Outputs/PDF/` |

### 转换文件配置（OneDrive 原生）

```
操作名称: 转换文件
├── 文件: @{body('复制文件')?['id']}
└── 目标格式: PDF
```

### 创建 PDF 文件配置

```
操作名称: 创建文件
├── 文件夹路径: /Documents/PowerbiTest/POC-Validator/Outputs/PDF/
├── 文件名: @{variables('varPdfFileName')}
└── 文件内容: @{body('转换文件')}
```

### 输出变量
- `body/webUrl`：PDF 文件的分享链接（Step 6 使用）

---

## Step 6：发送 Teams 通知（附链接）

### 目的
在指定 Teams 频道发布消息，告知本次校验完成，并附上 Excel 报告和 PDF 摘要的访问链接。

### 操作步骤

| # | 操作 | 说明 |
|---|------|------|
| 6.1 | 添加 **"发布消息（Microsoft Teams）"** 操作 | — |
| 6.2 | 配置频道和消息内容 | 见下方配置 |

### Teams 消息配置

```
操作名称: 在聊天或频道中发布消息
├── 发布为: Flow bot
├── 发布位置: Channel
├── 团队: <你的团队名>
├── 频道: <目标频道>
└── 消息（HTML）:
    <p>✅ <strong>Offshoring 数据校验完成</strong></p>
    <ul>
      <li>运行 ID：@{body('HTTP')?['report_model']?['run_id']}</li>
      <li>总行数：@{body('HTTP')?['report_model']?['total_rows']}</li>
      <li>有效行：@{body('HTTP')?['report_model']?['valid_rows']}</li>
      <li>Error 数：@{body('HTTP')?['report_model']?['error_count']}</li>
      <li>Warning 数：@{body('HTTP')?['report_model']?['warning_count']}</li>
    </ul>
    <p>
      📊 <a href="@{body('复制文件')?['webUrl']}">下载 Excel 报告</a>　|　
      📄 <a href="@{body('创建文件')?['webUrl']}">下载 PDF 摘要</a>
    </p>
```

---

## 校验规则摘要

### R-WS-ALL-001：全局首尾空白检测

| 属性 | 值 |
|------|----|
| **规则 ID** | `R-WS-ALL-001` |
| **规则名称** | Global Leading/Trailing Whitespace |
| **适用范围** | `tblOffshoring` 全部文本类型字段 |
| **严重级别** | Error |
| **PA 处理方式** | ⚠️ **PA 层不做任何 trim**，原始值透传给 AI |
| **AI 检测逻辑** | 若 `field_value != trim(field_value)` 则产生 issue |
| **错误消息模板** | `"Field '{field_name}' contains leading/trailing whitespace. Raw value: '{raw_value}'"` |

### 必填字段规则（Requiredness Logic）

| 规则 ID | 适用字段（示例） | 条件 | 严重级别 |
|---------|----------------|------|---------|
| `R-REQ-001` | `Employee Name` | 不为 null 且不为空字符串 | Error |
| `R-REQ-002` | `Employee ID` | 不为 null 且不为空字符串 | Error |
| `R-REQ-003` | `Cost Center` | 不为 null 且不为空字符串 | Error |
| `R-REQ-004` | `Offshoring Location` | 不为 null 且不为空字符串 | Error |
| `R-REQ-005` | `Effective Date` | 不为 null 且为有效日期格式 | Error |
| `R-REQ-006` | `Approval Status` | 不为 null，值在枚举列表内 | Error |
| `R-REQ-COND-001` | `End Date` | 仅当 `Contract Type = Fixed` 时必填 | Warning |

> **必填检测优先级**：AI 先检测 R-WS-ALL-001（空白），再检测 R-REQ-*（必填）。  
> 若字段仅含空格，同时触发 R-WS-ALL-001（Error）和 R-REQ-*（Error）两条规则。

---

## 表达式示例

### 表达式示例：Timestamp

```
变量名: varTimestamp
类型: 字符串
值表达式:
  formatDateTime(utcNow(), 'yyyyMMdd-HHmmss')

示例输出: 20240315-093045
```

### 表达式示例：文件名

```
变量名: varExcelFileName
类型: 字符串
值表达式:
  concat('ValidationReport_', variables('varTimestamp'), '.xlsx')

示例输出: ValidationReport_20240315-093045.xlsx

---

变量名: varPdfFileName
类型: 字符串
值表达式:
  concat('ValidationSummary_', variables('varTimestamp'), '.pdf')

示例输出: ValidationSummary_20240315-093045.pdf
```

### 表达式示例：RowKey

```
场景: tblIssues 每行唯一键，格式 = <Timestamp>-<RowIndex>
表达式:
  concat(variables('varTimestamp'), '-', string(items('Apply_to_each')?['row_index']))

示例输出: 20240315-093045-3
```

---

## 后续迁移到团队 SharePoint

当 POC 验证通过后，将以下配置从个人 OneDrive 迁移到团队 SharePoint：

| 需要修改的操作 | OneDrive（POC） | SharePoint（生产） |
|---------------|-----------------|-------------------|
| 读取输入文件 | OneDrive for Business | SharePoint - 获取文件内容 |
| 列出表中行 | OneDrive 路径 | SharePoint 站点 + 文档库 |
| 复制模板 | OneDrive 复制文件 | SharePoint 复制文件 |
| 写入 Excel | OneDrive 文件 ID | SharePoint 文件 ID |
| 存储 PDF | OneDrive 创建文件 | SharePoint 创建文件 |
| 文件路径前缀 | `/Documents/PowerbiTest/POC-Validator/` | `/sites/<SiteName>/Shared Documents/POC-Validator/` |

> **权限说明**：迁移到 SharePoint 后需确保 Flow 所用账号对目标文档库有 **Edit** 权限，Teams 通知频道需重新选择对应团队。
