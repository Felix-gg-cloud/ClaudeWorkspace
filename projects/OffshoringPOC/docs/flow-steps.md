# Power Automate 流程步骤详解

## 流程概览

**流程名称**：`Offshoring Data Validation - Manual Trigger`  
**触发方式**：手动（`Manually trigger a flow`）  
**预计耗时**：1-3 分钟（取决于数据行数与 AI 响应速度）

```
手动触发
  └─→ [1] 初始化变量
  └─→ [2] 从 OneDrive 读取 tblOffshoring 所有行
  └─→ [3] 批量发送给 AI 校验（含规则 + 原始行数据）
  └─→ [4] 解析 AI 返回的 issues JSON
  └─→ [5] 生成 Excel 报告（写入 SharePoint）
  └─→ [6] 生成 PDF 报告（写入 SharePoint）
  └─→ [7] 发送 Teams 通知（含报告链接）
```

---

## 详细步骤

### 步骤 1：手动触发（Trigger）

**动作**：`Manually trigger a flow`

**输入参数**（可选，在触发时由用户填写）：

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `YearMonth` | 文本 | 否 | 空（AI 从数据中读取） | 用于报告文件命名；如为空则从数据第一行提取 |
| `TopN` | 数字 | 否 | 20 | PDF 报告中展示的最大问题数 |

---

### 步骤 2：初始化变量

**动作**：`Initialize variable`（重复多次，分别初始化以下变量）

| 变量名 | 类型 | 初始值 | 说明 |
|--------|------|--------|------|
| `varYearMonth` | 文本 | `triggerBody()?['YearMonth']` 或空 | 报告月份 |
| `varTopN` | 整数 | `triggerBody()?['TopN']` 或 20 | PDF Top N 数量 |
| `varTimestamp` | 文本 | `formatDateTime(utcNow(), 'yyyyMMddTHHmm')` | 文件命名时间戳 |
| `varReportBaseName` | 文本 | 空（后续赋值） | 报告文件基础名 |
| `varIssues` | 数组 | `[]` | 存放 AI 返回的 issues |
| `varRawRows` | 数组 | `[]` | 存放原始行数据 |

---

### 步骤 3：从 OneDrive 读取数据

**动作**：`List rows present in a table`  
**连接器**：`Excel Online (OneDrive for Business)`

| 属性 | 值 |
|------|----|
| 位置 | OneDrive for Business |
| 文档库 | （用户 OneDrive 根目录） |
| 文件 | `/Offshoring/tblOffshoring.xlsx` |
| 表格 | `tblOffshoring` |

**注意**：
- Power Automate **不做任何数据处理**（不 trim、不转换、不过滤）
- 原始值（含前后空格）直接传递给 AI
- 若文件或表名不存在，流程会在此步骤失败并发送错误通知

**后续动作**：`Set variable varRawRows` = `body('List_rows_present_in_a_table')?['value']`

---

### 步骤 4：构建 AI 请求并发送（分批处理）

> 如果行数 ≤ 200，可以一次性发送；如果行数 > 200，需按批次发送。

#### 步骤 4a：判断是否需要分批

**动作**：`Condition`

```
length(varRawRows) > 200
  → Yes：进入"Apply to each batch"循环（每批 200 行）
  → No：直接进入单次 AI 调用
```

#### 步骤 4b：构建 AI 请求体

**动作**：`Compose`

将以下内容合并为 AI 输入（JSON 格式的字符串）：

```json
{
  "rules_summary": "<从 prompts/validation-prompt.md 复制规则摘要>",
  "data": <当前批次的行数组（原始值，不做处理）>
}
```

**注意**：`rules_summary` 为固定文本，直接写入流程的 `Compose` 动作；`data` 为动态值。

#### 步骤 4c：调用 Copilot Studio / Azure OpenAI

**动作**：根据配置选择以下之一：
- `HTTP` 动作调用 Azure OpenAI Chat Completion API
- `Copilot Studio` 连接器（如果已部署自定义 Copilot）

**请求示例（Azure OpenAI）**：

```
POST https://<resource>.openai.azure.com/openai/deployments/<model>/chat/completions?api-version=2024-02-01

Headers:
  api-key: <Azure OpenAI Key>（存储在 Key Vault 或 PA 环境变量中）
  Content-Type: application/json

Body:
{
  "messages": [
    { "role": "system", "content": "<系统提示词，来自 prompts/validation-prompt.md>" },
    { "role": "user", "content": "<用户消息，包含规则 + 当前批次行数据>" }
  ],
  "temperature": 0,
  "max_tokens": 4096,
  "response_format": { "type": "json_object" }
}
```

