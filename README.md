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
│   ├── learning-notes.md         # 学习笔记
│   └── power-automate-excel-runbook.md  # Power Automate × Excel 操作手顺
├── projects/
│   └── <project-name>/           # 每个独立项目一个子目录
└── README.md
```

## 使用方式

1. 用 VS Code 打开此目录作为工作区：`code ClaudeWorkspace`
2. `.github/copilot-instructions.md` 会自动被 Copilot 读取
3. 每个新项目在 `projects/` 下创建子目录
4. 可复用的提示词放 `prompts/`，方便跨项目引用

## 文档索引

| 文档 | 说明 |
|------|------|
| [学习笔记](docs/learning-notes.md) | Claude / Copilot 核心概念与使用技巧 |
| [Power Automate × Excel 操作手顺](docs/power-automate-excel-runbook.md) | 配置手动触发流程读取 OneDrive/SharePoint Excel 文件的完整指南 |
