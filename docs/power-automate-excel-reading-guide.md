# Power Automate 读取 Excel 数据指南

> 适用场景：通过 Power Automate 读取存储在 SharePoint 上的 Excel 文件，用于 Copilot Studio Agent 或自动化报告。

---

## 1. Excel Online (Business) Connector 核心 Actions

Power Automate 的 **Excel Online (Business)** 连接器提供三种主要读取方式，它们的底层机制完全不同：

| Action | 中文名 | 要求 | 返回内容 |
|---|---|---|---|
| **List rows present in a table** | 列出表中的行 | Excel 文件内必须存在**已命名的 Excel 表格（Table）** | JSON 数组，每行一个对象，键为表头列名 |
| **Get a row** | 获取单行 | Excel Table + 行键值 | 单行 JSON 对象 |
| **Get range** | 获取区域 | 必须指定**精确的单元格范围**，如 `A1:G50` | 二维数组（values），无列名 |

### 1.1 "List rows present in a table"

- **核心要求**：工作表中必须有一个通过 **插入 → 表格** 创建的正式 Excel Table（有表格名称，如 `Table1`、`DataTable`）。
- **自动扩展**：Excel Table 会随着向表格末尾追加新行而**自动扩展**，Power Automate 每次都能读到所有行，无需修改 Flow。
- **表头约定**：Excel Table 的第一行即为列标题，Action 返回的 JSON 键名就是这些标题。
- **限制**：
  - 无法直接读取"普通单元格区域"（即使看起来像表格）。
  - 如果表头行不在第 1 行（例如第 4 行），需要在创建 Table 时从第 4 行开始选定区域，而非从第 1 行。

### 1.2 "Get range"

- 需要在 Flow 中**硬编码**范围字符串，例如 `Sheet1!A4:H200`。
- 如果每个月数据行数增加，范围字符串不会自动更新，**会漏读新增行**——这就是"固定范围（Fixed Range）隐患"的来源。
- 返回的是二维数组（`values`），第一行通常是标题，需要在 Flow 中手动解析，增加复杂度。

### 1.3 为什么无法直接"读取整个工作表"

Excel Online (Business) Connector **没有"读取整张 Sheet"的 Action**。原因：

- Excel 工作表可以有百万行，没有边界的读取会导致超时或内存溢出。
- 连接器需要明确的边界（Table 定义 或 精确 Range 字符串）来控制数据量。
- 要读取整张 Sheet 的有效数据，需借助 **Office Scripts**（见第 3 节）。

---

## 2. 特殊场景：第 4 行为表头 & 多级表头

### 2.1 表头在第 4 行（非第 1 行）

这种情况在财务/KPI 报表中很常见（前 3 行可能是标题、Logo 或说明文字）。

**处理方式（推荐顺序）：**

1. **创建 Excel Table 时从第 4 行开始选定**：在 Excel 中选中从 A4 开始的实际数据区域，插入表格，Power Automate 的 "List rows" Action 就能正确识别第 4 行为列标题。
2. **如果无法修改文件格式**：使用 "Get range" 指定 `Sheet1!A4:Z500`（适当预留行数），然后在 Flow 中用 `first()` 提取第一行作为表头，循环其余行构建 JSON。
3. **使用 Office Scripts**：脚本可以动态找到第 4 行作为表头，读取到最后一个有数据的行（见第 3 节）。

### 2.2 多级表头（Merged Cells / 两层表头）

Power Automate 的 Excel Connector **不支持合并单元格或多级表头**，读取合并列会产生空值或错位。

**推荐处理方式：**

- **在 Excel 层面展平表头**：拆开合并单元格，将两级表头合并为一行（如 `销售额_华南`、`销售额_华北`）。这是最稳定的方案。
- **Office Scripts 辅助**：在脚本中手动处理合并单元格逻辑，输出展平后的 JSON 给 Power Automate。
- **Power Query / Dataflows**：如果数据量大，可考虑用 Power Query 在 Power BI 端处理多级表头，而非在 Flow 中处理。

---

## 3. 读取动态月度追加数据的推荐方案

场景：每个月在同一个 Excel 文件中追加新数据行，需要 Power Automate 每次都能读到所有行（含最新追加的行）。

