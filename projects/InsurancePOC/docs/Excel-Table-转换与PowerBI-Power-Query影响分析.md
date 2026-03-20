# Excel Table 转换（Ctrl+T）与 Power BI Power Query 影响分析

> 适用场景：SharePoint 上的 Excel 文件同时作为 Power BI 数据源，现需在 POC 阶段对该文件的数据区域执行 Ctrl+T（转换为 Excel 表格）

---

## 核心结论（先看这里）

| 问题 | 结论 |
|---|---|
| Ctrl+T 转换会破坏现有 PQ 连接吗？ | **看情况**，取决于 PQ 连接方式和是否引用了 Range/Sheet 名称 |
| 最安全的做法是什么？ | **不改生产文件，另存一份 POC 专用副本** |
| 如果必须在同一文件里改，有哪些注意事项？ | 见下方「安全操作步骤」 |

---

## 1. 什么是 Excel Table（结构化表格）

在 Excel 中，按 `Ctrl+T` 可以将普通数据区域（Range）转换为**结构化表格（Table）**。

转换后发生的变化：

| 变化项 | 说明 |
|---|---|
| 表格名称 | 自动生成（如 `Table1`），或在「表格设计」选项卡中重命名 |
| 结构化引用 | 可用 `表格名[列名]` 形式引用，如 `DataTable[Cost Center]` |
| 动态范围 | 表格会随新行增加自动扩展，无需手动调整范围 |
| 原有命名区域 | **不会自动删除**，但若与表格范围重叠，引用行为可能变化 |
| 列标题行为 | 列标题固定在表头行，列名即为 PQ 引用的字段名 |

---

## 2. Power Query 读取 Excel 的两种常见方式

### 方式 A：`Excel.Workbook` 连接器（直连文件）

```powerquery
let
    Source = Excel.Workbook(File.Contents("...路径..."), null, true),
    // 按 Sheet 名读取
    Sheet1_Sheet = Source{[Item="Sheet1",Kind="Sheet"]}[Data],
    // 按命名区域读取
    MyRange = Source{[Item="MyNamedRange",Kind="DefinedName"]}[Data],
    // 按表格名读取
    MyTable = Source{[Item="DataTable",Kind="Table"]}[Data]
in
    MyTable
```

### 方式 B：`SharePoint.Files` 连接器（遍历 SharePoint 文档库）

```powerquery
let
    Source = SharePoint.Files("https://yourorg.sharepoint.com/sites/YourSite", [ApiVersion = 15]),
    // 过滤到目标文件
    TargetFile = Source{[Name="data.xlsx"]}[Content],
    WorkbookContent = Excel.Workbook(TargetFile, null, true),
    // 后续同 Excel.Workbook 方式
    Sheet1 = WorkbookContent{[Item="Sheet1",Kind="Sheet"]}[Data]
in
    Sheet1
```

---

## 3. Ctrl+T 对不同 PQ 连接方式的影响

### 3.1 按 Sheet 名读取（`Kind="Sheet"`）

**影响：无**

- 转换为 Table 后，Sheet 名称不变（仍是 `Sheet1` 等）
- PQ 通过 Sheet 读取的是整个工作表内容，Table 只是改变了格式
- 列标题行为不变（PQ 通常读取第一行作为列名）

> ✅ **结论：按 Sheet 名读取，Ctrl+T 不会破坏连接**

---

### 3.2 按命名区域读取（`Kind="DefinedName"`）

**影响：需检查原命名区域是否仍存在**

- 转换为 Table **不会删除**已存在的命名区域（Named Range）
- 但如果你在转换后**手动删除或重命名**了原命名区域，PQ 会报错

> ⚠️ **结论：只要不手动删除/重命名原命名区域，连接不受影响**

---

### 3.3 按表格名读取（`Kind="Table"`）

**影响：转换前 PQ 不存在此引用；转换后如果 PQ 引用了旧表格名，需更新**

