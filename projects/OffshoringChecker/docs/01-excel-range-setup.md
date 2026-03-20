# 01 — Excel 数据范围定义指南

> **核心原则**：不改变底层表格内容，不影响 Power BI 现有的 SharePoint 文件夹读取方式。

---

## 背景：为什么要定义"稳定数据范围"？

你的 Excel 文件 `headcount_analysis.xlsx`（Sheet: `Offshoring`）：

- 第 4 行是列名（header row）
- 第 5 行起是数据行
- 每个月会在底部追加新行（月度增量）
- Power BI 通过 SharePoint 文件夹读取，当前 PQ 大概率是"展开 Workbook → 展开 Sheet → Promote Headers / 跳过前 N 行"

Power Automate 原生 Excel 连接器（"List rows present in a table"）**只能读 Excel Table 对象**；如果不建 Table，Automate 无法直接遍历 Sheet 里的行。

解决方案：使用 **Office Scripts（脚本）** 读取指定区域，避免改动 Excel 结构。

---

## 方案一：命名范围（Named Range）— 推荐 ⭐⭐⭐⭐⭐

### 什么是命名范围？
命名范围是给一个单元格区域起一个名字（例如 `rngOffshoring`），之后可以通过名字引用它。

**优点**：
- 对 Excel 外观/格式零影响（不会改变样式、筛选器、合并单元格状态）
- Power BI / PQ 读取 Sheet 数据时完全无感
- 名称可以使用动态公式，随数据行数自动扩展

### 操作步骤（在 Excel 里做，约 2 分钟）

1. 打开 `headcount_analysis.xlsx`（在 Excel Desktop 或 Excel Online 均可）
2. 找到 `Offshoring` Sheet
3. 选中数据区域：**从第 4 行（含表头）一直选到你预计的最大行数**
   - 例如选 `A4:P2000`（第 4 行表头 + 预留 1996 行数据，共 16 列）
   - 列数：根据你实际的列数调整（16 列就是 A 到 P，详见本文末尾的标准列名表）
4. 在 Excel 左上角的"名称框"（Name Box，显示当前单元格地址的那个输入框）中：
   - 直接输入 `rngOffshoring`
   - 按 **Enter**（必须按 Enter 确认，不然不会保存）
5. 验证：按 **Ctrl + G**（转到），输入 `rngOffshoring`，点确定，看是否跳转到正确区域

> **名称规范**：只用字母、数字、下划线，不能有空格。推荐 `rngOffshoring`。

### 使用动态范围（可选，适合数据行数变化大的情况）

如果你不想手动更新范围上限，可以用一个动态命名范围（通过公式自动扩展到最后一行）：

1. 在"公式"选项卡 → "名称管理器" → 新建
2. 名称：`rngOffshoring`
3. 引用位置（Refers to）填入：

```excel
=Offshoring!$A$4:INDEX(Offshoring!$A:$P,COUNTA(Offshoring!$A:$A)+3,16)
```

> 说明：`COUNTA($A:$A)+3` 表示找到 A 列有内容的最后一行（+3 是因为前 3 行是多层表头，第 4 行才是列名），`INDEX` 用于返回动态终止单元格（第 16 列为 P 列）。你需要根据实际情况调整偏移量。

---

## 方案二：Excel Table（Ctrl+T）— 适合 POC 副本

> ⚠️ **如果 Power BI 在用同一个文件，不建议在生产文件上做此操作。建议先在 POC 副本上验证。**

### 操作步骤

1. 选中数据区域（从 A4 到 K 列最后一行数据）
2. 按 **Ctrl + T**，勾选"表包含标题（My table has headers）"
3. 点确定，Excel 会把该区域变成"表对象"
4. 在表格设计选项卡，把表名改为 `tblOffshoring`（Table Design → Table Name）

### 对 Power BI 的影响分析

| Power BI 当前连接方式 | 影响 | 处理建议 |
|---------------------|-----|---------|
| 文件夹读取 → 展开 Workbook → 展开 Sheet → Promote Headers | 低风险；但如果 PQ 依赖"跳过前 N 行"，需同步调整 PQ | 在 PBI Desktop 刷新一次确认 |
| 文件夹读取 → 展开 Workbook → 展开某个 Table | 中风险；若引用的是 Table 名称，需确认 `tblOffshoring` 与 PQ 里一致 | 先在副本文件上测试 |
| 直接读某个命名 Range | 无影响 | 无需处理 |

