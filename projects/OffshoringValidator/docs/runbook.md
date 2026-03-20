# Offshoring POC 校验器 — 完整运营手册（Runbook）

> **版本**：v1.0  
> **适用场景**：每月 Offshoring 数据提交校验（AI-only 校验模式）  
> **维护者**：数据管理员  

---

## 1. 系统架构概述

```
数据填写员
    │ 在 Excel 中填写 tblOffshoring 数据
    ↓
SharePoint / OneDrive（数据源）
    │ tblOffshoring.xlsx（原始数据，不经任何处理）
    ↓
Power Automate（流程引擎）
    │ 机械读取原始行 → 调用 AI → 解析结果 → 生成报告 → 存档 → 通知
    ↓
Copilot Studio / AI Builder（AI 校验引擎）
    │ 按规则逐行校验，输出结构化 JSON
    ↓
报告存储（SharePoint ValidationReports/）
    ├── ValidationReport_<YearMonth>_<Timestamp>.xlsx（Excel 明细报告）
    └── ValidationReport_<YearMonth>_<Timestamp>.pdf（PDF 摘要报告）
    ↓
通知（Teams 消息 / 邮件）→ 数据管理员
```

---

## 2. 校验规则速查表

> 完整规则详见 `docs/rules-specification.md`

| 规则 ID | 严重级别 | 描述 | 适用行 |
|---------|---------|------|--------|
| R-WS-ALL-001 | **Error** | 任何列存在前导/尾随空格 | 所有行 |
| R-YM-001 | **Error** | YearMonth 为空 | 所有行 |
| R-YM-FMT-001 | **Error** | YearMonth 格式非 YYYYMM 或月份超出 01-12 | 所有行 |
| R-REQ-CCN-001 | **Error** | Cost Center Number 为空 | 所有行 |
| R-REQ-FUNC-001 | **Error** | Function 为空 | 所有行 |
| R-FUNC-WL-001 | **Error** | Function 不在白名单 | 所有行 |
| R-REQ-TEAM-001 | **Error** | Team 为空（非 Total 行）| 非 Total 行 |
| R-TEAM-WL-001 | **Error** | Team 不在白名单 | 非 Total 行 |
| R-MAP-FT-001 | **Error** | Function-Team 组合非法 | Function/Team 均非空 |
| R-REQ-OWNER-001 | **Error** | Owner 为空 | 所有行 |
| R-NUM-001 | **Error** | 数值列非数字或为负数 | 数值列非空时 |
| R-SR-001 | **Error** | ShoringRatio 格式错误 | ShoringRatio 非空时 |
| R-SR-REQ-001 | **Error** | ShoringRatio 为空（非 Total 行）| 非 Total 行 |

**关键规则说明**：
- Total 行：Function = "Total"，Team 允许为空（"Total + 空" 是合法组合）
- 数值列允许为空（空则跳过校验）；非空必须是数字且 ≥ 0
- ShoringRatio 格式：必须符合 `^\d+(\.\d+)?%$`，范围 0-100
- 空格规则（R-WS-ALL-001）最高优先级，触发后同列的其他格式规则不再重复报告

---

## 3. 正常运营流程（月度操作手顺）

### 3.1 数据准备阶段（D-3 至 D-1，由填写员完成）

| 步骤 | 操作 | 注意事项 |
|------|------|----------|
| 1 | 在 tblOffshoring.xlsx 中填写当月数据 | 所有列禁止前后空格；YearMonth 全行必填 |
| 2 | 检查 Total 行是否正确填写 | Total 行 Team 可以为空；Owner 必须填写 |
| 3 | 确认数值列（Headcount）已填写或留空 | 不允许填入负数或文字 |
| 4 | 确认 ShoringRatio 格式（如 `25%`，非 Total 行必填）| 注意：不允许 `25 %`（含空格）|
| 5 | 上传文件到 SharePoint 指定位置 | 路径：`SharePoint站点/OffshoringData/tblOffshoring.xlsx` |

### 3.2 校验执行阶段（D+1，由管理员触发或自动执行）

