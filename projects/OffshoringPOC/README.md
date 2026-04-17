# OffshoringPOC — Offshoring 数据校验 POC

## 项目概述

本 POC 实现一套"**手动触发式 Power Automate 流程**"，用于校验月度 Offshoring 数据（存放于 OneDrive 的 Excel 表 `tblOffshoring`）。  
校验由 AI（Copilot Studio / Azure OpenAI）全权完成，不依赖 Power Automate 做任何数据处理；校验结果以 **Excel**（完整明细）和 **PDF**（Top N 问题摘要）两种格式输出到 SharePoint。

---

## 目录结构

```
projects/OffshoringPOC/
├── README.md                          ← 本文件（概述 + OneDrive vs SharePoint）
├── docs/
│   ├── runbook.md                     ← 完整操作手顺（Runbook）
│   ├── flow-steps.md                  ← Power Automate 流程步骤详解
│   ├── onedrive-vs-sharepoint.md      ← OneDrive 与 SharePoint 差异说明及迁移路径
│   └── report-generation.md          ← 报告生成规格（Excel 全量 + PDF Top N）
├── prompts/
│   └── validation-prompt.md          ← Copilot Studio AI 校验提示词模板（含 JSON schema）
└── schema/
    ├── tblOffshoring-schema.json      ← tblOffshoring 表列定义与规则
    ├── validation-output-schema.json  ← AI 输出 JSON schema（issues 数组）
    └── sample-data.json               ← 示例行数据（含合法与违规样本）
```

---

## OneDrive 与 SharePoint 的角色分工

> **详细说明见 [`docs/onedrive-vs-sharepoint.md`](docs/onedrive-vs-sharepoint.md)**

| 角色 | 位置 | 说明 |
|------|------|------|
| 数据来源 | **OneDrive**（个人/共享） | `tblOffshoring.xlsx` 由业务团队手动填写并存放于此 |
| 报告输出 | **SharePoint** 文档库 | 校验报告（Excel + PDF）统一存放，便于团队访问与归档 |

**为什么这样分工？**
- OneDrive 对填表人友好（直接用 Excel Online 编辑），无需特殊权限
- SharePoint 提供版本控制、团队共享、权限管理，适合正式归档
- POC 阶段两者都可接受；正式上线后建议统一迁移到 SharePoint（详见迁移路径）

---

## 数据来源文件

| 属性 | 值 |
|------|----|
| 文件名 | `tblOffshoring.xlsx` |
| 存放位置 | OneDrive（共享文件夹或个人 OneDrive） |
| 表名 | `tblOffshoring`（Excel 表格/命名区域） |
| 触发方式 | 手动（在 Power Automate 中点击"手动运行"） |
| 更新频率 | 月度（每月初填写上月数据） |

---

## 报告输出文件

| 文件 | 格式 | 存放位置 | 内容 |
|------|------|----------|------|
| `ValidationReport_<YearMonth>_<Timestamp>.xlsx` | Excel | SharePoint `/Reports/Offshoring/` | 完整 issues 明细 + 原始数据 + 汇总统计 |
| `ValidationReport_<YearMonth>_<Timestamp>.pdf` | PDF | SharePoint `/Reports/Offshoring/` | Top N 问题摘要（面向管理层） |

> PDF 包含 Top N 高优先级问题（默认 N=20，可配置）；完整明细请查阅 Excel 报告。

---

## 校验规则概述

- **全局空格规则（R-WS-ALL-001）**：所有列的值，只要存在前导或尾随空白字符，即报 **Error**
- **YearMonth 必填**：所有行均要求填写 YearMonth，格式 `YYYYMM`，月份 01-12
- **Non-Total 行必填字段（Option A）**：Cost Center Number、Function、Team、Owner
- **Total 行**：Function 固定为 `Total`，Team 可为空；Owner 必填
- **Function-Team 映射**：必须为合法配对；`Total` + 空 Team 是合法组合
- **数值列**：空白则跳过；非空则必须为数字且 `>= 0`
- **ShoringRatio**：格式 `^\d+(\.\d+)?%$`，值 0-100；Non-Total 行必填，Total 行可选（有值则校验）
- **白名单匹配**：Function / Team 使用原始值精确匹配（不做 trim，从而捕获空格错误）

完整规则见 [`schema/tblOffshoring-schema.json`](schema/tblOffshoring-schema.json)。

---

## 快速开始

1. 参阅 [`docs/runbook.md`](docs/runbook.md) 了解完整操作流程
2. 参阅 [`docs/flow-steps.md`](docs/flow-steps.md) 了解 Power Automate 流程步骤
3. 将 [`prompts/validation-prompt.md`](prompts/validation-prompt.md) 中的提示词复制到 Copilot Studio
4. 参阅 [`schema/`](schema/) 了解数据结构与 AI 输出格式
