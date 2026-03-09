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

## Claude Code

### 1. 什么是 Claude Code
- Anthropic 官方推出的 **命令行 AI 编程助手**（agentic coding tool）
- 直接在终端中与 Claude 交互，能读写文件、执行命令、搜索代码库
- 支持整个项目级别的理解和操作，不限于单个文件

### 2. 安装与启动

```bash
# 安装（需要 Node.js 18+）
npm install -g @anthropic-ai/claude-code

# 在项目目录下启动
cd your-project
claude

# 带初始提示启动
claude "帮我看看这个项目的结构"

# 非交互模式（一次性执行）
claude -p "解释这个项目的架构"
```

### 3. 核心快捷键与操作

| 操作 | 快捷键 / 命令 | 说明 |
|------|---------------|------|
| 退出 | `Ctrl + C` 两次 或输入 `/exit` | 退出 Claude Code |
| 清屏 | `Ctrl + L` | 清除终端输出 |
| 中断回答 | `Escape` | 停止当前生成 |
| 多行输入 | `Shift + Enter` 或 `\` 结尾 | 输入多行提示 |
| 恢复上次对话 | `claude --continue` 或 `claude -c` | **继续最近一次会话** |
| 恢复指定对话 | `claude --resume` | 选择历史会话继续 |
| 查看历史 | `/history` | 在交互模式中查看对话历史 |

### 4. 斜杠命令（交互模式内使用）

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/exit` 或 `/quit` | 退出 Claude Code |
| `/clear` | 清除对话上下文（开始新话题） |
| `/history` | 查看之前的对话记录 |
| `/compact` | 压缩当前对话上下文（节省 token） |
| `/cost` | 查看当前会话已消耗的 token 和费用 |
| `/model` | 切换模型（如 sonnet、opus） |
| `/config` | 查看或修改配置 |
| `/permissions` | 管理工具权限（允许/拒绝文件操作等） |
| `/doctor` | 诊断环境问题 |
| `/mcp` | 查看 MCP 服务器状态 |

### 5. 权限系统

Claude Code 执行文件操作或命令前会请求授权：

- **Allow once**：仅本次允许
- **Allow for session**：本次会话内都允许
- **Always allow**：永久允许（记入配置）
- **Deny**：拒绝操作

可通过启动参数调整：
```bash
# 信任模式（跳过确认，适合自己熟悉的项目）
claude --dangerously-skip-permissions
```

### 6. 配置文件体系

| 文件 | 位置 | 作用 |
|------|------|------|
| `CLAUDE.md` | 项目根目录 | **项目级指令**，每次启动自动加载（类似 copilot-instructions.md） |
| `CLAUDE.md` | 子目录 | 进入该目录时加载的补充指令 |
| `~/.claude/settings.json` | 用户主目录 | 全局配置（权限、默认模型等） |
| `.claude/settings.json` | 项目目录 | 项目级配置 |

**CLAUDE.md 示例：**
```markdown
# 项目约定
- 使用 Python 3.11，虚拟环境在 .venv
- 测试用 pytest，运行命令：pytest tests/
- 代码风格遵循 PEP 8
- commit 信息用中文
```

### 7. 常用操作场景

#### 理解项目
```
> 分析这个项目的目录结构和技术栈
> 帮我画一个架构图（mermaid）
> 这个函数的调用链是什么？
```

#### 写代码
```
> 创建一个 Flask API，支持用户注册和登录
> 给 utils.py 里的函数加上类型注解
> 把这个 class 重构成 dataclass
```

#### Debug
```
> 运行 pytest 并修复所有失败的测试
> 这个报错是什么原因？帮我修复
> 分析日志找出性能瓶颈
```

#### Git 操作
```
> 帮我提交代码，commit 信息用中文
> 对比 main 分支的改动，写一个 PR 描述
> 查看最近 5 次提交的改动摘要
```

#### 文件操作
```
> 把 src/ 下所有 .js 文件改为 .ts
> 在每个 Python 文件头部加上 license 注释
> 找出所有包含 TODO 的文件
```

### 8. 实用技巧

- **大项目先压缩上下文**：对话太长后用 `/compact` 压缩，避免 token 超限
- **善用 CLAUDE.md**：把项目约定写进去，省去每次重复说明
- **链式任务**：可以一次给出多步骤任务，Claude Code 会依次执行
- **管道输入**：`cat error.log | claude -p "分析这个错误日志"` 
- **结合 git**：Claude Code 能直接操作 git，但推送（push）前建议先检查
- **费用控制**：用 `/cost` 随时查看消耗，opus 模型比 sonnet 贵约 5 倍

