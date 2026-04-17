# Headcount Data Quality POC — 运行手册总览

## 项目简介

本 POC（概念验证）旨在通过 **Power Automate + Copilot Studio + Power BI** 三件套，实现对 Offshoring Headcount Excel 数据文件的自动化校验、异常报告与结果可视化。

## 适用场景

- 业务团队按月填报 `tblOffshoring` Headcount 数据至共享 Excel
- 数据质量校验需要覆盖：字段必填性、格式合规性、枚举白名单、Function-Team 映射、数值非负性
- 异常汇总以结构化清单形式输出，供业务方修正并重新提交

---

## 文档目录

| 文档 | 说明 |
|------|------|
| [01 Excel 表格搭建](docs/01-excel-table-setup.md) | tblOffshoring 表结构、列定义、数据样例 |
| [02 校验规则说明](docs/02-validation-rules.md) | 完整规则清单（RuleId / 条件 / 严重性 / 错误提示） |
| [03 Power Automate Flow 配置](docs/03-power-automate-flow.md) | Flow 步骤拆解、表达式参考、调试技巧 |
| [04 Copilot Studio 提示词](docs/04-copilot-studio-prompt.md) | Copilot 报告提示词模板与使用说明 |
| [05 Power BI 输出、验证与回滚](docs/05-powerbi-output-validation-rollback.md) | 报告接入、验证流程与异常回滚操作 |

---

## 快速开始

```
1. 参照 docs/01 搭建 Excel 表格
2. 参照 docs/02 理解校验规则
3. 参照 docs/03 在 Power Automate 中搭建并运行 Flow
4. 参照 docs/04 在 Copilot Studio 中配置并触发报告生成
5. 参照 docs/05 在 Power BI 中查看结果，并在需要时执行回滚
```

---

## 关键约定

- **行类型**：`Function` 列的值决定行类型
  - **Total 行**：`Function` ∈ `{Total (All), Total (Core Operations)}`
  - **Non-Total 行**：其他所有 Function 值
- **必填逻辑**（Option A）：
  - 所有行：`YearMonth` 必填
  - Total 行额外：`Owner` 必填
  - Non-Total 行额外：`Target_YearEnd`、`Target_2030YearEnd`、`ShoringRatio` 必填
- **ShoringRatio 格式**：`^\d+(\.\d+)?%$`，范围 0%–100%（含小数）

---

## 相关链接

- Power Automate: <https://make.powerautomate.com>
- Copilot Studio: <https://copilotstudio.microsoft.com>
- Power BI: <https://app.powerbi.com>
