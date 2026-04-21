# Claude Workspace

个人 Claude / GitHub Copilot 学习与项目工作区。

---

## 🚀 快速导航 / Quick Links

| 资源 | 链接 | 说明 |
|------|------|------|
| **Copilot Studio 校验模板** | [📄 docs/copilot-studio-validation-template.md](docs/copilot-studio-validation-template.md) | 中英双语 Instructions 模板，可一键复制 |
| Copilot 全局指令 | [.github/copilot-instructions.md](.github/copilot-instructions.md) | VS Code Copilot 全局指令 |
| 学习笔记 | [docs/learning-notes.md](docs/learning-notes.md) | Claude / Copilot 学习记录 |

---

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
│   ├── copilot-studio-validation-template.md  # ⭐ Copilot Studio 校验 Instructions 中英双语模板
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
5. Copilot Studio 智能体 Instructions 模板见 [`docs/copilot-studio-validation-template.md`](docs/copilot-studio-validation-template.md)
