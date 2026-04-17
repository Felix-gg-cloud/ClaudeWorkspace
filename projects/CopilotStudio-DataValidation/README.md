# Copilot Studio 数据校验引擎

## 项目概述

本项目基于 Microsoft Copilot Studio 构建批量 Excel 数据行校验引擎，通过 Power Automate 自动化流程对接，实现每月运营数据的自动校验与问题上报。

---

## 架构设计原则

### AI Instructions（系统说明）区职责定位

> **核心原则：Instructions 只描述"如何使用知识库"，而非写死所有规则。**

| 职责 | 位置 | 说明 |
|------|------|------|
| 业务校验规则（Function 枚举、Team 枚举、映射关系等） | **Knowledge 知识模块** | 通过 .md 规则文档维护 |
| 月度正确数据模板 | **Knowledge 知识模块** | 每月上传最新 .md / .xlsx 模板 |
| 引擎行为约束（输入/输出格式、执行协议） | **Instructions 区** | 相对稳定，少改动 |
| 输入/输出 JSON Schema | **Instructions 区** | 供 Power Automate 端适配 |

### 规则更新流程

```
业务规则变更
    ↓
维护 Knowledge 知识模块（更新规则 .md / 替换月度模板）
    ↓
Copilot Studio 自动加载最新知识（Instructions 无需改动）
```

**若 Knowledge 模板数据与规则文档有出入，以规则文档优先。**

---

## 目录结构

```
CopilotStudio-DataValidation/
├── README.md                        # 本文件：项目概述与设计原则
├── docs/
│   ├── AI-Instructions-Reference.md # Copilot Studio Instructions 区参考填法（可直接复制）
│   └── Operations-Workflow.md       # 每月运维流程、文件命名规范与责任人
```

---

## 快速开始

1. 参考 [`docs/AI-Instructions-Reference.md`](docs/AI-Instructions-Reference.md) 填写 Copilot Studio 系统说明区。
2. 参考 [`docs/Operations-Workflow.md`](docs/Operations-Workflow.md) 完成每月知识库维护与 Power Automate 对接。
3. 规则如需更新，仅需维护 Knowledge 知识模块，无需修改 Instructions 区。