### 方案 A：将数据区域转为 Excel Table（推荐首选）

**操作步骤：**
1. 在 Excel 中选中数据区域（从表头行开始），点击 **插入 → 表格**，勾选"表包含标题"。
2. 给表格命名（如 `KPIData`）。
3. 每月追加数据时，直接在表格的最后一行下方粘贴新数据，Excel 会自动将其纳入表格范围。
4. Power Automate 使用 "List rows present in a table"，选择 `KPIData`，无需任何修改即可读取所有行。

**优点：**
- 零维护：新增行自动包含，Flow 无需改动。
- 返回结构化 JSON，列名清晰。
- 对 Power BI 友好：Power BI 的 Excel Connector 同样优先识别 Table，数据刷新稳定。

**缺点：**
- 需要修改 Excel 文件格式（初次设置）。
- 如果文件有合并单元格或多级表头，需要先展平。
- 表格样式（颜色/条带）会改变 Excel 的视觉外观（可自定义为"无样式"）。

**对 Power BI 的影响：**
- ✅ Power BI 的 "Excel" 数据源可以直接选取命名 Table，刷新更稳定。
- ✅ 列名固定，不受其他 Sheet 内容影响。

---

### 方案 B：使用 Office Scripts 读取 UsedRange

**适用场景：** 文件格式不能修改、有多级表头、或需要更灵活的逻辑。

**Office Script 示例：**

```typescript
function main(workbook: ExcelScript.Workbook): string {
  const sheet = workbook.getWorksheet("Sheet1");

  // 自动获取有数据的区域（UsedRange）
  const usedRange = sheet.getUsedRange();
  if (!usedRange) return JSON.stringify([]);

  const values = usedRange.getValues();

  // 第 4 行（索引 3）为表头，第 5 行起为数据
  const headerRowIndex = 3;
  const headers = values[headerRowIndex] as string[];

  const result: Record<string, unknown>[] = [];
  for (let i = headerRowIndex + 1; i < values.length; i++) {
    const row = values[i];
    // 跳过完全为空的行
    if (row.every(cell => cell === "" || cell === null)) continue;
    const obj: Record<string, unknown> = {};
    headers.forEach((h, idx) => {
      obj[h || `Col${idx + 1}`] = row[idx];
    });
    result.push(obj);
  }

  return JSON.stringify(result);
}
```

**Power Automate Flow 配置：**
1. 添加 "Run script" Action（Excel Online (Business) → Run script）。
2. 选择文件和工作表，选择上述脚本。
3. 脚本返回值为 JSON 字符串，用 `json()` 函数解析后即可在后续步骤中使用。

**优点：**
- 完全动态：自动识别 UsedRange，无论追加多少行都能读全。
- 可处理复杂表头逻辑（合并单元格、多级表头）。
- 不修改 Excel 文件结构。

**缺点：**
- 需要 Microsoft 365 E3/E5 或商业高级版许可证（Office Scripts 功能）。
- 需要编写和维护 TypeScript 脚本。
- 脚本执行有时间限制（单次最长约 5 分钟）。
- 对 Power BI 无直接帮助（Power BI 不调用 Office Scripts）。

**对 Power BI 的影响：**
- ⚠️ Power BI 无法使用 Office Scripts；若 Excel 文件没有 Table，Power BI 只能读取整个工作表或命名区域，列名解析可能不稳定。

---

### 方案 C：定义命名范围（Named Range）

**操作步骤：**
1. 在 Excel 中选中数据区域（含表头），在名称框中输入名称（如 `KPIRange`）并回车。
2. Power Automate 使用 "Get range" Action，范围填入命名范围名称 `KPIRange`。

**注意：** 命名范围是**静态定义**的，追加新行后**不会自动扩展**，需要手动更新范围定义。这与 Excel Table 的自动扩展行为不同。

**优点：**
- 不改变文件的视觉格式（无表格样式）。
- 对于固定行数的参考数据（如白名单、配置表）适合。

**缺点：**
- 每次追加数据后必须手动扩展命名范围（容易遗忘，导致漏读）。
- 不适合动态增长的月度数据。
- 返回二维数组，需在 Flow 中手动解析列名。

**对 Power BI 的影响：**
- ⚠️ 与 "Get range" 类似，Power BI 可以读取命名范围，但同样存在静态边界问题。