| 步骤 | 操作 | 工具 |
|------|------|------|
| 1 | 触发 Power Automate 流程（手动或自动）| Power Automate |
| 2 | 等待流程完成（通常 2-5 分钟）| — |
| 3 | 接收 Teams/邮件通知，查看结果摘要 | Teams / Outlook |
| 4 | 打开 Excel 报告查看 Issues 明细 | Excel |
| 5 | 打开 PDF 报告作为正式记录 | PDF 查看器 |

### 3.3 问题处理阶段（D+1 至 D+3，由填写员完成）

| 步骤 | 操作 | 工具 |
|------|------|------|
| 1 | 根据 Issues 工作表，逐条定位并修复数据 | Excel（tblOffshoring.xlsx）|
| 2 | 用「Row Key」定位对应行（RowIndex 和 YearMonth/Function/Team 等字段）| — |
| 3 | 参考「Fix Suggestion」列进行修正 | — |
| 4 | 修正完毕后重新上传，通知管理员再次触发校验 | SharePoint |
| 5 | 重复直到校验结果为「PASSED ✅」| — |

---

## 4. 报告文件说明

### 4.1 文件命名

```
ValidationReport_<YearMonth>_<YYYYMMDD-HHMMSS>.xlsx
ValidationReport_<YearMonth>_<YYYYMMDD-HHMMSS>.pdf

示例：
ValidationReport_202501_20250115-093000.xlsx
ValidationReport_202501_20250115-093000.pdf
```

### 4.2 存储位置

```
SharePoint 站点：OffshoringManagement
文档库：OffshoringReports
目录结构：
  OffshoringReports/
  └── ValidationReports/
      ├── 202501/
      │   ├── ValidationReport_202501_20250115-093000.xlsx
      │   └── ValidationReport_202501_20250115-093000.pdf
      ├── 202502/
      │   └── ...
      └── ...
```

### 4.3 文件保留策略

| 文件类型 | 保留期限 | 建议 |
|----------|---------|------|
| 所有报告（校验通过）| 12 个月 | 按年归档后可压缩存储 |
| 包含 Error 的报告 | 24 个月 | 合规审计备查 |

### 4.4 Excel 报告结构

**Summary 工作表**：
- 报告元数据（来源、期间、生成时间）
- 摘要统计（总行数、含问题行数、Error/Warning 数量）
- 校验结果（PASSED / FAILED）
- 按规则统计明细

**Issues 工作表**：
- 所有问题明细，每条 issue 一行
- 列：`#`、`Severity`、`Rule ID`、`Row Key`、`Column`、`Raw Value`、`Message`、`Fix Suggestion`
- 表格格式，支持筛选排序
- Error 行红色高亮，Warning 行黄色高亮

### 4.5 PDF 报告结构

PDF 由 HTML 模板生成，包含：
1. 报告头部（标题、来源、期间、生成时间）
2. 校验结果横幅（PASSED / FAILED）
3. 摘要卡片（6 个核心指标）
4. 按规则统计表
5. 问题明细表
6. 页脚

---

## 5. 校验规则白名单与映射表

### 5.1 Function 白名单（严格大小写匹配）

```
GBS, IT, Finance, HR, Legal, Procurement, Total
```

### 5.2 Team 白名单（严格大小写匹配）

```
EA, APAC, EMEA, Americas, Global
```

### 5.3 Function-Team 允许映射表

| Function | 允许的 Team |
|----------|------------|
| GBS | EA, APAC, EMEA, Americas, Global |
| IT | EA, APAC, EMEA, Americas, Global |
| Finance | EA, APAC, EMEA, Americas, Global |
| HR | EA, APAC, EMEA |
| Legal | EA, APAC, EMEA, Americas |
| Procurement | Global, APAC, EMEA |
| **Total** | **空（""）** 或任何白名单中的 Team |

> **特别注意**：`Function="Total"` 且 `Team=""` 是合法组合，不报错。

### 5.4 数值列清单

以下列适用 R-NUM-001（空跳过，非空须为数字且 ≥ 0）：
- Actual Headcount Onshore
- Actual Headcount Offshore
- Planned Headcount Onshore
- Planned Headcount Offshore
- Target Headcount Onshore
- Target Headcount Offshore

