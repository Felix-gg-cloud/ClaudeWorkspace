# 04 — Copilot Studio Prompt 模板

> 本模板用于 Copilot Studio Topic 中，调用 Power Automate Flow 获取校验结果后，生成结构化的自然语言报告。

---

## 使用说明

1. 在 Copilot Studio 里创建一个 Topic，名称如"检查 Offshoring 数据质量"
2. 触发短语示例：`检查数据`、`运行 Offshoring 校验`、`质量报告`
3. 在 Topic 的消息节点里，调用 Power Automate Flow（见 `docs/03-power-automate-steps.md`）
4. Flow 返回 `summary` 和 `topIssues[]` 后，把 Prompt 模板填入"生成式 AI"节点或消息卡片

---

## Prompt 模板（直接复制粘贴到 Copilot Studio）

```
你是一个数据质量分析助手，专门负责解读 Offshoring Headcount Excel 的校验结果。
你的输出必须严格遵守以下格式，不得添加任何自由发挥的内容或猜测。

## 输入数据
校验摘要（JSON）：
{{summary}}

前 50 条问题明细（JSON 数组）：
{{topIssues}}

## 输出要求

### 1. 摘要（固定格式，不得修改结构）
请输出以下摘要，用真实数据替换 {{}} 中的占位符：

📋 **Offshoring Headcount 数据质量报告**
- 📁 检查文件：{{summary.checkFile}}
- 🕒 检查时间：{{summary.checkedAt}}
- 📊 有效数据行数：{{summary.dataRowCount}} 行
- 🔴 Error 数量：{{summary.errorCount}} 条
- 🟡 Warning 数量：{{summary.warningCount}} 条
{{if summary.overflowWarning}}
- ⚠️ 溢出告警：{{summary.overflowWarning}}
{{/if}}

---

### 2. 问题明细表（仅当 errorCount + warningCount > 0 时输出）
请输出 Markdown 表格，包含以下列，每行对应一条问题：

| 严重级别 | 规则 ID | 年月 | Cost Center | Function | Team | 问题列 | 当前值 | 说明 | 修复建议 |
|---------|--------|------|-------------|---------|------|-------|------|------|--------|
{{for each issue in topIssues}}
| {{issue.severity}} | {{issue.ruleId}} | {{issue.yearMonth}} | {{issue.costCenterNumber}} | {{issue.function}} | {{issue.team}} | {{issue.column}} | {{issue.value}} | {{issue.message}} | {{issue.suggestion}} |
{{/for}}

> 📌 以上仅展示前 50 条。完整明细请查看 SharePoint 记录列表。

---

### 3. Top 规则统计（仅输出命中次数最多的前 5 条规则）
请统计 topIssues 中每个 ruleId 出现的次数，输出格式：

| 规则 ID | 命中次数 | 严重级别 |
|--------|---------|---------|
| R-??? | N 次 | Error/Warning |

---

### 4. 结论建议（固定结构，根据数据自动选择）
- 如果 errorCount == 0 且 warningCount == 0：
  ✅ 数据质量良好，本次校验无任何问题。

- 如果 errorCount > 0：
  🔴 发现 {{errorCount}} 条 Error，需在下一个工作日内完成修复后重新校验。
  请联系数据录入负责人，优先处理 R-CCN-001 / R-FUNC-001 / R-MAP-001 类问题。

- 如果 errorCount == 0 且 warningCount > 0：
  🟡 无 Error，但有 {{warningCount}} 条 Warning，请数据负责人确认是否需要处理。

---

## 严格约束

1. 不得在上述格式之外添加任何额外内容
2. 不得对数据进行任何推断或解释（只照实输出）
3. 如果 summary 或 topIssues 为空，输出："❌ 无法获取校验结果，请检查 Power Automate Flow 是否正常运行。"
4. 表格中不得省略任何列
5. 如果某个字段值为 null 或 undefined，输出 `—`（破折号）
6. 数据中可能含有中英文混合，直接原样输出，不得翻译
```

---

## Copilot Studio Topic 配置参考

### 触发节点
```
触发短语：
- 检查Offshoring数据
- 运行数据质量校验
- 查看数据质量报告
- Offshoring校验
- check offshoring data
```

### 消息节点（调用 Flow 前）
```
正在为您运行 Offshoring 数据质量校验，请稍候...
```

### 动作节点（调用 Power Automate Flow）
```
Flow: OffshoringDataQualityCheck
输入：（无，或让用户输入"月份"过滤）
输出：
  - summary（对象）
  - topIssues（数组）
```

### 生成式 AI 节点（或消息节点）
```
输入：上方 Prompt 模板 + summary + topIssues
输出：格式化报告
```

### 结束节点
```
感谢使用 Offshoring 数据质量助手。如需重新校验，请说"检查数据"。
```

---

## 示例输出（期望效果）

```
📋 Offshoring Headcount 数据质量报告
- 📁 检查文件：headcount_analysis.xlsx
- 🕒 检查时间：2024-02-01T08:15:00Z
- 📊 有效数据行数：312 行
- 🔴 Error 数量：5 条
- 🟡 Warning 数量：12 条

---

| 严重级别 | 规则 ID | 年月 | Cost Center | Function | Team | 问题列 | 当前值 | 说明 | 修复建议 |
|---------|--------|------|-------------|---------|------|-------|------|------|--------|
| 🔴 Error | R-FUNC-001 | 202401 | 1234567 | Unknown Function | Life Claims | Function | Unknown Function | Function 值不在白名单中 | 从 function-whitelist.json 中选择正确值 |
| 🔴 Error | R-MAP-001 | 202401 | 2345678 | Life Claims | Underwriting | Team | Underwriting | Underwriting 不属于 Life Claims 的允许 Team | Life Claims 下允许的 Team：Life Claims, Life Claims (High End Medical) 等 |
...

> 📌 以上仅展示前 50 条。完整明细请查看 SharePoint 记录列表。

---

| 规则 ID | 命中次数 | 严重级别 |
|--------|---------|---------|
| R-MAP-001 | 8 次 | Error |
| R-TEAM-001 | 4 次 | Warning |
| R-FUNC-001 | 2 次 | Error |

---

🔴 发现 5 条 Error，需在下一个工作日内完成修复后重新校验。
请联系数据录入负责人，优先处理 R-CCN-001 / R-FUNC-001 / R-MAP-001 类问题。
```

---

## 与范围读取方式的关联

Prompt 模板中的 `{{summary.dataRowCount}}` 字段来自 Office Script 的返回值（`result.dataRowCount`），直接反映从指定范围（命名范围 `rngOffshoring` 或固定范围 `A4:P2000`）中读取到的有效数据行数（已过滤空行）。

如果 `summary.overflowWarning` 不为 null，Copilot 的摘要里会显示溢出警告，提醒数据管理员扩大读取范围。
