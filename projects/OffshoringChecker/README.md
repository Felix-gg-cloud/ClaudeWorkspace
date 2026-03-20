# Offshoring Headcount Checker — POC

## 概述

本 POC 针对 `headcount_analysis.xlsx`（单 Sheet：`Offshoring`）提供一套完整的数据质量校验方案，包括：

- **Excel 数据范围配置指南**：在不改动现有 Power BI 连接的前提下，为 Power Automate 定义稳定的读取区域
- **规则清单（Rule Catalog）**：涵盖列级规则、枚举白名单、跨字段逻辑、Function-Team 组合映射
- **Power Automate 详细流程**：使用 Office Scripts 读取指定区域，解析成结构化行对象
- **Copilot Studio Prompt 模板**：强约束输出格式，确保可审计
- **Power BI 刷新验证 & 回滚方案**：确保添加表头/范围定义后，现有 Power BI 报表不受影响

## 文件结构

```
projects/OffshoringChecker/
├── README.md                           ← 本文件（项目概览）
├── docs/
│   ├── 01-excel-range-setup.md        ← Excel 范围定义指南（核心：不改底层数据）
│   ├── 02-rule-catalog.md             ← 规则清单（可直接用于 Flow 实现）
│   ├── 03-power-automate-steps.md     ← Power Automate 详细步骤（照着搭即可）
│   ├── 04-copilot-prompt-template.md  ← Copilot Studio Prompt 模板
│   └── 05-powerbi-validation.md       ← Power BI 刷新验证 & 回滚方案
└── configs/
    ├── function-whitelist.json         ← Function 枚举白名单（16 个值）
    ├── team-whitelist.json             ← Team 枚举白名单（43 个值）
    └── function-team-mapping.json      ← Function → Team 允许组合映射
```

## 快速上手路径（推荐顺序）

1. **阅读** `docs/01-excel-range-setup.md`，选择适合你情况的范围方案（命名范围推荐）
2. **阅读** `docs/05-powerbi-validation.md`，在做任何 Excel 改动前先了解验证与回滚方法
3. 按 `docs/03-power-automate-steps.md` 搭建 Power Automate Flow（附 Office Scripts 代码）
4. 按 `docs/04-copilot-prompt-template.md` 配置 Copilot Studio Topic
5. 规则细节参考 `docs/02-rule-catalog.md`，在 Flow 的校验步骤里逐条实现

## 关键设计决策

### 为什么不直接读整个 Sheet？

Power Automate 的标准 Excel 连接器（"List rows present in a table"）**必须依赖 Excel Table 对象**才能读取。如果直接读 Sheet，连接器拿到的是 Excel Workbook 的 raw 结构，无法直接遍历行。

面对你的场景（Power BI 已在用同一个文件、每月追加数据），我们选择：

> **使用 Office Scripts + 命名范围（Named Range）读取数据，不对 Excel 做结构性改动。**

这样：
- Power BI 的 PQ 连接完全不受影响（文件结构、Sheet 名、列位置均不变）
- Power Automate 通过 Office Scripts 读取指定范围，自动跳过空行
- 每月新追加的行只要在命名范围内，就会被自动读取

### 三种范围方案对比

| 方案 | 操作侵入性 | 自动扩展 | Automate 兼容性 | 推荐指数 |
|------|-----------|---------|---------------|---------|
| **命名范围（Named Range）** | 低（只定义名称） | 需手动扩大或使用动态公式 | ✅ 通过 Office Scripts | ⭐⭐⭐⭐⭐ |
| **Excel Table（Ctrl+T）** | 中（改变区域结构） | ✅ 自动扩展 | ✅ 原生连接器支持 | ⭐⭐⭐（PBI 可能受影响） |
| **固定范围（如 A4:K2000）** | 零（仅在 Flow 配置） | ❌ 需手动更新上限 | ✅ 通过 Office Scripts | ⭐⭐⭐ |

详细说明见 `docs/01-excel-range-setup.md`。