### 9. Claude Code vs VS Code Copilot Chat

| 对比项 | Claude Code（终端） | VS Code Copilot（编辑器） |
|--------|---------------------|--------------------------|
| 运行环境 | 终端 CLI | VS Code 内置 |
| 项目理解 | 全局代码库索引 | 当前文件 + 手动引用 |
| 文件操作 | 直接读写 | 通过 Agent 模式 |
| 命令执行 | 直接运行 shell 命令 | 通过终端工具 |
| 适合场景 | 大规模重构、自动化任务 | 日常编码、代码补全 |
| 费用 | 按 API 用量计费 | 包含在 Copilot 订阅中 |

## Hook、Agent、SubAgent、Skill、Plugin

### 1. Hook（钩子）

Hook 是 Claude Code 在特定事件发生时**自动触发**的脚本/命令，类似 git hook。

#### 支持的钩子类型

| 钩子 | 触发时机 | 典型用途 |
|------|----------|----------|
| `PreToolUse` | 工具调用**之前** | 拦截/校验操作（如禁止删除某些文件） |
| `PostToolUse` | 工具调用**之后** | 自动格式化、lint、日志记录 |
| `Notification` | Claude 发送通知时 | 长任务完成后推送通知到手机/桌面 |
| `Stop` | Claude 停止响应时 | 自动追加后续指令、触发验证 |

#### 配置方式

在 `.claude/settings.json` 或 `~/.claude/settings.json` 中配置：

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "write_file",
        "hooks": [
          {
            "type": "command",
            "command": "prettier --write $CLAUDE_FILE_PATH"
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "bash",
        "hooks": [
          {
            "type": "command",
            "command": "echo $CLAUDE_TOOL_INPUT | jq -e '.command | test(\"rm -rf\") | not'"
          }
        ]
      }
    ]
  }
}
```

#### 实用示例

```json
// 每次写文件后自动运行 ESLint
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "write_file",
        "hooks": [
          { "type": "command", "command": "npx eslint --fix $CLAUDE_FILE_PATH" }
        ]
      }
    ]
  }
}
```

---

### 2. Agent（代理/智能体）

Agent 是 Claude 的**工作模式**，具有自主规划、调用工具、循环执行的能力。

#### 核心概念

- **Agent 模式** ≠ 简单问答，它能：自主拆解任务 → 读取文件 → 搜索代码 → 编辑文件 → 运行命令 → 验证结果
- Claude Code 本身就是一个 Agent
- VS Code Copilot Chat 中选择 "Agent" 模式也能启用 Agent 能力

#### VS Code 中的 Agent 模式

在 VS Code Copilot Chat 中，有三种模式：
- **Ask**：只问答，不操作文件
- **Edit**：可以编辑你指定的文件
- **Agent**：自主规划，调用工具，读写文件，运行终端命令

#### 自定义 Agent（.agent.md 文件）

可以在项目中定义专用 Agent，让 Copilot 以特定角色工作：

```markdown
<!-- .github/agents/reviewer.agent.md -->
---
name: "Code Reviewer"
description: "代码审查专家，关注安全和性能"
tools: ["read_file", "grep_search", "semantic_search"]
---

你是一位严格的代码审查专家。
审查时关注：
1. 安全漏洞（SQL 注入、XSS、CSRF）
2. 性能瓶颈（N+1 查询、内存泄漏）
3. 代码规范（命名、注释、复杂度）
不要直接修改代码，只提出建议。
```

使用方式：在 Copilot Chat 中 `@Code Reviewer 帮我审查 src/api/` 即可调用。

---

### 3. SubAgent（子代理）

SubAgent 是 Agent 在执行复杂任务时**动态生成的子任务执行者**。

#### 工作原理

```
主 Agent（你对话的对象）
  ├── SubAgent A：搜索代码库，收集相关文件
  ├── SubAgent B：分析依赖关系
  └── SubAgent C：执行具体的代码修改
