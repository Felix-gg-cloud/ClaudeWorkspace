# POC-Validator Runbook

## 共享账户 + SharePoint 存储完整操作手册

**适用场景**：使用公共/共享账户（非个人账户），文档库名称为 **Documents**，所有文件统一存放在 SharePoint 站点的 `POC-Validator/` 目录下。

---

## 目录

1. [前置条件与基础信息确认](#1-前置条件与基础信息确认)
2. [SharePoint 目录结构](#2-sharepoint-目录结构)
3. [Power Automate Flow 总览](#3-power-automate-flow-总览)
4. [Step 1：读取 tblOffshoring 输入数据](#4-step-1读取-tbloffshoring-输入数据)
5. [Step 2：调用 Copilot Studio AI 验证器](#5-step-2调用-copilot-studio-ai-验证器)
6. [Step 3：从模板创建 Excel 报告并写入数据](#6-step-3从模板创建-excel-报告并写入数据)
7. [Step 4：生成 PDF（免 Premium 连接器方案）](#7-step-4生成-pdf免-premium-连接器方案)
8. [Step 5：保存 PDF 并发送 Teams 消息](#8-step-5保存-pdf-并发送-teams-消息)
9. [PDF 转换连接器选型指南](#9-pdf-转换连接器选型指南)
10. [权限最小集](#10-权限最小集)
11. [常见问题排查](#11-常见问题排查)

---

## 1. 前置条件与基础信息确认

在开始配置 Power Automate 之前，确认以下信息：

| 项目 | 值（填写你的实际值） |
|------|----------------------|
| SharePoint 团队站点 URL | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| 文档库名称 | `Documents` |
| 输入文件名 | `Master Excel_Strategy_OnOffshoring.xlsx` |
| Copilot Studio Agent 名称 | `POC-Validator-Agent`（按实际填写） |
| Teams 目标团队 / 频道 | 例如：`POC项目组` / `poc-validator` |
| Power Platform 环境 | 与 Copilot Studio Agent 所在环境一致 |

> **重要**：Power Automate 连接（Connections）必须全部使用共享账户登录，否则会出现"选不到站点/文件"的问题。进入 Power Automate → Data → Connections，检查以下连接均已用共享账户授权：
> - SharePoint
> - Excel Online (Business)
> - Microsoft Teams
> - HTTP（或 Power Virtual Agents，视调用方式而定）

---

## 2. SharePoint 目录结构

文档库 **Documents** 下的完整目录结构：

```
Documents/
└── POC-Validator/
    ├── Inputs/
    │   └── Master Excel_Strategy_OnOffshoring.xlsx   ← 含 tblOffshoring 的输入文件
    ├── Templates/
    │   └── ValidationReportTemplate.xlsx             ← 报告模板（含 Summary、Issues Sheet 和 tblIssues 表）
    ├── Outputs/
    │   ├── Excel/                                    ← 生成的 Excel 报告
    │   ├── PDF/                                      ← 生成的 PDF 报告
    │   └── JSON/                                     ← 可选：验证结果 JSON
    └── Temp/                                         ← 中间文件（HTML 等），可定期清理
```

> 模板文件 `ValidationReportTemplate.xlsx` 需包含：
> - Sheet `Summary`：固定单元格（如 B2=BatchID、B3=运行日期、B4=总行数、B5=通过数、B6=问题数、B7=通过率）
> - Sheet `Issues`：包含名为 `tblIssues` 的 Excel 表，列头：`RowID`、`EmployeeID`、`IssueSeverity`、`IssueCategory`、`IssueDetail`、`AISuggestion`

---

## 3. Power Automate Flow 总览

Flow 触发方式建议：**手动触发（Manually trigger a flow）** 或 **定时触发（Recurrence）**

整体步骤顺序：

```
[触发器]
  ↓
Step 1: 读取 tblOffshoring（SharePoint + Excel Online）
  ↓
Step 2: 循环每行 → 调用 Copilot Studio AI 验证
  ↓
Step 3: 从模板创建 Excel 报告 → 写 Summary 固定单元格 → 插入 tblIssues 行
  ↓
Step 4: 生成 HTML → 转换为 PDF（免 Premium 方案）
  ↓
Step 5: 保存 PDF 到 SharePoint → 发送 Teams 消息（含 Excel/PDF 链接）
```

---

## 4. Step 1：读取 tblOffshoring 输入数据

### 动作 1.1 — List rows present in a table（Excel Online Business）

| 字段 | 值 |
|------|----|
| 动作名称 | `Excel Online (Business)` → `List rows present in a table` |
| Location | SharePoint |
| Document Library | Documents |
| File | `/POC-Validator/Inputs/Master Excel_Strategy_OnOffshoring.xlsx` |
| Table | `tblOffshoring` |
| 高级选项 → Pagination | 打开（Top Count 留空或填 `5000`） |

**输出变量**：`List rows present in a table` → `value`（数组，每项为一行 tblOffshoring 数据）

> **注意**：若文件较大（超过 256 行），必须打开 Pagination，否则只返回前 256 行。

**初始化汇总变量**（在此步骤后添加 Initialize variable 动作）：

| 变量名 | 类型 | 初始值 |
|--------|------|--------|
| `varTotalRows` | Integer | `@{length(outputs('List_rows_present_in_a_table')?['body/value'])}` |
| `varPassCount` | Integer | `0` |
| `varIssueCount` | Integer | `0` |
| `varIssuesList` | Array | `[]` |
| `varBatchID` | String | `@{formatDateTime(utcNow(), 'yyyyMMdd-HHmmss')}` |
| `varBatchYearMonth` | String | `@{formatDateTime(utcNow(), 'yyyyMM')}` |

---

## 5. Step 2：调用 Copilot Studio AI 验证器

对 `tblOffshoring` 的每一行调用 Copilot Studio Agent 进行 AI 验证。

### 动作 2.1 — Apply to each（循环每行数据）

- 输入：`@{outputs('List_rows_present_in_a_table')?['body/value']}`

### 动作 2.2 — 调用 Copilot Studio Agent（HTTP 方式，无 Premium）

> Copilot Studio Agent 的 Direct Line API 是免费层可用的调用方式。

**获取 Direct Line Token**：

| 字段 | 值 |
|------|----|
| 动作类型 | HTTP |
| 方法 | POST |
| URI | `https://directline.botframework.com/v3/directline/tokens/generate` |
| 标头 `Authorization` | `Bearer <你的 Direct Line Secret>` |
| 标头 `Content-Type` | `application/json` |

**发送消息给 Agent**：

| 字段 | 值 |
|------|----|
| 动作类型 | HTTP |
| 方法 | POST |
| URI | `@{body('获取Token')?['conversationId']}` 下的 `/activities` |
| 请求体 | 见下方 JSON |

```json
{
  "type": "message",
  "from": { "id": "power-automate-flow" },
  "text": "Validate the following record: @{string(items('Apply_to_each'))}"
}
```

**获取 Agent 回复**：

| 字段 | 值 |
|------|----|
| 动作类型 | HTTP |
| 方法 | GET |
| URI | `https://directline.botframework.com/v3/directline/conversations/<conversationId>/activities` |

**解析 AI 验证结果**（Parse JSON）：

- 从 Agent 回复中提取：`IsValid`（Boolean）、`Severity`（String）、`Category`（String）、`Detail`（String）、`Suggestion`（String）

### 动作 2.3 — 条件判断：是否有问题

```
If @{body('Parse_JSON')?['IsValid']} equals false:
  → Increment varIssueCount
  → Append to varIssuesList:
    {
      "RowID": "@{items('Apply_to_each')?['RowID']}",
      "EmployeeID": "@{items('Apply_to_each')?['EmployeeID']}",
      "IssueSeverity": "@{body('Parse_JSON')?['Severity']}",
      "IssueCategory": "@{body('Parse_JSON')?['Category']}",
      "IssueDetail": "@{body('Parse_JSON')?['Detail']}",
      "AISuggestion": "@{body('Parse_JSON')?['Suggestion']}"
    }
Else:
  → Increment varPassCount
```

> **替代方案（如已有 Copilot Studio 连接器）**：若 Power Platform 环境中已安装 Copilot Studio 连接器且为内置（非 Premium），可直接使用 `Run a prompt` 动作，无需 HTTP 调用。但请确认该连接器在你的许可证下可用。

---

## 6. Step 3：从模板创建 Excel 报告并写入数据

### 动作 3.1 — 获取模板文件内容（SharePoint）

| 字段 | 值 |
|------|----|
| 动作类型 | SharePoint → `Get file content using path` |
| Site Address | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| File Path | `/Documents/POC-Validator/Templates/ValidationReportTemplate.xlsx` |

### 动作 3.2 — 创建 Excel 报告副本（SharePoint）

| 字段 | 值 |
|------|----|
| 动作类型 | SharePoint → `Create file` |
| Site Address | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| Folder Path | `/Documents/POC-Validator/Outputs/Excel` |
| File Name | `ValidationReport_@{variables('varBatchYearMonth')}__@{variables('varBatchID')}.xlsx` |
| File Content | `@{body('Get_file_content_using_path')}` |

**输出**：新文件的 `Id`（后续写入需要用到）

### 动作 3.3 — 写入 Summary Sheet 固定单元格（Excel Online Business）

对每个固定单元格分别使用 `Update a row` 动作，或使用 `Run script`（Office Scripts，免 Premium）批量写入。

**方法 A：逐个更新单元格（无需 Office Scripts）**

使用动作：Excel Online (Business) → `Update a row`（针对单行表）或 直接用 HTTP 调用 Graph API：

```
PATCH https://graph.microsoft.com/v1.0/sites/<siteId>/drives/<driveId>/items/<fileId>/workbook/worksheets/Summary/range(address='B2:B7')
Content-Type: application/json

{
  "values": [
    ["@{variables('varBatchID')}"],
    ["@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}"],
    ["@{variables('varTotalRows')}"],
    ["@{variables('varPassCount')}"],
    ["@{variables('varIssueCount')}"],
    ["@{if(equals(variables('varTotalRows'), 0), '0%', concat(string(div(mul(variables('varPassCount'), 100), variables('varTotalRows'))), '%'))}"]
  ]
}
```

> 使用 HTTP + Graph API 需要共享账户有 Files.ReadWrite 权限，但不需要 Premium 连接器。

**方法 B：Office Scripts（推荐，简洁）**

1. 在 Excel 文件中提前录制/编写 Office Script（`updateSummary`），接受参数：batchID、runDate、total、pass、issue、passRate
2. Power Automate 动作：Excel Online (Business) → `Run script`
   - Location：SharePoint
   - Document Library：Documents
   - File：新创建的报告文件
   - Script：`updateSummary`
   - 参数：填入对应变量

### 动作 3.4 — 向 tblIssues 插入所有问题行

使用 Apply to each 循环 `varIssuesList`，对每一项使用：

| 字段 | 值 |
|------|----|
| 动作类型 | Excel Online (Business) → `Add a row into a table` |
| Location | SharePoint |
| Document Library | Documents |
| File | 新创建报告文件的路径（用动作 3.2 的输出路径） |
| Table | `tblIssues` |
| Row | `@{items('Apply_to_each_issues')}` |

> **性能提示**：若问题行超过 50 条，建议改用 Office Scripts 批量插入，避免逐行调用 API 导致 Flow 超时。

---

## 7. Step 4：生成 PDF（免 Premium 连接器方案）

### 推荐方案：HTML → SharePoint/OneDrive → Convert file

此方案不需要任何 Premium 连接器，仅使用 OneDrive for Business 的内置转换功能。

#### 动作 4.1 — 组装 HTML 内容

使用 `Compose` 动作生成报告 HTML：

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <style>
    body { font-family: Calibri, sans-serif; margin: 40px; }
    h1 { color: #0078d4; }
    table { border-collapse: collapse; width: 100%; margin-top: 20px; }
    th { background: #0078d4; color: white; padding: 8px 12px; text-align: left; }
    td { border: 1px solid #ddd; padding: 6px 12px; }
    tr:nth-child(even) { background: #f2f2f2; }
    .summary-box { background: #e8f4fd; border-left: 4px solid #0078d4; padding: 12px; margin-bottom: 20px; }
  </style>
</head>
<body>
  <h1>POC Offshoring Validation Report</h1>
  <div class="summary-box">
    <p><strong>Batch ID：</strong>@{variables('varBatchID')}</p>
    <p><strong>Run Date：</strong>@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}</p>
    <p><strong>Total Rows：</strong>@{variables('varTotalRows')}</p>
    <p><strong>Passed：</strong>@{variables('varPassCount')}</p>
    <p><strong>Issues：</strong>@{variables('varIssueCount')}</p>
    <p><strong>Pass Rate：</strong>@{if(equals(variables('varTotalRows'), 0), '0%', concat(string(div(mul(variables('varPassCount'), 100), variables('varTotalRows'))), '%'))}</p>
  </div>
  <h2>Issue Details</h2>
  <table>
    <tr>
      <th>RowID</th><th>EmployeeID</th><th>Severity</th>
      <th>Category</th><th>Detail</th><th>AI Suggestion</th>
    </tr>
    @{join(apply_to_each_result, '')}
  </table>
</body>
</html>
```

> 在循环 `varIssuesList` 时，用 `Append to string variable`（变量名 `varHTMLRows`）拼接每行：
> ```html
> <tr>
>   <td>@{item()?['RowID']}</td>
>   <td>@{item()?['EmployeeID']}</td>
>   <td>@{item()?['IssueSeverity']}</td>
>   <td>@{item()?['IssueCategory']}</td>
>   <td>@{item()?['IssueDetail']}</td>
>   <td>@{item()?['AISuggestion']}</td>
> </tr>
> ```
> 然后在 Compose 里引用 `varHTMLRows`。

#### 动作 4.2 — 将 HTML 保存到 SharePoint Temp 目录

| 字段 | 值 |
|------|----|
| 动作类型 | SharePoint → `Create file` |
| Site Address | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| Folder Path | `/Documents/POC-Validator/Temp` |
| File Name | `Report_@{variables('varBatchID')}.html` |
| File Content | `@{outputs('Compose_HTML')}` |

> **说明**：SharePoint `Create file` 动作的 File Content 字段直接接受字符串，Power Automate 会自动处理编码。若遇到内容乱码，可改用：`@{base64ToBinary(base64(string(outputs('Compose_HTML'))))}`

#### 动作 4.3 — 转换 HTML 为 PDF（OneDrive for Business Convert）

| 字段 | 值 |
|------|----|
| 动作类型 | OneDrive for Business → `Convert file` |
| File | 使用动态内容，选择步骤 4.2 `Create file` 动作的输出：`@{outputs('Create_HTML_file')?['body/Id']}` |
| Target type | `PDF` |

> **说明**：`Convert file` 动作属于 **OneDrive for Business** 连接器，包含在 Microsoft 365 标准许可中，**不需要 Premium**。输出为 PDF 的二进制内容。

---

## 8. Step 5：保存 PDF 并发送 Teams 消息

### 动作 5.1 — 保存 PDF 到 SharePoint Outputs/PDF

| 字段 | 值 |
|------|----|
| 动作类型 | SharePoint → `Create file` |
| Site Address | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| Folder Path | `/Documents/POC-Validator/Outputs/PDF` |
| File Name | `ValidationReport_@{variables('varBatchYearMonth')}__@{variables('varBatchID')}.pdf` |
| File Content | `@{body('Convert_file')}` |

**获取文件链接（用于 Teams 消息）**：

| 字段 | 值 |
|------|----|
| 动作类型 | SharePoint → `Get file properties` |
| Site Address | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| Library Name | Documents |
| Id | `@{outputs('Create_PDF_file')?['body/ItemId']}` |

从输出中取 `{Link}` 或 `{Path}` 字段构造 SharePoint 访问链接。

### 动作 5.2 — （可选）清理 Temp 目录中的 HTML 文件

| 字段 | 值 |
|------|----|
| 动作类型 | SharePoint → `Delete file` |
| Site Address | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| File Identifier | `@{outputs('Create_HTML_file')?['body/ItemId']}` |

### 动作 5.3 — 发送 Teams 消息

| 字段 | 值 |
|------|----|
| 动作类型 | Microsoft Teams → `Post message in a chat or channel` |
| Post as | Flow bot 或 User |
| Post in | Channel |
| Team | `POC项目组`（选择你的 Team） |
| Channel | `poc-validator`（选择目标频道） |
| Message | 见下方 |

**消息内容（Adaptive Card 格式或 HTML 格式）**：

```
✅ POC Offshoring Validation 完成

📋 **Batch ID**：@{variables('varBatchID')}
📅 **Run Date**：@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')} (UTC)
📊 **结果**：总计 @{variables('varTotalRows')} 行 | 通过 @{variables('varPassCount')} 行 | 问题 @{variables('varIssueCount')} 行 | 通过率 @{if(equals(variables('varTotalRows'), 0), '0%', concat(string(div(mul(variables('varPassCount'), 100), variables('varTotalRows'))), '%'))}

📄 [查看 Excel 报告](@{outputs('Get_Excel_file_properties')?['body/{Link}']})
📑 [查看 PDF 报告](@{outputs('Get_PDF_file_properties')?['body/{Link}']})
```

---

## 9. PDF 转换连接器选型指南

| 方案 | 连接器 | 是否 Premium | 适用场景 | 备注 |
|------|--------|-------------|---------|------|
| **✅ 推荐**：HTML → OneDrive for Business `Convert file` | OneDrive for Business | **否** | 所有 M365 用户 | 支持 HTML → PDF，格式简洁，无额外费用 |
| SharePoint `Convert file` | SharePoint | **否** | 文件已在 SharePoint 时 | 部分租户可能未启用，可用 OneDrive 替代 |
| Word → PDF（Export）| Word Online (Business) | **否** | 有 Word 模板时 | 先生成 Word 文件，再用 `Convert` 导出 PDF |
| Adobe PDF Services | Adobe PDF Services | **是（Premium）** | 复杂排版需求 | 需要额外购买 |
| Encodian | Encodian | **是（Premium）** | 企业级 PDF 处理 | 功能最强，但有成本 |
| Muhimbi PDF | Muhimbi | **是（Premium）** | 历史老方案 | 现已被 Encodian 取代 |

### 推荐路径详解

```
[Compose HTML] → [SharePoint Create file (.html)] → [OneDrive for Business Convert file → PDF] → [SharePoint Create file (.pdf)]
```

**为什么选 HTML → Convert：**
- OneDrive for Business 的 `Convert file` 支持将 HTML 文件转换为 PDF
- 不需要任何 Premium 连接器
- HTML 可以完全自定义样式（表格、颜色、分页等）
- 生成的 PDF 文件直接存入 SharePoint

### 备用方案（如 OneDrive Convert 不可用）

某些租户或许可证可能限制了 `Convert file` 功能。按以下顺序尝试：

1. **尝试 SharePoint `Convert file`**：部分版本的 SharePoint 连接器也支持文件转换
2. **使用 Word 模板方式**：
   - 创建 `.docx` 模板，用 `Populate a Microsoft Word template`（需 Premium）或 HTTP + Graph API 填充
   - 再用 Word Online `Convert` 动作导出 PDF
3. **使用 Graph API 直接转换**（免 Premium）：
   ```
   GET https://graph.microsoft.com/v1.0/sites/<siteId>/drives/<driveId>/items/<htmlFileId>/content?format=pdf
   ```
   在 HTTP 动作中调用此端点，返回内容即为 PDF 二进制流。

---

## 10. 权限最小集

### 共享账户需要的权限

| 服务 | 所需权限 | 说明 |
|------|---------|------|
| SharePoint（POC-Validator 目录） | Edit（读写） | 读输入文件、创建输出文件 |
| SharePoint（整个站点） | Read | 选择站点/文档库时需要 |
| Microsoft Teams（目标频道） | Member（成员） | 发送频道消息 |
| Excel Online | 跟随 SharePoint 权限 | 读取 tblOffshoring、写入 tblIssues |
| Power Platform 环境 | Environment Maker 或更高 | 创建和运行 Flow |
| Copilot Studio Agent | User（使用者） | 调用 AI 验证 Agent |

### 配置步骤

1. 在 SharePoint 站点管理员页面，将共享账户添加为 **POC-Validator 文件夹的 Edit 权限**（继承或单独设置）
2. 在 Teams 管理中心，将共享账户添加为目标 Team 的成员
3. 在 Power Platform Admin Center → Environments，检查共享账户角色
4. 在 Copilot Studio → 你的 Agent → 设置 → 安全，确认共享账户有使用权限

---

## 11. 常见问题排查

### Q1：动作里找不到 SharePoint 站点

**原因**：连接使用了个人账户，或共享账户没有站点访问权限。

**解决**：
1. 打开 Power Automate → Data → Connections
2. 找到 SharePoint 连接，点击 ··· → Edit
3. 用共享账户重新登录
4. 回到 Flow，刷新动作的 Site Address 下拉框

---

### Q2：Excel Online 读取 tblOffshoring 只返回 256 行

**原因**：未开启 Pagination（分页）。

**解决**：在 `List rows present in a table` 动作的 Settings（⚙️）中：
- 打开 **Pagination**
- Threshold 设置为 `5000`（或你的最大预期行数）

---

### Q3：OneDrive for Business `Convert file` 返回错误

**可能原因**：
- 文件不在 OneDrive/SharePoint 路径中（Convert 需要先有文件 ID）
- 租户 DLP 策略阻止了转换操作
- HTML 文件编码问题

**解决**：
1. 确认动作 4.2（Create HTML file）成功，且输出了有效的文件 `Id`
2. 在动作 4.3 的 File 字段，使用动态内容选择步骤 4.2 的输出：`@{outputs('Create_HTML_file')?['body/Id']}`（注意是 `Id`，而非 `ItemId`）
3. 若 DLP 阻止，联系租户管理员将 OneDrive for Business 连接器加白名单
4. 若 HTML 编码有问题，在 Create file 的 File Content 字段改用：`@{base64ToBinary(base64(string(outputs('Compose_HTML'))))}`
5. **备用**：改用 Graph API 方案（见第 9 节）

---

### Q4：Teams 消息中的链接点击后"没有权限"

**原因**：SharePoint 文件链接需要接收者有对应文件/文件夹的访问权限。

**解决**：
- 在 Teams 频道所属的 Team 权限设置中，确保所有成员能访问 SharePoint 站点
- 或者在 SharePoint 创建文件后，用 `Grant access to an item or a folder` 动作共享链接

---

### Q5：Copilot Studio Agent 调用超时

**原因**：Direct Line API 调用需要轮询等待回复，默认轮询间隔可能不够。

**解决**：
1. 在 HTTP 获取回复动作后加 `Delay` 动作（等待 2-5 秒）
2. 再调用获取 activities 端点
3. 若 Agent 处理时间不稳定，可改为 Webhook 回调模式（复杂度更高，按需选择）

---

### Q6：Apply to each 循环处理 tblOffshoring 行太慢

**原因**：每行串行调用 Copilot Studio API，行数多时总时间很长。

**解决**：
1. 在 Apply to each 的 Settings（⚙️）中打开 **Concurrency Control**，并发数设为 5-10（视 API 限流而定）
2. 确保 varIssuesList 的 Append 操作线程安全（并发时建议用 Compose 合并而非 Append variable）

---

*最后更新：2026-03-20 | 版本：1.0*
