# POC-Validator

**Power Automate + Copilot Studio 数据验证 POC**

本项目记录 `tblOffshoring` Excel 表格数据验证流程的 POC 实现方案，包括：Power Automate Flow 设计、AI 验证规则、报告生成（Excel + PDF）、OneDrive 文件存储和 Teams 通知。

---

## 项目目录（本仓库）

```
projects/POC-Validator/
├── README.md                          本文件
└── docs/
    ├── runbook.md                     Power Automate Flow 操作手册
    ├── validation-rules.md            AI 验证规则清单（含 R-WS-ALL-001）
    ├── summary-sheet-cell-mapping.md  Summary Sheet 固定单元格映射表
    ├── teams-message-template.md      Teams 通知消息模板
    └── copilot-studio-api.md          Copilot Studio JSON 请求/响应示例
```

---

## OneDrive 根目录结构（POC 阶段）

所有输入文件、模板和输出产物统一存放在个人 OneDrive 的以下路径：

```
Documents/PowerbiTest/POC-Validator/
├── Templates/
│   └── ValidationReportTemplate.xlsx   Excel 报告模板（手动上传一次）
├── Outputs/
│   ├── Excel/                          Flow 生成的 Excel 验证报告
│   ├── PDF/                            Flow 生成的 HTML-to-PDF 验证报告
│   └── JSON/                           （可选）AI 返回的原始 JSON 结果
└── Inputs/                             （可选）存放待验证的源文件副本
```

> **注意**：`Templates/ValidationReportTemplate.xlsx` 需要在首次使用前手动上传至以上路径。模板文件需包含 `Summary` 工作表（固定单元格布局）和 `Issues` 工作表（表格 `tblIssues`）。

---

## 快速开始

1. 在个人 OneDrive 中手动创建上述目录结构
2. 上传 `ValidationReportTemplate.xlsx` 到 `Templates/` 目录
3. 参照 [`docs/runbook.md`](docs/runbook.md) 在 Power Automate 中搭建 Flow
4. 参照 [`docs/copilot-studio-api.md`](docs/copilot-studio-api.md) 配置 Copilot Studio Action
5. 手动触发 Flow，选择待验证的 Excel 源文件，验证 Flow 是否正常运行

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [runbook.md](docs/runbook.md) | Flow 每一步的详细配置方法 |
| [validation-rules.md](docs/validation-rules.md) | 所有 AI 验证规则（含优先级、字段范围、错误级别） |
| [summary-sheet-cell-mapping.md](docs/summary-sheet-cell-mapping.md) | Summary 工作表固定单元格坐标映射 |
| [teams-message-template.md](docs/teams-message-template.md) | Teams 通知的 Adaptive Card 模板 |
| [copilot-studio-api.md](docs/copilot-studio-api.md) | Copilot Studio 调用示例（请求 + 响应 JSON） |
