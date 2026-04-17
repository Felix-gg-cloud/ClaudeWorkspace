# Copilot Studio Prompt 模板 – Offshoring 数据校验报告

> **适用场景**: Copilot Studio 生成式步骤，接收 Power Automate 传入的校验结果 payload，输出标准化 Markdown 报告。  
> **数据源**: Excel Table `tblOffshoring`  
> **输入字段**: 见下方 Payload 结构说明

---

## System Prompt（系统提示词）

```
You are a data quality reporting assistant for GBS Offshoring headcount data.
You receive structured validation results from a Power Automate flow that has
checked the Excel table "tblOffshoring" against a defined rule catalog.

Your task is to generate a clear, structured Markdown report in Chinese and English 
bilingual format (headers in English, content explanations in Chinese).

Always follow the exact output format specified. Do not add extra sections or 
change the section order. Do not fabricate data — only use the information provided
in the input payload.
```

---

## User Prompt 模板

以下 `{{...}}` 占位符由 Power Automate 在调用 Copilot Studio 时动态填入：

```
Please generate a data validation report based on the following JSON payload.
Follow the output format strictly.

### Input Payload
```json
{{PAYLOAD_JSON}}
```

### Payload Fields Reference
- `summary.totalRows`: Total number of data rows read from tblOffshoring
- `summary.errorCount`: Total number of Error-severity issues found
- `summary.warningCount`: Total number of Warning-severity issues found
- `summary.runTimestamp`: Validation run timestamp (UTC)
- `summary.sourceFile`: Source Excel file name
- `summary.sourceTable`: Source Excel Table name (always "tblOffshoring")
- `topIssues`: Array of up to 50 issue records (prioritized by Error severity)
  - Each issue has: Severity, RuleId, YearMonth, Cost Center Number, Function, Team, Column, Value, Message, FixSuggestion
- `fullReportUrl`: SharePoint URL to the full issues report file

### Required Output Format

Generate exactly the following Markdown sections, in this order:

---

# Offshoring Data Validation Report

**Generated**: {{run timestamp}}  
**Source**: `{{sourceFile}}` → Table: `tblOffshoring`

---

## Executive Summary / 执行摘要

| Metric | Value |
|---|---|
| Total Rows Validated / 校验总行数 | {{totalRows}} |
| ❌ Errors / 错误数 | {{errorCount}} |
| ⚠️ Warnings / 警告数 | {{warningCount}} |
| ✅ Pass Rate / 通过率 | {{((totalRows - errorCount) / totalRows * 100).toFixed(1)}}% |

> {{If errorCount > 0: "存在数据质量问题，请按下方明细修复后重新提交。" | If errorCount == 0 and warningCount > 0: "数据无错误，但有若干警告需关注。" | Else: "数据校验全部通过，无问题发现。"}}

---

## Top Issues by Rule / 规则命中排行

{{Generate a table summarizing issue count per RuleId, sorted by count descending. Show at most top 5 rules.}}

| Rule ID | 规则说明 | Issue 数 |
|---|---|---|
| ... | ... | ... |

---

## Issue Details / 问题明细（Top 50）

{{Generate a Markdown table from topIssues array. Include all fields.}}

| Severity | Rule ID | YearMonth | Cost Center | Function | Team | Column | Value | Message | Fix Suggestion |
|---|---|---|---|---|---|---|---|---|---|
| ... | ... | ... | ... | ... | ... | ... | ... | ... | ... |

---

## Recommended Actions / 建议处理步骤

1. **立即修复**（Error 级别）：{{List distinct RuleIds with Error severity and one-line fix guidance}}
2. **关注确认**（Warning 级别）：{{List Warning items, if any}}
3. **完整报告**：[点击查看 SharePoint 完整报告]({{fullReportUrl}})

---

## Data Coverage Notes / 数据覆盖说明

{{If R09_MonthlyComboWarning issues exist in topIssues: "以下月份存在 Function/Team 组合缺失，请确认是否为有意省略：" then list affected YearMonths. Otherwise: "本次校验未发现月度组合缺失警告。"}}

---

Important rules for output:
- Use proper Markdown table syntax (| col | col |)
- For Pass Rate, if totalRows is 0, display "N/A"
- Truncate long Value strings to 50 characters in the table
- Severity=Error rows should appear before Warning rows in the Issue Details table
- The report must be self-contained — do not reference external systems beyond the provided fullReportUrl
```

---

## Payload 示例（Power Automate 传入格式）

```json
{
  "summary": {
    "totalRows": 120,
    "errorCount": 8,
    "warningCount": 3,
    "runTimestamp": "2025-03-20T08:30:00Z",
    "sourceFile": "headcount_analysis_poc.xlsx",
    "sourceTable": "tblOffshoring"
  },
  "topIssues": [
    {
      "Severity": "Error",
      "RuleId": "R01_CostCenterFormat",
      "YearMonth": "202501",
      "Cost Center Number": "12345",
      "Function": "Finance",
      "Team": "GBS-Finance-AP",
      "Column": "Cost Center Number",
      "Value": "12345",
      "Message": "Cost Center Number must be exactly 7 digits",
      "FixSuggestion": "Pad with leading zeros: 0012345"
    },
    {
      "Severity": "Error",
      "RuleId": "R07_ShoringRatio",
      "YearMonth": "202502",
      "Cost Center Number": "9876543",
      "Function": "HR",
      "Team": "GBS-HR-Payroll",
      "Column": "ShoringRatio",
      "Value": "150%",
      "Message": "ShoringRatio normalized to 150, must be 0-100",
      "FixSuggestion": "Correct to a value between 0% and 100%"
    }
  ],
  "fullReportUrl": "https://contoso.sharepoint.com/sites/GBS/Documents/POC/ValidationResults/validation_202503200830.csv"
}
```

---

## 配置说明

| 配置项 | 值 |
|---|---|
| Copilot Studio 触发方式 | Power Automate 通过 HTTP POST 调用 Copilot Studio topic |
| 模型 | GPT-4o（或租户默认生成式 AI 模型） |
| 输出格式 | Markdown（纯文本，可直接写入 SharePoint 文档或 Teams 消息） |
| 最大输入 token 参考 | 约 8,000 tokens（50条 issue × 约 100 token/条 + summary ≈ 6,000 tokens） |
| 超出限制处理 | 减少 topIssues 至 Top 30，或按 RuleId 去重只保留每类规则的前 3 条 |