---

## 4. 方案对比总结

| 维度 | Excel Table | Office Scripts (UsedRange) | 命名范围 |
|---|---|---|---|
| 是否需修改 Excel | ✅ 是（初次） | ❌ 否 | ✅ 是（初次） |
| 月度追加自动扩展 | ✅ 自动 | ✅ 自动 | ❌ 需手动更新 |
| 多级/合并表头支持 | ❌ 需展平 | ✅ 可处理 | ❌ 需展平 |
| 表头在第 4 行 | ✅ 支持（从第4行建表） | ✅ 支持（脚本逻辑） | ✅ 支持 |
| 许可证要求 | 标准 M365 | E3/E5 或商业高级版 | 标准 M365 |
| 开发复杂度 | 低 | 中 | 低 |
| Power BI 兼容性 | ✅ 最佳 | ⚠️ 无直接影响 | ⚠️ 静态边界 |
| 推荐用途 | 动态月度数据（首选） | 格式复杂/不可改文件 | 固定参考数据 |

---

## 5. FAQ

### Q1: 为什么不能直接"读取整个 Sheet"？

**A:** Excel Online (Business) Connector 没有"读取整张 Sheet"的内置 Action。Excel 工作表理论上可以有 1,048,576 行，没有明确边界的读取会导致 API 超时或内存溢出。连接器要求你提供清晰边界：要么是 Excel Table（边界由 Table 定义自动管理），要么是精确的 Range 字符串（如 `A1:H200`）。
如果必须读取整个有效数据区域，请使用 **Office Scripts** 的 `getUsedRange()` 方法，它能动态识别有数据的最大区域。

---

### Q2: 如何确保每月追加的新行都被包含在读取范围内？

**A:** 取决于你选择的方案：

- **Excel Table（推荐）**：在表格最后一行的下一行粘贴新数据，Excel 自动将其纳入表格范围，Power Automate 下次执行时自动读到新行，**无需任何操作**。
- **Office Scripts**：脚本使用 `getUsedRange()`，自动以最后一个有数据的单元格为边界，**无需操作**。
- **命名范围/Get range 硬编码**：每次追加数据后必须**手动更新范围**（在 Excel 中扩展命名范围，或在 Flow 中修改范围字符串）。这是最容易出错的方式，生产环境不推荐用于动态增长的数据。

---

### Q3: 为什么 Power Automate 读出来的数据比实际少（漏行）？

**A:** 最常见的原因是使用了固定 Range 字符串（如 `A1:H100`），而实际数据已超过 100 行。检查步骤：
1. 查看 Flow 中 "Get range" Action 的范围参数，确认是否硬编码了行数上限。
2. 如果是 Excel Table，检查新追加的行是否真的在表格范围内（在 Excel 中点击数据行，看"表格设计"选项卡是否显示表格名称）。
3. 如果有空行插入了数据区域中间，Excel Table 和 Office Scripts 可能在空行处截断——保持数据连续性，删除中间空行。

---

### Q4: 我的 Excel 文件表头在第 4 行，用 "List rows present in a table" 会把前 3 行也读进来吗？

**A:** 不会，前提是 Excel Table 是从第 4 行开始创建的。创建步骤：
1. 在 Excel 中选中从 **A4** 开始的数据区域（包含第 4 行的列标题和下面的所有数据行）。
2. 点击 **插入 → 表格**，勾选"表包含标题"（此时 Excel 会把第 4 行识别为列标题）。
3. Power Automate 的 "List rows" 返回的 JSON 键名将是第 4 行的列标题内容，第 1-3 行的内容不会被包含。

---

### Q5: 转成 Excel Table 之后，Power BI 的数据刷新会受影响吗？

**A:** 通常是正面影响：
- Power BI 的 Excel 数据源支持直接选取命名 Table，刷新更稳定、列名更清晰。
- 如果之前 Power BI 是通过"工作表"读取数据（而非 Table），切换到 Table 后需要在 Power BI Desktop 中重新选择数据源（选"表"而非"工作表"）并刷新，之后不受影响。
- Table 的列名如果改变，Power BI 的列引用会报错——更改列标题前需同步更新 Power BI 报表中的列引用。
