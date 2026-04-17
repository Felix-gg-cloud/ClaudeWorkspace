# AXA Validation POC — Workspace

本目录为 AXA Validation POC 场景的归档工作区快照，包含以下内容：

## 目录结构

```
AXA Validation POC/
├── .github/
│   └── copilot-instructions.md       # VS Code Copilot 全局指令
├── configs/                          # 配置文件（MCP Server、个人偏好）
├── docs/                             # 学习笔记
├── prompts/                          # 可复用提示词模板
├── projects/
│   ├── AXA-Validation-POC/           # AXA 校验 POC 项目文档
│   │   └── copilot-studio-validation-agent-instructions.md
│   ├── LingoBuddy/                   # LingoBuddy 语言学习项目
│   └── EnglishQuestArena/            # EnglishQuestArena 英语闯关项目
└── README.md                         # 本文件
```

## AXA Validation POC 说明

- **项目**：AXA Excel 行批量校验智能体（Copilot Studio）
- **功能**：对 Excel 上传数据进行行级批量校验，输出结构化 JSON 校验报告
- **技术**：Microsoft Copilot Studio 非对话型智能体
- **核心文档**：[Copilot Studio 校验智能体 Instructions](./projects/AXA-Validation-POC/copilot-studio-validation-agent-instructions.md)

## 快速使用

1. 用 VS Code 打开此目录作为工作区：`code "AXA Validation POC"`
2. 参考 `projects/AXA-Validation-POC/copilot-studio-validation-agent-instructions.md` 配置 Copilot Studio 智能体
3. 按照输入 Schema 构造 JSON，发送给智能体，获取校验结果
4. 可复用的提示词放 `prompts/`，方便跨项目引用
