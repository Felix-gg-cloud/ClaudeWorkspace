# Power Automate 操作手顺（逐动作级别）— POC-Validator

**触发方式**：手动触发  
**AI 引擎**：Copilot Studio（Agent/Action）  
**存储位置（POC 阶段）**：个人 OneDrive — `Documents/PowerbiTest/POC-Validator/`  
**通知方式**：Microsoft Teams

---

## 前置准备

在开始搭建 Flow 之前，确认以下内容已就绪：

| 准备事项 | 说明 |
|---------|------|
| ✅ 输入文件 | `Master Excel_Strategy_OnOffshoring.xlsx` 已在 OneDrive `Documents/PowerbiTest/` |
| ✅ Excel 模板 | `ValidationReportTemplate.xlsx` 已上传至 OneDrive `Documents/PowerbiTest/POC-Validator/Templates/` |
| ✅ 输出文件夹 | 已在 OneDrive 创建 `Documents/PowerbiTest/POC-Validator/Outputs/Excel/`、`Outputs/PDF/`、`Outputs/JSON/` |
| ✅ Copilot Studio | AI Agent 已发布，并已获得 Power Automate 连接权限 |
| ✅ Teams 频道 | 确认发送通知的 Teams 团队 / 频道名称 |

---

## 总流程图

```
[手动触发]
    │
    ▼
[Step 0] 初始化变量
    │
    ▼
[Step 1] 读取 Excel tblOffshoring（机械读取，不清洗）
    │
    ▼
[Step 2] 组装 AI 输入 payload（加 RowIndex）
    │
    ▼
[Step 3] 调用 Copilot Studio AI 校验
    │
    ▼
[Step 4a] 保存 JSON（审计，可选）→ Outputs/JSON/
    │
    ▼
[Step 4b] 复制 Excel 模板 → 写 Summary + Issues → Outputs/Excel/
    │
    ▼
[Step 5] 生成 PDF 报告 → Outputs/PDF/
    │
    ▼
[Step 6] Teams 通知（附 Excel + PDF 链接）
```

---

## Step 0：手动触发 + 初始化变量

### 0-A 手动触发（Manually trigger a flow）

在"**手动触发流**"动作中，添加以下输入：

| 输入参数名 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| `BatchYearMonth` | 文本 | ✅ | 本次校验的年月，格式 `YYYYMM`，如 `202603` |
| `TopNIssuesInPDF` | 数字 | 否 | PDF 中展示的最多 Issue 数，默认 `200` |

### 0-B 初始化变量（Initialize variable）

依次添加以下"初始化变量"动作：

| 动作序号 | 变量名 | 类型 | 初始值 |
|---------|-------|------|--------|
| 变量 1 | `YearMonth` | 字符串 | `@{triggerBody()['text']}` （即 BatchYearMonth 输入） |
| 变量 2 | `Timestamp` | 字符串 | `@{formatDateTime(utcNow(), 'yyyyMMdd-HHmm')}` |
| 变量 3 | `RowIndex` | 整数 | `0` |
| 变量 4 | `RowsArray` | 数组 | `[]` |
| 变量 5 | `ExcelFileUrl` | 字符串 | （留空，Step 4b 写入） |
| 变量 6 | `PDFFileUrl` | 字符串 | （留空，Step 5 写入） |

---

## Step 1：读取 Excel 表（机械读取）

### 动作：List rows present in a table（Excel Online - Business）

> ⚠️ **关键原则**：不做任何 trim / replace / toLower / 格式化。所有原始值原样传给 AI。

| 配置字段 | 值 | 说明 |
|---------|-----|------|
| **Location** | `OneDrive for Business` | POC 阶段使用个人 OneDrive |
| **Document Library** | `OneDrive` | |
| **File** | `/Documents/PowerbiTest/Master Excel_Strategy_OnOffshoring.xlsx` | 输入文件路径 |
| **Table** | `tblOffshoring` | 固定表名 |
| **Top Count** | （留空） | 读取全部行 |

**高级选项（展开）**：

| 配置字段 | 值 |
|---------|-----|
| Skip Count | `0` |
| Select Columns | （留空，读取全部列） |

