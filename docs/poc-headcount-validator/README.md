# POC Headcount Validator — 文档说明

本目录包含 tblOffshoring 离岸人力数据 POC 校验器的完整文档包。

## 目录结构

```
docs/poc-headcount-validator/
├── README.md                          # 本文件：总览与快速导航
├── flow-steps.md                      # Power Automate 流程逐步说明（含表达式）
├── copilot-studio-prompt.md           # Copilot Studio 提示词模板
├── sample-output-report.md            # 样本校验报告（含合法行与错误行示例）
└── rules/
    ├── rules-catalog.md               # 完整规则清单（主文档）
    ├── rules.json                     # 机器可读规则定义
    └── function-team-mapping.json     # Function/Team 白名单与映射表
```

## 数据表：tblOffshoring（16 列）

| # | 列名 | 类型 |
|---|------|------|
| 1 | Cost Center Number | 文本 |
| 2 | Function | 文本 |
| 3 | Team | 文本 |
| 4 | Owner | 文本 |
| 5 | YearMonth | 文本 |
| 6 | ShoringRatio | 文本 |
| 7 | Actual_HC | 数值 |
| 8 | Actual_Offshore_HC | 数值 |
| 9 | Actual_Cost | 数值 |
| 10 | Actual_Offshore_Cost | 数值 |
| 11 | Planned_HC | 数值 |
| 12 | Planned_Offshore_HC | 数值 |
| 13 | Planned_Cost | 数值 |
| 14 | Planned_Offshore_Cost | 数值 |
| 15 | Target_YearEnd | 数值 |
| 16 | Target_2030YearEnd | 数值 |

## 核心设计决策（v1.3 最终确认版）

### A) 数值列校验（R-NUM-001）
- **空值**：不校验，不报错
- **非空值**：必须是数字且 >= 0
- 适用于全部 10 个数值列（Actual_*、Planned_*、Target_*）

### B) Function-Team 映射（R-FM-001）
- Total 行 + 空 Team = **合法组合**（不报错）
- 实现方式：触发条件要求 Function 和 Team **均非空**，Total 行 Team 为空时自然跳过
- 规则**不针对 Total 行单独 skip**；空 Team 导致触发条件不满足，自然不检查

### C) 必填规则（Option A）
- **Total 行**（Function = 'Total (All)' 或 'Total (Core Operations)'）：
  - 仅 `Owner` 和 `YearMonth` 强制必填
  - 其余列（含三个 Target 列、ShoringRatio）允许为空
- **Non-Total 行**（其余所有行）：
  - 仅 `Target_YearEnd`、`Target_2030YearEnd`、`ShoringRatio` 强制必填
  - 其余列允许为空（有值则按格式规则校验）

### D) ShoringRatio（R-SR-001）
- 硬性 Error 规则
- 必须匹配 `^\d+(\.\d+)?%$`（允许小数百分比，如 12.5%）
- 数值范围：0 ~ 100（含两端）
- 空值处理：Total 行允许空；Non-Total 行由 R-REQ-NT-003 强制非空

## 快速开始

1. 阅读 [`rules/rules-catalog.md`](rules/rules-catalog.md) 了解完整规则逻辑
2. 参考 [`flow-steps.md`](flow-steps.md) 在 Power Automate 中搭建校验流程
3. 使用 [`copilot-studio-prompt.md`](copilot-studio-prompt.md) 中的提示词配置 Copilot Studio
4. 对照 [`sample-output-report.md`](sample-output-report.md) 验证报告格式与内容