**最安全做法**：先复制文件到 SharePoint POC 目录，在 POC 文件上 Ctrl+T，然后在 Power BI Desktop 里临时切换连接到 POC 文件刷新一次，确认无误后再决定是否在生产文件上操作。

---

## 方案三：固定范围（如 `Offshoring!A4:K2000`）— 最低侵入

### 说明
不需要在 Excel 里做任何操作。直接在 Power Automate 的 Office Scripts 步骤里指定范围地址，例如 `A4:K2000`。

### 如何选择最大行数上限

| 每月数据行数 | 预计运行月数 | 建议上限 | 推荐范围 |
|------------|-----------|---------|---------|
| ~50 行/月 | 12 个月 | 约 600 行 | `A4:P1000`（预留 buffer） |
| ~100 行/月 | 24 个月 | 约 2400 行 | `A4:P3000` |
| ~200 行/月 | 36 个月 | 约 7200 行 | `A4:P8000` |

> 建议至少预留 2 倍 buffer（比如预计 600 行，就设 1500）。

### 溢出检测（Overflow Warning）

在 Power Automate Flow 中，读取数据后，检查是否接近上限：

**在 Office Scripts 脚本里加入溢出检测**（见 `docs/03-power-automate-steps.md` 的 Step 3）：

```typescript
// 溢出检测：如果数据行数超过上限的 80%，发出警告
const MAX_ROWS = 1996; // 范围 A4:P2000 中最多 1996 行数据（不含第 4 行表头）
const dataRowCount = values.filter(row => row[0] !== "" && row[0] !== null).length;
const overflowWarning = dataRowCount > MAX_ROWS * 0.8
  ? `⚠️ 警告：当前数据行数（${dataRowCount}）已超过最大范围上限的 80%（${Math.floor(MAX_ROWS * 0.8)} 行），请尽快扩大读取范围。`
  : null;
```

当 `overflowWarning` 不为 null 时，在 Flow 里触发额外通知（如发邮件给数据管理员）。

### 每月追加数据后的操作

1. **月初追加数据**：在 Excel 里直接在数据末尾追加新行（不需要任何额外操作）
2. **Flow 读取**：Office Scripts 会读取整个范围，自动忽略末尾空行
3. **溢出检测**：如果数据行数接近上限，Flow 会自动告警

---

## 三种方案对比总结

| 对比维度 | 命名范围（推荐） | Excel Table | 固定范围 |
|---------|--------------|-------------|---------|
| Excel 改动 | 极小（只加名称） | 中等（改结构） | 零（只改 Flow 配置） |
| Power BI 风险 | 极低 | 低~中 | 零 |
| 月度追加数据 | 自动覆盖（动态公式） / 手动扩大 | ✅ 自动扩展 | 在上限内自动覆盖 |
| Automate 兼容 | 通过 Office Scripts | ✅ 原生连接器 | 通过 Office Scripts |
| 推荐场景 | **生产文件，POC 验证** | POC 副本 | 最简化测试 |

---

## 你的下一步

1. 在 Excel 里将 **第 4 行固定为列名**（单行，无合并单元格）
2. 选择上述三种方案之一（推荐命名范围）
3. 阅读 `docs/05-powerbi-validation.md` 了解如何在操作前验证 Power BI 不受影响
4. 按 `docs/03-power-automate-steps.md` 搭建 Flow

---

## 第 4 行标准列名（照此填入 Excel 第 4 行）

| 单元格 | 列名 |
|-------|------|
| A4 | Cost Center Number |
| B4 | Function |
| C4 | Team |
| D4 | Owner |
| E4 | YearMonth |
| F4 | Actual_GBS_TeamMember |
| G4 | Actual_GBS_TeamLeaderAM |
| H4 | Actual_EA |
| I4 | Actual_HKT |
| J4 | Planned_GBS_TeamMember |
| K4 | Planned_GBS_TeamLeaderAM |
| L4 | Planned_EA |
| M4 | Planned_HKT |
| N4 | Target_YearEnd |
| O4 | Target_2030YearEnd |
| P4 | Shoring Ratio |

> 以上列名为建议标准名。如果你的实际 Excel 列顺序或列名不同，请在 `docs/03-power-automate-steps.md` 的 Office Scripts 中调整列索引映射。
