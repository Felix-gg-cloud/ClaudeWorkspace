# Offshoring Data Validation — POC Runbook

## 概述

本 Runbook 描述"AI-only 校验模式"的端到端 POC 流程：  
**Power Automate** 仅负责机械读取 Excel 数据并原样传递给 AI；  
**AI 校验器**（Copilot Studio）负责按规则逐行、逐列进行完整的数据质量校验，包括空白字符检测。

---

## 1. 架构概述

```
Excel (tblOffshoring)
      │
      │  [Power Automate - 机械读取，不做任何清洗]
      ▼
原始行数据 JSON (raw values, 含空格/空白字符)
      │
      │  [Copilot Studio / AI 校验器]
      ▼
校验结果 { issues: [...], markdown_report: "..." }
      │
      ├──► SharePoint：issues JSON 文件
      ├──► SharePoint：Markdown 报告文件
      └──► Teams / 邮件：报告链接通知
```

---

## 2. Power Automate 职责（机械读取）

### 2.1 核心原则

> **Power Automate 只做机械读取，不做任何数据处理。**

这意味着：
- ❌ 不对字段值做 `trim()`（去首尾空格）
- ❌ 不做任何数据标准化（大小写转换、格式转换等）
- ❌ 不做空值合并或默认值填充
- ✅ 读出什么就传什么，原始值（raw value）原样交给 AI

### 2.2 原因说明

手动填报的数据极可能包含低级错误，例如：
- 字段值前后多余空格（如 `" Finance "` 而非 `"Finance"`）
- 纯空白字段（值看起来非空但全是空格）
- 数字字段中包含空格（如 `" 1234567 "`）

这些错误**必须被 AI 检测到**，因此 PA 绝不能提前清洗掉它们。

### 2.3 Flow 步骤

| # | 步骤 | 说明 |
|---|------|------|
| 1 | **触发器** | 手动触发 或 定时触发（按月） |
| 2 | **List rows from Excel** | 使用"List rows present in a table"读取 `tblOffshoring` 全部行；`@odata.context` 外的所有字段均原样保留 |
| 3 | **构造 RowKey** | 对每行生成 RowKey（见第 3 节），**不做 trim，不做格式化** |
| 4 | **批量组装 payload** | 把所有行打包为 `rows[]` 数组，连同规则摘要一并组成 prompt input |
| 5 | **调用 AI 校验器** | 将 payload 发送至 Copilot Studio 或 Azure OpenAI（system prompt + user message） |
| 6 | **解析响应** | 从响应中提取 `issues` 数组和 `markdown_report` 字符串 |
| 7 | **保存 issues JSON** | 写入 SharePoint 文件（如 `validation-issues-YYYYMM.json`） |
| 8 | **保存 Markdown 报告** | 写入 SharePoint 文件（如 `validation-report-YYYYMM.md`） |
| 9 | **发送通知** | 通过 Teams Adaptive Card 或邮件发送报告链接 |

### 2.4 数据量与分批策略

当行数较多时，按 `YearMonth` 分组分批处理，以避免 token 超限：
1. 从 `tblOffshoring` 读取全部行（无分页限制，注意 Flow 默认 256 行上限，需开启"Get All"）
2. 按 `YearMonth` 分组；每组独立发送一次 AI 校验请求
3. 合并所有组的 `issues` 后统一写入报告

---

## 3. RowKey 规范

RowKey 用于在校验结果中精确定位问题行。

### 3.1 格式

```
ROW-{RowIndex}|YM={YearMonth}|CC={CostCenterNumber}|F={Function}|T={Team}
```

- **RowIndex**：1-based 行序号（从 Excel 表中读取的顺序，第 1 行 = 1）
- **YearMonth**、**CostCenterNumber**、**Function**、**Team**：使用**原始值（raw value）**，不做 trim 或其他处理；若字段为空则用 `(empty)` 占位

### 3.2 示例

| RowIndex | YearMonth | CostCenterNumber | Function | Team | RowKey |
|----------|-----------|------------------|----------|------|--------|
| 1 | 202501 | 1234567 | Finance | Team A | `ROW-1\|YM=202501\|CC=1234567\|F=Finance\|T=Team A` |
| 2 | ` ` (空白) | 1234567 | Finance | (empty) | `ROW-2\|YM= \|CC=1234567\|F=Finance\|T=(empty)` |
| 3 | 202501 | (empty) | Total (All) | (empty) | `ROW-3\|YM=202501\|CC=(empty)\|F=Total (All)\|T=(empty)` |

> RowKey 中的原始值（包括空格）不得被修改，以确保 AI 能据此准确定位问题。

---

## 4. AI 校验器职责

AI 校验器按照 `ai-validator-system-prompt.md` 中的规则，对每一行的每一列执行完整校验，包括：

- **空白字符检测**：仅含空格的字段视为空（空白即空）
- **精确比对**：白名单 / 映射表比对不做 auto-trim，直接用原始字符串，以发现因多余空格导致的不匹配
- **数值解析**：含空格的数字字符串视为无效（除 ShoringRatio 的特殊规则外）
- **ShoringRatio 空格宽容规则**：整体值允许首尾空格，但需额外输出 Warning（R-WS-001）
- **结构化输出**：必须输出标准 JSON + Markdown 报告，不得自由发挥规则

---

## 5. 输出产物

### 5.1 校验结果 JSON

保存路径：`SharePoint:/Offshoring/Validation/validation-issues-{YYYYMM}.json`

格式参见 `output-contract.md`。

### 5.2 Markdown 报告

保存路径：`SharePoint:/Offshoring/Validation/validation-report-{YYYYMM}.md`

报告内容由 AI 生成，包含：
- 总览（多少行、多少 Error、多少 Warning）
- 按规则分类的 Top Issues
- 修复建议摘要

### 5.3 Teams / 邮件通知

通知内容：
```
📋 Offshoring 数据校验报告已生成
月份：{YYYYMM}
Error：{n} 条 | Warning：{m} 条
👉 查看完整报告：{SharePoint 链接}
```

---

## 6. 常见问题 FAQ

| 问题 | 说明 |
|------|------|
| Flow 只读到 256 行 | 在"List rows"动作的高级选项中，勾选"Get All"或使用分页循环 |
| AI 响应中含额外文字 | 检查 system prompt 是否明确要求"只输出 JSON + Markdown，不输出其他内容" |
| Token 超限 | 按 YearMonth 分组分批调用，每批不超过 200 行 |
| ShoringRatio 既报 Error 又报 Warning | 正常：格式错误输出 Error（R-SR-001），有前后空格但格式正确则只输出 Warning（R-WS-001） |
| 同一列同时触发"必填"和"格式"错误 | AI 应遵循规则优先级：字段为空时只报必填错误，跳过格式校验 |