- 如果 PQ 原本引用一个旧的 Table 名（比如 `OldTable`），而你删除了它，PQ 会报错
- 如果是全新的 Table（原来没有），PQ 不受影响（它不会自动知道新表格的存在）

> ⚠️ **结论：如果 PQ 有基于 Table 名的引用，转换/重命名表格后需同步更新 PQ**

---

### 3.4 列标题变化

**影响：列名改变会导致 PQ 下游步骤报错**

- 无论是 Table 还是普通 Range，列标题（第一行）是 PQ 识别字段的依据
- 如果 Ctrl+T 后你修改了列名 → PQ 中 `Table.SelectColumns`、`Table.RenameColumns` 等步骤会报"Column not found"错误

> ❌ **结论：列标题必须保持与 PQ 中引用的字段名完全一致（包括大小写、空格）**

---

## 4. 安全操作方案

### 方案一（✅ 强烈推荐）：POC 使用独立副本文件

**不要改动 Power BI 正在读取的生产 Excel 文件。**

操作步骤：
1. 将生产 Excel 复制到 POC 专用 SharePoint 文档库（如 `/POC-Data/`）
2. 对 POC 副本执行 Ctrl+T 转换，自由探索
3. POC 完成后，若需上线，重新评估是否修改生产文件

优点：零风险，不影响现有报表刷新

---

### 方案二（⚠️ 谨慎操作）：在同一文件里转换，但保护 PQ 连接点

如果必须在同一文件操作，请严格遵循以下步骤：

#### 转换前检查
- [ ] 打开 Power BI Desktop，查看当前 PQ 查询是通过 **Sheet 名 / 命名区域 / Table 名** 读取
- [ ] 记录当前所有**列标题名称**（截图或文档化）
- [ ] 记录所有**命名区域**名称（公式 → 名称管理器）

#### 执行转换（Ctrl+T）
- [ ] 选中数据区域，按 `Ctrl+T`
- [ ] 在弹出对话框中确认「我的表格包含标题」已勾选
- [ ] 在「表格设计」选项卡中，将表格名改为有意义的名称（如 `SourceData`）
- [ ] **不要修改列标题**
- [ ] **不要删除原有命名区域**（如有）

#### 转换后验证
- [ ] 在 Excel 中确认列标题与转换前完全一致
- [ ] 在 Power BI Desktop 中点击「刷新」→ 确认数据加载无错误
- [ ] 检查 PQ 编辑器中各步骤无警告（黄色三角）

#### 如何在 PQ 中显式引用新表格

如果将来希望 PQ 通过 Table 名读取（更稳健），可在 PQ 中更新引用：

```powerquery
// 修改前（按 Sheet 读取）
Sheet1_Sheet = Source{[Item="Sheet1",Kind="Sheet"]}[Data]

// 修改后（按 Table 名读取，更稳健）
DataTable = Source{[Item="SourceData",Kind="Table"]}[Data]
```

> **注意**：按 Table 名读取时，PQ 直接获取已处理好的表格（含正确列名），不需要额外的「提升标题」步骤。

---

## 5. 替代方案（不转换为 Table 也能实现 POC 目标）

### 替代方案 A：保持命名区域（Named Range）

- 不执行 Ctrl+T，而是为数据区域设置命名区域（公式 → 定义名称）
- PQ 通过 `Kind="DefinedName"` 读取，每月新数据添加后手动扩展区域
- 适合：数据量固定、不需要自动扩展的场景

### 替代方案 B：创建仅用于 POC 验证的独立 Sheet

- 生产 Sheet 不动，新建一个 `POC-Validation` Sheet
- 在新 Sheet 中建 Table，引用原始数据（`=Sheet1!A1:Z100` 或公式引用）
- Power BI 继续读取原 Sheet；POC 的 Copilot Studio 读取新 Sheet 中的 Table
- 适合：想在同一文件里同时支持两种读取场景

### 替代方案 C：Office Scripts / Power Automate 读取 Used Range

