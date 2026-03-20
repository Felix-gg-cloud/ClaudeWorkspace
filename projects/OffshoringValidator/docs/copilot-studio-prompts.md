# Copilot Studio / AI 提示词模板

> **项目**：Offshoring Validator POC  
> **用途**：在 Power Automate 中调用 AI 模型（Copilot Studio / AI Builder / Azure OpenAI）时使用的提示词  
> **对应文档**：[poc-runbook.md](./poc-runbook.md) | [flow-steps.md](./flow-steps.md)

---

## 目录

1. [系统提示词（System Prompt）](#系统提示词system-prompt)
2. [用户提示词（User Prompt）](#用户提示词user-prompt)
3. [完整调用示例](#完整调用示例)
4. [验证规则参考](#验证规则参考)
5. [AI Builder 提示词配置](#ai-builder-提示词配置)
6. [提示词调优说明](#提示词调优说明)

---

## 系统提示词（System Prompt）

> 此提示词设置 AI 模型的角色、规则和输出格式约束。

```
你是一个专业的数据质量验证助手，负责对企业人员离岸（Offshoring）数据进行字段级验证。

## 你的任务

接收一个 JSON 格式的数据表，对每一行、每一列进行验证，并以严格的结构化 JSON 格式返回验证结果。

## 验证规则

### 空白字符规则（严格执行）

1. **前/后空格检测（Error）**
   - 若某字段值的开头或结尾包含空格（或任何空白字符，如制表符 \t、换行符 \n 等），则报 Error
   - 规则标识符：`leading_whitespace`（仅前导空格）、`trailing_whitespace`（仅尾随空格）、`surrounding_whitespace`（前后都有）
   - 不对数据做自动修正，只报告

2. **空白即空（whitespace-only = empty）**
   - 若字段值全部由空白字符组成（如 `"   "`、`"\t"` 等），视为空值，适用必填规则
   - 此类字段同样报 `empty_whitespace`（Error，若该字段为必填）

### 必填字段规则（Error）

以下字段不允许为空（null、空字符串 ""、或仅含空白字符）：
- EmployeeID
- FullName
- Department
- OffshoreLocation
- StartDate
- CostCenter
- ContractType
- Status

规则标识符：`required_field_empty`

### 业务逻辑规则（Warning）

1. **日期顺序（Warning）**
   - 若 EndDate 存在且早于 StartDate，报 Warning
   - 规则标识符：`date_order_invalid`

2. **Status 枚举（Warning）**
   - Status 应为以下之一：Active、Inactive、Pending、Terminated
   - 不在此列表中（且非空）则报 Warning
   - 规则标识符：`status_invalid_value`

3. **ContractType 枚举（Warning）**
   - ContractType 应为以下之一：Full-Time、Part-Time、Contractor、Consultant
   - 不在此列表中（且非空）则报 Warning
   - 规则标识符：`contract_type_invalid_value`

## 严重级别

只使用以下两个级别，不使用 Info 或其他级别：
- `Error`：必须修正
- `Warning`：建议核查

## 输出格式要求（严格遵守）

必须返回如下 JSON，不得包含任何额外文字、Markdown 代码块标记或注释：

{
  "report_model": {
    "generated_at": "<ISO 8601 UTC 时间，如 2025-01-20T10:30:00Z>",
    "source_file": "<数据来源文件名或标识>",
    "table_name": "<表格名称>",
    "total_rows": <整数，数据总行数>,
    "error_count": <整数，Error 数量>,
    "warning_count": <整数，Warning 数量>
  },
  "issues": [
    {
      "row_number": <整数，从 1 开始，不含表头>,
      "column": "<字段名>",
      "value_preview": "<字段值预览，最多 100 字符，敏感数据可截断>",
      "severity": "<Error 或 Warning>",
      "rule": "<规则标识符>",
      "message": "<面向用户的说明，使用中文>"
    }
  ]
}

若无任何问题，issues 应为空数组 []。

## 重要约束

- 不要修正数据，不要猜测用户意图
- 不要遗漏任何字段的前/后空格问题
- 行号从 1 开始（不含表头行）
- value_preview 中若原值有前后空格，必须完整保留（如 \" John\"）
- 只输出 JSON，不要任何解释文字
```

---

## 用户提示词（User Prompt）

> 此提示词在每次调用时动态生成，包含实际数据。

```
请验证以下 Offshoring 数据表格。

表格名称：{table_name}
源文件：{source_file}

数据（JSON 格式，每个对象为一行记录，key 为列名，value 为单元格值）：
{table_data_json}

请按照系统指令中的规则进行验证，并返回结构化 JSON 结果。
```

### Power Automate 中的表达式替换

| 占位符 | Power Automate 表达式 |
|--------|----------------------|
| `{table_name}` | `varTableName` |
| `{source_file}` | 提取自 `varExcelFileUrl` 的文件名部分 |
| `{table_data_json}` | `varTableDataJson`（序列化的行数组） |

---

## 完整调用示例

### 输入数据示例

```json
[
  {
    "EmployeeID": "EMP001",
    "FullName": " John Smith",
    "Department": "Engineering",
    "OffshoreLocation": "India",
    "StartDate": "2025-01-15",
    "EndDate": "2025-12-31",
    "CostCenter": "CC-1001",
    "ContractType": "Full-Time",
    "Status": "Active"
  },
  {
    "EmployeeID": "EMP002",
    "FullName": "Jane Doe",
    "Department": "Finance ",
    "OffshoreLocation": "Philippines",
    "StartDate": "2025-06-01",
    "EndDate": "2025-01-01",
    "CostCenter": "CC-2001",
    "ContractType": "Freelancer",
    "Status": "Active"
  },
  {
    "EmployeeID": "",
    "FullName": "   ",
    "Department": "HR",
    "OffshoreLocation": "Vietnam",
    "StartDate": "2025-03-01",
    "EndDate": "",
    "CostCenter": "CC-3001",
    "ContractType": "Contractor",
    "Status": "Pending"
  }
]
```

### 期望 AI 输出

```json
{
  "report_model": {
    "generated_at": "2025-01-20T10:30:00Z",
    "source_file": "tblOffshoring_input.xlsx",
    "table_name": "tblOffshoring",
    "total_rows": 3,
    "error_count": 4,
    "warning_count": 2
  },
  "issues": [
    {
      "row_number": 1,
      "column": "FullName",
      "value_preview": "\" John Smith\"",
      "severity": "Error",
      "rule": "leading_whitespace",
      "message": "第 1 行 FullName 字段包含前导空格，请删除多余空格后重新提交。"
    },
    {
      "row_number": 2,
      "column": "Department",
      "value_preview": "\"Finance \"",
      "severity": "Error",
      "rule": "trailing_whitespace",
      "message": "第 2 行 Department 字段包含尾随空格，请删除多余空格后重新提交。"
    },
    {
      "row_number": 2,
      "column": "EndDate",
      "value_preview": "2025-01-01",
      "severity": "Warning",
      "rule": "date_order_invalid",
      "message": "第 2 行 EndDate（2025-01-01）早于 StartDate（2025-06-01），请确认日期是否正确。"
    },
    {
      "row_number": 2,
      "column": "ContractType",
      "value_preview": "Freelancer",
      "severity": "Warning",
      "rule": "contract_type_invalid_value",
      "message": "第 2 行 ContractType 值「Freelancer」不在允许列表中（Full-Time、Part-Time、Contractor、Consultant），请核查。"
    },
    {
      "row_number": 3,
      "column": "EmployeeID",
      "value_preview": "\"\"",
      "severity": "Error",
      "rule": "required_field_empty",
      "message": "第 3 行 EmployeeID 为必填字段，当前为空，请填写后重新提交。"
    },
    {
      "row_number": 3,
      "column": "FullName",
      "value_preview": "\"   \"",
      "severity": "Error",
      "rule": "required_field_empty",
      "message": "第 3 行 FullName 为必填字段，当前值仅含空白字符（视为空），请填写后重新提交。"
    }
  ]
}
```

---

## 验证规则参考

### 规则标识符完整列表

| 规则标识符 | 级别 | 触发条件 |
|-----------|------|---------|
| `leading_whitespace` | Error | 字段值有前导空格/空白 |
| `trailing_whitespace` | Error | 字段值有尾随空格/空白 |
| `surrounding_whitespace` | Error | 字段值前后都有空白 |
| `required_field_empty` | Error | 必填字段为空（含纯空白） |
| `empty_whitespace` | Error | 必填字段值为纯空白字符串 |
| `date_order_invalid` | Warning | EndDate 早于 StartDate |
| `status_invalid_value` | Warning | Status 不在枚举中 |
| `contract_type_invalid_value` | Warning | ContractType 不在枚举中 |

### 必填字段列表

```
EmployeeID, FullName, Department, OffshoreLocation, StartDate, CostCenter, ContractType, Status
```

### 枚举约束

```
Status:       Active | Inactive | Pending | Terminated
ContractType: Full-Time | Part-Time | Contractor | Consultant
```

---

## AI Builder 提示词配置

若使用 Power Automate 的 **AI Builder → Create text with GPT** action：

### 配置步骤

1. 在 AI Builder 中创建一个 **Prompt**（自定义提示词）
2. **Prompt 名称**：`OffshoringValidatorPrompt`
3. **Instructions（系统提示词）**：粘贴 [系统提示词](#系统提示词system-prompt) 全文
4. **Input Variables**：
   - `table_name`（Text）：表格名称
   - `table_data`（Text）：序列化的表格行 JSON
   - `source_file`（Text）：源文件名
5. **Prompt（用户消息模板）**：
   ```
   请验证以下 Offshoring 数据表格。
   
   表格名称：[table_name]
   源文件：[source_file]
   
   数据（JSON 格式）：
   [table_data]
   
   请按照系统指令中的规则进行验证，并返回结构化 JSON 结果。
   ```
6. 保存后在 Power Automate 中通过 **Run prompt** action 调用

### 重要 AI Builder 设置

| 设置 | 推荐值 |
|------|-------|
| Temperature | `0`（确定性输出，减少随机性） |
| Response format | JSON（若支持） |
| Max tokens | `4096`（根据数据量调整） |

---

## 提示词调优说明

### 常见问题及解决方案

| 问题 | 解决方案 |
|------|---------|
| AI 输出包含 Markdown 代码块（` ```json `） | 在系统提示词中强调：「不要任何 Markdown 格式，只输出纯 JSON」 |
| AI 遗漏了某些前/后空格问题 | 在系统提示词中增加示例，使用 few-shot 方式 |
| AI 自动修正了数据（trim 后判断） | 强调「不要修正数据，按原值验证」 |
| issues 数量为 0 但实际有问题 | 检查数据序列化方式，确保空格被保留为字面字符串 |
| JSON 解析失败 | 在 Parse JSON 前加 `trim()` 清理 AI 响应首尾，但不修改实际数据 |

### 数据序列化注意事项

在将 Excel 行传给 AI 之前，确保序列化正确保留特殊字符：

```
// Power Automate 表达式
// 正确：使用 string() 转换，保留原始值
string(outputs('List_rows')?['body/value'])

// 错误：不要使用 trim() 或 replace() 处理行数据
```

### 分批处理提示词调整

若需要分批验证（每批 50 行），在用户提示词中添加：

```
注意：这是第 {batch_number} 批数据（共 {total_batches} 批），行号从 {start_row} 开始。
请在 row_number 字段中使用全局行号（从整个表格第 1 行开始计数）。
```