> 💡 如果行数超过 256，需在 Flow 设置中开启分页（Pagination），阈值建议设为 `5000`。  
> 路径：Flow → 设置（⚙️）→ Pagination → 开启 → 阈值 `5000`

---

## Step 2：组装 AI 输入 Payload

### 2-A：Apply to each（遍历每一行，加 RowIndex）

**输入**：上一步 `value`（Excel 行数组）

**循环内操作**：

1. **递增 RowIndex 变量**  
   动作：`Increment variable`  
   - 名称：`RowIndex`  
   - 值：`1`

2. **追加到 RowsArray**  
   动作：`Append to array variable`  
   - 名称：`RowsArray`  
   - 值（JSON 表达式）：
   ```
   @{addProperty(items('Apply_to_each'), 'RowIndex', variables('RowIndex'))}
   ```

   > 💡 `addProperty` 会在原始行对象基础上附加 `RowIndex` 字段，其余所有列保持原样（不修改）。

### 2-B：Compose（构建最终 payload）

动作：`Compose`  
**输入（JSON 表达式）**：

```json
{
  "meta": {
    "source_file_name": "Master Excel_Strategy_OnOffshoring.xlsx",
    "table_name": "tblOffshoring",
    "generated_at": "@{utcNow()}",
    "batch_id": "@{concat(variables('YearMonth'), '_001')}"
  },
  "ruleset_version": "poc-v1",
  "rows": @{variables('RowsArray')}
}
```

---

## Step 3：调用 Copilot Studio AI 校验

### 动作：Run a prompt（Copilot Studio — Actions）

> 使用 Power Automate 的"**Microsoft Copilot Studio**"连接器，调用已发布的 Agent Action。

| 配置字段 | 值 | 说明 |
|---------|-----|------|
| **Agent** | `POC-Validator-Agent`（你的 Agent 名称） | 在下拉中选择 |
| **Action** | `ValidateOffshoringData`（你定义的 Action 名） | 在下拉中选择 |
| **Input: payload** | `@{outputs('Compose')}` | 上一步 Compose 的输出 |

**返回值**：AI 返回 JSON 字符串（见 [`ai-contract.md`](./ai-contract.md) §2）

### 3-A：解析 AI 返回（Parse JSON）

动作：`Parse JSON`

| 配置字段 | 值 |
|---------|-----|
| **Content** | `@{body('Run_a_prompt')?['text']}` （根据实际 Action 返回字段名调整） |
| **Schema** | 点击"从示例生成"，粘贴 `ai-contract.md` §2 中的完整输出 JSON 示例 |

---

## Step 4a：保存 JSON（审计，可选）

### 动作：Create file（OneDrive for Business）

| 配置字段 | 值 |
|---------|-----|
| **Location** | `OneDrive for Business` |
| **Folder Path** | `/Documents/PowerbiTest/POC-Validator/Outputs/JSON` |
| **File Name** | `ValidationResult_@{variables('YearMonth')}_@{variables('Timestamp')}.json` |
| **File Content** | `@{body('Run_a_prompt')?['text']}` |

---

## Step 4b：生成 Excel 报告

### 4b-1：复制模板文件（Copy file）

动作：`Copy file`（OneDrive for Business）

| 配置字段 | 值 | 说明 |
|---------|-----|------|
| **Location** | `OneDrive for Business` | |
| **File to Copy** | `/Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx` | **模板路径**（固定，不修改） |
| **Destination File Path** | `/Documents/PowerbiTest/POC-Validator/Outputs/Excel/ValidationReport_@{variables('YearMonth')}_@{variables('Timestamp')}.xlsx` | **输出路径** |
| **Overwrite** | `Yes` | 避免同名冲突 |

> 📌 复制完成后，新文件路径格式为：  
> `Documents/PowerbiTest/POC-Validator/Outputs/Excel/ValidationReport_202603_20260320-1001.xlsx`

### 4b-2：记录新文件 URL

动作：`Set variable`  
- 名称：`ExcelFileUrl`  
- 值：`@{body('Copy_file')?['webUrl']}`

### 4b-3：写入 Summary（Update a row — Summary Sheet）

对 Summary Sheet 中的每个关键单元格，使用"**Update a row in a table**"或"**Update cell**"动作依次写入：

