# Sample Output Report — POC Headcount Excel Validator

**版本：** 2.0（YearMonth 所有行均必填）  
**场景说明：** 以下为一个包含典型错误的示例数据集，展示 Copilot Studio 的报告输出格式。

---

## 示例输入数据（tblOffshoring，部分行）

| 行 | Function | Team | Owner | YearMonth | Cost Center Number | Target_YearEnd | Target_2030YearEnd | ShoringRatio |
|----|----------|------|-------|-----------|-------------------|----------------|-------------------|--------------|
| 1 | Total (All) | | Alice | 202501 | | | | 30% |
| 2 | Total (Core Operations) | | | | | | | |
| 3 | Operations | Team A | Bob | 202502 | 1234567 | 100 | 150 | 25% |
| 4 | Operations | Team A | Carol | | 1234567 | 80 | 120 | |
| 5 | Finance | Team B | Dave | 202503 | 123456 | -5 | 200 | 12.5% |
| 6 | Finance | Team X | Eve | 202503 | 1234568 | 90 | 130 | 0.5 |

---

## Power Automate 生成的校验 JSON（示例）

```json
{
  "ReportGeneratedAt": "2026-03-20T02:30:00Z",
  "TotalRows": 6,
  "TotalIssues": 8,
  "ErrorCount": 7,
  "WarningCount": 1,
  "Issues": [
    {
      "Severity": "Error",
      "RuleId": "R-REQ-TOTAL-OWNER",
      "YearMonth": "",
      "CostCenterNumber": "",
      "Function": "Total (Core Operations)",
      "Team": "",
      "Column": "Owner",
      "Value": "",
      "Message": "Total 行必须填写 Owner。",
      "FixSuggestion": "补充 Owner 后重新提交。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-REQ-ALL-YM",
      "YearMonth": "",
      "CostCenterNumber": "",
      "Function": "Total (Core Operations)",
      "Team": "",
      "Column": "YearMonth",
      "Value": "",
      "Message": "YearMonth 不允许为空，所有行（包括 Total 行）均须填写。",
      "FixSuggestion": "填写 YearMonth，格式为 YYYYMM（例如 202501）。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-REQ-ALL-YM",
      "YearMonth": "",
      "CostCenterNumber": "1234567",
      "Function": "Operations",
      "Team": "Team A",
      "Column": "YearMonth",
      "Value": "",
      "Message": "YearMonth 不允许为空，所有行（包括 Total 行）均须填写。",
      "FixSuggestion": "填写 YearMonth，格式为 YYYYMM（例如 202501）。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-REQ-NT-SR",
      "YearMonth": "",
      "CostCenterNumber": "1234567",
      "Function": "Operations",
      "Team": "Team A",
      "Column": "ShoringRatio",
      "Value": "",
      "Message": "非 Total 行 ShoringRatio 不允许为空。",
      "FixSuggestion": "填写 ShoringRatio（例如 23%）。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-CCN-001",
      "YearMonth": "202503",
      "CostCenterNumber": "123456",
      "Function": "Finance",
      "Team": "Team B",
      "Column": "Cost Center Number",
      "Value": "123456",
      "Message": "Cost Center Number 必须为 7 位纯数字（例如 1234567）。",
      "FixSuggestion": "检查 Cost Center Number 是否填写正确。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-NUM-002",
      "YearMonth": "202503",
      "CostCenterNumber": "123456",
      "Function": "Finance",
      "Team": "Team B",
      "Column": "Target_YearEnd",
      "Value": "-5",
      "Message": "Target_YearEnd 不能为负数。",
      "FixSuggestion": "将 Target_YearEnd 改为 0 或正数。"
    },
    {
      "Severity": "Warning",
      "RuleId": "R-TEAM-001",
      "YearMonth": "202503",
      "CostCenterNumber": "1234568",
      "Function": "Finance",
      "Team": "Team X",
      "Column": "Team",
      "Value": "Team X",
      "Message": "Team 值不在已知列表中，请确认拼写是否正确。",
      "FixSuggestion": "从下拉列表中选择有效的 Team 值，或联系管理员添加新 Team。"
    },
    {
      "Severity": "Error",
      "RuleId": "R-SR-001",
      "YearMonth": "202503",
      "CostCenterNumber": "1234568",
      "Function": "Finance",
      "Team": "Team X",
      "Column": "ShoringRatio",
      "Value": "0.5",
      "Message": "ShoringRatio 必须使用百分比格式，例如 23%（范围 0%–100%，允许小数如 12.5%）。",
      "FixSuggestion": "改为"数字+%"格式，不要填写 0.23 或 23。"
    }
  ]
}
```

