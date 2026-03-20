# 05 — Power BI 刷新验证 & 回滚方案

> 在对 Excel 做任何改动（添加第 4 行列名、定义命名范围等）之前，请先阅读本文档。  
> 本文档帮助你确认改动不会影响现有 Power BI 报表，并提供出问题时的回滚方法。

---

## 背景

你的 Power BI 使用 SharePoint 文件夹的读取方式（大概率）：
- Power Query 读取 SharePoint 文件夹 → 获取文件列表 → 筛选 `headcount_analysis.xlsx` → 展开 Workbook → 展开 `Offshoring` Sheet → Promote Headers（或 Skip Rows）→ 数据处理

在这种模式下，以下改动对 Power BI 的影响**极低**，但仍建议按本文档流程验证。

---

## 第一步：改动前备份（必须做）

### 操作步骤

1. 在 SharePoint 上找到 `headcount_analysis.xlsx`
2. **右键 → 复制到**，复制到同一个文档库下的 `Backup/` 子目录（如果没有就新建一个）
3. 文件命名为 `headcount_analysis_backup_YYYYMMDD.xlsx`（日期是今天）
4. 确认备份文件可以正常打开

> 这个备份是你的"回滚基准"。如果任何改动导致 Power BI 出问题，直接用备份覆盖原文件即可。

---

## 第二步：识别你当前的 Power Query 连接方式

### 操作步骤

1. 打开 **Power BI Desktop**
2. 点击"变换数据（Transform Data）"→ 进入 Power Query 编辑器
3. 找到连接 `headcount_analysis.xlsx` 的那个 Query（名称可能是 `Offshoring` 或 `headcount_analysis`）
4. 点击该 Query → 在右侧"应用的步骤（Applied Steps）"里逐步查看
5. 点击菜单"高级编辑器（Advanced Editor）"，复制全部 M 代码保存到文本文件（这是你的"PQ 代码备份"）

### 常见连接模式识别

**模式 A：文件夹读取**  
M 代码里你会看到类似：
```m
Source = SharePoint.Files("https://yourcompany.sharepoint.com/sites/YourSite", [ApiVersion = 15]),
```
然后过滤文件名，展开 Workbook。

**模式 B：直接读文件**
```m
Source = Excel.Workbook(File.Contents("...path/headcount_analysis.xlsx"), null, true),
```

**模式 C：通过 SharePoint.Contents 读单文件**
```m
Source = SharePoint.Contents("https://yourcompany.sharepoint.com/sites/YourSite"),
```

识别好模式后，对照下面的影响分析表。

---

## 第三步：影响分析

### 改动 1：给第 4 行添加标准列名

| PQ 模式 | 影响 | 需要调整 |
|--------|-----|---------|
| 模式 A（文件夹）| 如果 PQ 里有 `Table.Skip` 跳过前 N 行，可能需要调整 N | 低风险 |
| 模式 B（直接读文件）| 同上 | 低风险 |
| 所有模式 | 如果 PQ 里有 `Table.PromoteHeaders`，且原来第 4 行已经是 header，则无影响 | 无影响 |

**结论**：只改第 4 行文字内容（不改列数、不增减行），对 Power BI 影响极小。

---

### 改动 2：定义命名范围（Named Range）

| PQ 模式 | 影响 | 需要调整 |
|--------|-----|---------|
| 读取 Sheet（非 Table）| **无影响**：PQ 读的是 Sheet，命名范围不改变 Sheet 内容 | 无需调整 |
| 读取 Table | **无影响**：命名范围不是 Table，不会和 Table 混淆 | 无需调整 |
| 读取 Named Range | 仅当 PQ 也读取了同名命名范围时，才会影响 | 确认 PQ 里有无 `Excel.CurrentWorkbook` 引用 |

**结论**：定义命名范围对现有 Power BI 的 PQ 连接**几乎没有影响**。

---

### 改动 3：转成 Excel Table（Ctrl+T）

> ⚠️ 这是侵入性最高的改动，建议先在 POC 副本文件上验证。

| PQ 模式 | 影响 | 需要调整 |
|--------|-----|---------|
| 读取 Sheet（UsedRange）| 低风险；但 Table 会增加筛选器，样式变化，可能影响 PQ 的"自动检测标题"逻辑 | 在 Desktop 刷新一次确认 |
| 读取 Table | 中风险；若原来引用的 Table 名称不同，PQ 会找不到数据 | 需确认 Table 名称一致 |
| 文件夹读取（展开 Sheet）| 低风险；但 Sheet 里出现 Table 后，展开 Workbook 会多一个 Table item | 在展开时选对 item |

