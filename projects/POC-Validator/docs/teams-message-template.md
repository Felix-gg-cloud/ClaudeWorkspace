# POC-Validator — Teams 通知消息模板

**版本**：v1.0  
**通知方式**：Microsoft Teams Adaptive Card（通过 Power Automate "Post an Adaptive Card" 动作发送）

---

## 消息模板说明

Teams 通知在 Flow 的最后一步发送，包含以下信息：
- 验证执行时间和源文件名
- Error / Warning / Info 数量统计
- 整体验证状态（PASSED / FAILED）
- Excel 报告和 PDF 报告的 OneDrive 链接

---

## Power Automate 配置

**连接器**：Microsoft Teams  
**动作**：Post an Adaptive Card to a Teams channel (V2)

| 字段 | 值 |
|------|----|
| Team | 选择目标团队（POC 阶段可选自己的团队） |
| Channel | 选择目标频道（如 `General`） |
| Adaptive Card | 粘贴下方 JSON 模板，替换动态变量 |

---

## Adaptive Card JSON 模板

将以下 JSON 粘贴到 Power Automate 的 Adaptive Card 输入框，并将 `{{...}}` 替换为对应的 Flow 表达式：

```json
{
  "type": "AdaptiveCard",
  "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
  "version": "1.4",
  "body": [
    {
      "type": "Container",
      "style": "emphasis",
      "items": [
        {
          "type": "ColumnSet",
          "columns": [
            {
              "type": "Column",
              "width": "stretch",
              "items": [
                {
                  "type": "TextBlock",
                  "text": "📊 数据验证报告",
                  "weight": "Bolder",
                  "size": "Large",
                  "color": "Accent"
                },
                {
                  "type": "TextBlock",
                  "text": "{{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}}",
                  "size": "Small",
                  "isSubtle": true,
                  "spacing": "None"
                }
              ]
            },
            {
              "type": "Column",
              "width": "auto",
              "items": [
                {
                  "type": "TextBlock",
                  "text": "{{if(greater(variables('varErrorCount'), 0), '❌ FAILED', '✅ PASSED')}}",
                  "weight": "Bolder",
                  "size": "ExtraLarge",
                  "color": "{{if(greater(variables('varErrorCount'), 0), 'Attention', 'Good')}}"
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "type": "Container",
      "spacing": "Medium",
      "items": [
        {
          "type": "FactSet",
          "facts": [
            {
              "title": "📁 源文件",
              "value": "{{triggerBody()/file/name}}"
            },
            {
              "title": "📋 总行数",
              "value": "{{length(variables('varRawRows'))}} 行"
            },
            {
              "title": "❌ Error",
              "value": "{{variables('varErrorCount')}} 条"
            },
            {
              "title": "⚠️ Warning",
              "value": "{{variables('varWarningCount')}} 条"
            },
            {
              "title": "ℹ️ Info",
              "value": "{{variables('varInfoCount')}} 条"
            },
            {
              "title": "📌 合计问题",
              "value": "{{add(add(variables('varErrorCount'), variables('varWarningCount')), variables('varInfoCount'))}} 条"
            }
          ]
        }
      ]
    },
    {
      "type": "Container",
      "spacing": "Medium",
      "items": [
        {
          "type": "TextBlock",
          "text": "📂 报告文件",
          "weight": "Bolder",
          "size": "Medium"
        }
      ]
    },
    {
      "type": "ActionSet",
      "actions": [
        {
          "type": "Action.OpenUrl",
          "title": "📊 查看 Excel 报告",
          "url": "{{variables('varExcelUrl')}}"
        },
        {
          "type": "Action.OpenUrl",
          "title": "📄 查看 PDF 报告",
          "url": "{{variables('varPdfUrl')}}"
        }
      ]
    }
  ],
  "msteams": {
    "width": "Full"
  }
}
```

---

## Flow 表达式说明

在 Power Automate 中，将上方 JSON 里的 `{{...}}` 替换为实际的 Flow 表达式：

| 模板占位符 | Power Automate 表达式 |
|-----------|----------------------|
| `{{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}}` | `@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}` |
| `{{triggerBody()/file/name}}` | `@{triggerBody()?['file']?['name']}` |
| `{{length(variables('varRawRows'))}}` | `@{length(variables('varRawRows'))}` |
| `{{variables('varErrorCount')}}` | `@{variables('varErrorCount')}` |
| `{{variables('varWarningCount')}}` | `@{variables('varWarningCount')}` |
| `{{variables('varInfoCount')}}` | `@{variables('varInfoCount')}` |
| `{{add(add(...))}}` | `@{add(add(variables('varErrorCount'), variables('varWarningCount')), variables('varInfoCount'))}` |
| `{{variables('varExcelUrl')}}` | `@{variables('varExcelUrl')}` |
| `{{variables('varPdfUrl')}}` | `@{variables('varPdfUrl')}` |
| `{{if(greater(...), '❌ FAILED', '✅ PASSED')}}` | `@{if(greater(variables('varErrorCount'), 0), '❌ FAILED', '✅ PASSED')}` |
| `{{if(greater(...), 'Attention', 'Good')}}` | `@{if(greater(variables('varErrorCount'), 0), 'Attention', 'Good')}` |

---

## Teams 消息效果示例

**FAILED 场景（有 Error）：**

```
📊 数据验证报告                                    ❌ FAILED
2025-01-15 14:30:00

📁 源文件     tblOffshoring_2025Q1.xlsx
📋 总行数     250 行
❌ Error       3 条
⚠️ Warning    7 条
ℹ️ Info        2 条
📌 合计问题   12 条

📂 报告文件
[📊 查看 Excel 报告]  [📄 查看 PDF 报告]
```

**PASSED 场景（无 Error）：**

```
📊 数据验证报告                                    ✅ PASSED
2025-01-15 14:30:00

📁 源文件     tblOffshoring_2025Q1.xlsx
📋 总行数     250 行
❌ Error       0 条
⚠️ Warning    2 条
ℹ️ Info        5 条
📌 合计问题   7 条

📂 报告文件
[📊 查看 Excel 报告]  [📄 查看 PDF 报告]
```

---

## 错误通知模板（Flow 失败时）

当 Flow 运行异常时，在 "Configure run after" 的失败分支中发送以下简化通知。

> **注意**：`{{...}}` 为占位符，在 Power Automate 中需替换为 `@{...}` 格式的表达式（与主模板相同规则）。

```json
{
  "type": "AdaptiveCard",
  "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
  "version": "1.4",
  "body": [
    {
      "type": "TextBlock",
      "text": "🚨 数据验证 Flow 运行失败",
      "weight": "Bolder",
      "size": "Large",
      "color": "Attention"
    },
    {
      "type": "FactSet",
      "facts": [
        {
          "title": "时间",
          "value": "@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm:ss')}"
        },
        {
          "title": "源文件",
          "value": "@{triggerBody()?['file']?['name']}"
        },
        {
          "title": "错误信息",
          "value": "@{workflow()?['run']?['error']?['message']}"
        }
      ]
    }
  ]
}
```
