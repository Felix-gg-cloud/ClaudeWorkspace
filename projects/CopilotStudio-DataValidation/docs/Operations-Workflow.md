# 运维操作流程文档

## Copilot Studio 数据校验引擎 — 每月维护与操作规范

---

## 1. 整体维护原则

> **唯一原则：规则变了只改知识库，不改 Instructions。**

- AI 系统说明（Instructions）区描述引擎行为约束与 JSON Schema，**只在架构调整时才修改**。
- 所有业务规则（字段枚举、映射关系等）维护在 Knowledge 知识模块的 `Data Validation Rules.md`。
- 每月正确数据模板（.md 或 .xlsx）通过 Power Automate 自动或手动同步上传至 Knowledge 模块。
- Copilot Studio 加载最新知识模块后，AI 即刻应用最新规则，无需额外操作。

---

## 2. 知识模块文件命名规范

| 文件类型 | 命名格式 | 示例 | 说明 |
|----------|----------|------|------|
| 业务规则文档 | `Data Validation Rules.md` | `Data Validation Rules.md` | 固定文件名，每次更新直接替换 |
| 月度模板（Markdown） | `YYYYMM-正确数据模板.md` | `202503-正确数据模板.md` | 每月新增，保留历史 |
| 月度模板（Excel） | `YYYYMM-正确数据模板.xlsx` | `202503-正确数据模板.xlsx` | 每月新增，保留历史 |

---

## 3. 每月标准操作流程

### 3.1 月初数据模板上传（每月第 1 个工作日）

| 步骤 | 操作 | 执行方式 | 责任人 |
|------|------|----------|--------|
| 1 | 从业务系统导出当月正确数据样本 | 手动 | 数据负责人 |
| 2 | 将样本另存为 `YYYYMM-正确数据模板.xlsx`（如 `202503-正确数据模板.xlsx`） | 手动 | 数据负责人 |
| 3 | 同步生成对应 Markdown 版本 `YYYYMM-正确数据模板.md` | Power Automate 自动触发 / 手动 | 数据负责人 / Automate 流程 |
| 4 | 将上述两个文件上传至 Copilot Studio Knowledge 知识模块 | Power Automate 自动触发 / 手动 | Automate 流程 / 数据负责人 |
| 5 | 在 Copilot Studio 验证知识模块已加载最新文件（查看文件列表时间戳） | 手动 | 数据负责人 |

### 3.2 规则文档更新（按需，规则变更时）

| 步骤 | 操作 | 执行方式 | 责任人 |
|------|------|----------|--------|
| 1 | 修改 `Data Validation Rules.md` 中对应规则内容（枚举值、映射关系等） | 手动 | 规则维护人 |
| 2 | 将更新后的 `Data Validation Rules.md` 上传替换 Knowledge 知识模块中的旧版本 | 手动 | 规则维护人 |
| 3 | 在 Copilot Studio 确认知识模块文件时间戳已更新 | 手动 | 规则维护人 |
| 4 | 使用测试数据发起一次验证调用，确认 AI 按新规则执行 | 手动 | QA / 规则维护人 |
| 5 | **无需修改 Instructions 区** | — | — |

### 3.3 每月校验执行流程（每月 N 日，按业务日程定）

```
Power Automate 定时触发
    ↓
读取当月待校验 Excel 文件
    ↓
按批次（建议每批 ≤ 50 行）构造 JSON { "meta": {...}, "rows": [...] }
    ↓
调用 Copilot Studio API（发送 JSON）
    ↓
Copilot Studio AI 读取最新 Knowledge 知识模块
    ↓
逐行执行全部规则校验
    ↓
返回标准 JSON 结果 { "report_model": {...}, "issues": [...] }
    ↓
Power Automate 解析结果，生成校验报告 / 触发后续流程
    ↓
问题数据通知责任人处理
```

---

## 4. Power Automate 端配置说明

### 4.1 请求格式

每次调用 Copilot Studio 时，请求 body 为标准 JSON：

```json
{
  "meta": {
    "source": "PowerAutomate",
    "yearMonth": "202503",
    "batch": 1
  },
  "rows": [
    {
      "RowIndex": 1,
      "YearMonth": "202503",
      "CostCenterNumber": "12345",
      "Function": "Life Claims",
      "Team": "Life Claims",
      "Owner": "Felix Chan"
    }
  ]
}
```

### 4.2 响应格式

Copilot Studio 返回严格 JSON，Power Automate 端按如下 Schema 解析：

```json
{
  "report_model": {
    "rows_total": 1,
    "rows_with_issues": 0,
    "error_count": 0,
    "warning_count": 0,
    "validated_at": "2025-03-01T09:00:00Z"
  },
  "issues": []
}
```

### 4.3 异常处理

| 场景 | AI 行为 | Automate 端处理 |
|------|---------|----------------|
| rows 为空或格式异常 | 返回 JSON，issues 中注明原因 | 记录错误日志，通知维护人 |
| Knowledge 无当月模板 | 返回 JSON，issues 中提示补充知识库 | 触发告警，通知数据负责人上传模板 |
| 规则与模板冲突 | 以规则文档为准执行校验 | 正常处理，无需干预 |
| AI 未返回合法 JSON | — | Automate 捕获异常，记录原始响应，通知维护人 |

---

## 5. 责任人矩阵

| 职责 | 责任人 | 频率 |
|------|--------|------|
| 每月数据模板导出与上传 | 数据负责人 | 每月第 1 个工作日 |
| 规则文档维护与更新 | 规则维护人 | 按需（规则变更时） |
| Power Automate 流程维护 | Automate 工程师 | 按需（流程调整时） |
| Copilot Studio Instructions 维护 | AI 架构负责人 | 极少（仅架构调整时） |
| 校验结果审核与问题处理 | 数据负责人 / 业务团队 | 每月校验日 |

---

## 6. 知识库维护检查清单（每月）

- [ ] 当月正确数据模板（`.xlsx`）已导出并命名为 `YYYYMM-正确数据模板.xlsx`
- [ ] 当月正确数据模板（`.md`）已生成并命名为 `YYYYMM-正确数据模板.md`
- [ ] 上述两个文件已上传至 Copilot Studio Knowledge 知识模块
- [ ] Knowledge 模块文件时间戳已更新（在 Copilot Studio 界面确认）
- [ ] 如有规则变更：`Data Validation Rules.md` 已更新并替换上传
- [ ] 测试调用已执行，AI 按最新规则正常返回 JSON
- [ ] **Instructions 区未做不必要的修改**（如有修改需记录变更原因）

---

## 7. 常见问题

**Q: 新增了一个 Function 枚举值，需要改 Instructions 吗？**  
A: 不需要。只需更新 `Data Validation Rules.md` 中的 Function 枚举列表，并替换上传到 Knowledge 模块即可。

**Q: AI 返回了不符合 Schema 的内容怎么办？**  
A: 先检查 Instructions 区的"输出约定"是否完整粘贴（参考 `AI-Instructions-Reference.md`）。如确认 Instructions 正确，在 Copilot Studio 中发起知识库重新索引，再测试。

**Q: 如何验证 AI 用的是最新规则？**  
A: 使用包含新规则覆盖场景的测试数据调用 API，检查返回的 issues 是否按新规则报错。

**Q: 两个月的模板都在知识库里，AI 会用哪个？**  
A: AI 会根据输入 `meta.yearMonth` 或 `rows[].YearMonth` 字段识别月份，优先匹配对应月份的模板。若无法匹配，以规则文档为准执行校验，并在 issues 中提示。
