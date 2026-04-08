# AXA Validation POC — 项目总结 / Project Summary

> **维护分支 / Maintenance Branch:** `axa-validation-poc`  
> **唯一内容目录 / Single Content Directory:** `workspace/AXA Validation POC/`
>
> 本文对 AXA Validation POC 的目标、架构、流程、技术选型与协作方式做统一说明，方便交付、复盘与后续维护。
>
> This document provides a unified reference for the AXA Validation POC covering goals, architecture, workflow, technology stack, and collaboration model — for delivery, review, and ongoing maintenance.

---

## 目录 / Table of Contents

1. [项目背景与目标 / Background & Objectives](#1-项目背景与目标--background--objectives)
2. [目录结构 / Directory Structure](#2-目录结构--directory-structure)
3. [高层架构 / High-level Architecture](#3-高层架构--high-level-architecture)
4. [端到端校验流程 / End-to-end Validation Flow](#4-端到端校验流程--end-to-end-validation-flow)
5. [校验规则体系 / Rule System](#5-校验规则体系--rule-system)
6. [技术栈与平台 / Technology Stack & Platforms](#6-技术栈与平台--technology-stack--platforms)
7. [协作方式与分支策略 / Collaboration & Branch Strategy](#7-协作方式与分支策略--collaboration--branch-strategy)
8. [维护与扩展建议 / Maintenance & Extension Tips](#8-维护与扩展建议--maintenance--extension-tips)
9. [变更记录 / Change Log](#9-变更记录--change-log)

---

## 1. 项目背景与目标 / Background & Objectives

### 中文

AXA Validation POC 的目标是实现一套"可自动化调用"的数据校验能力，用于对批量 Excel（或由其转换而来的结构化 JSON 数据）逐行进行规则校验，输出标准化的校验结果（Errors / Warnings），以便在自动化流程中拦截异常数据并指导修复。

**核心目标：**

- 将 Excel 行数据转换为结构化输入（JSON），并对每一行应用全量校验规则。
- 输出结构化校验报告（JSON），便于下游自动化流程处理（如阻断、回写、通知）。
- 规则可维护、可扩展、可追踪（以规则编号 RuleId 管理）。
- 项目资产集中在单一目录、单一分支中，降低协作与定位成本。

### English

The AXA Validation POC aims to provide an automation-friendly data validation capability to validate batch Excel rows (or their JSON-converted representation) against a defined rule set, producing standardized outputs (Errors / Warnings) that can be used in automated workflows to block invalid submissions and guide remediation.

**Key goals:**

- Convert Excel row data into structured JSON and apply the full rule set to every row.
- Output a structured validation report (JSON) for downstream automation (blocking, write-back, notifications).
- Keep rules maintainable, extensible, and traceable via unique Rule IDs.
- Consolidate all project assets under a single directory and a single long-lived branch to minimize coordination overhead.

---

## 2. 目录结构 / Directory Structure

### 中文

本 POC 的所有内容统一归档在以下目录（唯一入口）：

```
workspace/AXA Validation POC/
├── SUMMARY.md            # 本总结文档（架构、流程、技术栈、协作方式）
└── Validation Rules.md   # 校验规则主文档（RuleId、严重级别、判定条件）
```

**各文件说明：**

| 文件 | 用途 |
|---|---|
| `SUMMARY.md` | 项目整体总结，供团队交付与交接使用 |
| `Validation Rules.md` | 校验规则权威文档，规则变更时首先更新此文件 |

### English

All POC assets are consolidated under the following single entry directory:

```
workspace/AXA Validation POC/
├── SUMMARY.md            # This summary document (architecture, workflow, tech stack, collaboration)
└── Validation Rules.md   # Validation rules master document (RuleId, severity, conditions)
```

**File reference:**

| File | Purpose |
|---|---|
| `SUMMARY.md` | Overall project summary for team delivery and handover |
| `Validation Rules.md` | Authoritative rules document; update this first when rules change |

---

## 3. 高层架构 / High-level Architecture

### 中文

该 POC 的逻辑架构分为三层：

```
┌─────────────────────────────────────────────────┐
│  1. 数据输入层 (Data Ingestion)                   │
│  Excel（人工维护）→ 逐行 JSON（含 RowIndex）       │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│  2. 校验执行层 (Validation Engine)                │
│  Copilot Studio / AI 能力                        │
│  • 对每行应用全量规则（Required / Format /        │
│    Type / Range / Mapping 等）                   │
│  • 非对话模式：不提问、不解释、不省略              │
│  • 每条问题逐条输出（不合并、不简写）              │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│  3. 结果输出层 (Reporting)                        │
│  固定 Schema 的 JSON：                           │
│  { report_model: {...}, issues: [...] }          │
│  下游基于 errors_total 决定阻断或放行             │
└─────────────────────────────────────────────────┘
```

**关键设计原则：**

- **非对话型引擎**：AI 校验时不与用户交互，不提问，只输出结果。
- **固定输出 Schema**：输出格式严格固定，便于下游系统解析。
- **规则全量执行**：每次校验必须对所有规则逐一判断，不允许跳过。

### English

The POC logical architecture is organized in three layers:

```
┌─────────────────────────────────────────────────┐
│  1. Data Ingestion                              │
│  Excel (user-maintained) → row-by-row JSON      │
│  (each row preserves RowIndex)                  │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│  2. Validation Execution                        │
│  Copilot Studio / AI capability                 │
│  • Applies all rules to every row               │
│    (Required / Format / Type / Range / Mapping) │
│  • Non-conversational: no clarifications,       │
│    no summaries, no omissions                   │
│  • Every issue emitted individually             │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│  3. Reporting                                   │
│  Fixed-schema JSON output:                      │
│  { report_model: {...}, issues: [...] }          │
│  Downstream decides to block or proceed         │
│  based on errors_total                          │
└─────────────────────────────────────────────────┘
```

**Key design principles:**

- **Non-conversational engine**: AI validates without interacting with users — output only, no questions.
- **Fixed output schema**: Strict output format enables reliable downstream parsing.
- **Full rule execution**: Every rule must be evaluated on every row; no skipping allowed.

---

## 4. 端到端校验流程 / End-to-end Validation Flow

### 中文

```
业务人员维护 Excel
        │
        ▼
Power Automate / 自动化流程
  将每行解析为 JSON 数组（保留 RowIndex）
  构造 payload：{ meta?, rows: [...] }
        │
        ▼
调用 Copilot Studio / AI 校验能力
  输入：{ meta?, rows }
        │
        ▼
AI 校验引擎执行
  对 rows 中每行应用全部规则
  生成 issues 列表（含 severity / rowIndex / column / value / suggestion）
        │
        ▼
返回固定格式 JSON
  {
    report_model: { total_rows, errors_total, warnings_total },
    issues: [ { ruleId, severity, rowIndex, column, value, message, suggestion }, ... ]
  }
        │
        ▼
下游判断与处理
  errors_total > 0 → 阻断流程 + 通知 + 生成修复清单
  errors_total = 0 → 允许进入后续步骤（写入系统 / 报表等）
  warnings_total  → 提醒但不阻断（按需）
```

**流程关键约定：**

- `RowIndex` 必须从输入一直传递到输出，确保问题可定位回原始 Excel 行。
- 每条 issue 单独输出，不合并同类项，确保下游可逐条处理。
- 严重级别：`ERROR`（阻断）、`WARNING`（提醒，不阻断）。

### English

```
Business user maintains Excel
        │
        ▼
Power Automate / automation flow
  Parses each row into JSON array (preserving RowIndex)
  Builds payload: { meta?, rows: [...] }
        │
        ▼
Calls Copilot Studio / AI validation capability
  Input: { meta?, rows }
        │
        ▼
AI validation engine executes
  Applies all rules to every row in rows
  Generates issues list (severity / rowIndex / column / value / suggestion)
        │
        ▼
Returns fixed-schema JSON
  {
    report_model: { total_rows, errors_total, warnings_total },
    issues: [ { ruleId, severity, rowIndex, column, value, message, suggestion }, ... ]
  }
        │
        ▼
Downstream processing
  errors_total > 0 → block flow + notify + generate remediation list
  errors_total = 0 → proceed to next steps (write to systems / reports)
  warnings_total   → alert but do not block (as needed)
```

**Key flow conventions:**

- `RowIndex` must be propagated from input through to output so each issue can be traced back to the original Excel row.
- Each issue is emitted individually — no grouping — so downstream systems can process them one by one.
- Severity levels: `ERROR` (blocking), `WARNING` (non-blocking alert).

---

## 5. 校验规则体系 / Rule System

### 中文

规则以「编号 + 严重级别 + 描述」方式维护，便于扩展与追踪。权威来源为 [`Validation Rules.md`](./Validation%20Rules.md)。

**当前已定义的规则类别：**

| 规则类型 | 示例 | 严重级别 |
|---|---|---|
| 必填校验 | YearMonth 不为空 | ERROR |
| 格式校验 | YearMonth 必须为 YYYYMM 格式 | ERROR |
| 字段类型校验 | 文本字段不得为纯数字；数值字段必须可解析为数值 | ERROR |
| 数值范围校验 | ShoringRatio ≤ 1 | ERROR |
| Total 行特殊策略 | 指定字段允许为空，其他规则仍适用 | — |
| 字典一致性 | Function / Team 值必须与参考模板一致 | ERROR |
| 组合一致性 | Function + Team 的组合必须在参考模板中存在 | ERROR |

**规则管理约定：**

- 每条规则有唯一 RuleId，便于在 issue 中引用与追踪。
- 新增规则时分配新 RuleId，保持向后兼容与可追溯性。
- 规则变更时直接更新 `Validation Rules.md`，无需同步到其他系统。

### English

Rules are maintained as "Rule ID + Severity + Description" for traceability and extension. The authoritative source is [`Validation Rules.md`](./Validation%20Rules.md).

**Current rule categories:**

| Rule Type | Example | Severity |
|---|---|---|
| Required check | YearMonth must not be blank | ERROR |
| Format check | YearMonth must match YYYYMM pattern | ERROR |
| Type check | Text fields must not be numeric-only; numeric fields must be parseable as numbers | ERROR |
| Range check | ShoringRatio ≤ 1 | ERROR |
| Total row handling | Specific columns allowed blank; other rules still apply | — |
| Dictionary consistency | Function / Team values must match reference template | ERROR |
| Combination consistency | Function + Team combination must exist in reference template | ERROR |

**Rule management conventions:**

- Every rule has a unique RuleId referenced in each emitted issue.
- Assign a new RuleId when adding rules; maintain backward compatibility and traceability.
- Update `Validation Rules.md` directly when rules change — no sync to any other system required.

---

## 6. 技术栈与平台 / Technology Stack & Platforms

### 中文

| 技术 / 平台 | 用途 |
|---|---|
| **GitHub** | 版本管理与团队协作（文档、规则、样例等），唯一维护分支 `axa-validation-poc` |
| **Copilot Studio / AI** | 校验执行端：接收 JSON 输入，执行规则，返回固定格式 JSON 结果 |
| **Power Automate** | 编排与集成层：触发校验、解析结果、实现阻断 / 通知 / 写回等下游动作 |
| **Markdown** | 规则与说明文档格式，便于版本化、审阅与协作 |
| **JSON** | 输入输出契约格式，便于系统集成与自动化处理 |
| **Excel** | 业务数据载体，由人工维护后转换为 JSON 输入 |

### English

| Technology / Platform | Purpose |
|---|---|
| **GitHub** | Version control and team collaboration (docs, rules, samples); single long-lived branch `axa-validation-poc` |
| **Copilot Studio / AI** | Validation execution: receives JSON input, runs rules, returns fixed-schema JSON result |
| **Power Automate** | Orchestration and integration: triggers validation, parses results, implements blocking / notifications / write-back |
| **Markdown** | Rules and documentation format — versioned, reviewable, collaborative |
| **JSON** | I/O contract format — integration- and automation-friendly |
| **Excel** | Business data carrier; maintained by users and converted to JSON for input |

---

## 7. 协作方式与分支策略 / Collaboration & Branch Strategy

### 中文

为避免混乱与分支泛滥，本 POC 采用「唯一目录 + 唯一分支」的维护模式：

| 项目 | 约定 |
|---|---|
| 唯一内容目录 | `workspace/AXA Validation POC/` |
| 唯一长期维护分支 | `axa-validation-poc` |
| 无关内容 | 不放入该目录；不在该分支上提交无关变更 |

**团队协作约定：**

1. **所有与 POC 相关的文件** 仅存放于 `workspace/AXA Validation POC/` 目录下。
2. **规则变更** 直接更新 `Validation Rules.md`，无需同步到其他知识库系统。
3. **文件更新** 以小步提交为佳，每次提交保持变更可理解、可回溯。
4. **每月数据模板** 由业务人员在 Excel 中维护，无需上传至本仓库；有变化时更新规则文档即可。
5. **新增规则或修正** 时，直接在 `Validation Rules.md` 中新增或修改对应条目，并更新本文档的规则类别说明。

### English

To avoid confusion and branch sprawl, this POC follows a "single directory + single long-lived branch" model:

| Item | Convention |
|---|---|
| Single content directory | `workspace/AXA Validation POC/` |
| Single long-lived branch | `axa-validation-poc` |
| Unrelated content | Must NOT be placed in this directory or committed to this branch |

**Team collaboration conventions:**

1. **All POC-related assets** must live under `workspace/AXA Validation POC/` only.
2. **Rule updates** go directly to `Validation Rules.md` — no sync to any external knowledge base system.
3. **File changes** should use small, descriptive commits for traceability and reviewability.
4. **Monthly data templates** are maintained by business users in Excel; they do not need to be committed to this repository — update the rules document instead when the template changes.
5. **New rules or corrections** are added or modified directly in `Validation Rules.md`; update the rule category table in this document accordingly.

---

## 8. 维护与扩展建议 / Maintenance & Extension Tips

### 中文

- **固化输入/输出 Schema**：建议后续补充正式的 JSON Schema（`schemas/` 子目录），降低对接风险，便于契约验证。
- **建立回归样例**：维护一套合法/非法输入输出样例（`samples/` 子目录），用于规则变更后的回归验证。
- **规则模块化**：当规则数量增大时，可将 `Validation Rules.md` 按主题拆分为多个章节，但保持 RuleId 全局唯一。
- **模板对齐机制**：对「字典一致性 / 组合一致性」类规则，明确参考模板的更新周期与对齐触发条件，防止规则与实际模板失步。
- **AI 输出稳定性**：本 POC 已验证通过 Instructions 精确定义输出格式和规则引用方式，以保障 AI 输出一致性。知识模块同步方案已评估后舍弃，现通过维护规则文档方式保持稳定。

### English

- **Formalize JSON Schemas**: Consider adding formal JSON Schema files (under a `schemas/` subdirectory) to reduce integration risk and enable contract validation.
- **Build regression samples**: Maintain a set of valid/invalid input-output pairs (under a `samples/` subdirectory) for regression testing after rule changes.
- **Modularize rules**: As the rule count grows, organize `Validation Rules.md` into thematic sections while keeping globally unique Rule IDs.
- **Template alignment**: For dictionary/combination consistency rules, define the reference template update cadence and alignment trigger conditions to prevent drift between rules and the actual template.
- **AI output stability**: This POC has validated that precisely defining the output format and rule references in Copilot Studio Instructions ensures consistent AI outputs. The knowledge-module sync approach was evaluated and discontinued; stability is now achieved by maintaining the rules document directly.

---

## 9. 变更记录 / Change Log

| 日期 / Date | 版本 / Version | 变更说明 / Description |
|---|---|---|
| 2026-03 | v1.0 | 初始化版本：建立 POC 目录与规则文档，形成总结说明 / Initial version: POC directory established with rules document and summary |

> 后续变更请在此表格追加记录。
>
> Append new entries to this table for future updates.

---

*本文档由团队共同维护，唯一入口：`workspace/AXA Validation POC/SUMMARY.md`，唯一分支：`axa-validation-poc`。*

*This document is team-maintained. Single entry: `workspace/AXA Validation POC/SUMMARY.md`, single branch: `axa-validation-poc`.*
