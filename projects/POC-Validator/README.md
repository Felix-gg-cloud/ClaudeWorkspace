# POC-Validator

Power Automate Flow POC：对 Offshoring/Onshoring 数据进行数据质量验证，并将结果输出为 Excel/PDF 报告，通过 Teams 通知相关人员。

## 目录结构

```
POC-Validator/
├── docs/
│   ├── runbook-sharepoint-setup.md         # 主 Runbook：SharePoint 迁移与配置
│   └── ValidationReportTemplate-spec.md   # Excel 模板非二进制规格说明
└── templates/
    └── office-scripts/
        └── bulkInsertIssues.ts             # Office Scripts：批量写入 tblIssues
```

## 快速开始

1. **阅读主 Runbook**：[docs/runbook-sharepoint-setup.md](docs/runbook-sharepoint-setup.md)
   - 包含完整的 SharePoint 目录创建步骤
   - Power Automate Flow 动作逐步配置
   - 共享账户连接与权限设置

2. **创建 Excel 模板**：参考 [docs/ValidationReportTemplate-spec.md](docs/ValidationReportTemplate-spec.md)
   - 按规格在 Excel 中创建 `ValidationReportTemplate.xlsx`
   - 上传至 `POC-Validator/Templates/`（SharePoint）

3. **优化批量写入**（Issues 数量较大时）：使用 [templates/office-scripts/bulkInsertIssues.ts](templates/office-scripts/bulkInsertIssues.ts)

## SharePoint 目录结构

```
{LIBRARY}/POC-Validator/
├── Inputs/          ← 输入 Excel（含 tblOffshoring）
├── Templates/       ← ValidationReportTemplate.xlsx（只读）
└── Outputs/
    ├── Excel/       ← 生成的 Excel 验证报告
    ├── PDF/         ← 生成的 PDF 报告
    └── JSON/        ← 结构化 JSON 输出（可选）
```

## 从个人 OneDrive 迁移

如果你之前使用个人 OneDrive（`Documents/PowerbiTest/POC-Validator/`），请按 Runbook 第 10 节的对照表更新所有路径和 Flow 连接。

## 相关连接器

| 连接器 | 用途 |
|-------|------|
| SharePoint | 读取模板、写入报告文件 |
| Excel Online (Business) | 读取输入数据、写入 tblIssues 和 Summary |
| Microsoft Teams | 发送报告完成通知 |
