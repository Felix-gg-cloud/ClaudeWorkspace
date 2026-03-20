# POC-Validator

Power Automate + Copilot Studio（AI-only）数据校验 POC 项目。

## 目标

用 Power Automate 手动触发，读取个人 OneDrive 上的 Offshoring Excel 文件（表：`tblOffshoring`），  
由 Copilot Studio AI 进行规则校验，输出：

- **Excel 报告**（全量 issues）→ OneDrive `Outputs/Excel/`
- **PDF 报告**（Top N issues 摘要）→ OneDrive `Outputs/PDF/`
- **Teams 通知**（附两个文件链接）

## 目录结构

```
POC-Validator/
├── README.md                  # 本文件
└── docs/
    ├── 01-Excel-Template-Guide.md   # ValidationReportTemplate.xlsx 创建手顺
    └── 02-Power-Automate-Runbook.md # Power Automate Flow 逐步操作手顺
```

## OneDrive 目录约定

```
Documents/PowerbiTest/POC-Validator/
├── Templates/
│   └── ValidationReportTemplate.xlsx   ← 先按文档创建此文件
├── Inputs/                              ← （可选）放源数据
├── Outputs/
│   ├── Excel/                           ← Flow 输出 xlsx 报告
│   └── PDF/                             ← Flow 输出 pdf 报告
```

## 快速开始

1. 阅读 [01-Excel-Template-Guide.md](docs/01-Excel-Template-Guide.md)，先在 OneDrive 创建模板文件。
2. 阅读 [02-Power-Automate-Runbook.md](docs/02-Power-Automate-Runbook.md)，搭建 Power Automate Flow。
3. 手动触发 Flow，验证输出报告。

## 后续迁移路径（POC → 生产）

| 阶段 | 输入 | 输出 | 触发 |
|------|------|------|------|
| POC  | 个人 OneDrive | 个人 OneDrive + Teams | 手动 |
| 生产 | 团队 SharePoint | 团队 SharePoint + 邮件 | 定时/事件 |

迁移时，Flow 逻辑不需要重写，只需将 OneDrive 连接器换成 SharePoint 连接器，更新文件路径即可。
