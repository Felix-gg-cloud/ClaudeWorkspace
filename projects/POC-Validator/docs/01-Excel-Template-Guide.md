# 01 — ValidationReportTemplate.xlsx 创建手顺

本文档说明如何在个人 OneDrive 上创建 `ValidationReportTemplate.xlsx`，  
该文件是 Power Automate Flow 生成校验报告所必需的模板。

---

## 1. 目标文件位置

```
OneDrive › Documents › PowerbiTest › POC-Validator › Templates › ValidationReportTemplate.xlsx
```

> **重要**：路径中每一级文件夹必须提前在 OneDrive 上手动创建好，  
> 否则 Power Automate 无法找到该模板文件。

---

## 2. 预先创建 OneDrive 文件夹

1. 打开 [OneDrive](https://onedrive.live.com) 或 Windows 资源管理器 › OneDrive 文件夹。
2. 进入 `Documents`，依次新建以下文件夹（若不存在）：

   ```
   Documents/
   └── PowerbiTest/
       └── POC-Validator/
           ├── Templates/          ← 放模板文件
           ├── Inputs/             （可选）
           └── Outputs/
               ├── Excel/          ← Flow 输出 xlsx 报告
               └── PDF/            ← Flow 输出 pdf 报告
   ```

---

## 3. 新建 Excel 工作簿

1. 在 `Documents/PowerbiTest/POC-Validator/Templates/` 文件夹内，  
   点击 **新建 › Excel 工作簿**（OneDrive 网页端）或在桌面 Excel 新建后上传。
2. 将文件命名为：`ValidationReportTemplate.xlsx`

---

## 4. 创建 Summary 工作表

1. 将 **Sheet1** 重命名为 `Summary`（右键标签页 › 重命名）。
2. 在 `Summary` 工作表中填入以下标签（A 列为标签，B 列留空供 Flow 填写）：

   | 行 | A 列（标签） | B 列（Flow 填写） |
   |----|-------------|-----------------|
   | 1  | Report Title | |
   | 2  | Generated At | |
   | 3  | YearMonth | |
   | 4  | Total Rows | |
   | 5  | Error Count | |
   | 6  | Warning Count | |
   | 7  | Top Rule (1) | |
   | 8  | Top Rule (2) | |
   | 9  | Top Rule (3) | |
   | 10 | Top Column (1) | |
   | 11 | Top Column (2) | |
   | 12 | Top Column (3) | |

   > `Summary` 工作表不需要建 Excel 表格（Table），Flow 通过"更新行"动作直接写入单元格。

---

## 5. 创建 Issues 工作表

### 5-1. 新建工作表

1. 点击底部 **+** 按钮，新建工作表，命名为 `Issues`。
2. 确保工作表名称**精确**为 `Issues`（区分大小写）。

### 5-2. 在第 1 行填写列标题

在 `Issues` 工作表的第 1 行（A1 开始），**按顺序**填入以下列标题：

| 列  | 标题 |
|-----|------|
| A   | Severity |
| B   | RuleId |
| C   | RowKey |
| D   | RowIndex |
| E   | YearMonth |
| F   | Cost Center Number |
| G   | Function |
| H   | Team |
| I   | Owner |
| J   | Column |
| K   | RawValue |
| L   | Message |
| M   | FixSuggestion |

> **共 13 列**，顺序和拼写必须与上表完全一致。

### 5-3. 将数据区域转换为 Excel 表格（Table）

这一步是**关键**，Power Automate 需要通过表格名称 `tblIssues` 来插入数据行。

1. 点击 **A1** 单元格（表头行第一列）。
2. 按 **Ctrl + Shift + End**，选中 A1 到 M1（含所有标题）。
3. 点击菜单：**插入 › 表格**（Insert › Table）。
4. 弹窗中确认范围为 `$A$1:$M$1`，勾选 **"表包含标题"**，点击 **确定**。
5. 此时 Excel 自动创建一个名为 `Table1`（或类似名称）的表格。

### 5-4. 将表格重命名为 tblIssues

1. 点击表格内任意单元格，顶部菜单出现 **"表设计"（Table Design）** 选项卡。
2. 在左侧 **"表名称"** 输入框中，将默认名称改为：

   ```
   tblIssues
   ```

3. 按 **Enter** 确认。

> ⚠️ 表格名称必须精确为 `tblIssues`，Power Automate 中的动作配置依赖此名称。

---

## 6. 可选格式优化建议

以下为可选但推荐的格式设置，提升报告可读性：

### 6-1. 启用自动筛选（已自动启用）

Excel 表格默认已启用筛选下拉按钮，无需额外操作。

### 6-2. 冻结标题行

1. 点击 **A2** 单元格（标题行下方第一行）。
2. 菜单：**视图 › 冻结窗格 › 冻结首行**（或"冻结至当前行以上"）。
3. 效果：滚动时 A1 标题行始终可见。

### 6-3. 条件格式（Severity 列色阶）

1. 点击 **A 列** 列头选中整列（或 A2 往下）。
2. 菜单：**开始 › 条件格式 › 新建规则**。
3. 规则 1（Error → 红色背景）：
   - 规则类型：**使用公式确定要设置格式的单元格**
   - 公式：`=$A2="Error"`
   - 格式 › 填充：选择 **红色**（如 `#FF0000` 或预设红色）
4. 规则 2（Warning → 琥珀色背景）：
   - 规则类型：**使用公式确定要设置格式的单元格**
   - 公式：`=$A2="Warning"`
   - 格式 › 填充：选择 **橙色/琥珀色**（如 `#FFC000`）
5. 点击 **确定** 保存。

### 6-4. 自动调整列宽

1. 选中所有列（点击左上角行列交叉处，全选工作表）。
2. 在任意列标题边框上**双击**，即可自动调整所有列宽为内容最优宽度。

---

## 7. 模板结构示意

```
ValidationReportTemplate.xlsx
│
├── 工作表: Summary
│   ┌─────────────────────┬──────────────────────┐
│   │ A（标签）            │ B（Flow 填写）        │
│   ├─────────────────────┼──────────────────────┤
│   │ Report Title        │ （Flow写入）           │
│   │ Generated At        │ （Flow写入）           │
│   │ YearMonth           │ （Flow写入）           │
│   │ Total Rows          │ （Flow写入）           │
│   │ Error Count         │ （Flow写入）           │
│   │ Warning Count       │ （Flow写入）           │
│   │ ...                 │ ...                  │
│   └─────────────────────┴──────────────────────┘
│
└── 工作表: Issues（Excel 表格名称: tblIssues）
    ┌──────────┬────────┬────────┬──────────┬───────────┬────────────────────┬──────────┬──────┬───────┬────────┬──────────┬───────────────────────────────┬───────────────────────────────┐
    │ Severity │ RuleId │ RowKey │ RowIndex │ YearMonth │ Cost Center Number │ Function │ Team │ Owner │ Column │ RawValue │ Message                       │ FixSuggestion                 │
    ├──────────┼────────┼────────┼──────────┼───────────┼────────────────────┼──────────┼──────┼───────┼────────┼──────────┼───────────────────────────────┼───────────────────────────────┤
    │ Error    │ R001   │ R-005  │ 5        │ 202501    │ CC-1234            │ Finance  │ FP&A │ Alice │ Owner │ " Alice" │ Leading space in Owner field  │ Remove leading/trailing spaces│
    └──────────┴────────┴────────┴──────────┴───────────┴────────────────────┴──────────┴──────┴───────┴────────┴──────────┴───────────────────────────────┴───────────────────────────────┘
```

> 上表中第 2 行为示例数据行，说明各列的数据格式。实际模板中**不需要**保留示例行，保持 tblIssues 为空表即可。

---

## 8. 字段说明

| 字段名 | 说明 | 示例值 |
|--------|------|--------|
| Severity | 问题严重程度 | `Error` 或 `Warning` |
| RuleId | 触发的校验规则 ID | `R001`、`SPACE_001` |
| RowKey | 数据行的业务唯一标识 | `R-005`、`CC1234-2025-01` |
| RowIndex | 数据行在源文件中的行号（从 1 开始） | `5` |
| YearMonth | 数据所属年月（YYYYMM 格式） | `202501` |
| Cost Center Number | 成本中心编号 | `CC-1234` |
| Function | 功能/部门 | `Finance` |
| Team | 团队名称 | `FP&A` |
| Owner | 数据负责人 | `Alice` |
| Column | 出现问题的列名 | `Owner` |
| RawValue | 单元格原始值（含空格等问题字符） | `" Alice"` |
| Message | 问题描述 | `Leading space in Owner field` |
| FixSuggestion | 修复建议 | `Remove leading/trailing spaces` |

---

## 9. 保存并上传

1. 如果在本地 Excel 编辑：**文件 › 保存**（Ctrl+S），文件应自动同步到 OneDrive。
2. 如果在 OneDrive 网页端编辑：更改自动保存，无需额外操作。
3. 确认文件路径：
   ```
   OneDrive › Documents › PowerbiTest › POC-Validator › Templates › ValidationReportTemplate.xlsx
   ```

---

## 10. 验证模板正确性

在 Power Automate 搭建 Flow 之前，请验证以下几点：

- [ ] 文件存在于正确路径
- [ ] 工作表名称精确为 `Summary` 和 `Issues`
- [ ] `Issues` 工作表中存在名为 `tblIssues` 的 Excel 表格
- [ ] `tblIssues` 共 13 列，列标题与本文档第 5-2 节完全一致
- [ ] 表格为空（无数据行，Flow 运行时自动插入）

> 验证方法：在 Excel 中点击表格内任意位置，查看 **"表设计"** 选项卡左侧显示的表名是否为 `tblIssues`。
