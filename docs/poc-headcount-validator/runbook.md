# POC Headcount Validator — 端到端操作手顺（Runbook）

> **适用场景**：Power Automate 手动触发流程，方案一（Copilot Studio AI-only 校验），POC 阶段。
>
> **报告交付物**：Excel (.xlsx) + PDF，存入 SharePoint；不生成 Markdown 文件。

---

## 目录

1. [前置条件与权限](#1-前置条件与权限)
2. [OneDrive 与 SharePoint 的使用边界](#2-onedrive-与-sharepoint-的使用边界)
3. [Step 1 — 手动触发](#step-1--手动触发)
4. [Step 2 — 读取 OneDrive Excel（tblOffshoring，带分页）](#step-2--读取-onedrive-exceltbloffshoring带分页)
5. [Step 3 — 组装行载荷（含 RowIndex）](#step-3--组装行载荷含-rowindex)
6. [Step 4 — 调用 Copilot Studio HTTP 端点](#step-4--调用-copilot-studio-http-端点)
7. [Step 5 — 接收并解析 AI 响应 JSON](#step-5--接收并解析-ai-响应-json)
8. [Step 6 — 生成 Excel 报告（模板填充）](#step-6--生成-excel-报告模板填充)
9. [Step 7 — 生成 PDF 报告](#step-7--生成-pdf-报告)
10. [Step 8 — 存储至 SharePoint](#step-8--存储至-sharepoint)
11. [Step 9 — （可选）Teams / Email 通知](#step-9--可选teams--email-通知)
12. [错误处理与重试策略](#错误处理与重试策略)
13. [从 OneDrive 迁移至 SharePoint 的路径](#从-onedrive-迁移至-sharepoint-的路径)

---

## 1. 前置条件与权限

| 资源 | 说明 | 权限要求 |
|------|------|----------|
| OneDrive（个人/商业） | 存放输入 Excel（tblOffshoring） | 流程账号需有读取权限 |
| SharePoint 站点 | 存放输出报告（Excel + PDF） | 流程账号需有上传/写入权限 |
| Copilot Studio 端点 | AI 校验 HTTP 端点 | 需要 Bot Framework Direct Line 或 Custom Connector 凭据 |
| Power Automate 环境 | Premium 连接器（SharePoint/HTTP） | 需 Per-Flow 或 Per-User Premium 授权 |
| Excel 报告模板 | `.xlsx` 模板文件（见 [excel-template-spec.md](./excel-template-spec.md)） | 存放在 SharePoint Templates 目录 |

---

## 2. OneDrive 与 SharePoint 的使用边界

### 当前 POC 阶段

```
OneDrive（个人/商业）
  └─ 用途：存放"待校验"的输入 Excel（tblOffshoring）
  └─ 原因：POC 快速启动，无需 IT 配置 SharePoint 站点；数据量小

SharePoint
  └─ 用途：存放"校验结果"报告（Excel + PDF）
  └─ 原因：报告是多人共享的交付物，SharePoint 更适合协作/权限管理
```

### 迁移至 SharePoint 的时机（生产阶段）

- 团队共享输入文件时（多人维护 tblOffshoring）
- 需要版本控制或审计追踪时
- 企业 IT 政策要求数据在 SharePoint 管理时

### 迁移操作步骤（仅修改 PA 连接器，业务逻辑不变）

1. 将 `tblOffshoring.xlsx` 上传至 SharePoint 目标文档库
2. 将流程中的 **OneDrive → 获取工作表行（分页）** 步骤替换为 **SharePoint → 获取工作表行（分页）**
3. 更新连接器配置（站点地址、文档库、文件路径）
4. 其余所有步骤（载荷组装、AI 调用、报告生成）**无需更改**

---

## Step 1 — 手动触发

**连接器**：`手动触发流（Manually trigger a flow）`

| 参数 | 类型 | 说明 |
|------|------|------|
| `RunLabel`（可选文本输入） | string | 本次运行的备注标签，用于报告文件名（例如 `"2025Q1-校验"`） |

**操作**：
1. 在 Power Automate 门户中，找到本流程并点击 **"运行"**。
2. 填写可选的 `RunLabel` 参数。
3. 点击确认，流程进入 Step 2。

> **注意**：POC 阶段为手动触发；生产阶段可改为定时触发（Recurrence）或 SharePoint 文件变更触发，无需更改后续步骤。

---

## Step 2 — 读取 OneDrive Excel（tblOffshoring，带分页）

**连接器**：`OneDrive for Business → 获取工作表行（分页）`
（或 `Excel Online (Business) → List rows present in a table`）

### 2.1 连接器配置

| 字段 | 值 |
|------|----|
| 位置 | `OneDrive for Business` |
| 文档库 | `OneDrive` |
| 文件 | `/Offshoring/tblOffshoring.xlsx`（实际路径按需调整） |
| 表 | `tblOffshoring` |
| 分页（Pagination） | **开启**，阈值设为 `5000`（或数据量上限） |

### 2.2 关键约束：**绝对不做 trim / 清洗**

> ⚠️ **Power Automate 中禁止使用 `trim()`、`replace()`、`toUpper()`、`toLower()` 等函数对单元格值做预处理。**
>
> 所有列的原始值（RawValue）必须原样传递给 AI，包括前后空格、特殊字符、空字符串等。
>
> 清洗逻辑**全部**由 Copilot Studio（AI）执行并在校验报告中体现。

### 2.3 分页处理逻辑

Power Automate `List rows present in a table` 默认返回最多 256 行。启用分页后：

```
设置 → 分页 → 开启
阈值（Threshold）= 5000（根据实际最大行数调整）
```

启用分页后，连接器会自动循环获取所有行，结果合并为一个数组，后续步骤无需手动处理分页。

### 2.4 输出变量

```
变量名：varAllRows
类型：array（每元素为一行的列名-值 map）
```

---

## Step 3 — 组装行载荷（含 RowIndex）

**操作类型**：`数据操作 → 选择（Select）` + `撰写（Compose）`

### 3.1 为每行注入 RowIndex

使用 `Apply to each` 循环，配合 `variables('varRowIndex')` 自增计数器（从 1 开始）：

```
初始化变量：varRowIndex = 0
Apply to each（items: varAllRows）:
  ├─ 设置变量：varRowIndex = add(variables('varRowIndex'), 1)
  └─ 追加到数组变量：varPayloadRows
       值（JSON 对象）：
       {
         "RowIndex": @{variables('varRowIndex')},
         "YearMonth": @{items('Apply_to_each')?['YearMonth']},
         "CostCenterNumber": @{items('Apply_to_each')?['Cost Center Number']},
         "Function": @{items('Apply_to_each')?['Function']},
         "Team": @{items('Apply_to_each')?['Team']},
         "Owner": @{items('Apply_to_each')?['Owner']},
         "ShoringRatio": @{items('Apply_to_each')?['ShoringRatio']},
         "ActualOnshore": @{items('Apply_to_each')?['Actual Onshore']},
         "ActualOffshore": @{items('Apply_to_each')?['Actual Offshore']},
         "ActualTotal": @{items('Apply_to_each')?['Actual Total']},
         "PlannedOnshore": @{items('Apply_to_each')?['Planned Onshore']},
         "PlannedOffshore": @{items('Apply_to_each')?['Planned Offshore']},
         "PlannedTotal": @{items('Apply_to_each')?['Planned Total']},
         "TargetOnshore": @{items('Apply_to_each')?['Target Onshore']},
         "TargetOffshore": @{items('Apply_to_each')?['Target Offshore']},
         "TargetTotal": @{items('Apply_to_each')?['Target Total']}
       }
```

> **重要**：列名使用 `?['列名']` 语法以避免列不存在时流程中断；值原样传递，**不调用任何字符串函数**。

### 3.2 组装最终请求载荷

使用 `撰写（Compose）` 动作：

```json
{
  "run_label": "@{triggerBody()?['text']}",
  "triggered_at": "@{utcNow()}",
  "rows": @{variables('varPayloadRows')}
}
```

输出变量：`varFinalPayload`

---

## Step 4 — 调用 Copilot Studio HTTP 端点

**连接器**：`HTTP`（Premium）或自定义连接器

### 4.1 连接器配置

| 字段 | 值 |
|------|----|
| 方法 | `POST` |
| URI | Copilot Studio Direct Line / 自定义端点 URL |
| 标头 `Content-Type` | `application/json` |
| 标头 `Authorization` | `Bearer @{variables('varCopilotToken')}` |
| 正文 | `@{outputs('Compose_FinalPayload')}` |

### 4.2 超时与重试

| 参数 | 建议值 |
|------|--------|
| 超时（Timeout） | `PT120S`（2 分钟，视数据量调整） |
| 重试策略 | `固定间隔`，重试 2 次，间隔 30 秒 |

### 4.3 关于认证（POC 阶段）

POC 阶段可使用 Copilot Studio 的 **Direct Line Secret** 作为 Bearer Token（明文配置在 PA 环境变量中）。生产阶段替换为 Azure AD App Registration + 托管标识。

---

## Step 5 — 接收并解析 AI 响应 JSON

**操作**：`数据操作 → 解析 JSON（Parse JSON）`

### 5.1 期望响应结构

```json
{
  "report_model": {
    "total_rows": 100,
    "error_count": 3,
    "warning_count": 5,
    "pass_count": 92,
    "generated_at": "2025-01-15T10:30:00Z"
  },
  "issues": [
    {
      "Severity": "Error",
      "RuleId": "R-WS-ALL-001",
      "RowKey": "RowIndex=3|YearMonth=202501|CCN=1001|Function=GBS |Team=EA",
      "Column": "Function",
      "RawValue": "GBS ",
      "Message": "列 Function 的值包含前后空格，不允许。",
      "FixSuggestion": "删除值开头/结尾的空格后重新提交。"
    }
  ]
}
```

### 5.2 Parse JSON Schema

```json
{
  "type": "object",
  "properties": {
    "report_model": {
      "type": "object",
      "properties": {
        "total_rows": { "type": "integer" },
        "error_count": { "type": "integer" },
        "warning_count": { "type": "integer" },
        "pass_count": { "type": "integer" },
        "generated_at": { "type": "string" }
      }
    },
    "issues": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "Severity": { "type": "string" },
          "RuleId": { "type": "string" },
          "RowKey": { "type": "string" },
          "Column": { "type": "string" },
          "RawValue": { "type": "string" },
          "Message": { "type": "string" },
          "FixSuggestion": { "type": "string" }
        }
      }
    }
  }
}
```

---

## Step 6 — 生成 Excel 报告（模板填充）

**连接器**：`Excel Online (Business)` 或 `OneDrive for Business`

### 6.1 流程

1. **复制 Excel 模板**：从 SharePoint Templates 目录将 `ValidationReport_Template.xlsx` 复制到 SharePoint Output 目录，重命名为：
   ```
   ValidationReport_YYYYMMDD_HHmm_<RunLabel>.xlsx
   ```
2. **填充汇总 Sheet（Summary）**：使用 `更新行（Update a row）` 或 `运行脚本（Run script）` 将 `report_model` 数据写入 `tblSummary` 表。
3. **填充明细 Sheet（Issues）**：循环 `issues[]`，使用 `在表中添加行（Add a row into a table）` 将每个 issue 追加到 `tblIssues` 表。

### 6.2 推荐方案：Office Scripts（Run script）

对于批量写入（issues 可能有数百行），推荐使用 **Office Scripts** 脚本一次性写入，避免每行触发一个 API 调用：

```
动作：Excel Online (Business) → 运行脚本（Run script）
脚本名：FillValidationReport
参数：
  - reportModel: @{body('Parse_JSON')?['report_model']}
  - issues: @{body('Parse_JSON')?['issues']}
```

> Office Scripts 实现详见 [excel-template-spec.md](./excel-template-spec.md)。

---

## Step 7 — 生成 PDF 报告

**方案**：Word 模板 → 填充 → 转换为 PDF

### 7.1 流程（使用 Word Online + PDF 转换）

1. **复制 Word 模板**：从 SharePoint Templates 目录将 `ValidationReport_Template.docx` 复制到临时目录，重命名为：
   ```
   ValidationReport_YYYYMMDD_HHmm_<RunLabel>_tmp.docx
   ```
2. **填充 Word 模板**（使用 `Word Online (Business) → Populate a Microsoft Word template`）：
   - 将 `report_model` 字段映射到模板占位符
   - 将 `issues[]` 重复区域（Repeating section）填充明细行
3. **转换为 PDF**（使用 `OneDrive for Business → 转换文件（Convert file）`）：
   - 格式选择 `PDF`
   - 输出文件存入 SharePoint Output 目录，命名为：
     ```
     ValidationReport_YYYYMMDD_HHmm_<RunLabel>.pdf
     ```
4. **删除临时 docx**（可选，保持目录整洁）。

### 7.2 备选方案：HTML → PDF（无需 Word 许可）

若无 Word Online 许可，可在流程中用 `撰写（Compose）` 生成 HTML 字符串，再通过第三方 PDF API（如 Adobe PDF Services、HTML2PDF.app）转换：

```
动作：HTTP POST → PDF API
正文：{ "html": "@{variables('varReportHTML')}", "filename": "ValidationReport_..." }
```

---

## Step 8 — 存储至 SharePoint

**连接器**：`SharePoint → 创建文件（Create file）`

### 8.1 Excel 文件

| 字段 | 值 |
|------|----|
| 站点地址 | `https://<tenant>.sharepoint.com/sites/<SiteName>` |
| 文件夹路径 | `/Documents/ValidationReports/@{formatDateTime(utcNow(), 'yyyy')}/@{formatDateTime(utcNow(), 'yyyyMM')}` |
| 文件名 | `ValidationReport_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmm')}_@{triggerBody()?['text']}.xlsx` |
| 文件内容 | Excel 文件的二进制内容 |

### 8.2 PDF 文件

| 字段 | 值 |
|------|----|
| 文件名 | `ValidationReport_@{formatDateTime(utcNow(), 'yyyyMMdd_HHmm')}_@{triggerBody()?['text']}.pdf` |
| 文件内容 | PDF 文件的二进制内容 |

> **目录结构详见** [sharepoint-folder-structure.md](./sharepoint-folder-structure.md)

---

## Step 9 — （可选）Teams / Email 通知

### 9.1 Teams 通知

**连接器**：`Microsoft Teams → 发布消息到频道（Post message in a chat or channel）`

```
频道：Headcount 校验通知
消息（Adaptive Card）：
  标题：✅ Offshoring 校验完成 — @{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm')}
  内容：
    - 总行数：@{body('Parse_JSON')?['report_model']?['total_rows']}
    - 错误：@{body('Parse_JSON')?['report_model']?['error_count']}
    - 警告：@{body('Parse_JSON')?['report_model']?['warning_count']}
    - 通过：@{body('Parse_JSON')?['report_model']?['pass_count']}
  按钮：[查看 Excel 报告](@{outputs('SharePoint_Excel_URL')})  [查看 PDF 报告](@{outputs('SharePoint_PDF_URL')})
```

### 9.2 Email 通知

**连接器**：`Office 365 Outlook → 发送电子邮件（Send an email）`

```
收件人：@{variables('varNotificationRecipients')}
主题：[Offshoring 校验] @{formatDateTime(utcNow(), 'yyyy-MM-dd')} — 错误 @{body('Parse_JSON')?['report_model']?['error_count']} 条
正文：（HTML，含汇总数字 + SharePoint 链接，不含 Markdown）
附件：（可选）Excel 报告文件
```

---

## 错误处理与重试策略

| 步骤 | 可能失败原因 | 处理方式 |
|------|-------------|----------|
| Step 2（读取 Excel） | 文件不存在、权限不足 | `Scope + Configure run after` → 发送失败通知 |
| Step 4（调用 Copilot Studio） | 超时、网络错误 | 配置重试策略（固定间隔，2 次） |
| Step 4（调用 Copilot Studio） | AI 返回非 JSON / 空响应 | 解析前检查 HTTP 状态码；失败时跳至通知步骤 |
| Step 6（写 Excel） | 模板不存在 | 提前检查模板文件存在；缺失时终止并通知 |
| Step 7（生成 PDF） | Word/PDF 服务异常 | 记录错误，仍保存 Excel（PDF 为可选交付） |
| Step 8（存储 SharePoint） | 权限不足、磁盘满 | 捕获错误，邮件通知管理员 |

---

## 从 OneDrive 迁移至 SharePoint 的路径

详见 [第 2 节 — OneDrive 与 SharePoint 的使用边界](#2-onedrive-与-sharepoint-的使用边界)。

迁移清单：
- [ ] 将 `tblOffshoring.xlsx` 上传至 SharePoint 文档库
- [ ] 修改 Step 2 连接器（OneDrive → SharePoint）
- [ ] 更新连接器配置中的站点/库/路径
- [ ] 测试完整流程一遍
- [ ] 归档 OneDrive 原文件（或删除）
