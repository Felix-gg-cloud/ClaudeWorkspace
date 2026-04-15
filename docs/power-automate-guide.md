# Power Automate 新手指南：POC-Validator Flow 完整搭建教程

本文档适合 Power Automate 经验不多的用户，每一步都写明"点哪里、选什么、填什么"。

---

## 目录

1. [准备工作：把 Excel 第 5 行变成表头](#1-准备工作把-excel-第-5-行变成表头)
2. [新建 Flow 并设置手动触发器](#2-新建-flow-并设置手动触发器)
3. [读取 Excel 表（List rows present in a table）](#3-读取-excel-表list-rows-present-in-a-table)
4. [初始化变量 RowIndex 和 RowsForAI](#4-初始化变量-rowindex-和-rowsforai)
5. [循环处理每一行（Apply to each）](#5-循环处理每一行apply-to-each)
6. [【核心】Append to array variable 的 Value 字段填什么](#6-核心append-to-array-variable-的-value-字段填什么)
7. [用 Compose 验证 RowsForAI](#7-用-compose-验证-rowsforai)
8. [常见问题排查](#8-常见问题排查)

---

## 1. 准备工作：把 Excel 第 5 行变成表头

> **背景**：Power Automate 的 Excel Online 连接器只能读取"Excel 表（Table）"，不能按行号读取。因此需要先把你的数据区域定义成 Table，并且表头自然就是你选中的起始行。

### 1.1 打开输入 Excel 文件

1. 打开 SharePoint：`Documents/POC-Validator/Inputs/`
2. 点击你的输入 Excel 文件（`.xlsx`）打开

### 1.2 确认第 5 行是列标题

确保第 5 行写的是字段名，例如：

```
YearMonth | Cost Center Number | Function | Team | Owner | ShoringRatio | ...
```

第 1–4 行可以是说明文字、标题装饰等，**不要把它们选进 Table**。

### 1.3 选中从第 5 行开始的数据区域

1. 鼠标点击 **A5**（第 5 行第 1 列）
2. 按 **Ctrl + Shift + End**（Windows）选到数据的最后一行最后一列
   - 或者手动拖选，只要包含第 5 行到末行即可
   - **关键**：不要把第 1–4 行包含进去

### 1.4 插入 Table

在 Excel 顶部菜单栏：

1. 点击 **Insert（插入）**
2. 点击 **Table（表格）**
3. 弹窗确认区域后，勾选 **"My table has headers"（我的表具有标题）**
4. 点击 **OK**

完成后，你会看到列头出现筛选的下拉箭头——这表示 Table 创建成功。

### 1.5 给 Table 命名为 `tblOffshoring`

1. 点击 Table 内任意单元格
2. 顶部出现 **Table Design（表格设计）** 选项卡
3. 在左侧 **Table Name（表名称）** 输入框中，把默认名称改为 `tblOffshoring`
4. 按 **Enter** 确认

### 1.6 保存文件

点击 **Save**（或等待自动保存）。

> ✅ 完成后，Power Automate 的"List rows present in a table"就会从 `tblOffshoring` 的第一行（你的第 5 行）开始读取，1–4 行不会被读取。

---

## 2. 新建 Flow 并设置手动触发器

### 2.1 新建 Flow

1. 打开 [Power Automate](https://make.powerautomate.com/)
2. 左侧菜单点击 **Create（创建）**
3. 选择 **Instant cloud flow（即时云端流）**
4. **Flow name** 填写：`POC-Validator-Manual`
5. 触发器选择：**Manually trigger a flow（手动触发流）**
6. 点击 **Create（创建）**

### 2.2 给触发器添加输入参数

在最顶部的 **Manually trigger a flow** 卡片里：

**添加第 1 个输入：**

1. 点击 **+ Add an input（添加输入）**
2. 选择 **Text（文本）**
3. 将默认名称改为 `BatchYearMonth`
4. 是否必填（Required）：开启（你每次测试时手填）

**添加第 2 个输入：**

1. 再次点击 **+ Add an input**
2. 选择 **Number（数字）**
3. 名称改为 `TopNIssuesInPDF`
4. 如果界面上有 **Default value（默认值）**，填入 `200`

点击右上角 **Save（保存）**。

---

## 3. 读取 Excel 表（List rows present in a table）

在触发器卡片下面点击 **+ New step（新建步骤）**：

1. 搜索框输入：`List rows present in a table`
2. 选择：**Excel Online (Business) – List rows present in a table**

在这个动作卡片里依次填写：

| 字段 | 填写内容 |
|------|---------|
| **Location** | 选择你的 SharePoint 站点（下拉里找到站点名） |
| **Document Library** | 选择 `Documents` |
| **File** | 点文件夹图标，浏览到 `POC-Validator/Inputs/<你的Excel文件名>.xlsx` |
| **Table** | 下拉选择 `tblOffshoring` |

> ⚠️ 如果 Table 下拉里找不到 `tblOffshoring`：说明 Excel 文件没有正确创建 Table，或表名没保存成功。回到第 1 节检查。

### 3.1 开启 Pagination（分页读取大数据量）

在这个动作卡片右上角点 **…（三个点）** → **Settings（设置）**：

- **Pagination（分页）**：打开开关
- **Threshold（阈值）**：填写纯数字，例如 `5000`（必须大于 0，不能是空）

点击 **Done（完成）**。

---

## 4. 初始化变量 RowIndex 和 RowsForAI

### 4.1 初始化 RowIndex（行计数器）

在 "List rows present in a table" 下面点 **+ New step**：

1. 搜索：`Initialize variable`
2. 选择：**Initialize variable（初始化变量）**

填写：

| 字段 | 值 |
|------|---|
| **Name** | `RowIndex` |
| **Type** | `Integer`（整数） |
| **Value** | `0` |

### 4.2 初始化 RowsForAI（存放处理结果的数组）

再点 **+ New step**，再次选择 **Initialize variable**：

| 字段 | 值 |
|------|---|
| **Name** | `RowsForAI` |
| **Type** | `Array`（数组） |
| **Value** | 留空，或填 `[]` |

---

## 5. 循环处理每一行（Apply to each）

再点 **+ New step**：

1. 搜索：`Apply to each`
2. 选择：**Apply to each（应用于每一个）**

在"Select an output from previous steps"（选择上一步的输出）里：

- 选择来自 **List rows present in a table** 的 **value**（这是行的数组）

> ⚠️ 注意：选的是 `value`（代表所有行的数组），不是某一列的值。

### 5.1 在循环里添加 RowIndex 自增

进入 Apply to each 后，点击 **Add an action（添加操作）**：

1. 搜索：`Increment variable`
2. 选择：**Increment variable（递增变量）**

填写：

| 字段 | 值 |
|------|---|
| **Name** | `RowIndex` |
| **Value** | `1` |

### 5.2 在循环里追加当前行到 RowsForAI

仍在循环里，再点 **Add an action**：

1. 搜索：`Append to array variable`
2. 选择：**Append to array variable（追加到数组变量）**

填写：

| 字段 | 值 |
|------|---|
| **Name** | `RowsForAI` |
| **Value** | 见下一节的详细说明 ↓ |

---

## 6. 【核心】Append to array variable 的 Value 字段填什么

> 这是最容易出错、也是最多人卡住的地方，本节给出完整说明。

### 6.1 核心原则：必须是 Object（对象），不能是 Array（数组）

**`AppendToArrayVariable` 的工作方式：** 每次调用，把 Value 里的**一个元素**追加到数组末尾。

- ✅ 允许的类型：**Object**（`{...}`）、String、Number、Boolean
- ❌ 不允许的类型：**Array**（`[...]`）

**错误示例（导致报错）：**

```json
[
  {
    "RowIndex": 1,
    "YearMonth": "202603"
  }
]
```

**错误信息：** `The input value is of type 'Array' which cannot be appended to the variable 'RowsForAI' of type 'Array'. The action type 'AppendToArrayVariable' only supports values of types 'Float, Integer, String, Boolean, Object'.`

**正确示例（一个 Object）：**

```json
{
  "RowIndex": 1,
  "YearMonth": "202603",
  "Function": "IT"
}
```

### 6.2 最小对象模板（先跑通，再扩展）

建议第一次先用最少的字段验证 Flow 能跑通，再逐步加列：

```json
{
  "RowIndex": <变量 RowIndex 的值>,
  "YearMonth": <当前行 YearMonth 列的值>,
  "Function": <当前行 Function 列的值>
}
```

### 6.3 完整 tblOffshoring 对象模板

当最小版验证通过后，把其余列也加进来：

```json
{
  "RowIndex":          <变量 RowIndex>,
  "YearMonth":         <当前行 YearMonth>,
  "CostCenterNumber":  <当前行 Cost Center Number>,
  "Function":          <当前行 Function>,
  "Team":              <当前行 Team>,
  "Owner":             <当前行 Owner>,
  "ShoringRatio":      <当前行 ShoringRatio>,
  "HeadCount":         <当前行 HeadCount>,
  "TargetRatio":       <当前行 TargetRatio>,
  "Region":            <当前行 Region>
}
```

> ℹ️ 实际列名以你的 Excel `tblOffshoring` 第 5 行的列头为准，保持完全一致（包括大小写和空格）。

### 6.4 两种 UI 填法

不同租户和 Power Automate 界面版本，Value 输入框的样式可能不同。以下是两种常见情况：

---

#### UI 方式 A：Value 字段支持直接输入表达式（Expression 模式）

适用场景：Value 输入框旁边有 **"Expression"** 或 **"fx"** 按钮，或者有"切换到表达式模式"选项。

**步骤：**

1. 在 `Append to array variable` 的 Value 输入框里，点击旁边的 **"Expression"** 按钮（或闪电图标 `fx`）
2. 在表达式输入框里粘贴以下内容（把 `RowIndex` 替换成实际变量名）：

```
json(concat('{"RowIndex":', variables('RowIndex'), ',"YearMonth":"', items('Apply_to_each')?['YearMonth'], '","CostCenterNumber":"', items('Apply_to_each')?['Cost Center Number'], '","Function":"', items('Apply_to_each')?['Function'], '","Team":"', items('Apply_to_each')?['Team'], '","Owner":"', items('Apply_to_each')?['Owner'], '"}'))
```

3. 点击 **OK**

> ⚠️ 注意：列名里如果有空格（如 `Cost Center Number`），在 `items('Apply_to_each')?['Cost Center Number']` 里必须保留原始空格。

---

#### UI 方式 B：Value 字段只显示一行输入框（需要切换到自定义值 / 动态内容模式）

适用场景：Value 字段只是一个普通文本框，没有明显的 "Expression" 按钮，或者界面上有 **"Enter custom value"（输入自定义值）** 选项。

**步骤（推荐新手用这种方式，逐字段点选）：**

1. 在 Value 输入框里，**先输入一个左花括号 `{`**，这样 Power Automate 会识别你要输入 JSON
2. 按 **Alt + Enter** 或在输入框里换行，开始逐行写字段

   但更常见的做法是：点击 Value 输入框后点 **"Add dynamic content"（添加动态内容）**，然后用动态内容面板里的变量和字段来构建对象。

**具体步骤（逐字段点选法）：**

实际上，在 Power Automate 的新版界面，Value 字段支持混合输入——你可以用纯文本写 JSON 框架，然后在需要动态值的地方插入动态内容令牌。

1. 在 Value 输入框里，先点一下，确保光标在框内
2. 如果看到提示 **"Switch to input entire array"（切换为输入整个数组）** 或 **"Enter custom value"**，点击这个选项切换模式

3. 然后手动输入 JSON 结构，在需要动态值的地方点击 **"Add dynamic content"** 面板里的对应项：

```
{  ← 直接键盘输入
  "RowIndex":  ← 直接输入文字
```

然后在 "Add dynamic content" 面板里，找到 **Variables** 区域下的 `RowIndex`，点击插入。

```
  ,
  "YearMonth":  ← 输入文字
```

然后在动态内容面板 → **Apply to each** 区域（或当前项区域），找到 `YearMonth`，点击插入。

以此类推，最后加上 `}`。

**最终 Value 框里的内容示意（令牌用 `@{...}` 表示）：**

```
{"RowIndex":@{variables('RowIndex')},"YearMonth":"@{items('Apply_to_each')?['YearMonth']}","Function":"@{items('Apply_to_each')?['Function']}","Team":"@{items('Apply_to_each')?['Team']}","Owner":"@{items('Apply_to_each')?['Owner']}"}
```

> 💡 **关键要点**：字符串类型的字段值（文字）需要用双引号包起来，如 `"YearMonth":"@{...}"`；数字类型（如 RowIndex、ShoringRatio）不需要引号，如 `"RowIndex":@{...}`。

---

### 6.5 常见错误与解决方法

#### ❌ 错误 1：Value 里用了方括号 `[...]`（误将 Object 写成 Array）

**症状：** 运行时报错：
```
The input value is of type 'Array' which cannot be appended to the variable 'RowsForAI' of type 'Array'.
```

**原因：** Value 里写成了 `[{"RowIndex":1,...}]`，外面多了 `[` `]`，导致类型变成了 Array。

**修复：** 删除最外层的 `[` 和 `]`，确保 Value 是 `{...}` 而不是 `[{...}]`。

---

#### ❌ 错误 2：误选了整个 `value` 数组（来自 List rows 的输出）

**症状：** 报错同上（Array 类型错误）。

**原因：** 在 Value 里点选动态内容时，错误选择了 **List rows present in a table** 的 `value`（这是所有行组成的数组），而不是循环内当前行的某个字段。

**修复：** 打开动态内容面板，确保选择的是 **Apply to each** 区域（当前项）里的字段，不要选 List rows 输出的 `value`。

---

#### ❌ 错误 3：Value 只填了 `{}`（空对象）

**症状：** 运行不报错，但 Compose 验证时看到 RowsForAI 里全是空对象：
```json
[{}, {}, {}, ...]
```

**原因：** Value 字段填了 `{}`，没有包含任何字段。

**修复：** 按照 [6.2](#62-最小对象模板先跑通再扩展) 或 [6.3](#63-完整-tbloffshoring-对象模板) 的模板，把具体字段填进去。

---

#### ❌ 错误 4：列名拼写错误或大小写不匹配

**症状：** 运行不报错，但 Compose 验证时，某些字段值是 `null` 或根本不存在。

**原因：** 在 JSON 里写的列名（key）和 Excel Table 的实际列名不完全一致，例如 Excel 里是 `Cost Center Number`，JSON 里写成了 `CostCenterNumber` 或 `cost center number`。

**修复：**
- 如果用 **UI 方式 B（点选动态内容）**：直接从动态内容面板点选，不要手打列名，这样不会出错。
- 如果用 **UI 方式 A（表达式）**：在 `items('Apply_to_each')?['Cost Center Number']` 里，列名必须和 Excel Table 列头**完全一致**（包括空格和大小写）。

---

#### ❌ 错误 5：部分行的字段值为 null

**症状：** RowsForAI 里某些行的字段显示 `null`。

**原因：** Excel 对应行的该列是空白单元格，Power Automate 读取后值为 null。

**处理建议：** 若不影响后续 AI 处理，可先保留 null。若需要替换默认值，可用表达式：
```
coalesce(items('Apply_to_each')?['YearMonth'], '')
```
这样 null 会被替换为空字符串 `""`。

---

### 6.6 预期输出示例

运行成功后，`RowsForAI` 变量的内容应类似：

```json
[
  {
    "RowIndex": 1,
    "YearMonth": "202603",
    "CostCenterNumber": "CC-001",
    "Function": "IT",
    "Team": "Platform",
    "Owner": "Alice",
    "ShoringRatio": 0.6
  },
  {
    "RowIndex": 2,
    "YearMonth": "202603",
    "CostCenterNumber": "CC-002",
    "Function": "Finance",
    "Team": "Reporting",
    "Owner": "Bob",
    "ShoringRatio": 0.4
  }
]
```

---

## 7. 用 Compose 验证 RowsForAI

**强烈建议**在 Apply to each 循环**外面**（Flow 最末尾）加一个 Compose 步骤，用来验证 RowsForAI 的结构是否正确。

### 7.1 添加 Compose 步骤

在 Apply to each 外面（循环卡片的下方）点 **+ New step**：

1. 搜索：`Compose`
2. 选择：**Compose（撰写）**（在 Data Operation 分类下）

在 **Inputs** 字段里：

1. 点击输入框
2. 在弹出的 **Add dynamic content** 面板里，找到 **Variables** 区域
3. 点击选择 **RowsForAI**

### 7.2 运行 Flow 并检查输出

1. 点击右上角 **Test（测试）** → 选 **Manually（手动）**
2. 在弹窗里填入触发器的必填参数：
   - **BatchYearMonth**：例如 `202603`
   - **TopNIssuesInPDF**：例如 `200`
3. 点击 **Run flow（运行流）**

4. 运行结束后，点击 **Run history（运行历史）** → 点进刚才这次运行
5. 找到 Compose 这一步，展开查看 **Outputs（输出）**

### 7.3 验证检查清单

看 Compose 的输出，确认以下几点：

| 检查项 | 期望结果 |
|--------|---------|
| 最外层是数组 | `[...]` |
| 数组元素个数 | 等于你的 Excel 数据行数（或 Top Count 限制的行数） |
| 每个元素都是对象 | `{...}`，不是字符串或数字 |
| RowIndex 从 1 开始递增 | `1, 2, 3, ...` |
| YearMonth 等列有实际值 | 不是全部为 `null` 或 `{}` |
| 列名（key）正确 | 与你在 Value 里定义的 key 名称一致 |

### 7.4 小批量验证技巧

如果 Excel 数据很多（几百/几千行），先在 **List rows present in a table** 里找到 **Top Count** 字段，填 `20`，这样每次只处理 20 行，验证速度很快。验证 OK 后再去掉 Top Count 或改成更大的数值。

---

## 8. 常见问题排查

### Q1：循环看起来跑不完

**可能原因和处理方式：**

1. **数据量太大**：Excel 有几千行，每行都要调用 Excel 连接器，自然耗时。先用 Top Count = 20 验证逻辑，再去掉限制。

2. **连接器限流（Throttling）**：Excel Online / SharePoint 连接器有速率限制，大量请求会被限流后自动重试，看起来像"卡住"。处理方式：
   - 在 Apply to each 设置里（`...` → Settings）把 **Concurrency control（并发控制）** 打开并设为 `1`（关闭并发），减少对连接器的并发压力。

3. **运行历史里的迭代数**：打开 Run history，点进 Apply to each，查看"完成 X / 总计 Y 次迭代"，能看到实际处理了多少行。

4. **取消当前运行**：在 Run history 里点开正在运行的记录，右上角通常有 **Cancel run（取消运行）** 按钮。

### Q2：Table 下拉里找不到 tblOffshoring

检查以下几点：
- Excel 文件是否已保存到 SharePoint（而不是本地）
- File 字段选择的 Excel 文件是否正确
- Excel 里是否真的有名为 `tblOffshoring` 的 Table（不是普通单元格范围）

可以在 Excel → **Name Manager（名称管理器）** 或 **Table Design → Table Name** 里确认表名。

### Q3：Pagination Threshold 报错

Threshold 必须是**大于 0 的纯整数**，不能包含空格、引号或其他字符。正确示例：`5000`。

### Q4：Test 时报错"BatchYearMonth is required"

说明触发器的输入参数设置了 Required，但 Test 时没有填值。点击 Test → Manually，在弹窗里把所有必填参数填上值再点 Run flow。

---

*文档最后更新：2026-03-20*
