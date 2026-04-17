# 校验规则目录 – tblOffshoring POC

> **数据源**: `tblOffshoring`（Excel Table）  
> **引用文件**: `function-whitelist.json` | `team-whitelist.json` | `function-team-mapping.json`

每条规则均包含：规则 ID、严重程度（Error / Warning）、目标列、校验逻辑、修复建议、Power Automate 表达式参考。

---

## 规则列表

### R01 – Cost Center Number 格式校验

| 字段 | 值 |
|---|---|
| Rule ID | `R01_CostCenterFormat` |
| Severity | Error |
| 目标列 | `Cost Center Number` |
| 校验逻辑 | 值为纯数字且长度等于 7 |
| Fix Suggestion | 补全前导零（如 `123456` → `0123456`）或联系数据录入人确认 |

**Power Automate 表达式（示例）**：
```
and(
  equals(length(string(item()?['Cost Center Number'])), 7),
  equals(
    string(int(string(item()?['Cost Center Number']))),
    string(item()?['Cost Center Number'])
  )
)
```
> 实际实现中用 `uriComponent` 或正则匹配 `^\d{7}$`；Power Automate 原生无正则，可借助 Office Script 或字符逐位判断。

---

### R02 – Function 白名单校验

| 字段 | 值 |
|---|---|
| Rule ID | `R02_FunctionWhitelist` |
| Severity | Error |
| 目标列 | `Function` |
| 校验逻辑 | 值必须在 `function-whitelist.json` 列表中（区分大小写） |
| Fix Suggestion | 使用标准值：Finance / HR / IT / Procurement / Legal / Supply Chain / Analytics / Compliance / Risk Management / Marketing / Operations / Customer Service / Tax / Treasury / Audit |

**Power Automate 表达式（示例）**：
```
contains(
  createArray('Finance','HR','IT','Procurement','Legal','Supply Chain','Analytics','Compliance','Risk Management','Marketing','Operations','Customer Service','Tax','Treasury','Audit'),
  item()?['Function']
)
```

---

### R03 – Team 白名单校验

| 字段 | 值 |
|---|---|
| Rule ID | `R03_TeamWhitelist` |
| Severity | Error |
| 目标列 | `Team` |
| 校验逻辑 | 值必须在 `team-whitelist.json` 列表中（区分大小写） |
| Fix Suggestion | 使用标准团队名称（见 `team-whitelist.json`） |

**Power Automate 表达式（示例）**：
```
contains(variables('allowedTeams'), item()?['Team'])
```
> `allowedTeams` 变量在 Flow 初始化时从 `team-whitelist.json` 或硬编码数组读取。

---

### R04 – Owner 非空校验

| 字段 | 值 |
|---|---|
| Rule ID | `R04_OwnerNonEmpty` |
| Severity | Error |
| 目标列 | `Owner` |
| 校验逻辑 | 值不为空、不为空字符串、不为纯空白 |
| Fix Suggestion | 填写负责人姓名或工号 |

**Power Automate 表达式（示例）**：
```
not(empty(trim(string(item()?['Owner']))))
```

---

### R05 – YearMonth 格式与有效月份校验

| 字段 | 值 |
|---|---|
| Rule ID | `R05_YearMonthFormat` |
| Severity | Error |
| 目标列 | `YearMonth` |
| 校验逻辑 | 值为 6 位纯数字；前 4 位为合理年份（2000–2099）；后 2 位为合法月份（01–12） |
| Fix Suggestion | 确保格式为 YYYYMM，例如 `202503` |

**Power Automate 表达式（示例）**：
```
and(
  equals(length(string(item()?['YearMonth'])), 6),
  greaterOrEquals(int(substring(string(item()?['YearMonth']), 4, 2)), 1),
  lessOrEquals(int(substring(string(item()?['YearMonth']), 4, 2)), 12),
  greaterOrEquals(int(substring(string(item()?['YearMonth']), 0, 4)), 2000),
  lessOrEquals(int(substring(string(item()?['YearMonth']), 0, 4)), 2099)
)
```

---

### R06 – 数值列非负校验（批量）

| 字段 | 值 |
|---|---|
| Rule ID | `R06_NumericNonNegative` |
| Severity | Error |
| 目标列 | `Actual_GBS_TeamMember`, `Actual_GBS_TeamLeaderAM`, `Actual_EA`, `Actual_HKT`, `Planned_GBS_TeamMember`, `Planned_GBS_TeamLeaderAM`, `Planned_EA`, `Planned_HKT`, `Target_YearEnd`, `Target_2030YearEnd` |
| 校验逻辑 | 1. 值可解析为数字（非 null、非空、非纯字母）；2. 解析后值 >= 0 |
| Fix Suggestion | 将负数或非数字值替换为 0 或正确数值 |

**Power Automate 表达式（对每列循环，示例以 `Actual_GBS_TeamMember` 为例）**：
```
and(
  not(empty(item()?['Actual_GBS_TeamMember'])),
  greaterOrEquals(float(item()?['Actual_GBS_TeamMember']), 0)
)
```
> 需在 Apply to each 外层用 `try/catch` 式错误处理（配置动作失败继续）捕获 `float()` 转换失败的情况。

