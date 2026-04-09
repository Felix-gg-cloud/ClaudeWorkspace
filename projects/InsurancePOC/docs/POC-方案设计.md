# 保险客户 KPI 审核 POC 方案设计

> 版本：v1.0 | 状态：POC 阶段

## 背景

保险客户现有 Power BI 报表，数据源分两部分：

1. **云端 SQL View**：定型后稳定，基本不会有错误
2. **Offline 手工 Excel**：每月人工填入新数据，上传至 SharePoint；因填写不规范，经常导致 KPI 展示不正确

**两个核心 Use Case：**

| # | Use Case | 描述 |
|---|---|---|
| 1 | **数据校验** | 每月新数据上传后，对比上月数据，列出不合规/异常差异（在数据进入 Power BI 之前） |
| 2 | **月度对比 Insights** | 对比 Power BI 展示的当月与上月 KPI 加工结果，生成分析报告 |

---

## POC 阶段架构（2 周内可交付）

```
手工填写 Excel → 上传 SharePoint
                       ↓
            Power Automate 触发读取
                       ↓
           Copilot Studio "KPI Reviewer"
                  ↙           ↘
       Topic 1: 数据校验    Topic 2: 月度对比
          ↓                      ↓
   Generative AI 节点      Generative AI 节点
   Prompt: 找出规则异常     Prompt: 对比两月差异
          ↓                      ↓
   返回: 异常清单（文字）   返回: 对比报告（可发 Teams）
```

### 数据流详述

#### Topic 1：数据校验（Use Case 1）

1. 用户在 Copilot Studio 中发送："帮我检查这个文件" + SharePoint 文件 URL
2. Power Automate Flow 接收请求，通过 SharePoint connector 读取指定 Excel
3. **预处理**（关键！避免 token 超限）：
   - 检测空值/格式异常行 → 仅传递候选异常行给 AI
   - 不要把整表原样传给 LLM
4. Generative AI 节点执行规则校验 Prompt
5. 返回结构化异常清单

#### Topic 2：月度对比（Use Case 2）

1. 用户："对比 2 月和 3 月数据"
2. Power Automate 读取两份 Excel（或同文件两个 sheet）
3. **预处理**：按主键对齐，计算变化量/变化率，仅传递 Top N 变动项
4. Generative AI 节点生成对比洞察
5. 返回报告（可选：发送到 Teams Channel）

---

## 关键设计决策

### 为什么要"预处理再给 AI"而不是"直接喂 Excel"

| 直接喂 Excel | 预处理后喂结构化数据 |
|---|---|
| Token 超限风险高 | 可控，仅传候选数据 |
| 输出不稳定 | 输出格式稳定 |
| 无法审计"为什么这行有问题" | 规则清晰可解释 |
| 客户难以信任 AI 判断 | 规则+AI 解释，双重背书 |

### 主键设计（务必先确认）

对齐上月数据的关键。建议主键组合为：

```
(月份, 机构代码/Cost Center, 产品代码)
```

如果 Excel 中无法唯一定位一行，Use Case 2 的"逐行对比"将无法实现，退化为"整体 KPI 对比"。

---

## POC 文件组织建议

```
SharePoint 文档库：/POC-Data/
├── 2025-01-data.xlsx    # 各月原始数据文件
├── 2025-02-data.xlsx
├── 2025-03-data.xlsx    # 最新月
└── PowerBI-Summary/
    ├── 2025-02-summary.xlsx   # Power BI Export Data 导出
    └── 2025-03-summary.xlsx
```

> **重要**：POC 阶段使用单独文件/文档库，不要修改 Power BI 正在读取的生产 Excel。
> 详见：[Excel Table 转换与 Power Query 影响分析](./Excel-Table-转换与PowerBI-Power-Query影响分析.md)

---

## 常见规则示例（需与客户确认后固化）

| 规则类型 | 示例 |
|---|---|
| 必填 | 月份、Cost Center、产品代码、KPI 数值不能为空 |
| 枚举 | Cost Center 必须在白名单范围内 |
| 范围 | 费率 0~1；人数 ≥ 0 且为整数 |
| 跨字段 | 若"是否退保=是"，则"退保金额 > 0" |
| 主键唯一 | (月份, Cost Center, 产品) 组合唯一 |
| MoM 异常 | 同指标变化率 > ±20% 标记为异常 |

---

## 中期演进路径（正式上线）

```
Power Automate 定时触发
        ↓
从数据库自动拉取数据 → 写入 SharePoint
        ↓
Agent 每月自动触发 → 完整对比分析 → 推送报告到 Teams/邮件
```

---

## 客户确认清单（开始开发前必须确认）

- [ ] Excel 主键字段是什么？（用于上下月行对齐）
- [ ] "不合规"的具体规则有哪些？（枚举、范围、必填、跨字段）
- [ ] 上月数据存放方式：每月一个文件 or 同文件追加？
- [ ] 是否可以建立 POC 专用 SharePoint 文档库（与生产隔离）
- [ ] Power BI 版本：Pro / Premium？（影响 KPI Snapshot 自动化方式）
- [ ] 报告输出形式：Teams 消息 / 邮件 / SharePoint 页面 / Word 文件

---

## 相关文档

- [Excel Table 转换与 Power Query 影响分析](./Excel-Table-转换与PowerBI-Power-Query影响分析.md)
