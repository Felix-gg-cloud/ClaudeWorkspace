# 03 — Power Automate Flow 配置指南

## Flow 概述

本 Flow 的职责：
1. 读取 SharePoint/OneDrive 上的 Excel 文件（`tblOffshoring` 表格）
2. 逐行执行所有校验规则（见 [02 校验规则说明](./02-validation-rules.md)）
3. 将所有 issue 追加到 SharePoint List 或写回另一个 Excel 结果表
4. 触发后续通知（邮件 / Teams 消息）

---

## 1. 前置准备

| 项目 | 要求 |
|------|------|
| Excel 文件位置 | SharePoint 文档库或 OneDrive，文件须共享给 Flow 服务账号 |
| Excel 表格 | 表名为 `tblOffshoring`，已按 [01 文档](./01-excel-table-setup.md) 搭建 |
| Power Automate 环境 | Cloud Flow（标准），推荐使用 Premium Excel Online (Business) 连接器 |
| 输出方式 | SharePoint List（推荐）或 Excel 结果表（issues_output） |

---

## 2. Flow 整体结构

```
[触发器] 手动触发 / 定时触发 / 新文件上传触发
    │
    ├─ [步骤 1] 初始化变量：issues 数组
    │
    ├─ [步骤 2] 列出 tblOffshoring 所有行
    │
    ├─ [步骤 3] Apply to each（遍历每一行）
    │   │
    │   ├─ [3.1] 计算 isTotal
    │   ├─ [3.2] YearMonth 校验（R-REQ-YM-001, R-YM-001）
    │   ├─ [3.3] 根据 isTotal 分支：Total / Non-Total 必填校验
    │   ├─ [3.4] Cost Center Number 格式校验（R-CCN-001）
    │   ├─ [3.5] Function 白名单校验（R-FUNC-001）
    │   ├─ [3.6] Team 白名单校验（R-TEAM-001）
    │   ├─ [3.7] Function-Team 映射校验（R-MAP-001）
    │   ├─ [3.8] 数值列逐列校验（R-NUM-001, R-NUM-002）
    │   └─ [3.9] ShoringRatio 格式校验（R-SR-001）
    │
    ├─ [步骤 4] 将 issues 数组写出（SharePoint List 或 Excel）
    │
    └─ [步骤 5] 发送通知（邮件/Teams）
```

---

## 3. 详细步骤说明

### 步骤 1：初始化变量

| 变量名 | 类型 | 初始值 |
|--------|------|--------|
| `varIssues` | Array | `[]` |
| `varFunctionWhitelist` | Array | `["Total (All)","Total (Core Operations)","Finance","HR","IT","Legal","Marketing","Operations","Procurement","Risk & Compliance","Strategy","Tax","Treasury","Audit","Customer Service","Data & Analytics"]` |
| `varTeamWhitelist` | Array | `["GBS","EA","HKT","Local","Shared Service"]` |
| `varNumericColumns` | Array | `["Actual_GBS_TeamMember","Actual_GBS_TeamLeaderAM","Actual_EA","Actual_HKT","Planned_GBS_TeamMember","Planned_GBS_TeamLeaderAM","Planned_EA","Planned_HKT","Target_YearEnd","Target_2030YearEnd"]` |
| `varAllowedFunctionTeamMap` | Array | （见下方 JSON） |

**varAllowedFunctionTeamMap JSON（粘贴到"初始化变量"的值字段）：**

```json
[
  {"Function":"Total (All)","Team":""},
  {"Function":"Total (Core Operations)","Team":""},
  {"Function":"Finance","Team":"GBS"},
  {"Function":"Finance","Team":"Local"},
  {"Function":"HR","Team":"GBS"},
  {"Function":"HR","Team":"Local"},
  {"Function":"IT","Team":"GBS"},
  {"Function":"IT","Team":"Local"},
  {"Function":"Legal","Team":"Local"},
  {"Function":"Marketing","Team":"Local"},
  {"Function":"Operations","Team":"GBS"},
  {"Function":"Operations","Team":"Local"},
  {"Function":"Procurement","Team":"GBS"},
  {"Function":"Procurement","Team":"Local"},
  {"Function":"Risk & Compliance","Team":"Local"},
  {"Function":"Strategy","Team":"Local"},
  {"Function":"Tax","Team":"Local"},
  {"Function":"Treasury","Team":"Local"},
  {"Function":"Audit","Team":"Local"},
  {"Function":"Customer Service","Team":"GBS"},
  {"Function":"Customer Service","Team":"Local"},
  {"Function":"Data & Analytics","Team":"GBS"},
  {"Function":"Data & Analytics","Team":"Local"}
]
```

