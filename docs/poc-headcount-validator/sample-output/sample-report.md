# 样例输出 — Headcount 月报数据质量报告

> 以下为 Power Automate Flow 调用 Copilot Studio 后生成的示例报告。  
> 本样例反映了 R-SR-001（ShoringRatio 百分比格式硬规则）的严格校验结果。

---

# Headcount 月报数据质量报告

**生成时间**：2025-01-15 08:32:00 UTC  
**文件**：headcount_analysis_poc.xlsx（Sheet: Offshoring, Table: tblOffshoring）  
**Flow 运行 ID**：`run-20250115-083200`

---

## 校验摘要

| 项目 | 数量 |
|------|------|
| 总行数（非空） | 342 |
| Error 数 | 17 |
| Warning 数 | 5 |
| 涉及规则数 | 4 |

**状态：❌ 存在 Error，请修正后重新提交**

---

## Top 命中规则

| 排名 | Rule ID | 说明 | 命中次数 | Severity |
|------|---------|------|----------|----------|
| 1 | R-SR-001 | ShoringRatio 格式不合规 | 12 | **Error** |
| 2 | R-CC-001 | Cost Center Number 格式错误 | 3 | **Error** |
| 3 | R-OW-001 | Owner 为空 | 2 | **Error** |
| 4 | R-NV-001 | 数值列含负数 | 5 | Warning |

---

## 问题明细（Top 50，Error 优先）

| Severity | RuleId | YearMonth | Cost Center | Column | Value | 错误消息 | 修正建议 |
|----------|--------|-----------|-------------|--------|-------|----------|----------|
| Error | R-SR-001 | 202501 | 1234567 | ShoringRatio | `0.23` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "0.23" 改为 "23%" |
| Error | R-SR-001 | 202501 | 1234568 | ShoringRatio | `0.5` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "0.5" 改为 "50%" |
| Error | R-SR-001 | 202501 | 1234569 | ShoringRatio | `30` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "30" 改为 "30%" |
| Error | R-SR-001 | 202501 | 1234570 | ShoringRatio | `75` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "75" 改为 "75%" |
| Error | R-SR-001 | 202501 | 1234571 | ShoringRatio | `23.5%` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "23.5%" 改为 "24%"（取整） |
| Error | R-SR-001 | 202501 | 1234572 | ShoringRatio | `101%` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 值超出范围，最大为 "100%" |
| Error | R-SR-001 | 202412 | 1234567 | ShoringRatio | `0.1` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "0.1" 改为 "10%" |
| Error | R-SR-001 | 202412 | 1234568 | ShoringRatio | `` (空) | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 请填写 ShoringRatio，格式如 "23%" |
| Error | R-SR-001 | 202412 | 1234573 | ShoringRatio | `100` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "100" 改为 "100%" |
| Error | R-SR-001 | 202412 | 1234574 | ShoringRatio | `0` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "0" 改为 "0%" |
| Error | R-SR-001 | 202412 | 1234575 | ShoringRatio | `50.0%` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 将 "50.0%" 改为 "50%" |
| Error | R-SR-001 | 202411 | 1234576 | ShoringRatio | `N/A` | ShoringRatio 必须使用百分比格式，例如 23%（0%~100%） | 请填写有效的百分比，如 "0%"（若尚无数据） |
| Error | R-CC-001 | 202501 | `123456` | Cost Center Number | `123456` | Cost Center Number 必须为 7 位纯数字，例如 1234567 | 检查是否缺少一位数字 |
| Error | R-CC-001 | 202501 | `CC12345` | Cost Center Number | `CC12345` | Cost Center Number 必须为 7 位纯数字，例如 1234567 | 删除前缀 "CC"，仅保留 7 位数字 |
| Error | R-CC-001 | 202412 | `1234-567` | Cost Center Number | `1234-567` | Cost Center Number 必须为 7 位纯数字，例如 1234567 | 删除连字符，改为 "1234567" |
| Error | R-OW-001 | 202501 | 1234580 | Owner | `` (空) | Owner 不能为空 | 请填写负责人姓名或工号 |
| Error | R-OW-001 | 202412 | 1234581 | Owner | `` (空) | Owner 不能为空 | 请填写负责人姓名或工号 |
| Warning | R-NV-001 | 202501 | 1234582 | Actual_GBS_TeamMember | `-1` | Actual_GBS_TeamMember 必须为非负数字 | 请确认是否为录入错误，正确值应 ≥ 0 |
| Warning | R-NV-001 | 202501 | 1234583 | Planned_EA | `-5` | Planned_EA 必须为非负数字 | 请确认是否为录入错误，正确值应 ≥ 0 |
| Warning | R-NV-001 | 202412 | 1234584 | Target_YearEnd | `-2` | Target_YearEnd 必须为非负数字 | 请确认是否为录入错误，正确值应 ≥ 0 |
| Warning | R-NV-001 | 202412 | 1234585 | Actual_HKT | `abc` | Actual_HKT 必须为非负数字 | 请填写数字，删除非数字字符 |
| Warning | R-NV-001 | 202411 | 1234586 | Planned_GBS_TeamLeaderAM | `-0.5` | Planned_GBS_TeamLeaderAM 必须为非负数字 | 请确认是否为录入错误，正确值应 ≥ 0 |

---

## 合规示例（供参考）

以下为格式完全合规的 ShoringRatio 值：

| 值 | 是否合规 |
|----|----------|
| `0%` | ✅ 合规 |
| `23%` | ✅ 合规 |
| `50%` | ✅ 合规 |
| `100%` | ✅ 合规 |
| `  30%  `（前后有空格） | ✅ 合规（系统自动 trim） |
| `0.23` | ❌ 不合规（小数格式） |
| `23` | ❌ 不合规（缺少 %） |
| `23.5%` | ❌ 不合规（含小数位） |
| `101%` | ❌ 不合规（超出范围） |
| 空值 | ❌ 不合规（不得为空） |

---

## 后续建议

### 必须处理（Error 级别）
1. **批量修正 ShoringRatio（12 条）**  
   在 Excel 中新增辅助列，用以下公式转换：
   - 若原值为小数比率（如 `0.23`）：`=TEXT(原列*100,"0")&"%"`
   - 若原值为纯数字（如 `23`）：`=原列&"%"`
   - 转换后检查数值是否在 0–100 范围内，再粘贴为"值"覆盖原列。

2. **修正 Cost Center Number（3 条）**  
   确保该列为 7 位纯数字，无前缀、无连字符。

3. **填写 Owner（2 条）**  
   联系对应 Cost Center 的数据维护人员补充 Owner 信息。

### 建议处理（Warning 级别）
4. **数值列负数（5 条）**  
   确认是否为录入错误，若确为 0 则改为 `0`。

### 系统提示
- 本次校验接近范围上限的预警：当前行数 342，远低于上限 20000，无需扩范围。
- 修正完成后，请重新上传文件并手动触发 Flow 进行再次校验。
- 如需将 Excel 模板中的 ShoringRatio 列设置数据验证（防止录入错误），可在 Excel 中：  
  数据 → 数据验证 → 允许"文本长度"或使用自定义公式 `=ISNUMBER(VALUE(LEFT(A1,LEN(A1)-1)))` 提前拦截。
