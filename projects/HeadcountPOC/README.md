# Headcount Analysis POC — Excel Table 方案

## 概述

本 POC 包用于在 **Power Automate + Copilot Studio** 流程中，对 `headcount_analysis.xlsx` 中 `Offshoring` Sheet 的人力数据进行自动化校验并生成报告。

**核心决策**：Excel 数据区域使用 **Excel Table（名称：`tblOffshoring`）** 作为数据入口，Power Automate 通过 `List rows present in a table` 动作读取，每月追加的新行自动纳入 Table，不会漏读。

---

## 目录说明

```
projects/HeadcountPOC/
├── README.md                          ← 本文件，总览
├── docs/
│   ├── 01-excel-table-setup.md        ← Step-by-step：在 Excel 里建 tblOffshoring
│   ├── 02-power-automate-flow.md      ← Step-by-step：搭建 Power Automate 校验流程
│   ├── 03-safety-and-rollback.md      ← 安全操作与回滚方案
│   └── 04-powerbi-compatibility.md   ← Power BI 兼容性说明与注意事项
├── rules/
│   ├── rule-catalog.md                ← 完整校验规则清单（引用 Table 数据入口）
│   ├── function-whitelist.json        ← Function 白名单
│   ├── team-whitelist.json            ← Team 白名单
│   └── function-team-mapping.json    ← Function → Team 合法组合映射
└── prompts/
    └── copilot-studio-prompt.md       ← Copilot Studio 系统提示词模板
```

---

## 快速开始（推荐顺序）

1. **阅读安全说明**：`docs/03-safety-and-rollback.md`，先在 SharePoint 创建 POC 副本文件再操作。
2. **建 Excel Table**：按 `docs/01-excel-table-setup.md` 在 POC 副本文件中操作（5 分钟）。
3. **搭 Power Automate 流程**：按 `docs/02-power-automate-flow.md` 逐步搭建（30~60 分钟）。
4. **配置规则与白名单**：参考 `rules/` 目录，把实际列名/白名单值补充进去。
5. **配置 Copilot Studio**：参考 `prompts/copilot-studio-prompt.md` 配置 Prompt。

---

## 关键约定

| 项目 | 值 |
|---|---|
| Excel 文件名 | `headcount_analysis.xlsx`（POC 副本建议加 `_poc` 后缀） |
| Sheet 名 | `Offshoring` |
| 表头行 | 第 4 行 |
| Excel Table 名称 | `tblOffshoring` |
| 数据起始行 | 第 5 行（第 4 行为列名） |
| 每月新增方式 | 在 Table 末尾紧接追加行（Table 自动扩展） |

---

## 列名标准（第 4 行）

以下为 POC 使用的标准列名，如你的 Excel 实际列名不同，请按实际列名修改 `rules/rule-catalog.md` 和 `prompts/copilot-studio-prompt.md` 中引用的字段名。

| 列名 | 说明 | 示例值 |
|---|---|---|
| `Cost Center Number` | 成本中心编号，7 位纯数字 | `1234567` |
| `Function` | 职能分类 | `Engineering` |
| `Team` | 团队名称 | `Platform` |
| `Owner` | 负责人姓名，不得为空 | `Zhang San` |
| `YearMonth` | 年月，格式 YYYYMM | `202501` |
| `Shoring Ratio` | Offshore 比例（数字或百分比） | `30` 或 `30%` |
| `Headcount` | 人力数量，数字且 ≥ 0 | `5` |
| `Offshore HC` | Offshore 人力数量，数字且 ≥ 0 | `1.5` |

> **注意**：列名区分大小写和空格，必须与第 4 行单元格内容完全一致。
