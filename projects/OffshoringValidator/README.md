# Offshoring POC 校验器

> **用途**：利用 Copilot Studio AI + Power Automate 自动校验 Excel 表 `tblOffshoring` 的 Offshoring 数据质量，并生成 Excel + PDF 双格式报告。

---

## 项目概述

本项目是一个 **AI-only 校验模式**的数据质量校验工具：

- **Power Automate** 仅做机械读取（不做任何数据清洗/trim），保留原始值（含空格等低级错误）
- **Copilot Studio AI** 负责按规则逐行校验，输出结构化 JSON（`issues[]` + `report_model`）
- **Power Automate** 根据 JSON 生成 Excel 报告（.xlsx）和 PDF 报告（.pdf），存档到 SharePoint

---

## 目录结构

```
OffshoringValidator/
├── README.md                          # 本文件
├── docs/
│   ├── runbook.md                     # 完整运营手册（月度操作手顺、FAQ、部署检查清单）
│   ├── rules-specification.md         # 校验规则完整规范（13条规则 + 优先级/去重说明）
│   └── power-automate-flow.md         # Power Automate 流程步骤详细说明
├── schemas/
│   └── ai-output-schema.json          # AI 输出 JSON Schema（用于 Parse JSON 步骤）
└── templates/
    ├── prompt-template.md             # Copilot Studio 提示词模板（可直接粘贴使用）
    ├── excel-report-schema.md         # Excel 报告规范（列定义、格式、Office Script 代码）
    └── html-report-template.html      # PDF 报告 HTML 模板（含 Power Automate 使用说明）
```

---

## 快速上手

### 1. 阅读文档
1. [`docs/runbook.md`](docs/runbook.md) — 月度操作手顺与 FAQ
2. [`docs/rules-specification.md`](docs/rules-specification.md) — 了解所有校验规则
3. [`docs/power-automate-flow.md`](docs/power-automate-flow.md) — 配置 Power Automate 流程

### 2. 配置 AI 校验
- 将 [`templates/prompt-template.md`](templates/prompt-template.md) 中的提示词复制到 Copilot Studio / AI Builder
- Temperature 设置为 **0**（确保输出一致性）

### 3. 配置报告生成
- Excel 报告：参考 [`templates/excel-report-schema.md`](templates/excel-report-schema.md) 创建 Office Scripts
- PDF 报告：将 [`templates/html-report-template.html`](templates/html-report-template.html) 上传到 SharePoint 模板库
- 使用 [`schemas/ai-output-schema.json`](schemas/ai-output-schema.json) 配置 Parse JSON 步骤

---

## 校验规则速查

| 规则 ID | 级别 | 说明 |
|---------|------|------|
| R-WS-ALL-001 | Error | 所有列禁止前后空格（最高优先级）|
| R-YM-001 | Error | YearMonth 全行必填 |
| R-YM-FMT-001 | Error | YearMonth 格式须为 YYYYMM，月份 01-12 |
| R-REQ-CCN-001 | Error | Cost Center Number 必填 |
| R-REQ-FUNC-001 | Error | Function 必填 |
| R-FUNC-WL-001 | Error | Function 须在白名单内 |
| R-REQ-TEAM-001 | Error | Team 必填（非 Total 行）|
| R-TEAM-WL-001 | Error | Team 须在白名单内 |
| R-MAP-FT-001 | Error | Function-Team 组合须在映射表内（Total+空 合法）|
| R-REQ-OWNER-001 | Error | Owner 必填（含 Total 行）|
| R-NUM-001 | Error | 数值列须为数字且 ≥ 0（空跳过）|
| R-SR-001 | Error | ShoringRatio 格式须为 `数字%`，范围 0-100 |
| R-SR-REQ-001 | Error | ShoringRatio 必填（非 Total 行）|

---

## 报告输出

### Excel 报告（.xlsx）
- **Summary 工作表**：摘要统计 + 校验结果 + 按规则统计
- **Issues 工作表**：所有 issue 明细，支持筛选，Error 红色/Warning 黄色高亮

### PDF 报告（.pdf）
- 由 HTML 模板生成，包含摘要卡片、规则统计表、问题明细表
- 支持通过 Encodian / Plumsail 连接器在 Power Automate 中直接转换

### 文件命名
```
ValidationReport_<YearMonth>_<YYYYMMDD-HHMMSS>.xlsx
ValidationReport_<YearMonth>_<YYYYMMDD-HHMMSS>.pdf
```

### 存储位置
```
SharePoint: OffshoringReports/ValidationReports/<YearMonth>/
```

---

## 关键设计决策

| 决策 | 说明 |
|------|------|
| Power Automate 不做数据处理 | 保留原始值（含空格），让 AI 发现低级填写错误 |
| AI 只输出 JSON（无 Markdown）| 机器可直接解析，无需额外处理 |
| 报告为 Excel + PDF（非 Markdown）| 用户无需特殊软件，办公环境直接可用 |
| Temperature = 0 | 减少 AI 输出的随机性，保证校验一致性 |
| 全列空格禁止（Error 级别）| 严格抓低级填写错误，不宽容任何前后空格 |
| Total+空 Team 合法 | Total 汇总行不要求填写 Team |
| 数值列空跳过 | 数值列允许留空（用 skip 而非报错）|