---

## 6. 数据填写规范（给数据填写员）

### 6.1 通用规则

| 规则 | 说明 |
|------|------|
| ❌ 禁止前后空格 | 所有单元格内容不得以空格开头或结尾 |
| ❌ 禁止纯空格 | 不能仅输入空格（系统视为空值） |
| ✅ 允许留空（数值列）| 数值列没有数据时直接留空，不要填 0 或 "-" |
| ✅ YearMonth 全行必填 | 包括 Total 行，YearMonth 必须填写 |

### 6.2 字段填写示例

| 字段 | 正确示例 | 错误示例 | 错误原因 |
|------|---------|---------|---------|
| YearMonth | `202501` | `2025-01`, `202500`, ` 202501` | 格式错误、月份非法、有前置空格 |
| Function | `GBS` | `GBS `, `gbs`, `GBS/IT` | 尾部空格、大小写错误、非白名单 |
| Team | `EA` | `ea`, `East Asia`, ` EA ` | 大小写错误、非白名单、前后空格 |
| ShoringRatio | `25%` | `25 %`, `0.25`, `25`, `25％` | 含空格、无%号、全角% |
| Headcount（非空）| `10`, `0`, `3.5` | `-1`, `N/A`, `TBD` | 负数、文字 |
| Total 行 Team | （空） | `Total`, `ALL` | Total 行 Team 只能为空或合法白名单值 |

---

## 7. 常见问题 FAQ

**Q：为什么 AI 会把 `GBS ` 报为错误，`GBS` 明明是合法的？**  
A：`GBS `（末尾有空格）与 `GBS` 是不同的字符串。规则 R-WS-ALL-001 规定所有列禁止前后空格，因此 `GBS ` 会先报空格错误，同时 R-FUNC-WL-001 也会报白名单不匹配（因为 `GBS ` 不在白名单里）。修复方法：删除末尾空格。

**Q：Total 行的 ShoringRatio 需要填写吗？**  
A：不强制。Non-Total 行必填（R-SR-REQ-001）；Total 行可留空。但如果 Total 行填写了 ShoringRatio，格式必须符合规范（会触发 R-SR-001 校验）。

**Q：数值列可以填 `0` 吗？**  
A：可以，`0` 是合法值（≥ 0）。

**Q：同一格数据报了多条错误，哪个优先处理？**  
A：优先处理 R-WS-ALL-001（空格），去掉空格后重新提交，很多其他 Error 可能会自动消失。

**Q：AI 校验结果会变化吗（每次跑结果不同）？**  
A：AI Temperature 设置为 0，尽量确保一致性。规则边界已明确写死在提示词中。若发现明显不一致，请记录并通知管理员。

**Q：Excel 行数很多（>200行），流程会超时吗？**  
A：若数据超过 200 行，建议按 YearMonth 分批调用 AI，每批 200 行内，最后合并 issues 数组。流程中已预留分批处理逻辑（见 `docs/power-automate-flow.md` 步骤 2）。

---

## 8. 初始化检查清单（首次部署）

- [ ] SharePoint 文档库已创建：`OffshoringReports/ValidationReports/`
- [ ] tblOffshoring.xlsx 已上传到 SharePoint，表格名称为 `tblOffshoring`
- [ ] HTML 报告模板已上传到 SharePoint（路径：`OffshoringReports/Templates/html-report-template.html`）
- [ ] Office Script `FillSummarySheet` 已在目标 Excel 文件中创建
- [ ] Office Script `FillIssuesSheet` 已在目标 Excel 文件中创建
- [ ] AI Builder / Copilot Studio 连接器已授权
- [ ] Power Automate 流程已导入并测试通过
- [ ] PDF 转换连接器（如 Encodian）已配置并测试
- [ ] Teams 通知频道已配置
- [ ] 数据管理员已在 SharePoint 文档库有写入权限
- [ ] 已用测试数据（含正常行、错误行）运行一次端对端测试

---

## 9. 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v1.0 | 2025-01 | 初始版本。AI-only 校验模式；报告输出 Excel + PDF；全列空格禁止（Error）；确立输出契约（JSON issues[] + report_model）|