```

#### 特点

- **自动创建**：Claude 判断任务复杂时自动拆分给 SubAgent
- **独立上下文**：每个 SubAgent 有自己的上下文，不共享对话历史
- **结果汇总**：SubAgent 完成后将结果返回给主 Agent
- **并行执行**：多个 SubAgent 可以同时工作，提高效率

#### 使用场景

- 在大型代码库中同时搜索多个相关文件
- 并行分析多个模块的代码结构
- 跨文件重构时同时修改多处

> 你不需要手动创建 SubAgent，Claude 会根据任务复杂度自动调度。

---

### 4. Skill（技能）

Skill 是一组**可复用的领域知识和指令**，让 Agent 在特定领域表现更专业。

#### 文件格式：SKILL.md

```markdown
<!-- .github/skills/database-migration/SKILL.md -->
---
name: "Database Migration"
description: "数据库迁移操作专家"
---

## 迁移规范
- 使用 Alembic 管理 Python 项目的数据库迁移
- 每次迁移必须包含 upgrade 和 downgrade
- 迁移文件命名：`YYYYMMDD_HHMMSS_描述.py`
- 生产环境迁移前必须先在 staging 测试

## 常用命令
- 创建迁移：`alembic revision --autogenerate -m "描述"`
- 执行迁移：`alembic upgrade head`
- 回滚：`alembic downgrade -1`

## 注意事项
- 不要在迁移中删除有数据的列，先标记废弃
- 大表加索引使用 `CREATE INDEX CONCURRENTLY`
```

#### Skill vs Agent 的区别

| 对比 | Skill | Agent |
|------|-------|-------|
| 本质 | 知识/指令集 | 独立的执行角色 |
| 触发 | 被 Agent 按需引用 | 由用户 @ 调用 |
| 是否有工具 | 没有独立工具 | 可限定工具集 |
| 类比 | 参考手册 | 专职员工 |

---

### 5. Plugin（插件 / 扩展）

Plugin 在 Claude 生态中主要指 **VS Code 扩展**和 **MCP Server**，用来扩展 AI 的能力边界。

#### 5.1 VS Code 扩展（与 Copilot 配合）

以下按使用场景分类，列出高频好用的扩展：

##### 前端开发

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **ES7+ React/Redux/React-Native Snippets** | React 代码片段 | AI 生成组件后快速补全 hooks/lifecycle |
| **Tailwind CSS IntelliSense** | Tailwind 类名补全 | 让 AI 生成的 Tailwind 代码有实时预览和校验 |
| **Auto Rename Tag** | HTML/JSX 标签同步重命名 | AI 修改组件时自动同步闭合标签 |
| **CSS Peek** | 在 HTML 中 peek CSS 定义 | 审查 AI 生成的样式引用是否正确 |
| **Thunder Client** | 轻量 REST API 测试 | 测试 AI 生成的 API 接口 |
| **Volar** | Vue 3 官方扩展 | Vue 项目必装，AI 生成的 SFC 有完整类型支持 |
| **Pretty TypeScript Errors** | 美化 TS 错误信息 | 更容易把错误信息喂给 AI 分析 |

##### 后端 — Python

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **Python (Microsoft)** | Python 语言支持 | 必装，AI 生成的代码有完整 lint/类型检查 |
| **Pylance** | Python 类型分析引擎 | 让 AI 修改后立即看到类型错误 |
| **Black Formatter** | 代码格式化 | Hook 配合：AI 写完自动格式化 |
| **Ruff** | 超快 Python linter | 替代 flake8+isort，AI 代码即时校验 |
| **Python Test Explorer** | 测试可视化 | 直观查看 AI 生成的测试用例结果 |
| **SQLTools** | 数据库管理 | 直接在 VS Code 里验证 AI 生成的 SQL |

##### 后端 — Java / Spring

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **Extension Pack for Java (Microsoft)** | Java 全家桶（Language Support + Debugger + Test Runner + Maven/Gradle） | 必装，AI 生成的 Java 代码有完整编译检查 |
| **Spring Boot Extension Pack** | Spring Boot 支持（Spring Initializr + Dashboard + Tools） | AI 生成 Spring 项目时有 Bean 导航、配置补全 |
| **Lombok Annotations Support** | Lombok 注解支持 | AI 生成的 `@Data`、`@Builder` 等有正确的代码提示 |
| **Java Decompiler** | 反编译 .class 文件 | 查看 AI 引用的第三方库源码 |

##### 后端 — Go

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **Go (Google)** | Go 官方扩展 | 必装，AI 生成的 Go 代码有 gopls 全功能支持 |
| **Go Test Explorer** | Go 测试可视化 | 一键运行 AI 生成的测试 |

##### 后端 — Rust

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **rust-analyzer** | Rust 语言服务器 | AI 生成 Rust 代码后实时编译检查和类型推导 |
| **Even Better TOML** | Cargo.toml 支持 | AI 添加依赖时有版本补全 |
| **crates** | Crates.io 依赖管理 | 显示依赖是否过时，配合 AI 升级 |

##### 后端 — Node.js / TypeScript

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **ESLint** | JS/TS 代码规范检查 | AI 生成代码后自动校验规范 |
| **Prisma** | Prisma ORM 支持 | AI 生成 schema 有语法高亮和自动补全 |
| **REST Client** | 在 `.http` 文件中测试 API | 让 AI 生成 `.http` 测试文件直接运行 |

##### 数据库 & DevOps

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **SQLTools + 驱动** | 支持 MySQL/PostgreSQL/SQLite 等 | 直接运行 AI 生成的 SQL，验证正确性 |
| **MongoDB for VS Code** | MongoDB 可视化 | 查看 AI 生成的聚合查询结果 |
| **Docker** | Docker 容器管理 | AI 生成 Dockerfile 后直接构建和运行 |
| **Kubernetes** | K8s 资源管理 | AI 生成 YAML 后有 schema 校验 |
| **YAML (Red Hat)** | YAML 语法支持 + Schema | AI 生成的 CI/CD 配置有校验 |

##### 通用效率类

| 扩展 | 说明 | 配合 AI 的用法 |
|------|------|---------------|
| **GitLens** | Git 增强（blame、history、graph） | 理解代码演变历史，给 AI 提供更好的上下文 |
| **Error Lens** | 行内显示错误/警告 | AI 修改后即时看到问题，反馈更快 |
| **Todo Tree** | 汇总所有 TODO/FIXME | 让 AI 批量处理遗留的 TODO |
| **Path Intellisense** | 文件路径补全 | AI 生成 import 路径时有校验 |
| **Markdown Preview Mermaid** | 预览 Mermaid 图表 | AI 生成的架构图/流程图直接预览 |
| **Code Spell Checker** | 拼写检查 | 检查 AI 生成的变量名/注释拼写 |
| **indent-rainbow** | 缩进彩色可视化 | 检查 AI 生成的 Python/YAML 缩进是否正确 |

#### 5.2 MCP Server（Model Context Protocol 插件）

MCP Server 是一种通过标准协议给 AI 扩展外部工具能力的插件机制。

##### 工作原理

```
Claude / Copilot
    ↓ (MCP 协议)