---

### 步骤 2：列出 tblOffshoring 所有行

- 连接器：**Excel Online (Business)**
- 操作：**List rows present in a table**
- 参数：
  - Location: 选择 SharePoint 站点或 OneDrive
  - Document Library: 选择文档库
  - File: 选择 Excel 文件
  - Table: `tblOffshoring`

---

### 步骤 3：Apply to each（遍历每一行）

将步骤 2 的输出（`value`）作为循环项。

#### 3.1 计算 isTotal

使用"初始化变量"或"设置变量"，在循环内设置：

```
变量名: varIsTotal
值: @{or(equals(trim(items('Apply_to_each')?['Function']), 'Total (All)'), equals(trim(items('Apply_to_each')?['Function']), 'Total (Core Operations)'))}
```

> 也可以使用"条件"步骤直接分支，而不用变量。

---

#### 3.2 YearMonth 校验

**辅助函数说明**（Power Automate 表达式）：
- 去除空格：`trim(items('Apply_to_each')?['YearMonth'])`
- 正则匹配（需用 `matches()` 或自定义 Compose）：使用 6 位纯数字 + 月份范围

**步骤：使用"条件"检查 YearMonth 必填（R-REQ-YM-001）**

```
条件：equals(trim(items('Apply_to_each')?['YearMonth']), '')
如果是（为空）：
  → Append to array variable: varIssues
    值（JSON）：
    {
      "Severity": "Error",
      "RuleId": "R-REQ-YM-001",
      "YearMonth": "",
      "CostCenterNumber": "@{items('Apply_to_each')?['Cost Center Number']}",
      "Function": "@{items('Apply_to_each')?['Function']}",
      "Team": "@{items('Apply_to_each')?['Team']}",
      "Column": "YearMonth",
      "Value": "",
      "Message": "YearMonth 不能为空，所有行均须填写。",
      "FixSuggestion": "请填写 6 位数字年月，例如 202501。"
    }
如果否（非空）：
  → 继续执行 YearMonth 格式校验（R-YM-001）
     条件（格式校验，使用 Compose 输出正则结果）：
     - length(trim(YearMonth)) != 6 → 报错
     - 或无法解析为数字 → 报错
     - 月份检查：int(substring(trim(YearMonth), 4, 2)) 在 1-12 之间
```

> **Tip**：Power Automate Cloud Flow 本身不支持正则 `matches()` 函数。推荐做法：
> 1. 用 `length(trim(ym))` 判断是否 6 位
> 2. 用 `int(trim(ym))` 捕获是否全是数字（失败则说明含非数字字符）
> 3. 用 `int(substring(trim(ym),4,2))` 取月份并检查范围

---

#### 3.3 必填规则（Total / Non-Total 分支）

使用"条件"步骤，条件为 `varIsTotal`：

**Total 行（如果是）：**
```
检查 Owner 是否为空（R-REQ-TOTAL-OWNER）：
  条件：equals(trim(Owner), '')
  如果是：Append issue（Severity=Error, RuleId=R-REQ-TOTAL-OWNER, ...）
```

**Non-Total 行（如果否）：**
```
依次检查三列必填：
  1. Target_YearEnd 是否为空（R-REQ-NT-TGT1）
  2. Target_2030YearEnd 是否为空（R-REQ-NT-TGT2）
  3. ShoringRatio 是否为空（R-REQ-NT-SR）
  （各自独立条件，每条各 Append 一条 issue）
```

---

#### 3.4 Cost Center Number 格式（R-CCN-001）

```
条件：not(equals(trim(Cost Center Number), ''))
如果是（非空）：
  检查格式：
  - length(trim(CCN)) == 7 AND int(trim(CCN)) >= 0（确保全为数字）
  - 如果不满足 → Append issue（Severity=Error, RuleId=R-CCN-001）
```

---

#### 3.5 Function 白名单（R-FUNC-001）

```
条件：not(equals(trim(Function), ''))
如果是（非空）：
  检查：contains(varFunctionWhitelist, trim(Function))
  如果不在白名单中 → Append issue（Severity=Error, RuleId=R-FUNC-001）
```

---

#### 3.6 Team 白名单（R-TEAM-001）

```
条件：not(equals(trim(Team), ''))
如果是（非空）：
  检查：contains(varTeamWhitelist, trim(Team))
  如果不在白名单中 → Append issue（Severity=Warning, RuleId=R-TEAM-001）
```

---

#### 3.7 Function-Team 映射（R-MAP-001）

由于 Power Automate 不支持复杂对象数组查找，推荐以下两种方法之一：

**方法 A：条件嵌套（简单但维护成本较高）**

