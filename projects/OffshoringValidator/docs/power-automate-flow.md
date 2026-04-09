# Power Automate 流程步骤说明

> **项目**：Offshoring POC 校验器  
> **版本**：v1.0  
> **最后更新**：2025-01  

---

## 流程概览

```
[触发器] 手动/定时
    ↓
[步骤 1] 读取 tblOffshoring（机械原样读取）
    ↓
[步骤 2] 构建输入 JSON（拼接提示词 + 行数据）
    ↓
[步骤 3] 调用 Copilot Studio（AI 校验）
    ↓
[步骤 4] 解析 AI JSON 输出
    ↓
[步骤 5A] 生成 Excel 报告（.xlsx）
[步骤 5B] 生成 PDF 报告（.pdf）
    ↓
[步骤 6] 上传到 SharePoint/OneDrive
    ↓
[步骤 7] 发送通知（Teams 消息或邮件）
```

---

## 详细步骤

### 步骤 0：触发器配置

**推荐触发方式：**

| 触发方式 | 适用场景 | 配置要点 |
|----------|----------|----------|
| 手动触发（Manually trigger a flow） | POC / 按需运行 | 可添加输入参数（如 SharePoint 文件链接）|
| 计划触发（Recurrence） | 每月定期运行 | 建议月初 D+1 日凌晨 |
| 文件创建/修改触发（SharePoint → When a file is created or modified）| 数据上传后自动触发 | 监听指定 SharePoint 文档库 |

---

### 步骤 1：读取 tblOffshoring（关键：不做任何数据处理）

**使用连接器**：Excel Online (Business)  
**动作**：「List rows present in a table」

| 参数 | 值 |
|------|----|
| Location | SharePoint Site（或 OneDrive） |
| Document Library | 数据源库（如 Shared Documents） |
| File | tblOffshoring.xlsx 的文件路径 |
| Table | tblOffshoring |

**⚠️ 重要约束**：
- **不使用** `trim()`、`toLower()`、`replace()` 等任何文本处理函数
- **不跳过** 任何行（包括看起来是空行的行）
- 保留所有原始值，包括前后空格、特殊字符等
- 输出变量命名为 `varRawRows`

---

### 步骤 2：构建 AI 输入 JSON

**动作**：Compose（或 Initialize Variable）

**目标**：将原始行数据序列化为 JSON 字符串，并拼入调用时间戳。

```
输入格式（ROWS_JSON）：
[
  {
    "RowIndex": 1,
    "YearMonth": "<原始值>",
    "Cost Center Number": "<原始值>",
    "Function": "<原始值>",
    "Team": "<原始值>",
    "Owner": "<原始值>",
    "ShoringRatio": "<原始值>",
    "Actual Headcount Onshore": "<原始值>",
    "Actual Headcount Offshore": "<原始值>",
    "Planned Headcount Onshore": "<原始值>",
    "Planned Headcount Offshore": "<原始值>",
    "Target Headcount Onshore": "<原始值>",
    "Target Headcount Offshore": "<原始值>"
  },
  ...
]

元数据（METADATA_JSON）：
{
  "run_timestamp": "<utcNow() ISO 8601>",
  "total_rows": <行数>,
  "source_table": "tblOffshoring"
}
```

**Power Automate 表达式参考**：
```
varRowsJson = string(varRawRows['value'])
varMetaJson = concat('{"run_timestamp":"', utcNow(), '","total_rows":', string(length(varRawRows['value'])), ',"source_table":"tblOffshoring"}')
```

**大数据量分批处理**（若行数 > 200）：
- 使用「Apply to each」按每 200 行分批
- 或按 YearMonth 分组，每组单独调用 AI
- 最后合并所有批次的 `issues` 数组

---

### 步骤 3：调用 Copilot Studio（AI 校验）

**选项 A（推荐 POC）：使用 AI Builder — Text Generation**

