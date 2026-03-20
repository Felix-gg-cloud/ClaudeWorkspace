# 04 — Power BI 兼容性说明

> **背景**：你的 Power BI 使用"文件夹读取 → 展开 Workbook → 展开 Sheet"的方式读取 SharePoint 上的 Excel 数据。本文说明在该模式下，`Ctrl+T` 创建 Excel Table 对现有 Power Query 的影响，以及如何安全操作。

---

## 一、你的 Power BI 连接方式分析

**文件夹/SharePoint 文件列表模式**的典型 M 代码结构如下：

```m
let
    Source = SharePoint.Files("https://your-tenant.sharepoint.com/sites/YourSite", [ApiVersion = 15]),
    FilteredFiles = Table.SelectRows(Source, each [Name] = "headcount_analysis.xlsx"),
    BinaryContent = FilteredFiles{0}[Content],
    Workbook = Excel.Workbook(BinaryContent, null, true),
    Sheet = Workbook{[Item="Offshoring", Kind="Sheet"]}[Data],
    PromotedHeaders = Table.PromoteHeaders(Sheet, [PromoteAllScalars=true]),
    ...
```

在此连接方式下，Power Query 读取的是 **整个 Sheet（Kind="Sheet"）的数据**，而不是 Table 对象。

---

## 二、Ctrl+T 对该连接方式的影响分析

### 2.1 不会影响的方面

- ✅ Sheet 名称（`Offshoring`）不变 → PQ 仍能找到这个 Sheet
- ✅ 单元格数据内容不变 → 数字、文字的值完全一致
- ✅ 列的数量和顺序不变（只要你没有插入或删除列）
- ✅ 文件名不变 → 文件夹查询仍能匹配到正确文件

### 2.2 可能影响的方面

| 影响点 | 说明 | 风险级别 |
|---|---|---|
| 筛选器下拉箭头 | Table 会在列名行加筛选箭头，但这是 UI 层面，不影响数据值 | 低 |
| 表格样式/颜色 | Table 套用条纹样式，若 PQ 对格式有依赖（极少见）才有影响 | 极低 |
| `PromoteHeaders` 步骤 | 如果 PQ 里有"提升标题（Promote Headers）"或"跳过行（Skip rows）"，需要确认行为是否一致 | 中 |
| `UsedRange` 行为 | 若 PQ 用 `UsedRange` 读取，Table 区域的 `UsedRange` 不变 | 低 |

### 2.3 你需要确认的一个关键点

如果你的 PQ 有以下类似步骤，在建 Table 后要特别验证：

```m
// 如果有类似"删除前 N 行"的步骤
Table.Skip(Sheet, 3)  // 跳过前 3 行，让第 4 行成为数据第一行

// 然后紧接着提升标题
Table.PromoteHeaders(...)
```

建 Table 之后，Sheet 层面读出来的数据结构不变（第 4 行仍然是第 4 行的内容），所以这类 "Skip N 行 + PromoteHeaders" 的逻辑通常不受影响。

**但如果你的 PQ 原来没有 Skip 步骤，直接 PromoteHeaders（默认把第 1 行提升为标题）**，说明你之前的数据第 1 行就是列名，建 Table 后如果第 4 行才是列名，PQ 就需要调整（这不是 Ctrl+T 导致的问题，而是原来的行结构就与 PQ 逻辑不一致）。

---

## 三、安全操作检查清单（建 Table 前后对照）

### 建 Table 前记录（基准）

- [ ] 记录 Power BI 报表"上次刷新时间"和关键指标值（总 Headcount 数量、Offshore 比例等）
- [ ] 在 Power BI Desktop 打开报表，记录数据行数（`Table.RowCount` 或在报表视觉效果中查看）
- [ ] 截图保存几个核心图表的当前状态

### 建 Table 后立即验证

- [ ] 打开 Power BI Desktop → 刷新 → 确认无错误
- [ ] 数据行数与基准一致
- [ ] 关键指标值与基准一致（允许 ±1% 误差，因时间差导致的正常数据变化）

---

## 四、如果 Power BI 读取的是 Table（而非 Sheet）

如果未来你想让 Power BI 更稳定地读取，可以在 PQ 里改为直接读 Table：

```m
// 原来读 Sheet：
Workbook{[Item="Offshoring", Kind="Sheet"]}[Data]

// 改为读 Table（更稳定，不受行/列插入影响）：
Workbook{[Item="tblOffshoring", Kind="DefinedName"]}[Data]
// 或
Workbook{[Item="tblOffshoring"]}[Data]
```

**改为读 Table 的好处：**
- 不需要 "Skip rows / PromoteHeaders" 步骤（Table 的首行自动就是列名）
- 即使前面几行有标题/说明文字，PQ 也只读 Table 内部的数据

**改动方法：**
1. Power BI Desktop → 转换数据 → 高级编辑器
2. 找到读取该 Sheet 的那一行，改为读取 `tblOffshoring`
3. 删除或调整后续的 `Skip rows / PromoteHeaders` 步骤
4. 刷新验证数据正确

> **注意**：这一步建议在 POC 验证通过、确认 Table 稳定后再做，属于可选的进一步优化。

---

## 五、Power BI 文件夹读取模式的特殊考虑

你描述的"文件夹读取"（`SharePoint.Files` 获取文件列表，然后展开 Workbook）通常有以下行为：

1. **文件匹配**：通过文件名（或文件夹路径）筛选文件，建 Table 不改变文件名，不影响
2. **Workbook 展开**：通过 `Excel.Workbook` 解析文件内容，返回 `Kind="Sheet"` 和 `Kind="DefinedName"`（Table 对象会以 DefinedName 形式出现）
3. **Sheet 内容**：读 Sheet 的数据，Table 不改变 Sheet 的行列内容

因此，**在文件夹读取 + 读 Sheet 的模式下，建 Table 的风险极低**，主要的风险点是"如果 PQ 里的行跳过/标题提升逻辑依赖于原来的多层表头结构"。

---

## 六、总结

| 问题 | 结论 |
|---|---|
| 建 Table 会不会改变数据内容？ | 不会 |
| 建 Table 会不会改变 Sheet 名称？ | 不会 |
| 建 Table 会不会影响"文件夹读取 + Sheet 展开"的 PQ？ | 极低风险，验证后即可确认 |
| 建 Table 后每月追加数据会不会漏读（Power BI 侧）？ | 不会，读 Sheet 的 PQ 仍然能读到追加的行 |
| 建 Table 后每月追加数据会不会漏读（Power Automate 侧）？ | 不会，只要追加在 Table 末尾，`List rows present in a table` 会自动读到 |