直接用多层 if 判断 Function 值后再检查 Team。

**方法 B：字符串拼接查找（推荐）**

```
1. 构造 key：concat(trim(Function), '|', trim(Team))
2. 构造合法 key 列表（Compose 变量）：
   ["Total (All)|","Total (Core Operations)|","Finance|GBS","Finance|Local",...]
3. 检查：contains(varKeyList, key)
4. 如果不在列表中 → Append issue（Severity=Error, RuleId=R-MAP-001）
```

> **注意**：Total 行 Team 为空时，key 为 `"Total (All)|"`，需要在合法列表中包含这个值。

---

#### 3.8 数值列校验（R-NUM-001 / R-NUM-002）

对每个数值列（10 列）执行以下逻辑（可用"Apply to each"嵌套循环列名，或手动逐列添加条件）：

```
变量：colName（列名），colValue（列值）

条件 1：not(equals(trim(colValue), ''))
如果是（非空）：
  条件 2（判断是否数字）：
    尝试 float(colValue)，若报错则 → Append issue（R-NUM-001）
  如果是数字：
    条件 3：float(colValue) < 0
    如果是 → Append issue（R-NUM-002）
```

**Power Automate 表达式（判断是否数字）**：

```
@{if(equals(string(float(items('Apply_to_each')?['Actual_GBS_TeamMember'])), 'NaN'), false, true)}
```

> **Tip**：Power Automate 中 `float()` 对非数字输入会触发错误而不是返回 NaN。推荐使用 `Try/Catch`（即"作用域 + 配置运行条件为失败时"）或先用 `isFloat()` 自定义 Compose 来判断。

---

#### 3.9 ShoringRatio 格式（R-SR-001）

```
条件：not(equals(trim(ShoringRatio), ''))
如果是（非空）：
  步骤 1：v = trim(ShoringRatio)
  步骤 2：检查最后一个字符是否为 '%'
    last(v) == '%'
  步骤 3：取数值部分 numStr = substring(v, 0, sub(length(v), 1))
  步骤 4：检查 numStr 是否能解析为浮点数
  步骤 5：检查 0 <= float(numStr) <= 100
  上述任一失败 → Append issue（Severity=Error, RuleId=R-SR-001）
```

**Power Automate 表达式参考**：

```
// 取最后一个字符
last(trim(items('Apply_to_each')?['ShoringRatio']))

// 去掉末尾 %，取数值部分
substring(trim(items('Apply_to_each')?['ShoringRatio']), 0, sub(length(trim(items('Apply_to_each')?['ShoringRatio'])), 1))

// 判断数值是否在 0~100
and(
  greaterOrEquals(float(numStr), 0),
  lessOrEquals(float(numStr), 100)
)
```

---

### 步骤 4：写出 issues 数据

#### 方案 A：写入 SharePoint List（推荐）

- 创建一个 SharePoint List，列名与 issue 结构一致
- 在 Flow 中 Apply to each `varIssues`，使用"Create item"操作逐条写入

#### 方案 B：写回 Excel 结果表

- 在同一 Excel 文件或另一文件中创建 `tblIssues` 表
- 在 Flow 中 Apply to each `varIssues`，使用"Add a row into a table"操作逐条写入

---

### 步骤 5：发送通知

**邮件通知（使用 Office 365 Outlook 连接器）：**

```
主题: [Headcount POC] 数据校验完成 - 共发现 X 条问题
正文:
  - Error 数量：X 条
  - Warning 数量：X 条
  - 请登录 Power BI 查看详情并修正数据
  - 详细 issue 清单：[SharePoint List 链接 / Excel 链接]
```

---

## 4. 调试技巧

| 问题 | 解决方案 |
|------|----------|
| "列出行"失败 | 检查 Excel 文件路径、表名是否完全匹配（区分大小写） |
| `float()` 触发错误 | 使用"作用域"包裹 float() 转换，并配置 `runAfter` 为 `Failed` 捕获异常 |
| issues 数组超大导致性能问题 | 每 500 行 issue 批量写入一次（改用"Select"+"Create item"批操作） |
| Function-Team 映射维护困难 | 将映射表存入 SharePoint List，Flow 中先读取列表再做查找 |
| YearMonth 正则在 Power Automate 中无法用 | 改用 length + int + substring 拼接多步校验 |

---

## 5. Flow 运行频率建议

| 场景 | 触发方式 |
|------|----------|
| 按月定期校验 | Recurrence（每月 1 日 9:00 触发） |
| 文件更新即校验 | SharePoint"当文件被修改时"触发 |
| 手动触发 POC 测试 | "手动触发 Flow"（Manually trigger a flow） |
