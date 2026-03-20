# Offshoring 数据校验 POC — 操作手顺（Runbook）

## 文档信息

| 属性 | 值 |
|------|----|
| 版本 | v1.0（POC） |
| 适用月份 | 2025-01 起 |
| 维护人 | 流程负责人 |
| 最后更新 | 2025-01 |

---

## 1. 前置准备

### 1.1 填表规范确认

在提交数据前，请确认 `tblOffshoring.xlsx` 满足以下基本要求：

- [ ] 文件已存放于 OneDrive 指定路径（`/Offshoring/tblOffshoring.xlsx`）
- [ ] Excel 中存在名为 `tblOffshoring` 的已命名表格（Excel 表格，不是普通区域）
- [ ] 数据已按当月月份（YearMonth 格式 `YYYYMM`）填写完整
- [ ] 所有单元格**不含前后空格**（这是最常见的低级错误）
- [ ] 所有必填列已填写（见下方"必填字段一览"）

### 1.2 必填字段一览

| 列名 | 所有行 | Non-Total 行 | Total 行 |
|------|--------|-------------|---------|
| YearMonth | ✅ 必填 | ✅ 必填 | ✅ 必填 |
| Cost Center Number | — | ✅ 必填 | ❌ 不适用 |
| Function | — | ✅ 必填 | 固定为 `Total` |
| Team | — | ✅ 必填 | 可为空 |
| Owner | — | ✅ 必填 | ✅ 必填 |
| ShoringRatio | — | ✅ 必填 | 可选 |
| 数值列（Actual/Planned/Target） | — | 空则跳过 | 空则跳过 |

> **如何判断是否为 Total 行**：Function 列值为 `Total`（精确匹配，区分大小写）。

### 1.3 ShoringRatio 格式要求

- 格式必须为 `数字%`，例如 `12%`、`33.5%`、`100%`
- 取值范围：0-100（包含边界）
- **不允许**：`12 %`（有空格）、`0.12`（无 % 号）、`120%`（超出范围）

### 1.4 Function-Team 合法组合

请确保 Function 和 Team 的组合在合法映射表中（详见 `schema/tblOffshoring-schema.json`）。  
特殊规则：`Function=Total` + `Team=（空）` 是合法组合。

---

## 2. 触发校验流程

### 2.1 登录 Power Automate

1. 打开浏览器，访问 [https://make.powerautomate.com](https://make.powerautomate.com)
2. 使用工作账号登录
3. 在左侧导航栏选择"**我的流程**"（My flows）

### 2.2 找到流程

1. 搜索流程名称：`Offshoring Data Validation - Manual Trigger`
2. 点击流程名称进入详情页

### 2.3 手动运行

1. 点击页面右上角"**运行**"（Run）按钮
2. 在弹出的面板中填写（可选）：
   - **YearMonth**：若留空，系统自动从数据第一行读取
   - **TopN**：PDF 摘要中展示的最大问题数（默认 20，可不填）
3. 点击"**运行流程**"（Run flow）
4. 点击"**完成**"（Done）

> 流程运行期间（约 1-3 分钟）可以关闭弹窗，流程会在后台继续运行。

### 2.4 查看运行状态

1. 在流程详情页，下滑至"**28 天运行历史记录**"（Run history）
2. 刷新页面，查看最新一次运行：
   - 🟢 已成功（Succeeded）：报告已生成
   - 🔴 已失败（Failed）：点击查看错误详情并联系流程负责人
   - 🟡 运行中（Running）：等待完成

---

## 3. 查看校验报告

### 3.1 通过 Teams 通知（推荐）

流程成功后，会自动在 Teams 频道发送通知消息，其中包含：
- 校验结果概览（Error 数、通过率）
- Excel 报告直链
- PDF 摘要直链

点击链接即可直接打开。

### 3.2 直接访问 SharePoint

1. 打开 SharePoint 站点
2. 导航至文档库 `Reports/Offshoring/`
3. 找到以当前月份开头的报告文件：
   - `ValidationReport_<YearMonth>_<Timestamp>.xlsx`（完整明细）
   - `ValidationReport_<YearMonth>_<Timestamp>.pdf`（摘要）

---

## 4. 处理 Error

### 4.1 查阅 Excel 报告（Issues Sheet）

Excel 报告的 **Issues** Sheet 包含所有 Error 的详细信息：

| 列 | 含义 |
|----|------|
| RowKey | 定位到问题行（含行号 + 业务字段） |
| Column | 哪一列有问题 |
| RawValue | 原始输入值（便于查看是否有空格） |
| Message | 问题描述 |
| FixSuggestion | 修复建议 |

### 4.2 常见 Error 类型与修复方法

| RuleId | 问题描述 | 修复方法 |
|--------|----------|----------|
| `R-WS-ALL-001` | 列值含前后空格 | 在 Excel 中找到对应单元格，删除首尾空格（或使用 TRIM 函数检查） |
| `R-YM-001` | YearMonth 缺失或格式错误 | 确认格式为 6 位数字 `YYYYMM`，月份 01-12 |
| `R-REQ-001` | 必填列为空 | 填写缺失的值 |
| `R-WL-FUNC-001` | Function 值不在白名单 | 检查是否有拼写错误或多余空格；参阅合法 Function 列表 |
| `R-WL-TEAM-001` | Team 值不在白名单 | 同上 |
| `R-MAP-FT-001` | Function-Team 组合不合法 | 参阅合法映射表，确认组合正确 |
| `R-NUM-001` | 数值列含非数字或负数 | 确认该列只填数字，且 >= 0 |
| `R-SR-001` | ShoringRatio 格式错误或超范围 | 使用 `数字%` 格式，范围 0-100 |

### 4.3 修复后重新提交

1. 在 OneDrive 中打开 `tblOffshoring.xlsx`，按报告提示修复所有 Error
2. 保存文件（确保 OneDrive 同步完成）
3. 重新触发校验流程（见第 2 节）
4. 确认新报告中 Error 数量为 0

---

## 5. 报告归档

- 每次校验均会生成带时间戳的新报告文件，**不覆盖历史报告**
- 历史报告保存在 SharePoint `/Reports/Offshoring/` 中，可按文件名中的 YearMonth 筛选
- 建议在月度数据正式确认（Error=0）后，将通过版本的报告链接发送给相关方存档

---

## 6. 常见问题（FAQ）

**Q：流程运行失败，怎么排查？**  
A：在 Power Automate 运行历史中点击失败记录，查看哪个步骤出错。常见原因：
- OneDrive 文件不存在或路径错误
- Excel 中没有名为 `tblOffshoring` 的表格
- Azure OpenAI 配额不足或网络超时

**Q：报告没有收到 Teams 通知，怎么找报告？**  
A：直接去 SharePoint `/Reports/Offshoring/` 文档库查找，按"修改时间"降序排列即可找到最新报告。

**Q：AI 校验结果是否每次相同？**  
A：因为 AI 的 `temperature` 参数设为 0，且规则以结构化方式写死在提示词中，同一份数据的校验结果应当高度一致。但如遇极端边界情况，建议人工复核。

**Q：能否只校验特定行？**  
A：当前 POC 版本不支持部分行校验，每次运行都会校验 `tblOffshoring` 表中的所有行。如有需要，可在后续迭代中增加行筛选参数。

**Q：数值列我填了 0，是合法的吗？**  
A：是的，`0` 是合法值（满足 `>= 0` 要求）。只有负数或非数字才会报 Error。

---

## 7. 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v1.0 | 2025-01 | POC 初始版本；AI-only 校验模式；Excel + PDF 报告输出 |