---

## Copilot Studio 输出示例（中文摘要报告）

---

### 📊 校验摘要

本次校验共处理 **6 行** 数据，发现 **8 处问题**，其中 **Error（必须修复）7 处**、**Warning（建议检查）1 处**。请数据提交人员按照以下列表逐一修复后重新提交。

---

### ❌ Error 问题汇总（共 7 处，必须修复）

| YearMonth | Cost Center No. | Function | Team | 问题列 | 问题描述 |
|-----------|-----------------|----------|------|--------|----------|
| *(空)* | *(空)* | Total (Core Operations) | *(空)* | Owner | Total 行必须填写 Owner |
| *(空)* | *(空)* | Total (Core Operations) | *(空)* | **YearMonth** | **YearMonth 不允许为空，所有行（含 Total 行）均须填写** |
| *(空)* | 1234567 | Operations | Team A | **YearMonth** | **YearMonth 不允许为空，所有行（含 Total 行）均须填写** |
| *(空)* | 1234567 | Operations | Team A | ShoringRatio | 非 Total 行 ShoringRatio 不允许为空 |
| 202503 | 123456 | Finance | Team B | Cost Center Number | 必须为 7 位纯数字（当前：6 位） |
| 202503 | 123456 | Finance | Team B | Target_YearEnd | 不能为负数（当前值：-5） |
| 202503 | 1234568 | Finance | Team X | ShoringRatio | 格式错误，应为"数字+%"（当前：0.5） |

---

### ⚠️ Warning 问题汇总（共 1 处，建议检查）

| YearMonth | Cost Center No. | Function | Team | 问题列 | 问题描述 |
|-----------|-----------------|----------|------|--------|----------|
| 202503 | 1234568 | Finance | Team X | Team | Team 值"Team X"不在已知列表中，请确认拼写 |

---

### 🔧 修复优先级建议

**P1 — 立即修复（YearMonth 为空）：**
- ⚠️ **重要提示**：YearMonth 为**所有行必填项**（包括 Total 行）。本次发现 **2 行** YearMonth 为空，请补全后才能进行月度汇总分析。
  - 行：Function = `Total (Core Operations)`
  - 行：Function = `Operations`，Team = `Team A`

**P2 — 数据完整性修复：**
- `Total (Core Operations)` 行：补充 Owner
- `Operations` / `Team A` 行：补充 ShoringRatio（格式：xx%）

**P3 — 数据格式修复：**
- `Finance` / `Team B` 行：
  - Cost Center Number 改为 7 位数字
  - Target_YearEnd 改为 ≥ 0 的数字
- `Finance` / `Team X` 行：
  - ShoringRatio 改为百分比格式（如 `0.5%`）

**P4 — 建议核实（Warning）：**
- `Finance` / `Team X`：Team 名称"Team X"不在白名单，请确认是否为新增团队

---

### ✅ 通过行（无问题）

| 行 | YearMonth | Function | Team |
|----|-----------|----------|------|
| 1 | 202501 | Total (All) | *(空)* |
| 3 | 202502 | Operations | Team A |

---

> **说明**：行 1（Total (All)）YearMonth = 202501 已填写，Owner = Alice，符合 Total 行必填要求。  
> 如有疑问请联系数据管理员。

---

## 规则变更说明（v2.0 更新点）

| 变更项 | v1.x | v2.0（当前） |
|--------|------|--------------|
| YearMonth 必填范围 | 仅 Total 行必填 | **所有行均必填**（含 Non-Total 行） |
| YearMonth 格式检查 | 仅 Total 行校验格式 | 所有行校验（空时跳过格式检查避免重复） |
| 其他规则 | 同上 | 不变 |
