# POC-Validator — Excel 输入文件准备指南（标题行在第 5 行）

> **适用场景**：输入 Excel 文件中，第 1–4 行为前言/说明行（preamble），第 5 行为列标题行，数据从第 6 行开始。  
> Power Automate 的 **Excel Online (Business) – List rows present in a table** 动作读取的是 Excel **表格对象（Table）**，而不是工作表的原始行号。因此，只要把数据区域定义为一个命名表格，并让第 5 行成为该表格的标题行，连接器就能正确读取，与工作表中实际的行号无关。

---

## 第一节：核心概念——Excel Table vs 原始行号

| 概念 | 说明 |
|---|---|
| **工作表原始行号** | Excel 左侧显示的 1, 2, 3, 4, 5, 6… 行号 |
| **Excel Table（表格）** | 通过「插入 → 表格」创建的命名区域，有独立的列标题和数据行 |
| **Power Automate 读取方式** | 只认识 Table 名称（如 `tblOffshoring`），完全忽略原始行号 |

**关键结论**：  
如果你的标题行在第 5 行，只需把"第 5 行开始的区域"定义为一个 Excel Table，Power Automate 就会以第 5 行的内容作为列标题，从第 6 行开始逐行读取数据，第 1–4 行的前言行不会被读取。

---

## 第二节：分步操作——将第 5 行标题转换为 Excel Table

### 前提条件
- 已在 SharePoint 上传好输入文件，路径：`Documents/POC-Validator/Inputs/<你的输入文件>.xlsx`
- 文件可以用 Excel Online 或桌面版 Excel 打开

### 步骤 1：打开文件并找到数据区域

1. 在 **SharePoint → Documents → POC-Validator → Inputs** 中找到输入文件。
2. 点击文件名 → 选择「**在 Excel Online 中打开**」（或「在桌面应用中打开」）。
3. 确认：
   - 第 1–4 行：前言行（例如文件描述、生成日期等）
   - 第 5 行：列标题行（例如 `Cost Center Number`、`Function`、`Team`、`Owner` 等）
   - 第 6 行起：实际数据

### 步骤 2：选中"标题行 + 数据行"的完整区域

1. 点击 **第 5 行、第 A 列的单元格**（即 `A5`，第一个标题）。
2. 按住 **Shift** 键，点击数据区域的**最后一行、最后一列的单元格**（例如 `J1500`，具体列数和行数根据你的文件而定）。  
   > 快捷方式：先点击 `A5`，然后按 `Ctrl + Shift + End` 自动选到最后有数据的单元格。
3. 确认蓝色选框包含：第 5 行（标题）到最后一行数据，第 A 列到最后一列。
4. **不要选入第 1–4 行**，这 4 行必须在选区之外。

### 步骤 3：插入 Excel Table

#### 使用 Excel Online：
1. 顶部菜单栏点击 **「插入」**。
2. 点击 **「表格」**（英文界面为 **Insert → Table**）。
3. 弹出对话框中：
   - **「表数据的来源」**：显示你刚才选的区域（例如 `=$A$5:$J$1500`），确认正确。
   - **勾选「表包含标题」**（英文：My table has headers）。
4. 点击 **「确定」**。
5. 此时工作表中会出现带颜色的表格样式，第 5 行变为蓝色标题行。

#### 使用桌面版 Excel：
1. 顶部功能区点击 **「插入」选项卡**。
2. 点击 **「表格」**（英文：Insert → Table，或快捷键 `Ctrl + T`）。
3. 同上确认区域和标题选项，点击「确定」。

### 步骤 4：重命名 Table 为 tblOffshoring

Excel 默认会把新建的表格命名为 `表格1`（或英文 `Table1`）。必须改成 `tblOffshoring` 才能被 Power Automate 正确识别。

#### 使用 Excel Online：
1. 点击表格内任意单元格（让表格处于激活状态）。
2. 顶部出现 **「表格」选项卡**（英文：Table Design）。
3. 在左上角的 **「表格名称」** 输入框中，把 `表格1` 改成 `tblOffshoring`。
4. 按 **Enter** 确认。