> 下表按单元格地址列出（请逐一添加动作，或使用批量写入方案）：

| 动作 | 目标文件 | Sheet | 单元格/行 | 写入值 |
|------|---------|-------|-----------|-------|
| Update cell B3 | Outputs/Excel/ 中刚复制的新文件 | `Summary` | `B3` | `@{body('Parse_JSON')?['report_model']?['generated_at']}` |
| Update cell B4 | 同上 | `Summary` | `B4` | `@{body('Parse_JSON')?['report_model']?['source_file_name']}` |
| Update cell B5 | 同上 | `Summary` | `B5` | `@{body('Parse_JSON')?['report_model']?['table_name']}` |
| Update cell B6 | 同上 | `Summary` | `B6` | `@{body('Parse_JSON')?['report_model']?['batch_id']}` |
| Update cell B9 | 同上 | `Summary` | `B9` | `@{body('Parse_JSON')?['report_model']?['metrics']?['rows_total']}` |
| Update cell B10 | 同上 | `Summary` | `B10` | `@{body('Parse_JSON')?['report_model']?['metrics']?['issues_total']}` |
| Update cell B11 | 同上 | `Summary` | `B11` | `@{body('Parse_JSON')?['report_model']?['metrics']?['errors_total']}` |
| Update cell B12 | 同上 | `Summary` | `B12` | `@{body('Parse_JSON')?['report_model']?['metrics']?['warnings_total']}` |
| Update cell B13 | 同上 | `Summary` | `B13` | `@{body('Parse_JSON')?['report_model']?['metrics']?['pass_rate']}` |

**写入 Top Rules（A17:C21）**：

使用"Apply to each"遍历 `body('Parse_JSON')?['report_model']?['top_rules']`，再用"Add a row into a table"写入 Summary Sheet 中的对应表格区域（需在模板中预建名为 `tblTopRules` 的表格，列：RuleId / Severity / Count）。

**写入 Top Columns（A25:B29）**：类似上述方式，表格名 `tblTopColumns`，列：Column / Count。

### 4b-4：写入 Issues 明细（批量添加行）

动作：`Apply to each`  
**输入**：`@{body('Parse_JSON')?['issues']}`

循环内：动作 `Add a row into a table`

| 配置字段 | 值 |
|---------|-----|
| **Location** | `OneDrive for Business` |
| **Document Library** | `OneDrive` |
| **File** | （使用表达式指向 Step 4b-1 复制的新文件路径，可用变量 `ExcelFileUrl` 或直接拼接路径） |
| **Table** | `tblIssues` |
| **Severity** | `@{items('Apply_to_each_issues')?['Severity']}` |
| **RuleId** | `@{items('Apply_to_each_issues')?['RuleId']}` |
| **RowIndex** | `@{items('Apply_to_each_issues')?['RowIndex']}` |
| **YearMonth** | `@{items('Apply_to_each_issues')?['YearMonth']}` |
| **Cost Center Number** | `@{items('Apply_to_each_issues')?['Cost Center Number']}` |
| **Function** | `@{items('Apply_to_each_issues')?['Function']}` |
| **Team** | `@{items('Apply_to_each_issues')?['Team']}` |
| **Owner** | `@{items('Apply_to_each_issues')?['Owner']}` |
| **Column** | `@{items('Apply_to_each_issues')?['Column']}` |
| **RawValue** | `@{items('Apply_to_each_issues')?['RawValue']}` |
| **Message** | `@{items('Apply_to_each_issues')?['Message']}` |
| **FixSuggestion** | `@{items('Apply_to_each_issues')?['FixSuggestion']}` |
| **RowKey** | `@{items('Apply_to_each_issues')?['RowKey']}` |

---

## Step 5：生成 PDF 报告

### 5-A：构建 HTML 内容（Compose）