| 参数 | 值 |
|------|----|
| 动作 | AI Builder → Create text with GPT using a prompt |
| Prompt | 粘贴 `templates/prompt-template.md` 中的完整提示词 |
| 动态内容 | 将提示词末尾的 `{{ROWS_JSON}}` 替换为步骤 2 的 `varRowsJson` |
| | 将 `{{METADATA_JSON}}` 替换为步骤 2 的 `varMetaJson` |
| Temperature | 0（最低随机性，确保输出一致） |

**选项 B：使用 Copilot Studio 自定义 Copilot**

| 参数 | 值 |
|------|----|
| 动作 | Copilot Studio → Send a message to Copilot |
| Copilot | 预先创建的「Offshoring Validator」Copilot |
| Message | 构建好的完整提示词字符串 |

**选项 C：使用 Azure OpenAI**

| 参数 | 值 |
|------|----|
| 动作 | Azure OpenAI → Get a response using a chat model |
| System Message | 提示词（不含数据部分）|
| User Message | `ROWS_JSON` + `METADATA_JSON` |
| Temperature | 0 |

**输出变量**：`varAiRawOutput`（字符串形式的 JSON）

---

### 步骤 4：解析 AI JSON 输出

**动作**：Parse JSON

| 参数 | 值 |
|------|----|
| Content | `varAiRawOutput`（或 `outputs('AI_Step')['text']`） |
| Schema | 使用 `schemas/ai-output-schema.json` 的内容 |

**容错处理**：
```
条件：if(startsWith(trim(varAiRawOutput), '{'), '合法 JSON', '非法输出')
  → 非法：发送错误通知邮件并终止流程
  → 合法：继续解析
```

**输出变量**：
- `varIssues` = `body('Parse_JSON')['issues']`
- `varReportModel` = `body('Parse_JSON')['report_model']`

---

### 步骤 5A：生成 Excel 报告

**动作序列**：

**5A-1：创建空白 Excel 文件（使用预置模板）**

```
动作：SharePoint → Create file
位置：输出 SharePoint 文档库
文件夹：ValidationReports/
文件名：concat('ValidationReport_', varReportModel['period'], '_', formatDateTime(utcNow(), 'yyyyMMdd-HHmmss'), '.xlsx')
文件内容：从模板文件读取的空白 .xlsx 二进制内容
```

**5A-2：运行 Office Script 写入 Summary**

```
动作：Excel Online (Business) → Run script
文件：步骤 5A-1 创建的文件
脚本：FillSummarySheet（预先在 Office Scripts 中创建）
参数：
  reportModelJson = string(varReportModel)
```

> 脚本内容见 `templates/excel-report-schema.md` 第 6 节

**5A-3：运行 Office Script 写入 Issues**

```
动作：Excel Online (Business) → Run script
文件：步骤 5A-1 创建的文件
脚本：FillIssuesSheet（预先在 Office Scripts 中创建）
参数：
  issuesJson = string(varIssues)
```

> 脚本内容见 `templates/excel-report-schema.md` 第 7 节

**输出变量**：`varExcelFileUrl`（SharePoint 文件 URL）

---

### 步骤 5B：生成 PDF 报告

**推荐方案（从 HTML 模板生成）**：

**5B-1：读取 HTML 模板**

```
动作：SharePoint → Get file content
文件：templates/html-report-template.html（存放在 SharePoint 模板库）
输出：varHtmlTemplate
```

**5B-2：替换占位符（构建最终 HTML）**

使用 Compose + `replace()` 函数链式替换所有 `{{占位符}}`：

```
replace(
  replace(
    replace(
      replace(
        replace(
          replace(varHtmlTemplate,
            '{{source_table}}', varReportModel['source_table']),
          '{{period}}', varReportModel['period']),
        '{{generated_at}}', formatDateTime(varReportModel['generated_at'], 'yyyy-MM-dd HH:mm:ss UTC')),
      '{{total_rows_checked}}', string(varReportModel['summary']['total_rows_checked'])),
    ... 继续替换其余占位符 ...
  )
)
```

`{{issues_by_rule_rows}}` 和 `{{issue_rows}}` 需用「Apply to each」循环拼接 HTML 字符串后再替换。

**5B-3：HTML 转 PDF**

