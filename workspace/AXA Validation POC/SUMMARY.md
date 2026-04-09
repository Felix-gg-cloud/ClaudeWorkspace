# AXA Validation POC — 项目总结文档 / Project Summary

> 维护分支 / Maintained Branch：`axa-validation-poc`  
> 唯一目录 / Canonical Directory：`workspace/AXA Validation POC/`  
> 最后更新 / Last Updated：2026-03

---

## 目录 / Table of Contents

1. [项目简介与业务定位 / Project Introduction & Business Context](#1-项目简介与业务定位--project-introduction--business-context)
2. [整体技术架构与目录结构 / Technical Architecture & Directory Structure](#2-整体技术架构与目录结构--technical-architecture--directory-structure)
3. [自动化校验流程与操作链路 / Automated Validation Workflow](#3-自动化校验流程与操作链路--automated-validation-workflow)
4. [主要技术与平台 / Key Technologies & Platforms](#4-主要技术与平台--key-technologies--platforms)
5. [团队协作约定 / Team Collaboration Conventions](#5-团队协作约定--team-collaboration-conventions)
6. [日常维护与扩展建议 / Maintenance & Extension Guidelines](#6-日常维护与扩展建议--maintenance--extension-guidelines)

---

## 1. 项目简介与业务定位 / Project Introduction & Business Context

### 中文

AXA Validation POC（概念验证项目）旨在验证利用 AI 辅助工具（以 Copilot Studio 为核心）对 AXA 保险业务数据进行自动化规则校验的可行性。项目以每月标准数据模板（Excel）为基准，通过结构化的校验规则对提交数据进行合规性检查，确保数据质量和业务一致性，并为后续正式系统集成积累实践经验与技术储备。

**业务背景：**
- 每月需对批量险种/业务数据进行合规审查。
- 人工校验耗时长、错误率高，亟需自动化手段提升效率。
- POC 阶段聚焦"规则定义 → AI辅助校验 → 结果输出"的最小可行流程。

### English

The AXA Validation POC (Proof of Concept) project validates the feasibility of using AI-assisted tools—primarily Copilot Studio—to automate rule-based validation of AXA insurance business data. Using monthly standard data templates (Excel) as the reference baseline, the project applies structured validation rules to submitted data to ensure compliance, data quality, and business consistency, while building practical experience for future production system integration.

**Business Background:**
- Monthly batch compliance reviews are required for insurance/business data.
- Manual validation is time-consuming and error-prone; automation is needed to improve efficiency.
- The POC phase focuses on the minimal viable flow: Rule Definition → AI-Assisted Validation → Result Output.

---

## 2. 整体技术架构与目录结构 / Technical Architecture & Directory Structure

### 中文

#### 技术架构概览

```
┌─────────────────────────────────────────────────────┐
│                   业务数据输入层                      │
│         (每月 Excel 数据文件 / 人工上传)               │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│               AI 校验处理层（Copilot Studio）         │
│  · 读取 Validation Rules.md（规则知识文档）            │
│  · 解析数据结构，执行字段级校验                        │
│  · 生成校验结果报告（通过 / 异常 / 详细说明）           │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│              自动化触发与集成层                        │
│  · Power Automate / GitHub Actions                  │
│  · 触发校验流程，汇总结果，通知相关人员                 │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│              文档与版本管理层（GitHub）                │
│  · 唯一分支：axa-validation-poc                      │
│  · 唯一目录：workspace/AXA Validation POC/           │
│  · 规则文档：Validation Rules.md                     │
└─────────────────────────────────────────────────────┘
```

#### 目录结构

```
workspace/
└── AXA Validation POC/
    ├── SUMMARY.md                  # 本总结文档
    ├── Validation Rules.md         # 校验规则主文档（持续维护）
    ├── data/
    │   └── template_YYYY-MM.xlsx   # 每月标准数据模板（参考基准）
    ├── samples/
    │   └── sample_YYYY-MM.xlsx     # 月度上传数据样本
    └── outputs/
        └── validation_YYYY-MM.md  # 校验结果输出报告
```

### English

#### Technical Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                 Business Data Input Layer             │
│         (Monthly Excel data files / Manual upload)    │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│          AI Validation Layer (Copilot Studio)         │
│  · Reads Validation Rules.md (rule knowledge doc)    │
│  · Parses data structure, performs field-level check │
│  · Generates validation result report (Pass/Fail)    │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│          Automation & Integration Layer               │
│  · Power Automate / GitHub Actions                   │
│  · Triggers validation flows, aggregates results,    │
│    and notifies relevant stakeholders                │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│         Document & Version Management Layer (GitHub)  │
│  · Single branch: axa-validation-poc                 │
│  · Single directory: workspace/AXA Validation POC/  │
│  · Rule document: Validation Rules.md                │
└─────────────────────────────────────────────────────┘
```

#### Directory Structure

```
workspace/
└── AXA Validation POC/
    ├── SUMMARY.md                  # This summary document
    ├── Validation Rules.md         # Primary validation rules doc (continuously maintained)
    ├── data/
    │   └── template_YYYY-MM.xlsx   # Monthly standard data template (reference baseline)
    ├── samples/
    │   └── sample_YYYY-MM.xlsx     # Monthly uploaded data samples
    └── outputs/
        └── validation_YYYY-MM.md  # Validation result output reports
```

---

## 3. 自动化校验流程与操作链路 / Automated Validation Workflow

### 中文

#### 整体流程

```
每月数据到达
     │
     ▼
[步骤1] 人工/自动上传数据文件
  · 将当月业务数据（Excel）上传至指定位置
  · 命名规范：sample_YYYY-MM.xlsx
     │
     ▼
[步骤2] 触发 AI 校验（Copilot Studio Agent）
  · Agent 读取 Validation Rules.md 中的当前规则
  · 对上传数据逐字段、逐规则进行校验
  · 规则类型包括：必填项检查、格式校验、值域范围、跨字段逻辑等
     │
     ▼
[步骤3] 生成校验结果
  · 输出结构化报告，标注通过项与异常项
  · 异常项附带规则说明与修正建议
  · 报告保存至 outputs/ 目录
     │
     ▼
[步骤4] 自动化通知（可选 Power Automate / GitHub Actions）
  · 将校验结果摘要发送给相关业务负责人
  · 如有严重异常，触发人工复核流程
     │
     ▼
[步骤5] 规则更新（按需）
  · 如业务规则发生变更，直接修改 Validation Rules.md
  · 提交至 axa-validation-poc 分支
  · 无需重新训练模型，次月自动生效
```

#### 关键设计决策

- **规则与 AI 解耦**：规则以 Markdown 文档维护，而非内嵌 AI Instructions，降低维护成本，提高可读性。
- **放弃知识库同步**：鉴于 AI 输出稳定性问题，不再将规则文档同步至 Copilot Studio 知识模块，改为直接在 Agent 对话中引用文档内容。
- **单文件规则管理**：所有校验规则集中在 `Validation Rules.md` 中，便于团队审查与版本对比。

### English

#### Overall Workflow

```
Monthly data arrives
     │
     ▼
[Step 1] Manual/Automatic data file upload
  · Upload current month's business data (Excel) to designated location
  · Naming convention: sample_YYYY-MM.xlsx
     │
     ▼
[Step 2] Trigger AI Validation (Copilot Studio Agent)
  · Agent reads current rules from Validation Rules.md
  · Validates each field against each rule in sequence
  · Rule types include: required field checks, format validation,
    value range checks, cross-field logic validation
     │
     ▼
[Step 3] Generate Validation Results
  · Outputs structured report marking passed and failed items
  · Failed items include rule descriptions and correction suggestions
  · Reports saved to outputs/ directory
     │
     ▼
[Step 4] Automated Notification (optional: Power Automate / GitHub Actions)
  · Sends validation result summary to relevant business owners
  · Critical failures trigger manual review workflow
     │
     ▼
[Step 5] Rule Updates (as needed)
  · When business rules change, directly edit Validation Rules.md
  · Commit to axa-validation-poc branch
  · No model retraining required; changes take effect next month
```

#### Key Design Decisions

- **Rules decoupled from AI**: Rules are maintained as a Markdown document rather than embedded in AI Instructions, reducing maintenance overhead and improving readability.
- **Knowledge base sync abandoned**: Due to AI output instability, rule documents are no longer synced to Copilot Studio's knowledge module; instead, document content is referenced directly in Agent conversations.
- **Single-file rule management**: All validation rules are centralized in `Validation Rules.md` for easy team review and version comparison.

---

## 4. 主要技术与平台 / Key Technologies & Platforms

### 中文

| 技术 / 平台 | 用途 | 说明 |
|---|---|---|
| **Microsoft Copilot Studio** | AI 校验 Agent 核心 | 配置 Agent Instructions，调用规则文档执行校验，生成结果报告 |
| **Power Automate** | 自动化触发与通知 | 月度触发数据上传流程，汇总结果，邮件/Teams 通知 |
| **GitHub Actions** | CI/CD 与版本管理 | 管理规则文档版本变更，可配置自动化校验触发器 |
| **Markdown（.md）** | 规则知识文档格式 | `Validation Rules.md` 以 Markdown 编写，结构清晰，AI 可直接读取解析 |
| **Excel / XLSX** | 数据载体 | 每月标准模板与上传样本均为 Excel 格式，与业务系统兼容 |
| **JSON** | 数据交换与校验配置 | 可用于结构化规则配置或 API 数据交换（扩展场景） |
| **GitHub（ClaudeWorkspace 仓库）** | 文档与代码版本管理 | 唯一分支 `axa-validation-poc`，唯一目录 `workspace/AXA Validation POC/` |

### English

| Technology / Platform | Usage | Notes |
|---|---|---|
| **Microsoft Copilot Studio** | Core AI validation Agent | Configures Agent Instructions, references rule documents for validation, generates result reports |
| **Power Automate** | Automation trigger & notification | Monthly data upload flow triggers, result aggregation, email/Teams notifications |
| **GitHub Actions** | CI/CD & version management | Manages rule document version changes; can configure automated validation triggers |
| **Markdown (.md)** | Rule knowledge document format | `Validation Rules.md` written in Markdown—clear structure, directly readable and parseable by AI |
| **Excel / XLSX** | Data carrier | Monthly standard templates and uploaded samples in Excel format, compatible with business systems |
| **JSON** | Data exchange & validation config | Used for structured rule configuration or API data exchange (extended scenarios) |
| **GitHub (ClaudeWorkspace repo)** | Document & code version management | Single branch `axa-validation-poc`, single directory `workspace/AXA Validation POC/` |

---

## 5. 团队协作约定 / Team Collaboration Conventions

### 中文

#### 5.1 唯一目录、唯一分支原则

- **所有 POC 相关内容**（文档、数据、规则、输出结果）一律存放于 `workspace/AXA Validation POC/` 目录下，不得分散至其他位置。
- **开发与维护全程在 `axa-validation-poc` 分支进行**，禁止为单次操作新建额外分支，以防分支混乱。
- 若确有需要进行实验性变更，完成后须及时合并回主分支并删除临时分支。

#### 5.2 规则文档维护约定

- `Validation Rules.md` 是本项目的**唯一规则来源（Single Source of Truth）**。
- 每次业务规则变更后，**当责人须在 7 天内更新 Validation Rules.md 并提交到分支**。
- 更新时须注明变更日期、变更原因及变更内容摘要（可在文档顶部维护变更日志）。
- **不再将规则同步至 Copilot Studio 知识模块**（已放弃，原因：AI 输出不稳定）。

#### 5.3 文件命名规范

| 文件类型 | 命名格式 | 示例 |
|---|---|---|
| 数据模板 | `template_YYYY-MM.xlsx` | `template_2026-03.xlsx` |
| 上传样本 | `sample_YYYY-MM.xlsx` | `sample_2026-03.xlsx` |
| 校验输出 | `validation_YYYY-MM.md` | `validation_2026-03.md` |

#### 5.4 提交与 PR 规范

- Commit message 使用中文或英文均可，需简洁描述变更内容。
- 每次规则更新建议单独提交，便于回溯。
- 无需走 PR 审核流程（小团队可直接推送到 `axa-validation-poc` 分支），如团队规模扩大可按需开启保护规则。

### English

#### 5.1 Single Directory, Single Branch Principle

- **All POC-related content** (documents, data, rules, output results) must reside in `workspace/AXA Validation POC/`—no scattering to other locations.
- **All development and maintenance occurs on the `axa-validation-poc` branch**. Creating additional branches for individual operations is prohibited to prevent branch proliferation.
- If experimental changes are necessary, merge them back to the main branch and delete the temporary branch promptly.

#### 5.2 Rule Document Maintenance Conventions

- `Validation Rules.md` is the **Single Source of Truth** for all validation rules in this project.
- After each business rule change, the **responsible person must update Validation Rules.md and commit within 7 days**.
- Updates must include the change date, reason, and a brief summary of changes (maintain a changelog at the top of the document).
- **Rules are no longer synced to Copilot Studio's knowledge module** (abandoned due to AI output instability).

#### 5.3 File Naming Conventions

| File Type | Naming Format | Example |
|---|---|---|
| Data template | `template_YYYY-MM.xlsx` | `template_2026-03.xlsx` |
| Uploaded sample | `sample_YYYY-MM.xlsx` | `sample_2026-03.xlsx` |
| Validation output | `validation_YYYY-MM.md` | `validation_2026-03.md` |

#### 5.4 Commit & PR Conventions

- Commit messages may be in Chinese or English; keep them concise and descriptive.
- Each rule update should be a separate commit for easy traceability.
- No PR review required for small teams (push directly to `axa-validation-poc`); enable branch protection rules as the team grows.

---

## 6. 日常维护与扩展建议 / Maintenance & Extension Guidelines

### 中文

#### 6.1 月度维护清单

- [ ] 将当月标准模板文件（`template_YYYY-MM.xlsx`）存入 `data/` 目录。
- [ ] 收集业务侧提交的数据样本（`sample_YYYY-MM.xlsx`），存入 `samples/` 目录。
- [ ] 使用 Copilot Studio Agent 执行校验，将结果报告存入 `outputs/` 目录。
- [ ] 如有规则变更，更新 `Validation Rules.md` 并附变更说明。
- [ ] 提交所有变更到 `axa-validation-poc` 分支。

#### 6.2 扩展建议

1. **规则版本化**：在 `Validation Rules.md` 顶部增加变更日志表格，记录每次规则修改历史，便于审计。
2. **结果归档自动化**：可通过 Power Automate 或 GitHub Actions 自动将校验结果邮件发送给业务负责人，减少人工转发。
3. **规则格式标准化**：如规则数量增多，可将部分规则转换为 JSON Schema 格式，配合脚本进行更精确的程序化校验（作为 AI 校验的补充）。
4. **多语言支持**：如涉及跨国业务数据，可在规则文档中增加多语言说明，确保各地区团队理解一致。
5. **定期回顾**：建议每季度回顾一次校验规则的覆盖率与准确性，根据业务变化及时调整。

#### 6.3 注意事项

- **AI 输出稳定性**：Copilot Studio AI 的输出可能因提问方式不同而有所差异，建议每次校验使用固定的提示词模板，确保结果一致性。
- **数据安全**：上传的业务数据可能包含敏感信息，请确保仓库访问权限设置合理，避免数据泄露。
- **规则歧义**：如某条规则表述模糊，AI 理解可能出现偏差，建议规则描述尽量具体，并附带正反例。
- **Excel 兼容性**：不同版本的 Excel 文件格式（`.xls` vs `.xlsx`）可能影响解析，统一使用 `.xlsx` 格式。
- **分支纪律**：严格遵守"唯一分支"约定，避免历史混乱重演。

### English

#### 6.1 Monthly Maintenance Checklist

- [ ] Save the current month's standard template (`template_YYYY-MM.xlsx`) to the `data/` directory.
- [ ] Collect business-submitted data samples (`sample_YYYY-MM.xlsx`) and save to the `samples/` directory.
- [ ] Run validation using Copilot Studio Agent and save result reports to the `outputs/` directory.
- [ ] If rules have changed, update `Validation Rules.md` with a change description.
- [ ] Commit all changes to the `axa-validation-poc` branch.

#### 6.2 Extension Recommendations

1. **Rule versioning**: Add a changelog table at the top of `Validation Rules.md` to record each rule modification history for audit purposes.
2. **Automated result archiving**: Use Power Automate or GitHub Actions to automatically email validation results to business owners, reducing manual forwarding.
3. **Rule format standardization**: As the number of rules grows, consider converting some rules to JSON Schema format for more precise programmatic validation (as a complement to AI validation).
4. **Multilingual support**: For cross-border business data, add multilingual descriptions to rule documents to ensure consistent understanding across regional teams.
5. **Periodic review**: Recommend quarterly reviews of validation rule coverage and accuracy, adjusting promptly based on business changes.

#### 6.3 Important Notes

- **AI output stability**: Copilot Studio AI output may vary depending on how questions are phrased. Use fixed prompt templates for each validation run to ensure consistent results.
- **Data security**: Uploaded business data may contain sensitive information. Ensure repository access permissions are set appropriately to prevent data leakage.
- **Rule ambiguity**: Vaguely worded rules may cause AI misunderstanding. Keep rule descriptions specific and include both positive and negative examples where possible.
- **Excel compatibility**: Different Excel file formats (`.xls` vs `.xlsx`) may affect parsing. Standardize on `.xlsx` format throughout.
- **Branch discipline**: Strictly follow the "single branch" convention to avoid repeating the historical branch proliferation issues.

---

*文档维护人 / Document Maintainer：AXA Validation POC 团队*  
*分支 / Branch：`axa-validation-poc`*  
*目录 / Directory：`workspace/AXA Validation POC/`*
