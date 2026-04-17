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
│   ├── OffshoringValidator/      # Offshoring 数据质量验证 POC
│   │   └── docs/
│   │       ├── poc-runbook.md           # POC 运行手册（触发、输入、输出、迁移路径）
│   │       ├── flow-steps.md            # Power Automate 流程步骤详细配置
│   │       └── copilot-studio-prompts.md # AI 提示词模板（验证规则 + 输出格式）
│   └── <project-name>/           # 其他独立项目
└── README.md
```

## 使用方式

1. 用 VS Code 打开此目录作为工作区：`code ClaudeWorkspace`
2. `.github/copilot-instructions.md` 会自动被 Copilot 读取
3. 每个新项目在 `projects/` 下创建子目录
4. 可复用的提示词放 `prompts/`，方便跨项目引用
