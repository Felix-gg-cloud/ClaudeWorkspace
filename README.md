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
│   └── <project-name>/           # 每个独立项目一个子目录
└── README.md
```

## 使用方式

1. 用 VS Code 打开此目录作为工作区：`code ClaudeWorkspace`
2. `.github/copilot-instructions.md` 会自动被 Copilot 读取
3. 每个新项目在 `projects/` 下创建子目录
4. 可复用的提示词放 `prompts/`，方便跨项目引用

## 项目列表

| 项目 | 说明 |
|------|------|
| [LingoBuddy](projects/LingoBuddy/) | 英语学习打卡应用（Spring Boot + React） |
| [EnglishQuestArena](projects/EnglishQuestArena/) | 英语学习游戏化平台 |
| [CopilotStudio-DataValidation](projects/CopilotStudio-DataValidation/) | Copilot Studio 批量 Excel 数据校验引擎（Power Automate 对接） |