| 方案 | 连接器 | 动作 | 说明 |
|------|--------|------|------|
| **推荐**：Encodian | Encodian | Convert HTML to PDF | 需 Encodian 订阅（有免费层）|
| 备选：Plumsail | Plumsail Documents | Convert HTML to PDF | 需 Plumsail 订阅 |
| 免费备选 | SharePoint | 保存 HTML 文件 → 通知用户打印为 PDF | 最简化，无需额外订阅 |

**5B-4：保存 PDF 文件**

```
动作：SharePoint → Create file
文件夹：ValidationReports/
文件名：concat('ValidationReport_', varReportModel['period'], '_', formatDateTime(utcNow(), 'yyyyMMdd-HHmmss'), '.pdf')
文件内容：5B-3 的 PDF 二进制输出
```

**输出变量**：`varPdfFileUrl`

---

### 步骤 6：上传确认与权限设置

**6-1：获取共享链接**

```
动作：SharePoint → Create sharing link for a file or folder（×2，分别为 Excel 和 PDF）
类型：Organization（组织内共享，不需要登录的外部访问）
角色：View（只读）
```

**6-2：文件夹结构建议**

```
SharePoint 文档库：OffshoringReports/
└── ValidationReports/
    └── 202501/               ← 按 YearMonth 归档
        ├── ValidationReport_202501_20250115-093000.xlsx
        └── ValidationReport_202501_20250115-093000.pdf
```

---

### 步骤 7：发送通知

**选项 A：Teams 消息**

```
动作：Microsoft Teams → Post message in a chat or channel
收件人/频道：Offshoring 数据管理员频道
消息内容（Adaptive Card 或普通消息）：
  📋 Offshoring 数据校验完成
  期间：{{period}}
  结果：{{PASSED ✅ / FAILED ❌}}
  Error 数：{{error_count}}  |  Warning 数：{{warning_count}}
  
  📥 [下载 Excel 报告]({{varExcelFileUrl}})
  📄 [查看 PDF 报告]({{varPdfFileUrl}})
```

**选项 B：邮件通知**

```
动作：Office 365 Outlook → Send an email (V2)
收件人：数据管理员邮件地址（或来自触发器的输入参数）
主题：concat('[Offshoring 校验] ', varReportModel['period'], ' - ', if(varReportModel['summary']['validation_passed'], '通过 ✅', '未通过 ❌'))
正文：HTML 格式，包含摘要数据与文件链接
附件（可选）：若 PDF 文件不大（< 25MB），可直接作为附件
```

---

## 变量汇总

| 变量名 | 类型 | 来源步骤 | 说明 |
|--------|------|----------|------|
| `varRawRows` | Array | 步骤 1 | Excel 原始行数据 |
| `varRowsJson` | String | 步骤 2 | 序列化的行 JSON |
| `varMetaJson` | String | 步骤 2 | 元数据 JSON |
| `varAiRawOutput` | String | 步骤 3 | AI 返回的原始文本 |
| `varIssues` | Array | 步骤 4 | 解析后的 issues 数组 |
| `varReportModel` | Object | 步骤 4 | 解析后的 report_model |
| `varExcelFileUrl` | String | 步骤 5A | Excel 文件 SharePoint URL |
| `varPdfFileUrl` | String | 步骤 5B | PDF 文件 SharePoint URL |

---

## 错误处理建议

| 错误场景 | 处理方式 |
|----------|----------|
| tblOffshoring 为空表（无数据行）| 提前检查行数，若为 0 则发通知"无数据"并终止 |
| AI 返回非 JSON 内容 | 条件判断 `startsWith('{')` → 失败则发错误通知 |
| AI 返回的 JSON 不含 issues/report_model | Parse JSON 出错时 → Catch + 发错误通知 |
| Excel 文件创建失败（权限/路径错误）| 使用 Scope + Configure run after → 发错误通知 |
| PDF 转换服务超时 | 设置超时 → 降级为只发 Excel + 通知 PDF 暂不可用 |
| Token 超限（行数过多）| 分批处理（见步骤 2 说明）|