MCP Server（独立进程）
    ↓
外部资源（文件系统、数据库、API、浏览器...）
```

##### 常用 MCP Server

| MCP Server | 能力 | 安装 |
|------------|------|------|
| **@anthropic/mcp-filesystem** | 文件系统读写 | `npx @anthropic/mcp-filesystem` |
| **@anthropic/mcp-github** | GitHub API（issue、PR、repo） | `npx @anthropic/mcp-github` |
| **@anthropic/mcp-memory** | 持久化记忆存储 | `npx @anthropic/mcp-memory` |
| **@anthropic/mcp-puppeteer** | 浏览器自动化（截图、爬取） | `npx @anthropic/mcp-puppeteer` |
| **mcp-server-sqlite** | SQLite 数据库操作 | `npx mcp-server-sqlite` |
| **@anthropic/mcp-fetch** | HTTP 请求（调用外部 API） | `npx @anthropic/mcp-fetch` |

##### VS Code 中配置 MCP Server

在 `.vscode/mcp.json` 中：

```json
{
  "servers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@anthropic/mcp-github"],
      "env": {
        "GITHUB_TOKEN": "${env:GITHUB_TOKEN}"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@anthropic/mcp-filesystem", "/path/to/allowed/dir"]
    }
  }
}
```

#### 5.3 Claude Code 自有扩展生态

Claude Code 有自己的扩展机制，包括 **`/install` 命令 + Discover 市场**、**自定义斜杠命令** 和 **社区 MCP Server 注册表**，可以大幅提升生成代码的质量和美观度。

##### `/install` 命令 与 Discover 市场（一键安装全家桶）

Claude Code 内置了类似"扩展商店"的 **Discover** 界面，通过 `/install` 命令进入，可以浏览和一键安装官方 / 社区预打包的扩展包。

**基本操作：**
```
# 在 Claude Code 交互模式中输入
> /install

# 进入 Discover 界面后，可以看到分类目录：
# - frontend-design    → 前端设计全家桶
# - backend-python     → Python 后端全家桶
# - fullstack          → 全栈开发包
# - devops             → CI/CD & 部署工具
# - mobile             → 移动端开发
# ...

