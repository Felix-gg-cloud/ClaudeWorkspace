# POC-Validator — Copilot Studio JSON 请求/响应示例

**版本**：v1.0  
**适用场景**：Power Automate Flow 通过 HTTP 动作调用 Copilot Studio AI Action，执行 `tblOffshoring` 数据验证

---

## 接口概述

| 属性 | 值 |
|------|----|
| 调用方式 | HTTP POST |
| 认证 | Bearer Token（Azure Entra ID，Copilot Studio 自动颁发） |
| Content-Type | `application/json` |
| 端点 URL | 从 Copilot Studio Action 发布后获取（格式：`https://xxx.api.crm.dynamics.com/api/data/...`） |

---

## 请求格式（Request）

### 请求 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["datasetName", "rows"],
  "properties": {
    "datasetName": {
      "type": "string",
      "description": "数据集名称，固定值 tblOffshoring"
    },
    "rows": {
      "type": "array",
      "description": "待验证的数据行数组，每行必须包含 RowIndex",
      "items": {
        "type": "object",
        "required": ["RowIndex"],
        "properties": {
          "RowIndex": {
            "type": "integer",
            "description": "对应 Excel 中的实际行号，从 2 开始（第 1 行为标题）"
          },
          "EmployeeId": { "type": "string" },
          "EmployeeName": { "type": "string" },
          "DepartmentCode": { "type": "string" },
          "CostCenter": { "type": "string" },
          "OffshoringStartDate": { "type": "string" },
          "OffshoringEndDate": { "type": "string" },
          "OffshoringRatio": { "type": ["string", "number"] }
        },
        "additionalProperties": true
      }
    }
  }
}
```

### 请求 JSON 示例

```json
{
  "datasetName": "tblOffshoring",
  "rows": [
    {
      "RowIndex": 2,
      "EmployeeId": "123456",
      "EmployeeName": " John Smith",
      "DepartmentCode": "GBS",
      "CostCenter": "CC-001234",
      "OffshoringStartDate": "2025-01-01",
      "OffshoringEndDate": "2025-12-31",
      "OffshoringRatio": "0.8"
    },
    {
      "RowIndex": 3,
      "EmployeeId": "",
      "EmployeeName": "Li Wei",
      "DepartmentCode": "INVALID_DEPT",
      "CostCenter": "CC-005678",
      "OffshoringStartDate": "2025-06-01",
      "OffshoringEndDate": "2025-03-01",
      "OffshoringRatio": "1.2"
    },
    {
      "RowIndex": 4,
      "EmployeeId": "789012",
      "EmployeeName": "Zhang San",
      "DepartmentCode": "ITO",
      "CostCenter": "CC-009999",
      "OffshoringStartDate": "2025-02-01",
      "OffshoringEndDate": "2025-08-31",
      "OffshoringRatio": "1"
    }
  ]
}
```

**注意事项**：
- `RowIndex` 为必填字段，Flow 在发送前已按公式（Excel 行号 = 数组索引 + 2）注入
- 所有字段值**原样传送**，不做任何 trim 或格式化处理
- `rows` 数组可包含 Excel 表格中的全部列，AI 仅检查已定义规则的字段

---

## 响应格式（Response）

### 响应 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["validatedAt", "totalRows", "issues"],
  "properties": {
    "validatedAt": {
      "type": "string",
      "description": "AI 执行验证的 UTC 时间戳（ISO 8601）"
    },
    "totalRows": {
      "type": "integer",
      "description": "本次验证的数据行总数"
    },
    "errorCount": {
      "type": "integer",
      "description": "Error 级别问题数量"
    },
    "warningCount": {
      "type": "integer",
      "description": "Warning 级别问题数量"
    },
    "infoCount": {
      "type": "integer",
      "description": "Info 级别问题数量"
    },
    "issues": {
      "type": "array",
      "description": "所有验证问题列表，无问题时为空数组 []",
      "items": {
        "type": "object",
        "required": ["RowIndex", "field", "ruleId", "level", "message"],
        "properties": {
          "RowIndex": {
            "type": "integer",
            "description": "对应 Excel 源数据的行号，与请求中的 RowIndex 一致"
          },
          "field": {
            "type": "string",
            "description": "触发问题的字段名"
          },
          "ruleId": {
            "type": "string",
            "description": "触发的验证规则 ID，如 R-WS-ALL-001"
          },
          "level": {
            "type": "string",
            "enum": ["Error", "Warning", "Info"],
            "description": "问题级别"
          },
          "message": {
            "type": "string",
            "description": "对用户友好的错误描述（中文）"
          },
          "actualValue": {
            "type": "string",
            "description": "触发问题的实际字段值（原始值）"
          }
        }
      }
    }
  }
}
```

### 响应 JSON 示例（对应上方请求）