- 通过 Office Scripts 脚本读取工作表的 `usedRange`，不依赖 Table 或命名区域
- 以 JSON 格式输出，由 Power Automate 传递给 Copilot Studio
- 适合：Excel 格式随月份变化较大、难以固化结构的场景

```typescript
// Office Scripts 示例：读取 Sheet1 的已用区域并输出 JSON
function main(workbook: ExcelScript.Workbook): object[][] {
    const sheet = workbook.getWorksheet("Sheet1");
    const usedRange = sheet.getUsedRange();
    return usedRange.getValues();
}
```

---

## 6. POC 最小风险测试清单

在对生产文件进行任何改动前，请按此清单操作：

### 准备阶段
- [ ] 确认当前 Power BI 报表刷新正常（记录最后一次成功刷新时间）
- [ ] 备份 Excel 文件（下载到本地 or 在 SharePoint 中建版本历史快照）
- [ ] 截图记录 PQ 查询的关键步骤（尤其是 Source 和数据获取步骤）
- [ ] 截图记录所有列标题

### 变更阶段
- [ ] 操作之前在 SharePoint 中「查看版本历史」确认有备份版本
- [ ] 执行 Ctrl+T，**只做这一个改动**，不做其他修改
- [ ] 保存文件（Ctrl+S）

### 验证阶段
- [ ] 在 Power BI Desktop 中点击「刷新所有」
- [ ] 无错误 → ✅ 验证通过
- [ ] 有错误 → 查看错误信息，对照第 3 节定位原因
- [ ] 如需回滚：在 SharePoint 文件的「版本历史」中恢复上一版本

### POC 验证项（针对 Copilot Studio 读取 Table）
- [ ] Power Automate 能成功读取 SharePoint 文件中的 Excel Table 行数据
- [ ] 列名与 Prompt 中引用的字段名一致
- [ ] 数据行数与 Excel 中实际行数匹配（验证未截断）
- [ ] Copilot Studio 返回的分析结果引用了正确的字段

---

## 7. 常见问题 FAQ

**Q: 转换为 Table 后，Power BI 数据集会自动感知新行吗？**

A: 是的，如果 PQ 通过 Table 名读取，每次刷新会自动包含表格中所有新增行。如果通过 Sheet 名读取，也可以，但需要确保 PQ 中没有硬编码行数范围。

---

**Q: 我的 PQ 里有 `Table.SelectRows` 和其他转换步骤，Ctrl+T 会影响吗？**

A: 不会，只要列名不变、Sheet/Table 名不变，PQ 内部的所有转换步骤都不受影响。

---

**Q: 现在不确定 PQ 用的哪种方式读取，怎么快速查看？**

A: 在 Power BI Desktop 中：开始 → 转换数据（Power Query 编辑器） → 找到对应查询 → 查看「应用的步骤」中第一步（Source）的公式栏，`Kind="Sheet"` / `Kind="DefinedName"` / `Kind="Table"` 一目了然。

---

**Q: 如果我已经破坏了 PQ 连接怎么办？**

A: 
1. 先在 SharePoint 中恢复 Excel 文件到上一版本（版本历史）
2. 在 Power BI 中刷新，确认恢复正常
3. 再按本文「安全操作步骤」重新操作

---

## 8. 结构化参考：各场景影响速查表

| PQ 连接方式 | Ctrl+T 影响 | 列名改变影响 | 命名区域删除影响 |
|---|---|---|---|
| `Kind="Sheet"` 读取 | ✅ 无影响 | ❌ 会报错 | 无关 |
| `Kind="DefinedName"` 读取 | ✅ 无影响（区域仍在） | ❌ 会报错 | ❌ 会报错 |
| `Kind="Table"` 读取（旧表名） | ⚠️ 重命名后需更新 PQ | ❌ 会报错 | 无关 |
| `Kind="Table"` 读取（新 Table） | ✅ 新表，PQ 需显式引用 | ❌ 会报错 | 无关 |

---

*文档维护：InsurancePOC 项目组 | 最后更新：2025 年*
