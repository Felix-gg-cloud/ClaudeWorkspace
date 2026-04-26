# Operation Guide — AXA Validation POC

> 本文档描述每月的操作流程、职责分工和知识库维护方法。  
> This document describes the monthly operation workflow, responsibilities, and knowledge base maintenance.

---

## 系统架构概览 System Overview

```
Power Automate (Automate)
    │
    │  发送待验证数据（JSON/CSV）
    ▼
Copilot Studio AI Agent
    │  读取知识模块中的规则和模板
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

### Step 1 — 更新知识模块（月初）

在 Copilot Studio 的知识模块（Knowledge）中确认以下文件为最新版本：

| 文件 | 说明 | 命名规范 |
|------|------|----------|
| `Validation Rules.md` | 校验规则（如无变动则无需重新上传） | 固定文件名 |
| 当月数据模板 | 包含本月合法 Function、Team 及其组合的参考 xlsx/csv | 建议命名：`Template_YYYYMM.xlsx` |

> ⚠️ 如校验规则有变更，请先更新 `Validation Rules.md`（本目录），再上传至知识模块。

---

### Step 2 — 触发验证（Power Automate）

1. Power Automate 流读取待验证的月度数据文件。
2. 将数据转换为 JSON 格式后，通过 HTTP Action 或 Copilot Studio Connector 发送至 AI Agent。
3. AI Agent 按照知识模块中的规则和模板逐行校验，返回 JSON 报告。

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
- 如有规则变更，同步更新 `Validation Rules.md` 并提交至本分支（`copilot/axa-validation-poc`）。

---

## 职责分工 Responsibilities

| 角色 | 职责 |
|------|------|
| 数据提交方 | 按模板格式准备月度数据，按时提交 |
| 流程负责人 | 每月月初更新知识模块中的数据模板；规则变更时更新 `Validation Rules.md` |
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