```json
{
  "validatedAt": "2025-01-15T06:30:00.000Z",
  "totalRows": 3,
  "errorCount": 4,
  "warningCount": 0,
  "infoCount": 1,
  "issues": [
    {
      "RowIndex": 2,
      "field": "EmployeeName",
      "ruleId": "R-WS-ALL-001",
      "level": "Error",
      "message": "字段 \"EmployeeName\" 第 2 行存在前导或尾随空格，请删除多余空格后重试。",
      "actualValue": " John Smith"
    },
    {
      "RowIndex": 3,
      "field": "EmployeeId",
      "ruleId": "R-EMP-002",
      "level": "Error",
      "message": "员工编号为必填字段（第 3 行）。",
      "actualValue": ""
    },
    {
      "RowIndex": 3,
      "field": "DepartmentCode",
      "ruleId": "R-DEPT-001",
      "level": "Error",
      "message": "部门代码 \"INVALID_DEPT\" 不在允许的枚举范围内（第 3 行）。",
      "actualValue": "INVALID_DEPT"
    },
    {
      "RowIndex": 3,
      "field": "OffshoringEndDate",
      "ruleId": "R-DATE-002",
      "level": "Error",
      "message": "结束日期不得早于开始日期（第 3 行）。",
      "actualValue": "2025-03-01"
    },
    {
      "RowIndex": 3,
      "field": "OffshoringRatio",
      "ruleId": "R-RATIO-001",
      "level": "Error",
      "message": "离岸比例值 \"1.2\" 超出有效范围 [0, 1]（第 3 行）。",
      "actualValue": "1.2"
    },
    {
      "RowIndex": 4,
      "field": "OffshoringRatio",
      "ruleId": "R-RATIO-002",
      "level": "Info",
      "message": "该员工离岸比例为 100%，请确认是否正确（第 4 行）。",
      "actualValue": "1"
    }
  ]
}
```

> **说明**：第 3 行触发了多条规则（R-EMP-002、R-DEPT-001、R-DATE-002、R-RATIO-001），AI 全部返回，不因前一条规则触发而跳过后续检查。

---

## Copilot Studio Action 配置要点

### System Prompt（AI 指令）摘要

在 Copilot Studio 中配置 AI Action 时，System Prompt 应包含以下关键内容：

```
你是一个数据验证助手，负责检查 tblOffshoring 表格中的数据质量。

验证规则（按优先级执行）：

1. R-WS-ALL-001（Error，最高优先级）
   对所有文本类字段：若字段值的原始内容存在前导空格或尾随空格，
   即 value !== value.trim()，则报告 Error。
   注意：不要自动纠正或 trim，原样报告问题。

2. R-EMP-002（Error）：EmployeeId 不得为空。
3. R-EMP-001（Error）：EmployeeId 必须为 6-10 位纯数字。
4. R-NAME-001（Error）：EmployeeName 不得为空。
5. R-DEPT-002（Error）：DepartmentCode 不得为空。
6. R-DEPT-001（Error）：DepartmentCode 必须在允许的枚举列表内。
7. R-DATE-001（Error）：日期字段格式必须有效。
8. R-DATE-002（Error）：OffshoringEndDate 不得早于 OffshoringStartDate。
9. R-RATIO-001（Error）：OffshoringRatio 必须在 [0, 1] 范围内。
10. R-RATIO-002（Info）：OffshoringRatio 等于 1 时给出 Info 提示。

要求：
- 每条 issue 必须包含 RowIndex（从请求的 rows 数组中获取）
- 同一行可触发多条规则，全部返回
- 无问题时返回空数组 issues: []
- 返回标准 JSON 格式，不要包含任何额外文字
```

### 输入/输出参数配置

| 参数 | 方向 | 类型 | 说明 |
|------|------|------|------|
| `datasetName` | Input | String | 数据集名称 |
| `rows` | Input | Array | 待验证行数组（含 RowIndex） |
| `validatedAt` | Output | String | 验证时间戳 |
| `totalRows` | Output | Integer | 验证行数 |
| `errorCount` | Output | Integer | Error 数量 |
| `warningCount` | Output | Integer | Warning 数量 |
| `infoCount` | Output | Integer | Info 数量 |
| `issues` | Output | Array | 问题列表 |

---

## Power Automate Parse JSON Schema

在 Flow 的 **Parse JSON** 动作中使用以下 Schema 解析 AI 响应：

```json
{
  "type": "object",
  "properties": {
    "validatedAt": { "type": "string" },
    "totalRows": { "type": "integer" },
    "errorCount": { "type": "integer" },
    "warningCount": { "type": "integer" },
    "infoCount": { "type": "integer" },
    "issues": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "RowIndex": { "type": "integer" },
          "field": { "type": "string" },
          "ruleId": { "type": "string" },
          "level": { "type": "string" },
          "message": { "type": "string" },
          "actualValue": { "type": "string" }
        }
      }
    }
  }
}
```
