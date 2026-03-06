# Claude 学习笔记

## 核心概念

### 1. System Prompt vs User Prompt
- System Prompt: 设定角色、规则、约束（放在 copilot-instructions.md）
- User Prompt: 每次对话的具体请求

### 2. MCP (Model Context Protocol)
- 让 AI 调用外部工具（文件系统、数据库、API 等）
- 配置文件位置：
  - VS Code: `.vscode/mcp.json`
  - Claude Desktop: `~/Library/Application Support/Claude/claude_desktop_config.json`

### 3. Prompt 技巧
- 给明确约束（"不超过 5 条"、"用表格输出"）
- 提供示例输入/输出（few-shot）
- 分步骤拆任务（chain-of-thought）
- 要求结构化输出（JSON/Markdown/表格）

### 4. 常用场景
- 代码生成/重构/Debug
- 数据分析（SQL/DAX/Power Query）
- 文档撰写
- 方案设计与评审

## 踩坑记录
（在这里记录遇到的问题和解决方式）
