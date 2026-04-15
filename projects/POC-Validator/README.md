# POC-Validator

Power Automate + Copilot Studio 数据验证 POC 项目。

## 文档

| 文档 | 说明 |
|---|---|
| [Excel 输入文件准备指南](docs/runbook-excel-table-setup.md) | 如何将标题在第 5 行的 Excel 文件转换为 `tblOffshoring` 表格，以供 Power Automate 正确读取 |
| [Power Automate 逐步搭建指南](docs/runbook-power-automate-build-guide.md) | 新手完整版点选教程：创建 Flow、读 Excel、开 Pagination、初始化变量、循环构建行数据 |

## SharePoint 文件夹结构

```
Documents/
└── POC-Validator/
    ├── Inputs/
    │   └── offshoring_<YearMonth>.xlsx      ← 输入文件（含 tblOffshoring）
    ├── Templates/
    │   ├── ValidationReportTemplate.xlsx    ← Excel 报告模板（含 tblIssues）
    │   └── ValidationReportTemplate.docx    ← Word 报告模板
    ├── Outputs/
    │   ├── Excel/
    │   ├── Word/
    │   └── PDF/
    └── JSON/
```