动作：`Compose`  
**输入（HTML 字符串，含动态表达式）**：

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <style>
    body { font-family: Arial, sans-serif; margin: 30px; color: #333; }
    h1 { color: #1a5276; }
    h2 { color: #2e86c1; border-bottom: 1px solid #aed6f1; padding-bottom: 4px; }
    table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }
    th { background: #2e86c1; color: white; padding: 8px; text-align: left; }
    td { padding: 6px 8px; border: 1px solid #ddd; }
    tr:nth-child(even) { background: #f2f9ff; }
    .error { color: #c0392b; font-weight: bold; }
    .warning { color: #e67e22; font-weight: bold; }
    .metric-box { display: inline-block; border: 1px solid #aed6f1; border-radius: 6px;
                  padding: 12px 20px; margin: 6px; text-align: center; }
    .metric-value { font-size: 2em; font-weight: bold; color: #1a5276; }
  </style>
</head>
<body>
  <h1>POC Data Validation Report</h1>
  <p><strong>源文件：</strong>@{body('Parse_JSON')?['report_model']?['source_file_name']}</p>
  <p><strong>表名：</strong>@{body('Parse_JSON')?['report_model']?['table_name']}</p>
  <p><strong>批次 ID：</strong>@{body('Parse_JSON')?['report_model']?['batch_id']}</p>
  <p><strong>生成时间：</strong>@{body('Parse_JSON')?['report_model']?['generated_at']}</p>

  <h2>校验指标摘要</h2>
  <div>
    <div class="metric-box">
      <div class="metric-value">@{body('Parse_JSON')?['report_model']?['metrics']?['rows_total']}</div>
      <div>总行数</div>
    </div>
    <div class="metric-box">
      <div class="metric-value error">@{body('Parse_JSON')?['report_model']?['metrics']?['errors_total']}</div>
      <div>Error</div>
    </div>
    <div class="metric-box">
      <div class="metric-value warning">@{body('Parse_JSON')?['report_model']?['metrics']?['warnings_total']}</div>
      <div>Warning</div>
    </div>
    <div class="metric-box">
      <div class="metric-value">@{mul(body('Parse_JSON')?['report_model']?['metrics']?['pass_rate'], 100)}%</div>
      <div>通过率</div>
    </div>
  </div>

  <h2>Top Rules（命中最多的规则）</h2>
  <table>
    <tr><th>RuleId</th><th>Severity</th><th>命中次数</th></tr>
    <!-- Power Automate: Apply to each top_rules, append <tr> rows here -->
    @{join(body('Parse_JSON')?['report_model']?['top_rules'], '')}
  </table>

  <h2>Top Columns（问题最多的列）</h2>
  <table>
    <tr><th>Column</th><th>命中次数</th></tr>
    @{join(body('Parse_JSON')?['report_model']?['top_columns'], '')}
  </table>

  <h2>问题明细（Top @{triggerBody()['number']} 条）</h2>
  <p><em>完整明细请查看 Excel 报告。</em></p>
  <table>
    <tr>
      <th>Severity</th><th>RuleId</th><th>RowIndex</th><th>YearMonth</th>
      <th>Cost Center Number</th><th>Function</th><th>Team</th><th>Owner</th>
      <th>Column</th><th>RawValue</th><th>Message</th><th>修复建议</th>
    </tr>
    <!-- PA: Apply to each (first N items of issues[]), append <tr> rows -->
  </table>

  <hr/>
  <p style="font-size:0.85em;color:#888;">
    本报告由 Power Automate + Copilot Studio 自动生成。
    如有疑问请联系数据责任人。
  </p>
</body>
</html>
```

> 💡 **Top N 截断**：在"Apply to each"之前，使用 `first(body('Parse_JSON')?['issues'], triggerBody()['number'])` 取前 N 条（`triggerBody()['number']` 即触发参数 `TopNIssuesInPDF`）。

### 5-B：将 HTML 转换为 PDF

**推荐方案**：使用 Power Automate 中"**Convert HTML to PDF**"动作（需要 Encodian、PDF4me 或 Adobe PDF 连接器之一）。

| 配置字段 | 值 |
|---------|-----|
| **HTML Content** | `@{outputs('Compose_HTML')}` |
| 其他参数 | 按连接器要求填写（页面大小：A4，方向：Portrait） |

**如无 PDF 连接器**（备选方案）：

将 HTML 存为 `.html` 文件到 OneDrive，提示用户手动用浏览器另存为 PDF，或使用 Microsoft Graph API 的打印端点。

### 5-C：保存 PDF 到 OneDrive

动作：`Create file`（OneDrive for Business）

| 配置字段 | 值 |
|---------|-----|
| **Location** | `OneDrive for Business` |
| **Folder Path** | `/Documents/PowerbiTest/POC-Validator/Outputs/PDF` |
| **File Name** | `ValidationReport_@{variables('YearMonth')}_@{variables('Timestamp')}.pdf` |
| **File Content** | PDF 转换动作的输出内容（二进制） |

### 5-D：记录 PDF 文件 URL

动作：`Set variable`  
- 名称：`PDFFileUrl`  
- 值：`@{body('Create_file_PDF')?['webUrl']}`

---

## Step 6：Teams 通知（附 OneDrive 文件链接）

### 动作：Post message in a chat or channel（Microsoft Teams）

| 配置字段 | 值 |
|---------|-----|
| **Post as** | `Flow bot` 或 `User`（根据权限选择） |
| **Post in** | `Channel` |
| **Team** | （选择你的 Teams 团队） |
| **Channel** | （选择发送通知的频道） |

**Message（支持 HTML/Adaptive Card 格式）**：

```html
<b>✅ POC 数据校验完成</b><br/>
<br/>
<b>批次：</b>@{variables('YearMonth')}<br/>
<b>生成时间：</b>@{variables('Timestamp')}<br/>
<b>源文件：</b>Master Excel_Strategy_OnOffshoring.xlsx<br/>
<br/>
📊 <b>校验结果摘要</b><br/>
• 总行数：@{body('Parse_JSON')?['report_model']?['metrics']?['rows_total']}<br/>
• ❌ Error：@{body('Parse_JSON')?['report_model']?['metrics']?['errors_total']}<br/>
• ⚠️ Warning：@{body('Parse_JSON')?['report_model']?['metrics']?['warnings_total']}<br/>
• ✅ 通过率：@{mul(body('Parse_JSON')?['report_model']?['metrics']?['pass_rate'], 100)}%<br/>
<br/>
📎 <b>报告文件（OneDrive）</b><br/>
• <a href="@{variables('ExcelFileUrl')}">📗 Excel 明细报告（可筛选，适合数据修复）</a><br/>
• <a href="@{variables('PDFFileUrl')}">📄 PDF 摘要报告（适合阅读与归档）</a><br/>
<br/>
<i>文件位置：OneDrive → Documents/PowerbiTest/POC-Validator/Outputs/</i>
```

> 📌 **链接说明**：  
> - `ExcelFileUrl` 和 `PDFFileUrl` 均指向 OneDrive 中 `Documents/PowerbiTest/POC-Validator/Outputs/` 下相应子目录中的文件。  
> - 收件人需要有对应 OneDrive 文件的访问权限（POC 阶段可手动分享链接，或设置文件夹共享）。

---

## 常见问题

| 问题 | 原因 | 解决方法 |
|------|------|---------|
| Excel 读取行数截断为 256 | 未开启分页 | Flow 设置 → Pagination → 开启，阈值 5000 |
| AI 返回 Markdown 而非 JSON | Copilot Studio 提示词未约束输出格式 | 在 System Prompt 中明确"只输出 JSON，不输出其他内容" |
| 复制模板失败（文件不存在） | 模板尚未上传 | 手动将 `ValidationReportTemplate.xlsx` 上传至 `Documents/PowerbiTest/POC-Validator/Templates/` |
| PDF 转换动作不可用 | 未安装 PDF 连接器 | 在 Power Automate 中添加 Encodian 或 PDF4me 连接器（需许可证） |
| Teams 消息链接无法打开 | 权限问题 | 右键 OneDrive 文件 → 共享 → 复制链接（组织内可查看） |

---

## 后续迁移到 SharePoint（正式阶段）

迁移时只需改动 Flow 中以下字段，业务逻辑不需修改：

| 动作 | POC（OneDrive）→ 正式（SharePoint） |
|------|-------------------------------------|
| Step 1：读取 Excel | Location 改为 `SharePoint`，填入站点 URL 和文档库 |
| Step 4a/4b/5：保存文件 | Location 改为 `SharePoint`，路径改为 SharePoint 对应目录 |
| Teams 消息 | 链接自动变为 SharePoint 链接（无需手动修改） |
