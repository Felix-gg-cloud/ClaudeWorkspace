# Offshoring Data Validation — AI-Only POC

## 项目概述

本项目是一个概念验证（POC），验证使用 **AI-Only 校验模式**对 SharePoint Excel 文件（表格 `tblOffshoring`）中的 Offshoring 数据进行逐行校验的可行性。

校验由 **Copilot Studio / LLM** 负责执行：Power Automate 只做数据读取与打包，把原始行数据和规则目录一并发送给 AI；AI 直接基于规则目录进行逐行校验，并返回结构化的 JSON issues 列表和 Markdown 汇总报告。

---

## AI-First 校验流程（概述）

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Power Automate                              │
│                                                                     │
│  1. 触发（定时 / 手动 / 文件变更）                                    │
│  2. 从 SharePoint Excel tblOffshoring 读取所有行（支持分页）          │
│  3. 按 YearMonth 分批（每批 ≤ N 行，避免 token 超限）                │
│  4. 为每批构造请求 payload：{ "rows": [...], "batchInfo": {...} }    │
│  5. 调用 Copilot Studio（HTTP action）                               │
│  6. 接收返回的 { "issues": [...], "report": "..." }                 │
│  7. 合并所有批次 issues，汇总写入 SharePoint / 发送 Teams 通知       │
└─────────────────────────────────────────────────────────────────────┘
                              │
                    HTTP POST payload
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Copilot Studio / LLM                            │
│                                                                     │
│  • 接收 rows 数组 + 规则目录（内嵌于系统提示词中）                    │
│  • 逐行、逐规则执行校验（temperature = 0，确定性输出）               │
│  • 返回 JSON issues 数组（固定 schema）+ Markdown 报告               │
└─────────────────────────────────────────────────────────────────────┘
```

### 与传统 Deterministic 校验的对比

| 维度           | Deterministic（Power Automate 写规则） | AI-Only（本 POC）                    |
|--------------|--------------------------------------|-------------------------------------|
| 规则维护       | 需修改 Flow 表达式                      | 只需更新 Prompt 中的规则目录           |
| 规则透明度     | 表达式散落在各 Action                   | 规则集中在 Prompt，一目了然            |
| 输出稳定性     | 完全确定性                              | temperature=0 近似确定性              |
| 报告可读性     | 需额外格式化                            | AI 直接生成自然语言报告                |
| Token 限制    | 无限制                                 | 大文件需分批（按 YearMonth）           |
| 适用场景       | 规则多、数据量极大                       | 规则复杂/经常变更、报告质量要求高       |

---

## 目录结构

```
projects/OffshoringValidation/
├── README.md                                  ← 本文件（项目概述 + 流程说明）
├── docs/
│   ├── POC-AI-Validation-Runbook.md           ← Power Automate 流程步骤详解
│   └── risk-mitigation.md                     ← 风险与缓解措施
├── prompts/
│   └── copilot-studio-validation.prompt.md   ← Copilot Studio 完整提示词模板
└── samples/
    ├── payload-sample.json                    ← Power Automate → Copilot Studio 请求示例
    └── expected-output-sample.json            ← Copilot Studio 返回结果示例
```

---

## 快速开始

1. 阅读 [`docs/POC-AI-Validation-Runbook.md`](docs/POC-AI-Validation-Runbook.md) 了解完整 Power Automate 流程步骤。
2. 将 [`prompts/copilot-studio-validation.prompt.md`](prompts/copilot-studio-validation.prompt.md) 中的内容作为 Copilot Studio Topic 的系统提示词。
3. 参考 [`samples/payload-sample.json`](samples/payload-sample.json) 构造 Power Automate 发送的请求体。
4. 对照 [`samples/expected-output-sample.json`](samples/expected-output-sample.json) 验证 AI 返回格式是否符合预期。
5. 阅读 [`docs/risk-mitigation.md`](docs/risk-mitigation.md) 了解 POC 风险与应对策略。
