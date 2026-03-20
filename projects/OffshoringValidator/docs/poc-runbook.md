# Offshoring Validator — POC 运行手册

> **阶段**：POC（概念验证）  
> **触发方式**：Power Automate 手动触发（Manual trigger）  
> **输出目标**：用户个人 OneDrive（POC 阶段）；后续迁移至 SharePoint（见[迁移路径](#迁移路径sharepoint)）

---

## 目录

1. [概述](#概述)
2. [前提条件](#前提条件)
3. [输入说明](#输入说明)
4. [验证规则](#验证规则)
5. [AI 输出格式](#ai-输出格式)
6. [输出文件与命名规范](#输出文件与命名规范)
7. [OneDrive 文件夹结构](#onedrive-文件夹结构)
8. [Teams 通知](#teams-通知)
9. [流程概览](#流程概览)
10. [迁移路径 — SharePoint](#迁移路径sharepoint)
11. [已知限制与说明](#已知限制与说明)

---

## 概述

本 POC 演示一个完整的**数据质量验证工作流**，针对存储在用户 OneDrive 中的 Excel 文件（表格 `tblOffshoring`），通过 AI 模型进行字段级验证，并输出：

- **Excel 报告**（`.xlsx`）：明细 + 汇总，支持筛选
- **PDF 报告**（`.pdf`）：可直接分发的只读版本
- **Teams 消息**：包含两个报告文件的 OneDrive 链接

---

## 前提条件

| 项目 | 要求 |
|------|------|
| Power Automate 许可 | 用户具有 Power Automate 标准/高级版授权 |
| OneDrive for Business | 用户已开通 OneDrive for Business（Microsoft 365） |
| Microsoft Teams | 用户已加入目标通知频道/聊天 |
| Copilot Studio / AI Builder | 具备调用 AI 模型的授权（GPT / Copilot 插件） |
| 输入文件权限 | 运行流程的账号对输入 Excel 文件具有读取权限 |
| Excel 输入文件 | 文件已上传至 OneDrive，包含命名表格 `tblOffshoring`（见下方） |

---

## 输入说明

### Excel 文件

- **位置**：用户个人 OneDrive（URL 在流程触发时手动输入或在流程变量中配置）
- **文件链接示例**：
  ```
  https://companyname-my.sharepoint.com/personal/username_company_com/Documents/OffshoringValidator/InputData/tblOffshoring_input.xlsx
  ```
  > 将上述 URL 替换为实际文件的 OneDrive 共享链接或直接路径。

- **表格名称**：`tblOffshoring`（Excel 内部命名表格，Name Manager 可见）

### 数据读取原则

- **不做任何修剪（trim）或清洗**：数据原样读取，前/后空格、特殊字符均保留用于验证
- 流程仅读取，不修改源文件

### `tblOffshoring` 推荐列结构

| 列名 | 数据类型 | 说明 |
|------|----------|------|
| `EmployeeID` | 文本 | 员工编号，必填，需唯一 |
| `FullName` | 文本 | 全名，必填 |
| `Department` | 文本 | 部门，必填 |
| `OffshoreLocation` | 文本 | 离岸地点，必填 |
| `StartDate` | 日期 | 合同开始日期，必填 |
| `EndDate` | 日期 | 合同结束日期，可选 |
| `CostCenter` | 文本 | 成本中心，必填 |
| `ContractType` | 文本 | 合同类型，必填 |
| `Status` | 文本 | 状态，必填 |

> ⚠️ 实际列根据业务调整；在 Copilot Studio 提示词中更新对应的列名列表。

---

## 验证规则

POC 阶段采用 **AI-only 验证**，不使用数据库或规则引擎。

### 严重级别

| 级别 | 说明 |
|------|------|
| `Error` | 必须修正，否则数据不可用 |
| `Warning` | 建议修正，不阻断处理 |

> POC 阶段仅使用 `Error` 和 `Warning` 两个级别，不启用 `Info`。

### 核心验证规则

#### 1. 空白字符（Whitespace）规则 — `Error`

- **前/后空格检测**：任何字段值若包含**前导或尾随空格**（`\s+` at start/end），均报 `Error`
  - 规则不做自动修正，只报告；修正由数据提供方处理
  - 示例：`" John Smith"` → Error（有前导空格）；`"John Smith "` → Error（有尾随空格）
  - 示例：`"John Smith"` → 正常
- **空白即空（whitespace-only = empty）**：若字段值全为空白字符（如 `"   "`），视为**空值**，适用必填规则

#### 2. 必填字段规则 — `Error`

- 必填字段为空（`null`、空字符串 `""`、或全空白）时，报 `Error`

#### 3. 业务逻辑规则 — `Warning`

- `EndDate` 早于 `StartDate` → `Warning`
- `Status` 值不在预期枚举中 → `Warning`
- `ContractType` 值不在预期枚举中 → `Warning`

---

## AI 输出格式

AI 模型（Copilot Studio / AI Builder）返回严格的结构化 JSON：

```json
{
  "report_model": {
    "generated_at": "2025-01-20T10:30:00Z",
    "source_file": "tblOffshoring_input.xlsx",
    "table_name": "tblOffshoring",
    "total_rows": 150,
    "error_count": 3,
    "warning_count": 7
  },
  "issues": [
    {
      "row_number": 5,
      "column": "FullName",
      "value_preview": "\" John Smith\"",
      "severity": "Error",
      "rule": "leading_whitespace",
      "message": "字段 FullName 第5行包含前导空格，请修正后重新提交。"
    },
    {
      "row_number": 12,
      "column": "EndDate",
      "value_preview": "2024-01-01",
      "severity": "Warning",
      "rule": "date_order",
      "message": "EndDate (2024-01-01) 早于 StartDate (2025-06-01)，请确认。"
    }
  ]
}
```

### JSON 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `report_model.generated_at` | ISO 8601 字符串 | 报告生成时间（UTC） |
| `report_model.source_file` | 字符串 | 源文件名 |
| `report_model.table_name` | 字符串 | 读取的表格名称 |
| `report_model.total_rows` | 整数 | 表格总行数（不含表头） |
| `report_model.error_count` | 整数 | Error 数量 |
| `report_model.warning_count` | 整数 | Warning 数量 |
| `issues[].row_number` | 整数 | 数据行号（从 1 开始，不含表头） |
| `issues[].column` | 字符串 | 问题字段名 |
| `issues[].value_preview` | 字符串 | 值预览（敏感数据可截断） |
| `issues[].severity` | 枚举 | `"Error"` 或 `"Warning"` |
| `issues[].rule` | 字符串 | 规则标识符 |
| `issues[].message` | 字符串 | 面向用户的说明文字 |

---

## 输出文件与命名规范

### 文件命名

```
ValidationReport_YYYYMMDD_HHmm.xlsx
ValidationReport_YYYYMMDD_HHmm.pdf
```

示例：
```
ValidationReport_20250120_1030.xlsx
ValidationReport_20250120_1030.pdf
```

> 时间使用流程触发时的本地时间（或 UTC，在流程中统一配置）。

### Excel 报告结构（`.xlsx`）

| Sheet 名称 | 内容 |
|------------|------|
| `Summary` | 汇总：总行数、Error 数、Warning 数、生成时间、源文件 |
| `Issues` | 明细：所有 issues（可按 Severity 筛选） |

`Issues` Sheet 列：

| 列 | 说明 |
|----|------|
| Row | 行号 |
| Column | 字段名 |
| Severity | Error / Warning（配色：红/橙） |
| Rule | 规则标识符 |
| ValuePreview | 值预览 |
| Message | 说明文字 |

### PDF 报告

- 与 Excel 内容一致，导出为只读 PDF
- 通过 Power Automate 使用 **OneDrive 的 Convert to PDF** action 生成，或使用 **Encodian / Muhimbi** 等连接器

---

## OneDrive 文件夹结构

所有输出文件存储在运行账号的个人 OneDrive：

```
OneDrive (个人 / Business)
└── OffshoringValidator/
    └── Reports/
        └── YYYY-MM/                         ← 按月分组（如 2025-01）
            ├── ValidationReport_20250120_1030.xlsx
            └── ValidationReport_20250120_1030.pdf
```

### Power Automate 配置路径

在 OneDrive 操作中填写相对路径：

```
/OffshoringValidator/Reports/{year}-{month:00}
```

> 月份不足两位时补零（如 `2025-01`）。

### 文件夹创建

首次运行时，Power Automate 的 **Create folder** 操作会自动创建目录；若已存在则跳过。

---

## Teams 通知

### 通知内容模板

```
📊 **Offshoring 数据验证完成**

🕐 生成时间：{generated_at_local}
📁 源文件：{source_file}
📋 总行数：{total_rows}

✅ 验证结果：
  🔴 Error：{error_count} 条
  🟡 Warning：{warning_count} 条

📂 报告文件（点击打开）：
  📗 [Excel 报告]({excel_link})
  📕 [PDF 报告]({pdf_link})

{result_summary}
```

`result_summary` 取值：
- 若 `error_count == 0 && warning_count == 0`：`✅ 数据完全通过验证，无需修正。`
- 若 `error_count == 0 && warning_count > 0`：`⚠️ 存在 {warning_count} 条警告，建议核查。`
- 若 `error_count > 0`：`❌ 存在 {error_count} 条错误，请修正数据后重新提交。`

### Teams 发送方式

- **Power Automate Action**：`Post message in a chat or channel`
- **目标**：用户个人 Chat（`Post in chat` → `Flow bot`），或指定 Teams 频道（POC 阶段建议用个人 Chat 方便测试）
- **链接生成**：通过 OneDrive 的 **Create share link** action 生成文件直链，粘贴到 Teams 消息

---

## 流程概览

```
[手动触发]
    │
    ▼
[读取输入参数]
  - Excel 文件路径 / URL（触发时填写或配置变量）
  - 表格名称（固定：tblOffshoring）
    │
    ▼
[从 OneDrive 读取 Excel 表格行]
  - Action: List rows present in a table (Excel Online - Business)
  - 注意：不修剪/清洗，原样传给 AI
    │
    ▼
[构造 AI 输入 Payload]
  - 序列化表格行为 JSON 字符串
  - 包含列名列表
    │
    ▼
[调用 AI 模型（Copilot Studio / AI Builder）]
  - 传入序列化数据和系统提示词
  - 返回结构化 JSON {report_model, issues[]}
    │
    ▼
[解析 AI JSON 输出]
  - Parse JSON action
    │
    ▼
[生成 Excel 报告（.xlsx）]
  - 使用 Excel Online 连接器写入 Summary + Issues sheets
  - 保存到 OneDrive /OffshoringValidator/Reports/{YYYY-MM}/
    │
    ▼
[生成 PDF 报告（.pdf）]
  - Convert Excel to PDF（OneDrive / Encodian 连接器）
  - 保存到同一 OneDrive 文件夹
    │
    ▼
[获取文件分享链接]
  - Create share link (OneDrive connector) for .xlsx
  - Create share link (OneDrive connector) for .pdf
    │
    ▼
[发送 Teams 通知]
  - Post message with Excel + PDF 链接
    │
    ▼
[结束]
```

> 详细步骤配置见 [flow-steps.md](./flow-steps.md)。

---

## 迁移路径 — SharePoint

> **POC 阶段**输出至用户个人 OneDrive，方便快速验证，无需配置 SharePoint 权限。  
> **正式上线**时，将输出目标迁移至团队 SharePoint 文档库，以支持多人访问与版本管理。

### 迁移步骤

1. **创建 SharePoint 文档库**
   - 在目标团队 SharePoint 站点创建文档库，如 `OffshoringReports`
   - 设置适当的权限（读取权限给验证人员，写入权限给流程账号）

2. **替换 Power Automate OneDrive Actions → SharePoint Actions**

   | POC（OneDrive） | 正式（SharePoint） |
   |----------------|-------------------|
   | Create file (OneDrive) | Create file (SharePoint) |
   | Create folder (OneDrive) | Create folder (SharePoint) |
   | Create share link (OneDrive) | Create sharing link for a file or folder (SharePoint) |

3. **更新 Teams 通知**
   - 改为发送到共享频道（而非个人 Chat）

4. **配置变量**
   - 将 `ONEDRIVE_OUTPUT_PATH` 变量替换为 `SHAREPOINT_SITE_URL` + `SHAREPOINT_LIBRARY` 变量

5. **文档库路径示例**
   ```
   SharePoint Site: https://companyname.sharepoint.com/sites/team-site
   Library: OffshoringReports
   Folder: Reports/{YYYY-MM}
   ```

---

## 已知限制与说明

| 项目 | 说明 |
|------|------|
| 行数限制 | Power Automate Excel 连接器默认最多返回 256 行；超出需分页或使用 Office Scripts |
| AI Token 限制 | 若表格行数多，需分批调用 AI，每批约 50 行 |
| PDF 生成 | OneDrive 原生 Convert to PDF 仅支持特定格式；建议使用 Encodian 连接器或 Office Scripts |
| 冷启动 | 首次调用 AI 连接器可能有延迟（5-15 秒） |
| 数据安全 | AI 调用会将数据发送给 AI 服务；POC 阶段请使用脱敏数据 |
| 不自动修正 | 流程只报告问题，不修改源 Excel 文件 |
