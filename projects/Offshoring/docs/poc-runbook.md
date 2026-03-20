# tblOffshoring AI-Only 校验 POC Runbook

> **版本：** v2.0（含全列空格 Error 策略）  
> **更新日期：** 2026-03-20  
> **适用场景：** Copilot Studio（或 Azure OpenAI）作为唯一校验引擎；Power Automate 仅负责机械读取与结果落地

---

## 一、架构概览

```
Excel (tblOffshoring)
        │  ① 机械读取（无任何清洗）
        ▼
Power Automate Flow
        │  ② 构造 payload（含原始值 + 规则配置）
        ▼
Copilot Studio / AI 校验步骤
        │  ③ 按规则清单逐行逐列校验 → 输出 issues[] + markdown_report
        ▼
Power Automate Flow
        │  ④ 落地：保存 issues JSON + Markdown 报告到 SharePoint
        │  ⑤ 通知：Teams 消息 / 邮件（含报告链接）
        ▼
业务用户
```

---

## 二、Power Automate 流程步骤

### 关键原则

> **PA 只做机械读取，不对任何字段做清洗、trim 或格式转换。**  
> 手动填写的数据可能包含误输入的空格等低级错误，这类问题必须原样传递给 AI 校验器，由 AI 检测并报告。

### Step 1 — 触发器

- 类型：手动触发（`Manually trigger a flow`）或定时触发（`Recurrence`，每月初）
- 可选：接收输入参数（如目标 SharePoint Site URL、Excel 文件路径）

### Step 2 — 读取 Excel 数据

**操作：** `List rows present in a table`

| 参数 | 值 |
|---|---|
| Location | SharePoint 站点地址 |
| Document Library | 文档库名称 |
| File | Excel 文件路径（相对路径） |
| Table | `tblOffshoring` |

> ⚠️ **注意：** 此步骤读取的每个字段值均为原始字符串。Power Automate 不会自动 trim，这是我们期望的行为。

### Step 3 — 构造批次数据

**操作：** `Compose`（或 `Apply to each` + 累加数组）

将读取到的每行数据转换为如下 JSON 格式，确保字段名与规则清单一致：

```json
{
  "RowIndex": @{iterationIndex()},
  "CostCenterNumber": @{items('Apply_to_each')?['Cost Center Number']},
  "Function": @{items('Apply_to_each')?['Function']},
  "Team": @{items('Apply_to_each')?['Team']},
  "Owner": @{items('Apply_to_each')?['Owner']},
  "YearMonth": @{items('Apply_to_each')?['YearMonth']},
  "Actual_Jan": @{items('Apply_to_each')?['Actual_Jan']},
  "Actual_Feb": @{items('Apply_to_each')?['Actual_Feb']},
  "Planned_Jan": @{items('Apply_to_each')?['Planned_Jan']},
  "Planned_Feb": @{items('Apply_to_each')?['Planned_Feb']},
  "Target_YearEnd": @{items('Apply_to_each')?['Target_YearEnd']},
  "Target_2030YearEnd": @{items('Apply_to_each')?['Target_2030YearEnd']},
  "ShoringRatio": @{items('Apply_to_each')?['ShoringRatio']}
}
```

> ⚠️ **绝对不要使用 `trim()`、`toLower()`、`replace()` 等函数处理字段值。**

### Step 4 — 分批（可选，超过 200 行时使用）

若 `tblOffshoring` 行数超过 200 行（或接近 AI 上下文 token 限制），按 `YearMonth` 字段分组：

- 使用 `Filter array` 按 YearMonth 筛选
- 每个 YearMonth 组单独调用 AI 校验步骤（Step 5）
- 最后用 `Union` 合并所有批次的 issues 数组

### Step 5 — 调用 AI 校验

**操作：** `HTTP`（调用 Azure OpenAI/Copilot Studio API）或 Copilot Studio 的生成式 AI 操作

构造请求体（替换占位符）：

```json
{
  "systemPrompt": "你是一个严格的数据质量校验器...",
  "userPrompt": "请按照以下规则清单对下方行数据进行逐行逐列校验...\n\n数据：@{variables('batchRowsJson')}"
}
```

**必须在 prompt 中传入的配置：**
- `allowedFunctions`：Function 白名单 JSON 数组
- `allowedTeams`：Team 白名单 JSON 数组
- `allowedPairs`：Function-Team 合法组合 JSON 数组
- `rows`：本批次行数据（RawValue 原始字符串）

> 参考完整 prompt 模板：[ai-only-validator.prompt.md](../prompts/ai-only-validator.prompt.md)

### Step 6 — 解析 AI 输出

**操作：** `Parse JSON`

Schema 示例：

```json
{
  "type": "object",
  "properties": {
    "issues": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "Severity": { "type": "string" },
          "RuleId": { "type": "string" },
          "RowKey": { "type": "string" },
          "Column": { "type": "string" },
          "RawValue": { "type": "string" },
          "Message": { "type": "string" },
          "FixSuggestion": { "type": "string" }
        }
      }
    },
    "markdown_report": { "type": "string" }
  }
}
```

