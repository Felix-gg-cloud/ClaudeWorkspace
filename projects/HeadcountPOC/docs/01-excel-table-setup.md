# 01 — 在 Excel 中创建 tblOffshoring 表

> **前置条件**：请先阅读 `03-safety-and-rollback.md`，建议在 SharePoint POC 副本文件上操作，而非直接修改生产文件。

---

## 目标

在 `headcount_analysis.xlsx` 的 `Offshoring` Sheet 上，将第 4 行作为列名，把数据区域转换为名为 **`tblOffshoring`** 的 Excel Table。  
转换后：
- Power Automate 可使用 `List rows present in a table` 稳定读取数据
- 每月在表格末尾追加新行时，Table 自动扩展，不会漏读新数据

---

## Step 1：确认第 4 行列名（必须先做）

1. 打开 `headcount_analysis.xlsx`，切换到 **`Offshoring`** Sheet
2. 检查 **第 4 行**：
   - 确保每个列名是单独一行（没有合并单元格跨多行）
   - 确保列名文字与下方标准一致（含空格和大小写）
   - 确保列名行下方紧接数据行（第 5 行开始），中间没有空行

**标准列名（第 4 行各列应包含的内容）：**

| 列 | 列名 |
|---|---|
| A | `Cost Center Number` |
| B | `Function` |
| C | `Team` |
| D | `Owner` |
| E | `YearMonth` |
| F | `Shoring Ratio` |
| G | `Headcount` |
| H | `Offshore HC` |

> 如果你的 Excel 有更多列，记录下实际列范围（比如 A 到 K），后续步骤中替换对应的列字母。

---

## Step 2：选中数据区域

1. 点击第 4 行的第一个列名单元格（例如 `A4`）
2. 按住 **Ctrl + Shift + End**，选中到最后一个有数据的单元格  
   （选中范围应包含第 4 行列名和所有数据行，例如 `A4:H500`）
3. 确认选中范围覆盖了全部有数据的列和行

> **如果数据行之间有空行**：Ctrl+Shift+End 可能会选到空行以下的区域，此时手动选中 `A4` 到实际最后一行的最后一列即可。

---

## Step 3：创建 Excel Table（Ctrl+T）

1. 在选中区域状态下，按 **`Ctrl + T`**  
   （或通过菜单：**插入 → 表格**）
2. 弹出"创建表"对话框：
   - 确认数据区域（例如 `$A$4:$H$500`）正确
   - **勾选"表包含标题"**（My table has headers）
3. 点击 **确定**

此时 Excel 会把选中区域转换为带样式和筛选器的 Table，第 4 行自动成为表头。

---

## Step 4：命名 Table 为 tblOffshoring

1. 点击 Table 内任意单元格
2. 顶部菜单出现 **"表设计（Table Design）"** 选项卡，点击它
3. 在左上角的 **"表名称（Table Name）"** 输入框中，把默认的 `Table1`（或其他名称）改为：

   ```
   tblOffshoring
   ```

4. 按 **Enter** 确认

---

## Step 5：验证 Table 是否正确

1. 点击 Table 内任意单元格，顶部出现"表设计"选项卡，且左上角显示 `tblOffshoring` ✅
2. 第 4 行（列名行）显示筛选下拉箭头 ✅
3. 每列列名与预期一致（无多余空格） ✅
4. 第 5 行起是数据行 ✅

---

## Step 6：验证月度追加数据的正确位置

每月追加新月份数据时，**必须**在 Table 最后一行的紧邻下一行直接录入：

- ✅ 正确：在 Table 最后一行（比如第 200 行）下方的第 201 行直接输入数据，Table 自动扩展包含该行
- ❌ 错误：在 Table 下方隔了空行再录入（例如第 202 行），该行不会自动纳入 Table

**验证方法**：在 Table 最后一行下方紧邻行输入任意内容，确认该行的格式自动变成表格样式（有条纹或与表格颜色一致），说明已自动纳入 Table。

---

## Step 7：保存文件

1. 按 **Ctrl + S** 保存
2. 如果文件保存在 SharePoint/OneDrive，确保文件已同步（不显示"正在上传"的转圈图标）

---

## 常见问题

**Q：加了 Table 之后，Excel 里的格式（颜色/字体）发生变化了怎么办？**  
A：Table 默认套用蓝色条纹样式，可以在"表设计"选项卡里换成其他样式，或选"无"样式。这不影响数据内容。

**Q：加了 Table 之后，Power BI 刷新报错怎么办？**  
A：参考 `04-powerbi-compatibility.md` 中的诊断步骤，以及 `03-safety-and-rollback.md` 中的回滚方案。

**Q：我的 Excel 第 1~3 行有标题或说明文字，第 4 行才是列名，这样可以吗？**  
A：可以。Excel Table 只管 Table 区域内部（从第 4 行开始），第 1~3 行的内容不受 Table 影响，Power Automate 读取 Table 时也只读 Table 内部的数据行（第 5 行起）。

**Q：原来数据区域有一些空行，转成 Table 后这些空行还在，怎么处理？**  
A：建议先删除空行再转 Table，这样数据最干净。Power Automate 流程里会过滤空行，但减少源数据的空行能提升读取效率。
