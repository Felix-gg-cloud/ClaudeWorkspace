# Operation Guide — AXA Validation POC

> 本文档描述每月的操作流程、职责分工和规则维护方法。  
> This document describes the monthly operation workflow, responsibilities, and rule maintenance.

---

## 系统架构概览 System Overview

```
Power Automate (Automate)
    │
    │  发送待验证数据（JSON/CSV）
    ▼
Copilot Studio AI Agent
    │  读取 Instructions 中的规则说明（来自 Validation Rules.md）
    │  执行校验
    │  返回 JSON 报告
    ▼
Power Automate (Automate)
    │
    │  解析报告，触发后续动作（通知/存档等）
    ▼
结果通知 / 存档
```

---

## 每月操作流程 Monthly Workflow

### Step 1 — 确认规则文档（月初）

确认本目录中的 `Validation Rules.md` 为最新版本。如校验规则有任何变更，直接在此文件中修改并提交至本分支，无需上传至 Copilot Studio 知识模块。

---

### Step 2 — 触发验证（Power Automate）

1. Power Automate 流读取待验证的月度数据文件。
2. 将数据转换为 JSON 格式后，通过 HTTP Action 或 Copilot Studio Connector 发送至 AI Agent。
3. AI Agent 按照 Instructions 中配置的规则逐行校验，返回 JSON 报告。

---

### Step 3 — 处理验证结果

Power Automate 解析 AI Agent 返回的 JSON 报告：

- `issues` 为空（`[]`）：数据全部通过，执行后续存档/通知流程。
- `issues` 不为空：根据 `severity` 和 `rule_code` 分类处理：
  - 通知数据提交方修正对应行。
  - 记录错误日志，可选：生成错误报告 Excel 回传。

---

### Step 4 — 归档（月末）

- 将当月已验证的数据文件和验证报告存档至指定位置（SharePoint / OneDrive）。
- 如有规则变更，直接更新 `Validation Rules.md` 并提交至本分支（`copilot/axa-validation-poc`），无需上传至知识模块。

---

## 职责分工 Responsibilities

| 角色 | 职责 |
|------|------|
| 数据提交方 | 按模板格式准备月度数据，按时提交 |
| 流程负责人 | 规则变更时直接更新 `Validation Rules.md` 并提交至本分支 |
| 系统维护方 | 维护 Power Automate 流、Copilot Studio AI Agent 配置；本分支代码/文档维护 |

---

## 文件命名规范 File Naming

| 文件类型 | 命名规范 | 示例 |
|----------|----------|------|
| 月度数据模板 | `Template_YYYYMM.xlsx` | `Template_202503.xlsx` |
| 验证报告 | `ValidationReport_YYYYMM.json` | `ValidationReport_202503.json` |
| 规则文档 | `Validation Rules.md`（固定） | — |

---

## 维护本分支 Maintaining This Branch

- **唯一分支**：`copilot/axa-validation-poc`
- 所有 POC 相关文档变更均在此分支提交，不再新建其他分支。
- 本目录（`workspace/AXA Validation POC/`）为唯一归档位置，不与仓库其他项目混淆。
- 如需新增文档，直接在本目录下创建，并提交至本分支。
