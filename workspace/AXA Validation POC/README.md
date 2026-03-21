# AXA Validation POC

> **唯一维护入口 / Single Entry Point**  
> 所有与本 POC 相关的文档、规则和操作指南均集中于本目录。  
> All documents, rules, and operation guides for this POC are consolidated in this directory.

---

## 目录结构 Directory Structure

```
workspace/AXA Validation POC/
├── README.md                          # 本文件：POC 概览与导航
├── Validation Rules.md                # 校验规则文档（知识模块上传源）
├── Copilot-Studio-AI-Instructions.md  # Copilot Studio AI Agent 系统说明模板
└── Operation-Guide.md                 # 每月操作流程与维护指南
```

---

## POC 概览 Overview

本 POC 使用 **Microsoft Power Automate** 结合 **Copilot Studio AI Agent**，对 AXA 月度人力数据进行自动化校验。

- **校验引擎**：Copilot Studio AI Agent（知识驱动，规则来自知识模块）
- **触发方式**：Power Automate 流，每月定时或手动触发
- **输出格式**：结构化 JSON 报告，包含每行的校验结果和错误详情

---

## 文件说明 File Descriptions

| 文件 | 用途 |
|------|------|
| [`Validation Rules.md`](./Validation%20Rules.md) | 全部校验规则（中英双语）。每月如规则变更，先更新此文件，再同步上传至 Copilot Studio 知识模块。 |
| [`Copilot-Studio-AI-Instructions.md`](./Copilot-Studio-AI-Instructions.md) | Copilot Studio AI Agent 的 Instructions 区参考模板，包含输入输出 JSON Schema。规则调整无需修改此文件，只需更新知识模块。 |
| [`Operation-Guide.md`](./Operation-Guide.md) | 每月操作流程、职责分工、文件命名规范及分支维护说明。 |

---

## 分支维护说明 Branch Maintenance

- **唯一维护分支**：`copilot/axa-validation-poc`
- 本 POC 所有文档变更均在此分支上提交，不再创建其他分支。
- 合并（Merge）至 `main` 后，`main` 分支即为当前最新归档版本。

---

## 快速上手 Quick Start

1. 查阅 [`Validation Rules.md`](./Validation%20Rules.md) 了解所有校验规则。
2. 参照 [`Copilot-Studio-AI-Instructions.md`](./Copilot-Studio-AI-Instructions.md) 配置 Copilot Studio AI Agent。
3. 按 [`Operation-Guide.md`](./Operation-Guide.md) 中的流程每月执行数据验证。
4. 规则如有变更：更新 `Validation Rules.md` → 提交到本分支 → 重新上传至 Copilot Studio 知识模块。