# 选择一个分类即可一键安装
```

**常用 Discover 扩展包：**

| 扩展包 | 包含内容 | 效果 |
|--------|---------|------|
| **frontend-design** | shadcn/ui 规范 + Tailwind 最佳实践 + 响应式模板 + 动画预设 + 色彩系统 | AI 生成的前端代码**自带现代设计感**，不再是毛坯房 |
| **react-nextjs** | Next.js App Router 约定 + React 模式 + 组件结构规范 | 生成的 React 代码符合 Next.js 最佳实践 |
| **vue-nuxt** | Vue 3 + Nuxt 3 + Composition API 规范 | 生成的 Vue 代码用最新写法 |
| **backend-python** | FastAPI/Django 模板 + SQLAlchemy 规范 + pytest 约定 | Python 后端代码结构规范 |
| **backend-node** | Express/Fastify 模板 + Prisma + Zod 校验 | Node.js 后端代码专业化 |
| **backend-java** | Spring Boot 约定 + Maven/Gradle + JPA 规范 | Java 代码符合企业级标准 |
| **backend-go** | Go 项目布局 + Gin/Echo 模板 + 错误处理模式 | 惯用 Go 风格代码 |
| **mobile-react-native** | React Native + Expo 规范 + 导航模板 | 移动端代码开箱即用 |
| **database** | Schema 设计规范 + 迁移模板 + 查询优化建议 | AI 写的 SQL/Schema 更规范 |
| **devops-docker** | Dockerfile 最佳实践 + docker-compose 模板 + CI 配置 | 部署配置专业化 |

**安装后的效果：**

安装一个扩展包相当于自动帮你做了以下事情：
1. 在 `CLAUDE.md` 中注入了该领域的最佳实践和设计规范
2. 添加了一批预配置的自定义斜杠命令
3. 配置了相关的 MCP Server（如需要）
4. 设置了 Hook（如自动格式化、lint）

```
# 安装前：AI 生成的 React 页面
→ 功能能用，但样式粗糙，间距不统一，配色随意

# 安装 frontend-design 后：AI 生成的 React 页面
→ 自动使用 shadcn/ui 组件，Tailwind 间距一致，
   配色协调，响应式布局，dark mode 支持，
   交互动效自然
```

**前端 frontend-design 全家桶具体包含：**

```
✅ 设计系统
   - 色彩：预设 neutral + accent 配色方案
   - 排版：font-size / line-height / letter-spacing 体系
   - 间距：4px 基数的 spacing scale
   - 圆角 / 阴影 / 边框的统一规范

✅ 组件规范
   - 基于 shadcn/ui（Radix UI + Tailwind）
   - Button / Card / Dialog / Form / Table / Toast 等
   - 每个组件有 variants（default / outline / ghost / destructive）

✅ 布局模板
   - Dashboard 布局（侧边栏 + 顶栏 + 内容区）
   - Landing Page（Hero + Features + Pricing + Footer）
   - Auth 页面（登录 / 注册 / 忘记密码）

✅ 交互规范
   - hover / focus / active 状态
   - 加载态（skeleton / spinner）
   - 空状态 / 错误状态
   - framer-motion 动画预设

✅ 自动配置
   - Tailwind config 优化
   - PostCSS 配置
   - 相关斜杠命令（/component /page /layout）
```

---

##### 自定义斜杠命令（Custom Slash Commands）

可以在项目或全局创建可复用的命令模板：

```
# 项目级命令（团队共享）
.claude/commands/命令名.md

# 用户级命令（个人全局）
~/.claude/commands/命令名.md
```

**示例 — 生成现代化 React 组件：**

```markdown
<!-- .claude/commands/ui-component.md -->
根据以下需求生成 React 组件：

$ARGUMENTS

要求：
- 使用 TypeScript + 函数组件 + Hooks
- 样式使用 Tailwind CSS，遵循现代设计规范
- 包含响应式布局（mobile first）
- 使用 shadcn/ui 组件库风格
- 动画使用 framer-motion
- 包含 accessible aria 属性
- 导出 Props 类型定义
```

使用方式：在 Claude Code 中输入 `/ui-component 一个带搜索和筛选的用户列表页`

**示例 — 生成美观的落地页：**

```markdown
<!-- .claude/commands/landing-page.md -->
生成一个现代化落地页：

$ARGUMENTS

