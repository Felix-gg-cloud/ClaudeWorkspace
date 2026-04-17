# Power Automate — Apply to each 循环故障排查指南

> 适用场景：使用 **Excel Online (Business) → List rows present in a table** 读取数据后，通过 **Apply to each** 循环处理每行，出现循环"跑不完"、报错、或超时等问题。

---

## 目录

1. [为什么 Apply to each 看起来会无限运行](#1-为什么-apply-to-each-看起来会无限运行)
2. [如何诊断问题](#2-如何诊断问题)
3. [常见原因与解决方法](#3-常见原因与解决方法)
4. [如何停止/取消正在运行的 Flow](#4-如何停止取消正在运行的-flow)
5. [确保-list-rows-present-in-a-table-返回有限结果](#5-确保-list-rows-present-in-a-table-返回有限结果)
6. [动态内容选择：value-vs-current-item](#6-动态内容选择value-vs-current-item)
7. [添加防护措施](#7-添加防护措施)
8. [常见报错与修复](#8-常见报错与修复)

---

## 1. 为什么 Apply to each 看起来会无限运行

Apply to each 循环依赖它的**输入数组大小**来决定执行次数。以下情况会导致循环显得"跑不完"：

| 原因 | 说明 |
|------|------|
| **数据集过大** | 表格有几万行，逐行处理需要很长时间 |
| **Pagination Threshold 设置过高** | 把阈值设成 100,000，实际返回 10 万行，循环要跑 10 万次 |
| **嵌套循环** | Apply to each 里面还有 Apply to each，执行次数是乘积关系 |
| **动作重试机制** | 循环内某个动作反复失败重试，拖慢整体 |
| **并发设置过低** | 并发度为 1（串行），行数多时总时间 = 行数 × 单行耗时 |
| **误选了 Array 作为循环输入** | 把一个嵌套数组传给 Apply to each，导致多层循环 |

---

## 2. 如何诊断问题

### 2.1 查看运行历史（Run History）

1. 打开 Power Automate → 找到你的 Flow
2. 点击 **28 day run history** 或 **Run history**
3. 点进最近一次运行记录
4. 展开 **Apply to each** 节点，查看：
   - **Iterations**（迭代次数）：实际循环了多少次
   - 每次迭代的耗时

> **关键检查点**：如果 Iterations 显示 5000、10000 或更大，说明输入数组太大。

### 2.2 检查 Pagination Threshold

在 **List rows present in a table** 动作上：

1. 点击右上角 **…（三个点）**
2. 点 **Settings（设置）**
3. 查看 **Pagination** 开关状态和 **Threshold** 值

> **建议值**：POC 阶段填 `200`~`1000`，不要盲目填 `100000`。Threshold 是"最多返回多少行"，不是"每页多少行"。

### 2.3 在循环外加 Compose 做快速验证

在 Apply to each **之前**（不是之后）加一个 **Compose** 动作：

- Inputs：选择 `List rows present in a table` 的 **value**
- 运行后查看 Compose 输出：`length(outputs('List_rows_present_in_a_table')?['body/value'])` 就是实际行数

如果实际行数远超预期，先解决数据量问题再跑循环。

---

## 3. 常见原因与解决方法

### 3.1 数据集过大

**症状**：循环慢，最终超时（Power Automate 单次运行上限 30 天，但单个动作有超时）。

**解决**：
- 在 List rows 的 **Filter Query** 里加筛选条件（只取本月数据）
- 设置 **Top Count**（见[第 7 节](#7-添加防护措施)）

### 3.2 Pagination Threshold 过高

**症状**：返回了远超预期的行数，Apply to each 迭代次数爆炸。

**解决**：把 Threshold 改小到一个合理上限（例如 `500`）。

### 3.3 嵌套循环

**症状**：外层循环 100 次，内层循环 50 次 → 实际执行 5000 次。

**解决**：
- 尽量展平数组，用 **Select** 或 **Filter array** 在循环外预处理
- 检查内层 Apply to each 的输入是否必要

### 3.4 动作重试

**症状**：某个 API 动作（如 HTTP、AI Builder）失败后反复重试，循环卡在某行。

**解决**：
- 打开该动作的 **Settings → Retry Policy**
- 改为 **None**（调试期间），或减少重试次数

### 3.5 并发度为 1

**症状**：循环行数不多，但总时间 = 行数 × 单行耗时（完全串行）。

**解决**：
- 打开 Apply to each 的 **Settings → Concurrency Control**
- 开启并发，设置并发度（例如 `5` 或 `10`）

> ⚠️ 注意：开启并发后，循环内对变量（如 `RowIndex`、`RowsForAI`）的读写可能出现竞态条件，不适合需要严格顺序的场景。

---

## 4. 如何停止/取消正在运行的 Flow

### 方法 A：从运行历史取消

1. Power Automate → 点进你的 Flow
2. 点 **28 day run history**
3. 找到正在运行的记录（状态为 **Running**）
4. 点进去 → 右上角点 **Cancel（取消）**

### 方法 B：从 Flow 详情页取消

1. 在 Flow 详情页，找到 **Run history** 右侧的 **…** 或直接点运行记录
2. 选择 **Cancel run**

### 方法 C：禁用 Flow（紧急）

如果无法取消单次运行，可以临时禁用整个 Flow：

1. Flow 详情页 → 点 **Turn off（关闭）**
2. 待处理完毕后再重新开启

> 注意：取消运行后，已经完成的迭代不会回滚（如果有写操作，需手动处理）。

---

## 5. 确保 List rows present in a table 返回有限结果

### 5.1 Filter Query（服务端过滤，最推荐）

在动作的 **Filter Query** 里填 OData 表达式，例如：

```
YearMonth eq '202603'
```

或数字列：

```
Amount gt 0
```

> Filter Query 在数据源端过滤，**不会**把多余数据传到 Flow 再过滤，效率最高。

### 5.2 Top Count（限制最大返回行数）

在动作里找 **Top Count**（高级选项），填一个安全上限，例如 `500`。

即使 Pagination Threshold 很大，Top Count 也能作为兜底限制。

### 5.3 Pagination Threshold 设置建议

| 数据规模 | 建议 Threshold |
|----------|---------------|
| 测试/POC（< 100 行） | `100` 或 `200` |
| 小表（< 1000 行） | `1000` |
| 中表（< 5000 行） | `5000` |
| 大表 | 先用 Filter Query 缩小范围，再设 Threshold |

**Threshold 必须是 `> 0` 的纯整数**，不能填 `0`、空白、`5,000`（带逗号）或带空格的值。

---

## 6. 动态内容选择：value vs current item

这是 Apply to each 最容易踩的坑之一。

### 6.1 循环输入选 value（整个数组）

设置 Apply to each 时，在 **"Select an output from previous steps"** 里：

- ✅ 正确：选择 `List rows present in a table` 的 **value**（这是行数组）
- ❌ 错误：选择其他动态内容（如整个 body、其他变量）

如果选错，循环可能只跑 1 次（输入是单个对象）或报类型错误。

### 6.2 循环内选 Current item（当前行）

在循环内部的动作里，引用当前行数据时：

- ✅ 正确：使用 `current item` 或 `items('Apply_to_each')` 里的字段，例如动态内容中选择 `YearMonth`（来自 Apply to each）
- ❌ 错误：再次选择 `List rows present in a table` 的 `value`（这是整个数组，不是当前行）

**如何辨别**：在动态内容面板里，Apply to each 下方列出的字段（如 `YearMonth`、`Cost Center Number`）才是"当前行"的字段。

---

## 7. 添加防护措施

### 7.1 Top Count 限制

在 **List rows present in a table** 动作 → 高级选项 → **Top Count**：

填入一个安全上限（例如 `500`），防止意外读取超量数据。

### 7.2 Filter array（循环前二次过滤）

如果无法在 Filter Query 里做精确过滤，可以在 List rows 之后、Apply to each 之前加一个 **Filter array** 动作：

- **From**：选 `List rows present in a table` 的 **value**
- **Condition**：设置你需要的过滤条件（例如 `YearMonth` **等于** `BatchYearMonth` 触发器参数；`BatchYearMonth` 是在 **Manually trigger a flow** 中定义的 Text 输入字段，代表要处理的年月批次，如 `202603`）

这样循环输入的数组已经是过滤后的小数组。

### 7.3 Condition（循环内条件跳过）

在循环内加 **Condition** 动作，对不符合条件的行直接走 "If no" 分支（不做任何事），避免无效处理。

### 7.4 终止检查（Terminate）

在循环外加一个 **Condition** 检查数组长度，如果超过安全阈值就调用 **Terminate** 动作提前结束：

```
length(outputs('List_rows_present_in_a_table')?['body/value']) > 1000
```

- If yes → **Terminate**（Status: Failed，Message: "数据量超过安全阈值，请缩小查询范围"）
- If no → 继续循环

---

## 8. 常见报错与修复

### 8.1 Paging count invalid. Value must be a number greater than 0

**原因**：Pagination 已开启，但 Threshold 为空、0、或非纯数字。

**修复**：
1. 打开 List rows 动作 → **…** → **Settings**
2. Pagination 开关确保为 **On**
3. Threshold 填入 `>0` 的纯整数（如 `1000`）
4. 不要填：`0`、空白、`5,000`（逗号）、`5000 `（空格）

---

### 8.2 The input value is of type 'Array' which cannot be appended to the variable of type 'Array'

**完整报错**：
```
Action 'Append_to_array_variable' failed: The input value is of type 'Array' which cannot be 
appended to the variable 'RowsForAI' of type 'Array'. The action type 'AppendToArrayVariable' 
only supports values of types 'Float, Integer, String, Boolean, Object'.
```

**原因**：`Append to array variable` 每次只能追加**一个元素**（Object、String 等），不能把整个数组追加进去。如果你在 Value 里选了 `value`（整个行数组），就会出现这个错误。

**修复**：Value 里填的必须是一个**单个 Object**（当前行的数据），而不是整个数组。

正确做法（在循环内）：

**方式 A：直接在 Value 里构建 JSON 对象（推荐）**

在 Append to array variable 的 **Value** 输入框里，切换到**表达式（Expression）**模式，填：

```
json(concat('{"RowIndex":', string(variables('RowIndex')), ',"YearMonth":"', items('Apply_to_each')?['YearMonth'], '","Function":"', items('Apply_to_each')?['Function'], '"}'))
```

或使用更简洁的方式（如果 UI 支持直接输入 JSON）：

```json
{
  "RowIndex": @{variables('RowIndex')},
  "YearMonth": @{items('Apply_to_each')?['YearMonth']},
  "Function": @{items('Apply_to_each')?['Function']}
}
```

**方式 B：用 Compose 先组建对象，再 Append**

1. 在循环内先加 **Compose** 动作，Inputs 填：
   ```json
   {
     "RowIndex": @{variables('RowIndex')},
     "YearMonth": @{items('Apply_to_each')?['YearMonth']},
     "Function": @{items('Apply_to_each')?['Function']}
   }
   ```
2. 在 Compose 下面加 **Append to array variable**：
   - Name：`RowsForAI`
   - Value：选择 Compose 的 **Outputs**（这是一个 Object，不是 Array）

> **关键原则**：每次循环只追加一个 Object（当前行），不追加数组。循环结束后 `RowsForAI` 自然就是所有行组成的数组。

---

### 8.3 循环验证：最小可跑通步骤

完成循环搭建后，在 Apply to each **外面**（最底下）加：

- **Compose**
- Inputs：选变量 `RowsForAI`

手动运行一次，检查 Compose 输出：

```json
[
  { "RowIndex": 1, "YearMonth": "202603", "Function": "Finance" },
  { "RowIndex": 2, "YearMonth": "202603", "Function": "HR" },
  ...
]
```

确认每条都有 `RowIndex: 1, 2, 3...` 且字段值正确后，再把 `RowsForAI` 传给后续动作（如 Copilot Studio）。
