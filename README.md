# Claude Workspace

个人 Claude / GitHub Copilot 学习与项目工作区。

## 目录结构

```
ClaudeWorkspace/
├── .github/
│   └── copilot-instructions.md   # VS Code Copilot 全局指令（对本工作区生效）
├── configs/
│   ├── mcp-servers.json          # MCP Server 配置参考
│   └── settings.json             # 个人偏好设定
├── prompts/
│   └── *.prompt.md               # 可复用的提示词模板
├── docs/
│   └── learning-notes.md         # 学习笔记
├── projects/
│   ├── POC-Validator/            # Power Automate Offshoring 校验 Flow
│   │   └── docs/
│   │       └── power-automate-runbook.md  # 完整新手搭建手册（含排障与验证清单）
│   └── <project-name>/           # 每个独立项目一个子目录
└── README.md
```

## 项目索引

| 项目 | 说明 | 文档 |
|------|------|------|
| [POC-Validator](projects/POC-Validator/) | Power Automate Flow：读取 SharePoint Excel → Copilot Studio 校验 → 输出 PDF + Teams 通知 | [Runbook](projects/POC-Validator/docs/power-automate-runbook.md) |

## 使用方式

1. 用 VS Code 打开此目录作为工作区：`code ClaudeWorkspace`
2. `.github/copilot-instructions.md` 会自动被 Copilot 读取
3. 每个新项目在 `projects/` 下创建子目录
4. 可复用的提示词放 `prompts/`，方便跨项目引用
