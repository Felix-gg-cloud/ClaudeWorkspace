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
├── workspace/
│   └── AXA Validation POC/       # AXA 月度数据校验 POC（唯一入口，见下方说明）
└── README.md
```

## 使用方式

1. 用 VS Code 打开此目录作为工作区：`code ClaudeWorkspace`
2. `.github/copilot-instructions.md` 会自动被 Copilot 读取
3. 每个新项目在 `projects/` 下创建子目录
4. 可复用的提示词放 `prompts/`，方便跨项目引用

---

## AXA Validation POC

> **唯一入口**：[`workspace/AXA Validation POC/`](./workspace/AXA%20Validation%20POC/README.md)  
> **维护分支**：`copilot/axa-validation-poc`

所有与 AXA 月度人力数据校验 POC 相关的文档、规则和操作指南均集中于 `workspace/AXA Validation POC/` 目录。

| 文件 | 说明 |
|------|------|
| [`README.md`](./workspace/AXA%20Validation%20POC/README.md) | POC 概览与快速导航 |
| [`Validation Rules.md`](./workspace/AXA%20Validation%20POC/Validation%20Rules.md) | 完整校验规则（中英双语，知识模块上传源） |
| [`Copilot-Studio-AI-Instructions.md`](./workspace/AXA%20Validation%20POC/Copilot-Studio-AI-Instructions.md) | Copilot Studio AI Agent 系统说明模板 |
| [`Operation-Guide.md`](./workspace/AXA%20Validation%20POC/Operation-Guide.md) | 每月操作流程与维护指南 |

**维护原则**：所有变更仅在 `copilot/axa-validation-poc` 分支进行，不再新建其他分支。