**AI 响应参数设置**：
- `temperature: 0`（最大确定性，减少随机性）
- `response_format: json_object`（强制 JSON 输出，防止乱输文字）

#### 步骤 4d：提取并合并 issues

**动作**：`Parse JSON` → 解析 AI 返回的 JSON  
**Schema**：来自 `schema/validation-output-schema.json`

**动作**：`Append to array variable varIssues`  
将本批次 issues 合并到全局 issues 数组。

---

### 步骤 5：确认 YearMonth（如用户未输入）

**动作**：`Condition`

```
empty(varYearMonth)
  → Yes：从 varRawRows 第一行的 YearMonth 字段赋值
         Set variable varYearMonth = first(varRawRows)?['YearMonth']
  → No：保持不变
```

**动作**：`Set variable varReportBaseName`

```
ValidationReport_@{variables('varYearMonth')}_@{variables('varTimestamp')}
```

---

### 步骤 6：生成 Excel 报告

**动作**：`Copy file`（复制 SharePoint 上的 Excel 模板文件）
- 源：`/Reports/Offshoring/_Template/ValidationReport_Template.xlsx`
- 目标：`/Reports/Offshoring/ValidationReport_@{variables('varYearMonth')}_@{variables('varTimestamp')}.xlsx`

**动作**：`Run script`（Office Script，或使用"Update a row"逐行写入）
- 写入 Summary Sheet（汇总统计）
- 写入 Issues Sheet（全部 issues）
- 写入 RawData Sheet（原始行数据）

> 详细 Excel 格式见 [`docs/report-generation.md`](report-generation.md)

---

### 步骤 7：生成 PDF 报告

**方案（Word 模板 + 转换）**：

1. **动作**：`Get file content`（获取 SharePoint 上的 Word 模板）
   - 路径：`/Reports/Offshoring/_Template/ValidationReport_PDF_Template.docx`

2. **动作**：`Populate a Microsoft Word template`
   - 填充占位符：校验月份、生成时间、统计数据、Top N issues 列表
   - Top N = 按 Error 优先 → R-WS 优先 → RowIndex 升序排序后取前 `varTopN` 条

3. **动作**：`Convert file`
   - 输入：上一步生成的 Word 文件内容
   - 目标格式：PDF

4. **动作**：`Create file`（将 PDF 写入 SharePoint）
   - 路径：`/Reports/Offshoring/ValidationReport_@{variables('varYearMonth')}_@{variables('varTimestamp')}.pdf`

---

### 步骤 8：发送 Teams 通知

**动作**：`Post message in a chat or channel`（Teams 连接器）

**频道**：由流程变量 `varTeamsChannelId` 配置（环境变量）

**消息内容**：

```
📊 Offshoring 数据校验完成

月份：@{variables('varYearMonth')}
总行数：@{length(varRawRows)}
Error 数：@{...}    Warning 数：@{...}
通过率：@{...}%

@{if(errorCount > 0, '❌ 发现问题，请查阅报告并修复后重新提交', '✅ 本月数据校验通过')}

📄 Excel 报告（完整明细）：@{excelSharePointLink}
📑 PDF 摘要（Top @{variables('varTopN')}）：@{pdfSharePointLink}
```

---

### 步骤 9：错误处理（Configure run after）

在步骤 3（读取 OneDrive）、步骤 4c（AI 调用）上配置 `Configure run after`：
- 失败时：发送 Teams 消息 "⚠️ 校验流程出错，请联系流程负责人。错误信息：@{outputs(...)}"
- 超时时：同上

---

## 流程变量/环境变量清单

| 变量名 | 存储位置 | 用途 |
|--------|----------|------|
| `AZURE_OPENAI_KEY` | PA 环境变量（加密） | Azure OpenAI API Key |
| `AZURE_OPENAI_ENDPOINT` | PA 环境变量 | Azure OpenAI 端点 URL |
| `SHAREPOINT_SITE_URL` | PA 环境变量 | SharePoint 站点地址 |
| `TEAMS_CHANNEL_ID` | PA 环境变量 | 通知目标 Teams 频道 ID |
| `ONEDRIVE_FILE_PATH` | PA 环境变量 | tblOffshoring.xlsx 路径 |
