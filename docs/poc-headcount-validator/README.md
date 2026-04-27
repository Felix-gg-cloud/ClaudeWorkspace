# POC Headcount Validator — 文档索引

本目录包含使用 **Power Automate + Copilot Studio（方案一：AI-only 校验）** 的 Headcount 数据离岸率（Offshoring）校验 POC 的完整技术文档。

## 文档列表

| 文件 | 说明 |
|------|------|
| [runbook.md](./runbook.md) | 端到端操作手顺（Power Automate 每步操作级别） |
| [validation-rules.md](./validation-rules.md) | 强制校验规则汇总（含 R-WS-ALL-001 全列空格规则） |
| [copilot-studio-prompt-template.md](./copilot-studio-prompt-template.md) | Copilot Studio 提示词模板（输出纯结构化 JSON） |
| [sample-payload-and-response.json](./sample-payload-and-response.json) | 示例请求载荷与 AI 响应 JSON |
| [sharepoint-folder-structure.md](./sharepoint-folder-structure.md) | 文件命名规范与 SharePoint 目录结构 |
| [excel-template-spec.md](./excel-template-spec.md) | Excel / PDF 报告模板规格（Sheet/表名/列定义） |

## 方案概述

```
手动触发
  └─► 读取 OneDrive Excel（tblOffshoring，带分页）
        └─► 组装行载荷（含 RowIndex，不做任何 trim/清洗）
              └─► 调用 Copilot Studio HTTP 端点
                    └─► 接收 JSON { report_model, issues[] }
                          ├─► 生成 Excel 报告（模板填充）
                          ├─► 生成 PDF 报告（HTML/Word → PDF）
                          └─► 存储至 SharePoint
                                └─► （可选）Teams / Email 通知
```

## 关键设计决策

- **报告格式**：用户交付物为 **Excel (.xlsx)** 和 **PDF**，不使用 Markdown。
- **原始值传递**：Power Automate **不做任何 trim/清洗**，原始值完整传给 AI。
- **空格即错误**：所有列前后存在空格一律判定为 **Error**（规则 R-WS-ALL-001）。
- **AI-only 校验**：所有业务规则均由 Copilot Studio 执行，Power Automate 只做机械读写和路由。