**受影响的 10 列完整列表**（在 Flow 中逐一校验）：
- `Actual_GBS_TeamMember`
- `Actual_GBS_TeamLeaderAM`
- `Actual_EA`
- `Actual_HKT`
- `Planned_GBS_TeamMember`
- `Planned_GBS_TeamLeaderAM`
- `Planned_EA`
- `Planned_HKT`
- `Target_YearEnd`
- `Target_2030YearEnd`

---

### R07 – ShoringRatio 解析与范围校验

| 字段 | 值 |
|---|---|
| Rule ID | `R07_ShoringRatio` |
| Severity | Error |
| 目标列 | `ShoringRatio` |
| 校验逻辑 | 支持 3 种输入格式，统一归一化为 0–100 后验证范围 |
| Fix Suggestion | 使用 0–100 之间的数字（如 `23.5`），或百分比字符串（如 `23.5%`） |

**接受格式与归一化规则**：

| 输入格式 | 示例 | 归一化逻辑 | 归一化后值 |
|---|---|---|---|
| 0–1 小数 | `0.235` | 乘以 100 | 23.5 |
| 0–100 数字 | `23.5` | 原值 | 23.5 |
| 百分比字符串 | `23.5%` | 去掉 `%`，解析数字 | 23.5 |

**归一化判断逻辑（伪代码）**：
```
rawVal = item()?['ShoringRatio']

if rawVal ends with '%':
    normalized = float(trim('%', rawVal))
elif float(rawVal) <= 1.0:
    normalized = float(rawVal) * 100
else:
    normalized = float(rawVal)

isValid = (normalized >= 0) and (normalized <= 100)
```

**Power Automate 实现建议**：使用 Office Script 函数封装此逻辑，返回 `{ normalized: number, isValid: boolean }`。

---

### R08 – Function-Team 组合映射校验

| 字段 | 值 |
|---|---|
| Rule ID | `R08_FunctionTeamMapping` |
| Severity | Error |
| 目标列 | `Function` + `Team`（联合校验） |
| 校验逻辑 | 当前行的 `(Function, Team)` 组合必须在 `function-team-mapping.json` 中存在 |
| Fix Suggestion | 参照 `function-team-mapping.json` 修正 Function 或 Team 的值 |

**Power Automate 表达式（示例，mapping 已预加载为变量 `mappingObj`）**：
```
contains(
  variables('mappingObj')?[item()?['Function']],
  item()?['Team']
)
```
> `mappingObj` 在 Flow 初始化时通过 `Parse JSON` 从 mapping 文件读取（或硬编码 JSON）。

---

### R09 – 每月预期组合覆盖度警告（可选）

| 字段 | 值 |
|---|---|
| Rule ID | `R09_MonthlyComboWarning` |
| Severity | Warning |
| 目标列 | `YearMonth` + `Function` + `Team` |
| 校验逻辑 | 对于每个 YearMonth，`function-team-mapping.json` 中的所有允许组合若有缺失则产生 Warning |
| Fix Suggestion | 确认该月该 Function/Team 组合是否有意缺失，或补录相应数据行 |

**实现建议**：
1. 获取所有出现的 `YearMonth` 唯一值列表。
2. 对每个 YearMonth，计算 `(Function, Team)` 出现的组合集合。
3. 与 `function-team-mapping.json` 中的全量允许组合对比，缺失组合输出 Warning。

---

## 规则汇总表

| Rule ID | Severity | 目标列 | 说明 |
|---|---|---|---|
| R01_CostCenterFormat | Error | Cost Center Number | 7 位纯数字 |
| R02_FunctionWhitelist | Error | Function | 白名单校验 |
| R03_TeamWhitelist | Error | Team | 白名单校验 |
| R04_OwnerNonEmpty | Error | Owner | 非空 |
| R05_YearMonthFormat | Error | YearMonth | YYYYMM + 合法月 |
| R06_NumericNonNegative | Error | 10 个数值列 | 可解析数字且 >= 0 |
| R07_ShoringRatio | Error | ShoringRatio | 解析归一化 + 0-100 验证 |
| R08_FunctionTeamMapping | Error | Function + Team | 允许组合映射 |
| R09_MonthlyComboWarning | Warning | YearMonth + Function + Team | 月度组合覆盖度 |

---

## Issue 记录字段定义

每条发现的问题记录包含以下字段（用于 SharePoint 持久化与 Copilot Studio 摘要）：

| 字段名 | 说明 | 示例 |
|---|---|---|
| `Severity` | 严重程度 | `Error` / `Warning` |
| `RuleId` | 规则编号 | `R01_CostCenterFormat` |
| `YearMonth` | 所在行年月 | `202503` |
| `Cost Center Number` | 所在行成本中心编号 | `1234567` |
| `Function` | 所在行职能 | `Finance` |
| `Team` | 所在行团队 | `GBS-Finance-AP` |
| `Column` | 问题所在列名 | `Cost Center Number` |
| `Value` | 实际值（原始） | `12345` |
| `Message` | 问题描述 | `Cost Center Number must be exactly 7 digits` |
| `FixSuggestion` | 修复建议 | `Pad with leading zero: 0123456` |