### Step 7 — 保存 issues JSON

**操作：** `Create file`（SharePoint）

| 参数 | 值 |
|---|---|
| Folder Path | `/Data Quality Reports/` |
| File Name | `issues_@{utcNow('yyyyMMdd_HHmm')}.json` |
| File Content | `@{body('Parse_JSON')?['issues']}` |

### Step 8 — 保存 Markdown 报告

**操作：** `Create file`（SharePoint）

| 参数 | 值 |
|---|---|
| Folder Path | `/Data Quality Reports/` |
| File Name | `report_@{utcNow('yyyyMMdd_HHmm')}.md` |
| File Content | `@{body('Parse_JSON')?['markdown_report']}` |

### Step 9 — 发送 Teams 通知

**操作：** `Post message in a chat or channel`（Microsoft Teams）

消息模板：

```
📊 tblOffshoring 数据质量校验完成

✅ 总行数：@{variables('totalRows')}
❌ Error 数量：@{length(body('Parse_JSON')?['issues'])}

📄 完整报告：@{outputs('Create_report_file')?['body/webUrl']}
📋 Issues JSON：@{outputs('Create_issues_file')?['body/webUrl']}
```

---

## 三、AI 校验规则速查

详细规则定义见：[validation-rules.md](./validation-rules.md)

| 规则 | 适用列 | Severity | 关键触发条件 |
|---|---|---|---|
| **R-WS-ALL-001** | **所有列** | **Error** | **RawValue 有前后空格** |
| R-REQ-YM-001 | YearMonth | Error | 空值 |
| R-REQ-CCN-001 | Cost Center Number | Error | 空值 |
| R-REQ-FUNC-001 | Function | Error | 空值 |
| R-REQ-OWN-001 | Owner | Error | Total 行且空值 |
| R-REQ-NT-001 | Target_*/ShoringRatio | Error | Non-Total 行且空值 |
| R-YM-001 | YearMonth | Error | 非 YYYYMM 格式 |
| R-SR-001 | ShoringRatio | Error | 非合法百分比或超 0-100 |
| R-FUNC-001 | Function | Error | 不在白名单 |
| R-TEAM-001 | Team | Error | 不在白名单 |
| R-FT-MAP-001 | Function+Team | Error | 非合法组合 |
| R-NUM-001 | Actual_*/Planned_*/Target_* | Error | 非数字或 < 0 |

---

## 四、常见问题排查

### Q1：AI 输出不是合法 JSON？

**原因：** AI 在 JSON 前后添加了 Markdown code fence 或自然语言。  
**处理：** 在 prompt 中强调"只输出 JSON，不添加任何包装"；在 PA 中用正则 `\{[\s\S]*\}` 提取 JSON 子串。

### Q2：同一个问题被重复报告（如空格 + 格式错误）？

**原因：** AI 同时报了 R-WS-ALL-001 和格式规则。  
**说明：** 根据设计，若 trim 后仍存在格式问题，应同时报出。若 trim 后合法，则只报 R-WS-ALL-001。  
**处理：** 检查 prompt 中的"避免重复噪音"指令是否生效；如需仅报空格错误，可在 prompt 中增加"若 trim(RawValue) 通过格式校验，则不额外报格式规则"。

### Q3：Token 超限？

**处理：**  
1. 按 YearMonth 分批（Step 4）  
2. 简化白名单/映射表格式（用紧凑 JSON 而非自然语言）  
3. 若仍超限，每批限制为 100 行

### Q4：R-WS-ALL-001 未被触发？

**检查：**  
1. 确认 PA 读取的字段值未被 Power Automate 框架自动 trim（通常不会，但需验证）  
2. 在调试时，用 `Compose` 输出原始行数据查看字段值是否保留空格

---

## 五、POC 演示脚本（5 分钟版）

1. **（30秒）** 打开 Excel，展示 tblOffshoring 原始数据（包含故意填写错误的行：有空格、格式错误、缺填）
2. **（1分钟）** 手动触发 Power Automate Flow，展示 Flow 运行中状态
3. **（1分钟）** 展示 AI 校验步骤的输入（原始 JSON payload）和输出（issues + markdown_report）
4. **（1分钟）** 打开 SharePoint，展示自动生成的 Markdown 报告和 issues JSON 文件
5. **（1分钟）** 展示 Teams 通知，点击链接跳转到报告
6. **（30秒）** 总结：全流程端到端，PA 零数据处理，AI 发现所有低级错误（包括空格）

---

## 六、变更日志

| 版本 | 日期 | 变更内容 |
|---|---|---|
| v1.0 | 2026-03-01 | 初始版本，AI-only 校验模式，Error/Warning 两级 |
| v2.0 | 2026-03-20 | 新增 R-WS-ALL-001 全列空格 Error 规则；明确 PA 不做任何数据清洗；更新 prompt 模板、样例 payload 和期望输出 |
