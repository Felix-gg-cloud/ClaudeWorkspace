# 报告生成规格说明

## 概述

本 POC 产出两种格式的校验报告：
- **Excel（.xlsx）**：完整 issues 明细，面向数据处理人员和问题追踪
- **PDF**：Top N 高优先级问题摘要，面向管理层快速决策

两份文件均保存至 SharePoint 文档库 `/Reports/Offshoring/`。

---

## 1. Excel 报告（完整明细）

### 文件命名
```
ValidationReport_<YearMonth>_<Timestamp>.xlsx
示例：ValidationReport_202501_20250115T0830.xlsx
```

### 工作表结构

#### Sheet 1：Summary（汇总统计）

| 字段 | 示例值 |
|------|--------|
| 报告生成时间 | 2025-01-15 08:30:00 |
| 校验月份（YearMonth） | 202501 |
| 数据来源文件 | tblOffshoring.xlsx（OneDrive） |
| 总行数 | 120 |
| 有问题的行数 | 8 |
| Error 总数 | 15 |
| Warning 总数 | 0 |
| 通过行数 | 112 |
| 通过率 | 93.3% |

#### Sheet 2：Issues（问题明细，完整列表）

| 列名 | 类型 | 说明 |
|------|------|------|
| `#` | 序号 | 自动编号 |
| `Severity` | Error / Warning | 严重程度 |
| `RuleId` | 文本 | 规则编号（如 `R-WS-ALL-001`） |
| `RowKey` | 文本 | 行定位键（格式见下方） |
| `Column` | 文本 | 问题所在列名 |
| `RawValue` | 文本 | 单元格原始值（含空格等字符） |
| `Message` | 文本 | 问题描述（中文） |
| `FixSuggestion` | 文本 | 修复建议（中文） |

> RowKey 格式：`RowIndex=<n>|YearMonth=<raw>|CCN=<raw>|Function=<raw>|Team=<raw>`  
> 例：`RowIndex=5|YearMonth=202501|CCN=CC001|Function=GBS|Team=EA`

**Excel 格式要求：**
- 首行冻结（标题行固定）
- 启用自动筛选（支持按 Severity / RuleId / Column 筛选）
- Error 行：单元格背景色 `#FFE0E0`（浅红色）
- Warning 行：单元格背景色 `#FFF8E0`（浅黄色）

#### Sheet 3：RawData（原始输入行）

原样保存 Power Automate 从 Excel 读取到的所有行（未经任何 trim 或处理），便于与 issues 对照。

---

## 2. PDF 报告（Top N 摘要）

### 文件命名
```
ValidationReport_<YearMonth>_<Timestamp>.pdf
示例：ValidationReport_202501_20250115T0830.pdf
```

### 页面内容结构

#### 页头
```
Offshoring 数据校验报告（摘要）
校验月份：202501        生成时间：2025-01-15 08:30
数据来源：tblOffshoring.xlsx（OneDrive）
```

#### 第 1 节：校验结果概览
以表格形式展示：
- 总行数 / 问题行数 / 通过行数 / 通过率
- Error 数量 / Warning 数量

#### 第 2 节：Top N 问题列表（默认 N=20）

按以下优先级排序，取前 N 条展示：
1. **Severity**：Error 优先于 Warning
2. **RuleId 字母顺序**：相同 Severity 内，空格规则（R-WS-*）排最前
3. **RowIndex 升序**：相同规则内，行号小的优先

每条问题展示：
| 字段 | 示例 |
|------|------|
| # | 1 |
| 严重程度 | Error |
| 规则 | R-WS-ALL-001 |
| 行定位 | RowIndex=5 \| CCN=CC001 \| Function=GBS \| Team=EA |
| 问题列 | Function |
| 原始值 | `"GBS "` |
| 问题说明 | 该单元格包含尾随空格，不允许 |
| 修复建议 | 删除 "Function" 列值末尾的空格后重新提交 |

#### 页脚
```
本报告为摘要版本（Top 20 问题）。完整明细请查阅配套 Excel 报告。
```

---

## 3. Top N 配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `TOP_N_ISSUES` | 20 | PDF 中展示的最大问题条数 |
| 排序逻辑 | Error 优先 → R-WS 优先 → RowIndex 升序 | 固定排序，确保最重要的问题排在前面 |

> POC 阶段该值在 Power Automate 流程的变量步骤中配置（`Initialize variable: TOP_N_ISSUES`）。

---

## 4. 报告生成技术路径

### Excel 生成
Power Automate 使用 **Excel Online (Business)** 连接器或 **Office Script** 动作：
1. 在 SharePoint 目标路径创建新 Excel 文件（从模板复制，模板含预设格式）
2. 逐一写入 Summary / Issues / RawData 三张表的数据
3. 应用条件格式（或使用 Office Script 脚本设置颜色）

### PDF 生成
Power Automate 使用以下方案之一（按优先级）：
1. **Office Script（推荐）**：在 Excel 中先生成一个"PDF_Preview"隐藏工作表，再用 `Export to PDF` 方式导出指定 Sheet
2. **Word + PDF 转换**：先将 Top N 数据填充到 Word 模板，再通过 `Convert file` 动作转换为 PDF
3. **OneDrive/SharePoint 内置 PDF 导出**：上传 Excel 后使用 Graph API 的 `/drive/items/{item-id}/content?format=pdf` 端点导出

> POC 阶段推荐方案 2（Word 模板 + PDF 转换），操作最直观，不需要 Office Script 权限。

---

## 5. 文件命名时间戳格式

```
<YearMonth>：数据月份，格式 YYYYMM，例：202501
<Timestamp>：流程运行时间，格式 YYYYMMDDTHHmm（UTC），例：20250115T0830

完整示例：
ValidationReport_202501_20250115T0830.xlsx
ValidationReport_202501_20250115T0830.pdf
```

---

## 6. Teams 通知

报告生成后，Power Automate 发送 Teams 消息到指定频道，内容包括：
- 校验月份与通过率
- Error / Warning 数量
- Excel 报告链接（SharePoint 直链）
- PDF 摘要链接（SharePoint 直链）
- 若 Error=0，则显示"✅ 本月数据校验通过"；否则显示"❌ 发现 N 个 Error，请处理后重新提交"