#### 使用桌面版 Excel：
1. 点击表格内任意单元格。
2. 顶部出现 **「表格设计」选项卡**。
3. 找到「属性」组中的「表格名称」输入框，改成 `tblOffshoring`。
4. 按 **Enter** 确认。

### 步骤 5：保存文件

- **Excel Online**：自动保存，不需要手动操作，看右上角出现「已保存」即可。
- **桌面版 Excel**：按 `Ctrl + S` 保存。如文件在 SharePoint 上，保存后会自动同步。

### 步骤 6：验证（可选但建议）

1. 点击表格内任意单元格。
2. 在 Excel 左上角的「名称框」（显示当前单元格地址的地方）旁边，下拉列表中应能看到 `tblOffshoring`。
3. 或在「表格设计」选项卡中确认表格名称显示为 `tblOffshoring`。

---

## 第三节：前言行（第 1–4 行）注意事项

- 前言行必须**完全在表格区域之外**。只要选区从第 5 行开始，这些行就不会被纳入表格。
- 前言行可以保留任意内容（说明文字、生成时间等），不影响 Power Automate 读取。
- **不能把前言行合并进表格范围**，否则 Power Automate 会把前言行的某一行当作列标题。
- 如果日后需要调整表格范围（例如增加列），右键点击表格 → 「表格」→「调整表格大小」，从 `A5` 开始重新指定范围即可。

---

## 第四节：故障排查

### 问题 1：Power Automate 提示"找不到表格"

**原因**：文件中没有名为 `tblOffshoring` 的 Excel Table，或文件路径不对。

**排查步骤**：
1. 用 Excel Online 打开文件，检查「表格设计」选项卡中的表格名称，确认是 `tblOffshoring`（区分大小写）。
2. 在 Power Automate 的 **List rows present in a table** 动作中，点击 Table 字段的下拉箭头，确认 `tblOffshoring` 出现在列表中。
3. 如果下拉列表为空或没有 `tblOffshoring`，说明文件中还没有创建 Excel Table，请重新按照第二节的步骤操作。
4. 确认 Power Automate 中 File 字段指向的文件路径与实际文件一致（例如 `POC-Validator/Inputs/offshoring_202603.xlsx`）。

### 问题 2：Power Automate 读取的列名不对（列名是 Column1/Column2 等）

**原因**：创建表格时没有勾选「表包含标题」，导致 Excel 把标题行当成了数据行，并自动生成 Column1、Column2 等列名。

**解决方案**：
1. 选中整个表格。
2. 右键 → 「表格」→「转换为区域」，把表格删除。
3. 重新按照步骤 3，这次**确保勾选「表包含标题」**。
4. 重新命名为 `tblOffshoring`。

### 问题 3：Power Automate 读取到了前言行（第 1–4 行）的内容

**原因**：创建表格时选区包含了第 1–4 行。

**解决方案**：
1. 选中表格，右键 → 「表格」→「转换为区域」。
2. 重新选区，确保从 `A5`（第 5 行第 A 列）开始。
3. 重新插入表格并命名。

### 问题 4：新增数据行后 Power Automate 读不到新行

**原因**：新数据行添加在了表格范围之外（Excel Table 有固定范围）。

**解决方案**：
- 直接在表格最后一行的**下一行**输入数据，Excel 会自动扩展表格范围，包含新行。
- 或右键表格 → 「表格」→「调整表格大小」手动扩大范围。

---

## 第五节：SharePoint 文件夹结构参考

```
Documents/
└── POC-Validator/
    ├── Inputs/
    │   └── offshoring_<YearMonth>.xlsx      ← 输入文件（含 tblOffshoring）
    ├── Templates/
    │   ├── ValidationReportTemplate.xlsx    ← Excel 报告模板（含 tblIssues）
    │   ├── ValidationReportTemplate.docx    ← Word 报告模板
    ├── Outputs/
    │   ├── Excel/
    │   │   └── ValidationReport_<YearMonth>_<yyyyMMdd-HHmm>.xlsx
    │   ├── Word/
    │   │   └── ValidationReport_<YearMonth>_<yyyyMMdd-HHmm>.docx
    │   └── PDF/
    │       └── ValidationReport_<YearMonth>_<yyyyMMdd-HHmm>.pdf
    └── JSON/
        └── (可选)
```

---

*最后更新：2026-03*
