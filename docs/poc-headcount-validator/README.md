# POC Headcount Validator – 文档总览

## 概述

本 POC 基于 SharePoint 上的 Excel 文件，通过 Excel Table（`tblOffshoring`）读取 Offshoring 人员数据，使用 Power Automate + Copilot Studio 完成数据校验、问题报告生成的端到端流程。

## 数据源配置

| 配置项 | 值 |
|---|---|
| Excel 文件 | `headcount_analysis_poc.xlsx`（SharePoint POC 文件夹中的副本） |
| Sheet 名称 | `Offshoring` |
| Excel Table 名称 | `tblOffshoring` |
| 读取方式 | Excel Online (Business) → List rows present in a table |

> **注意**：POC 使用正式文件的 SharePoint 副本（`*_poc.xlsx`），不影响 Power BI 正在读取的原始文件。

## 表列名（精确，区分大小写）

以下为 `tblOffshoring` 的完整列清单，必须与 Excel 第一行（表头行）完全一致：

| # | 列名 | 数据类型 | 说明 |
|---|---|---|---|
| 1 | `Cost Center Number` | 数字型字符串 | 成本中心编号，7 位纯数字 |
| 2 | `Function` | 文本 | 职能分类（见白名单） |
| 3 | `Team` | 文本 | 团队名称（见白名单） |
| 4 | `Owner` | 文本 | 负责人，不可为空 |
| 5 | `YearMonth` | 数字/文本 | 年月，格式 YYYYMM |
| 6 | `Actual_GBS_TeamMember` | 数字 | 实际 GBS 团队成员数 |
| 7 | `Actual_GBS_TeamLeaderAM` | 数字 | 实际 GBS 团队负责人/AM 数 |
| 8 | `Actual_EA` | 数字 | 实际 EA 人员数 |
| 9 | `Actual_HKT` | 数字 | 实际 HKT 人员数 |
| 10 | `Planned_GBS_TeamMember` | 数字 | 计划 GBS 团队成员数 |
| 11 | `Planned_GBS_TeamLeaderAM` | 数字 | 计划 GBS 团队负责人/AM 数 |
| 12 | `Planned_EA` | 数字 | 计划 EA 人员数 |
| 13 | `Planned_HKT` | 数字 | 计划 HKT 人员数 |
| 14 | `Target_YearEnd` | 数字 | 年末目标 |
| 15 | `Target_2030YearEnd` | 数字 | 2030 年末目标 |
| 16 | `ShoringRatio` | 数字/百分比 | Shore 比率，支持 0-1、0-100 或 `xx%` 格式 |

## 仓库产物结构

```
docs/poc-headcount-validator/
├── README.md                          # 本文件 – 总览与配置
├── flow-steps.md                      # Power Automate 流程步骤说明
├── copilot-prompt-template.md         # Copilot Studio Prompt 模板
├── sample-output-report.md           # 示例输出报告（Markdown）
└── rules/
    ├── rule-catalog.md                # 校验规则目录（含规则定义与逻辑）
    ├── function-whitelist.json        # Function 允许值列表
    ├── team-whitelist.json            # Team 允许值列表
    └── function-team-mapping.json     # Function→Team 允许组合映射
```

## 快速开始

1. 在 SharePoint 中复制正式 Excel 文件到 POC 目录：  
   `headcount_analysis.xlsx` → `POC/headcount_analysis_poc.xlsx`

2. 打开副本，确认第一行列名与上表完全一致（区分大小写、空格）。

3. 选中表头行 + 所有数据行列，按 **Ctrl+T** 建表，勾选"表包含标题"，将表名设为 **`tblOffshoring`**。

4. 保存并上传/确认 SharePoint 同步。

5. 按照 `flow-steps.md` 在 Power Automate 中搭建校验流程。

6. 将 `copilot-prompt-template.md` 中的 Prompt 配置到 Copilot Studio 生成式步骤。

## Excel Table 对 Power BI 的影响与规避方案

| Power Query 读取方式 | Ctrl+T 影响 | 建议 |
|---|---|---|
| 读取某个工作表 UsedRange | 通常不影响数据值；若有"跳过行/提升标题"步骤需确认 | 在副本上操作，刷新 PBI 验证 |
| 读取命名范围 | 与 Table 并存，通常不影响 | 不改 Named Range 名称即可 |
| 读取 Excel Table（按表名） | 表名匹配即可；若以前表名为 Table1 需更新 PBI 引用 | 统一表名为 `tblOffshoring` |

> **最保险做法**：始终用副本（`*_poc.xlsx`）做 POC，正式文件留给 Power BI。确认流程无误后再决定是否合并。

## 每月数据追加规范

- 新月份数据追加在 `tblOffshoring` 表格**最后一行的下方**（不要隔空行）。
- Excel Table 会自动扩展，Power Automate `List rows present in a table` 将自动读取新行。
- 追加后建议在 Power Automate 中手动触发一次校验，确认新月份数据被正确读取。
