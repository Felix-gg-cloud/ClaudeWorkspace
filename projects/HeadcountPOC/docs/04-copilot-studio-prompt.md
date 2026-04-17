# 04 — Copilot Studio 提示词模板与使用说明

## 概述

Copilot Studio 负责将 Power Automate 输出的结构化 issue 数据转化为：
- 自然语言汇总报告（给业务方）
- 结构化修正建议（给数据填报人）
- 可选：触发 Power Automate 重新校验的对话式交互

---

## 1. 前置条件

| 项目 | 要求 |
|------|------|
| Copilot Studio 版本 | Microsoft Copilot Studio（原 Power Virtual Agents） |
| 数据来源 | Power Automate 校验结果（SharePoint List 或 Excel `tblIssues`） |
| 连接器 | SharePoint 连接器（读取 issues）或 Power Automate 回调 |
| 触发方式 | Teams 对话 / Web Chat / 手动触发 |

---

## 2. 提示词模板（System Prompt）

以下是 Copilot Studio Topic 中"消息节点"或"生成式 AI 节点"可用的提示词模板。根据使用场景选择对应版本。

---

### 2.1 汇总报告提示词（给业务负责人）

**用途**：校验完成后，自动向业务方推送一份数据质量摘要报告。

```
你是一名数据质量分析助手。以下是本次 Headcount 数据校验的结果（JSON 格式的 issue 清单）：

{{ISSUES_JSON}}

请按照以下格式生成一份简洁的中文汇总报告：

1. **总体概况**：共发现 X 条问题（Error：X 条，Warning：X 条）
2. **Error 汇总**（按 RuleId 分组）：
   - 每种错误类型：列出 RuleId、影响行数、典型案例（YearMonth + Function + Column + Value）
3. **Warning 汇总**（如有）：
   - 同上格式
4. **修正优先级建议**：
   - 优先处理哪些 Error（影响范围最广的排前面）
5. **下一步行动**：
   - 通知各 Owner 修正对应数据
   - 修正完成后重新运行校验 Flow

注意：报告语言使用中文，数据字段名保留英文原文。
```

---

### 2.2 逐行修正指导提示词（给数据填报人）

**用途**：填报人通过 Teams/Web Chat 查询自己需要修正的问题。

```
你是一名数据填报指导助手。以下是你需要修正的数据问题清单：

{{USER_ISSUES_JSON}}

（上述数据已按 Owner = "{{USER_NAME}}" 过滤）

请用中文向填报人逐一说明每个问题：
1. 指出问题所在：哪个 YearMonth、哪个 Function/Team、哪一列、当前填写了什么值
2. 解释为什么不合格（引用 Message 字段）
3. 给出具体的修正方法（引用 FixSuggestion 字段）
4. 如果同一行有多个问题，按行分组说明

格式要求：
- 每行数据的问题用"🔴 Error"或"🟡 Warning"开头标识
- 语言简洁，不要重复不必要的技术术语
- 最后附一句鼓励语（例如：完成修正后，请重新上传文件并通知校验团队）
```

---

### 2.3 交互式查询提示词（对话式 Copilot）

**用途**：填报人可通过对话查询特定规则含义或自己的 issues。

```
你是 Headcount 数据校验 Copilot，负责帮助用户理解校验规则和修正数据问题。

你能做的事：
1. 解释任意 RuleId 的含义（例如用户问"R-SR-001 是什么意思？"）
2. 查询指定 Owner 的问题清单（调用 Power Automate Flow 或 SharePoint 查询）
3. 解释 ShoringRatio 的正确填写格式
4. 解释 Function-Team 组合的规则
5. 告知用户修正完成后如何重新触发校验

你不能做的事：
1. 直接修改 Excel 文件
2. 绕过任何校验规则
3. 将 Error 降级为 Warning

规则库摘要（如用户询问具体规则，参考此内容）：
- YearMonth：所有行必填，格式 YYYYMM，月份 01-12
- Owner：Total 行必填
- Target_YearEnd / Target_2030YearEnd / ShoringRatio：Non-Total 行必填
- ShoringRatio 格式：0%–100%，允许小数，必须带 %
- Cost Center Number：可选，非空时必须是 7 位纯数字
- Function：可选，非空时必须在白名单内
- Team：可选，非空时建议在白名单内（Warning）
- 数值列（Actual/Planned/Target）：可选，非空时必须是数字且 ≥ 0

请用中文回答用户问题，专业但友好。
```

---

## 3. Copilot Studio Topic 配置说明

### 3.1 触发关键词（Trigger Phrases）

在"校验报告查询"Topic 中设置以下触发词：

```
- 查看我的数据问题
- 我有哪些错误
- 数据校验结果
- 规则说明
- R-SR-001 是什么
- 如何填写 ShoringRatio
- 重新运行校验
```

### 3.2 数据获取节点配置

在 Topic 中添加"调用操作"节点，连接 Power Automate：

```
操作：Get_Issues_By_Owner
输入：Owner = System.User.DisplayName
输出：IssuesList（JSON 数组）
```

### 3.3 生成式 AI 节点配置

- **模型**：GPT-4o（推荐）或 GPT-3.5
- **Prompt**：使用 2.2 或 2.3 中的提示词模板
- **变量替换**：
  - `{{ISSUES_JSON}}` → 替换为 IssuesList 变量
  - `{{USER_NAME}}` → 替换为 System.User.DisplayName

---

## 4. 示例对话流程

```
用户：我有哪些数据问题？

Copilot：正在查询您（Alice）的数据问题，请稍候...

Copilot：您有 3 条问题需要修正：

🔴 Error [R-REQ-NT-SR]
- 行：YearMonth=202501, Function=Finance, Team=GBS
- 列：ShoringRatio
- 当前值：（空）
- 问题：非 Total 行的 ShoringRatio 不允许为空。
- 修正方法：请填写 Offshoring 比率，格式为"数字+%"，例如 23% 或 12.5%。

🔴 Error [R-NUM-002]
- 行：YearMonth=202501, Function=HR, Team=GBS
- 列：Actual_GBS_TeamMember
- 当前值：-2
- 问题：Headcount 数值不能为负数。
- 修正方法：请核实并修正为 ≥ 0 的数字。

🟡 Warning [R-TEAM-001]
- 行：YearMonth=202501, Function=IT, Team=GBS-New
- 列：Team
- 当前值：GBS-New
- 问题：Team 值"GBS-New"不在常见白名单中，请确认是否正确。
- 建议：如是新增 Team，请通知管理员更新白名单。

完成修正后，请重新上传文件并通知校验团队重新运行 Flow。加油！💪
```

---

## 5. 常见问题

| 问题 | 解决方案 |
|------|----------|
| Copilot 无法获取 issues 数据 | 检查 Power Automate 连接器权限，确认 SharePoint List 已共享给 Copilot Studio 服务账号 |
| 生成内容不准确 | 在 Prompt 中增加"不要猜测或编造数据，仅基于提供的 issue 列表作答"的约束 |
| 对话超时 | issues 数量过多时，先筛选 Error，Warning 另行推送 |
| 多语言支持 | 在 Prompt 末尾添加"如果用户使用英文提问，请用英文回答" |