设计规范：
- 使用 Next.js App Router + Tailwind CSS
- 遵循 Apple/Linear 风格的极简设计美学
- 配色使用中性色 + 一个强调色
- 字体层级清晰：hero > heading > subheading > body
- 包含：Hero Section、Features、CTA、Footer
- 所有间距使用 Tailwind 的 spacing scale（一致的 4px 基数）
- 渐变、阴影、圆角要克制
- 支持 dark mode
```

##### Claude Code 社区 MCP Server 注册表

除了 Anthropic 官方的 MCP Server，社区维护了大量开源 MCP Server，可在以下地方查找：

- **MCP Hub**：社区 MCP Server 目录，按分类浏览和搜索
- **GitHub 搜索**：搜 `mcp-server-` 前缀可发现大量社区项目
- **npm 搜索**：搜 `@anthropic/mcp-` 或 `mcp-server`

##### 让前端代码更美观的扩展 / MCP 方案

| 名称 | 类型 | 作用 |
|------|------|------|
| **shadcn/ui** | 组件库（写入 CLAUDE.md） | 让 AI 用 Radix + Tailwind 生成高质量 UI 组件 |
| **v0.dev** | Vercel 的 AI UI 生成器 | 生成现代化 React/Next.js 界面，可复制代码 |
| **mcp-server-figma** | MCP Server | 读取 Figma 设计稿，AI 按设计稿生成代码 |
| **mcp-server-browser** | MCP Server | AI 打开浏览器预览生成的页面并截图校对 |
| **21st.dev Magic MCP** | MCP Server | 专为 UI 组件生成优化，集成 21st.dev 组件市场 |

##### 通过 CLAUDE.md 提升前端代码审美

最简单有效的方式是在 `CLAUDE.md` 中写明设计规范：

```markdown
# 前端代码规范

## 技术栈
- React 18 + TypeScript + Next.js 14 (App Router)
- 样式：Tailwind CSS（不用 CSS 模块/Styled Components）
- 组件库：shadcn/ui（基于 Radix UI）
- 动画：framer-motion
- 图标：lucide-react

## 设计规范
- 设计风格：参考 Linear.app 的极简现代风格
- 配色：neutral 色系为主，primary 用 blue-600
- 圆角：按钮 rounded-lg，卡片 rounded-xl
- 阴影：使用 shadow-sm 到 shadow-md，不要 shadow-2xl
- 间距节奏：section 间 py-24，组件间 gap-6，内边距 p-6
- 字体：标题 font-bold tracking-tight，正文 text-muted-foreground
- 响应式：mobile-first（sm → md → lg）
- 交互反馈：hover 变色/位移，transition-all duration-200
- Dark mode：始终支持，使用 Tailwind dark: 变体

## 禁止
- 不要用 inline style
- 不要用 !important
- 不要用浮动布局（float），全部用 flex/grid
- 配色不要超过 3 种主色
```

##### 完整工作流：AI 生成美观前端代码

```
1. CLAUDE.md 写好设计规范（定义风格基调）
     ↓
2. 自定义斜杠命令准备好模板（如 /ui-component）
     ↓
3. 安装 MCP Server（如 figma、browser 截图预览）
     ↓
4. Claude Code 生成代码 → Hook 自动 prettier/eslint
     ↓
5. 浏览器预览 → 截图反馈 → Claude 迭代优化
```

#### 5.4 各概念关系总结

```
┌─────────────────────────────────────────────────┐
│  你（用户）                                       │
│    └─→ 对话 / 指令                                │
│         └─→ Agent（主智能体）                      │
│              ├── 引用 Skill（领域知识）             │
│              ├── 触发 Hook（自动化钩子）            │
│              ├── 调用 Plugin                       │
│              │    ├── VS Code 扩展（编辑器能力）    │
│              │    └── MCP Server（外部工具）        │
│              └── 派生 SubAgent（子任务执行者）       │
│                   ├── SubAgent A                   │
│                   └── SubAgent B                   │
└─────────────────────────────────────────────────┘
```

| 概念 | 一句话定义 | 类比 |
|------|-----------|------|
| **Hook** | 事件触发的自动化脚本 | Git Hook / CI Pipeline |
| **Agent** | 能自主规划和执行的 AI 角色 | 全能员工 |
| **SubAgent** | Agent 拆分出的子任务执行者 | 分工协作的团队成员 |
| **Skill** | 可复用的领域知识包 | 操作手册 / SOP |
| **Plugin** | 扩展 AI 能力的外部组件 | 手机上的 App |

## 踩坑记录
（在这里记录遇到的问题和解决方式）