**结论**：如果 Power BI 当前读的是 Sheet（不是 Table），Ctrl+T 后 PQ 大概率仍然可用，但必须在 Desktop 刷新验证。

---

## 第四步：验证步骤（每次改动后执行）

### 最简验证流程（5 分钟）

1. 在 Excel 里做完改动（添加列名 / 定义命名范围）
2. **保存并关闭** Excel 文件（确保 SharePoint 上的版本已更新）
3. 打开 **Power BI Desktop**
4. 点击"刷新（Refresh）"（主页 → 刷新，或 Ctrl+Alt+F5）
5. 等待刷新完成，观察：
   - 是否有报错弹窗？
   - 关键报表页面的数据是否显示正常？
   - 行数是否和改动前一致？

### 详细验证检查清单

```
□ Power BI Desktop 刷新无错误
□ 数据行数与改动前一致（在 PQ 编辑器里看 Preview 行数）
□ 关键列（Function, Team, YearMonth, Shoring Ratio）的数据值与原始 Excel 一致
□ 报表页面的关键图表/数字显示正常（对比截图）
□ 如果有自动刷新计划（Power BI Service），手动在 Service 里"立即刷新"一次确认
```

### 如何快速对比行数

在 PQ 高级编辑器里，在数据加载步骤后加一行：
```m
RowCount = Table.RowCount(YourTableName)
```
对比改动前后的行数是否一致。

---

## 第五步：回滚方案

### 情况 A：刷新报错，原因是列名变化

**症状**：PQ 里有引用列名的步骤（如 `Table.SelectColumns(Source, {"YearMonth", ...})`），列名改变后找不到列。

**回滚**：
1. 在 Excel 里把第 4 行列名改回原来的内容
2. 或者：在 PQ 里调整引用的列名与新列名一致

---

### 情况 B：刷新报错，原因是范围/结构变化

**症状**：展开 Workbook 后，原来的 Sheet item 消失了，或者多了 Table item 导致解析混乱。

**回滚**：
1. **紧急回滚**：把 SharePoint 上的 `headcount_analysis.xlsx` 替换为 `headcount_analysis_backup_YYYYMMDD.xlsx`
   - SharePoint 文件右键 → "还原此版本"（SharePoint 会保留版本历史），或者直接上传备份文件覆盖
2. **Power BI 刷新**：替换文件后，在 Desktop 重新刷新确认恢复正常

---

### 情况 C：数据数值对不上（行数少了或数值异常）

**症状**：刷新成功但数据行数减少，或某些行的值变成 null。

**诊断**：
1. 在 PQ 编辑器里逐步检查各个步骤的 Preview，找到哪一步开始数据不对
2. 常见原因：`Table.Skip` 步骤跳过了太多行（列名行计入了跳过数）

**回滚**：
1. 先用备份文件恢复 Excel 到改动前状态（参照情况 B 的步骤 1）
2. 再在 PQ 里调整 `Table.Skip` 的行数后，重新做 Excel 改动

---

### SharePoint 版本历史恢复

SharePoint 默认保留文件版本历史，不需要手动备份也能回滚：

1. 在 SharePoint 找到 `headcount_analysis.xlsx`
2. 点文件右侧的"…（更多选项）"→"版本历史记录（Version history）"
3. 找到改动前的版本（时间戳对照）
4. 点"还原（Restore）"

> **注意**：版本历史仅对 SharePoint 在线存储的文件有效。如果你是通过"同步到本地"再编辑的，可能不会有细粒度版本记录，建议改用在线编辑方式。

---

## 总结：推荐的安全操作顺序

```
1. 做改动前：
   ✅ 备份 Excel 文件（或确认 SharePoint 版本历史可用）
   ✅ 备份 Power Query M 代码（从高级编辑器复制）
   ✅ 记录当前报表关键数字（截图）

2. 做改动：
   → 只添加第 4 行列名（最低风险）
   → 或定义命名范围（低风险）
   → 不要在生产文件上 Ctrl+T（中风险，先在副本验证）

3. 改动后验证：
   ✅ Power BI Desktop 刷新无报错
   ✅ 行数与截图一致
   ✅ 关键列数据值正常

4. 如有问题：
   → 立即用 SharePoint 版本历史还原
   → 或用备份文件覆盖
   → 确认 PBI 刷新恢复正常后再继续
```
