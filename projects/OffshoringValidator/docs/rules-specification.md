# 校验规则完整规范 — Offshoring POC 校验器

> **版本**：v1.0  
> **适用表**：tblOffshoring  
> **校验模式**：AI-only（Power Automate 机械读取，AI 负责全部校验逻辑）

---

## 1. 全局约定

### 1.1 Power Automate 读取约定
- Power Automate **不对任何字段做 trim、toLower、replace 等处理**
- 读取什么，原样传给 AI，包括前后空格、特殊字符等
- 目的：让 AI 能发现用户误输入的低级错误（如多输了空格）

### 1.2 空值定义
- 对**必填校验**：使用 `isBlank(trim(RawValue))` 判断空，即空字符串或纯空白字符均视为空
- 对**空格规则**：只要 `RawValue != trim(RawValue)` 就触发（不管 trim 后是否为空）

### 1.3 总行（Total 行）定义
- `Function` 字段的原始值，经 trim 后严格等于 `"Total"` 的行为 Total 行
- Total 行在部分规则下有不同要求（见具体规则）

### 1.4 规则执行顺序与去重
- **R-WS-ALL-001 最高优先级**：同一列触发空格规则后，该列的格式/白名单/数值规则不再重复报告
- 跨列规则（如 R-MAP-FT-001）在任何情况下都需报告
- 同一行同一列只报一条 issue（取触发的第一条规则）

---

## 2. 规则详细说明

### R-WS-ALL-001 — 全列前后空格禁止

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 所有列，所有行 |
| 优先级 | 最高 |

**触发条件**：
```
RawValue 非空字符串（不是 null / ""）
AND RawValue ≠ trim(RawValue)
（即存在前导或尾随空白字符，包括普通空格、制表符等）
```

**不触发（豁免）**：
- RawValue 为空字符串 ""

**示例**：

| RawValue | 触发？| 说明 |
|----------|-------|------|
| `"GBS "` | ✅ 触发 | 尾部有1个空格 |
| `" GBS"` | ✅ 触发 | 前置有1个空格 |
| `" GBS "` | ✅ 触发 | 前后均有空格 |
| `"GBS"` | ❌ 不触发 | 无多余空格 |
| `""` | ❌ 不触发 | 空值，由必填规则处理 |
| `"   "` | ✅ 触发 | 纯空格（非空字符串，且有前后空格）|

---

### R-YM-001 — YearMonth 必填

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 所有行 |
| 适用列 | YearMonth |

**触发条件**：`isBlank(trim(YearMonth))`（即为空或纯空格）

> 若 RawValue 含前后空格（如 `" "`），同时触发 R-WS-ALL-001 和 R-YM-001（两条都报）

---

### R-YM-FMT-001 — YearMonth 格式校验

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | YearMonth 非空（trim 后）且未触发 R-WS-ALL-001 |

**触发条件**：
1. 不满足正则：`^\d{6}$`（必须恰好6位数字）
2. 或月份部分（后两位）不在 01–12 范围内（00 和 13-99 均非法）

**合法示例**：`202501`, `202412`, `202601`  
**非法示例**：`2025-01`（含横线），`20251`（5位），`202500`（月份 00），`202513`（月份 13），`202501 `（有空格，但先触发 R-WS-ALL-001）

---

### R-REQ-CCN-001 — Cost Center Number 必填

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 所有行 |
| 适用列 | Cost Center Number |

**触发条件**：`isBlank(trim(Cost Center Number))`

---

### R-REQ-FUNC-001 — Function 必填

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 所有行 |
| 适用列 | Function |

**触发条件**：`isBlank(trim(Function))`

---

### R-FUNC-WL-001 — Function 白名单

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | Function 非空（trim 后）且未触发 R-WS-ALL-001 |
| 适用列 | Function |

**合法值（严格区分大小写）**：

```json
["GBS", "IT", "Finance", "HR", "Legal", "Procurement", "Total"]
```

**说明**：`Total` 是合法的 Function 值（汇总行标识）。

**触发条件**：`trim(Function) 不在上述合法值列表中`

---

### R-REQ-TEAM-001 — Team 必填（非 Total 行）

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 非 Total 行（`trim(Function) ≠ "Total"`） |
| 适用列 | Team |

**触发条件**：`isBlank(trim(Team))`

**豁免**：Total 行（`trim(Function) == "Total"`）的 Team 可以为空，不报错。

---

### R-TEAM-WL-001 — Team 白名单（非 Total 行）

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 非 Total 行，Team 非空（trim 后）且未触发 R-WS-ALL-001 |
| 适用列 | Team |

**合法值（严格区分大小写）**：

```json
["EA", "APAC", "EMEA", "Americas", "Global"]
```

---

### R-MAP-FT-001 — Function-Team 映射

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | Function 和 Team 都非空（trim 后）|
| 适用列 | Function + Team（跨列规则）|

**允许的 Function-Team 组合**：

```
Function=Total  + Team=""             → 合法（Total 行，Team 为空）
Function=Total  + Team∈白名单        → 合法（Total 行，Team 有值但在白名单内）
Function=GBS    + Team∈{EA,APAC,EMEA,Americas,Global}  → 合法
Function=IT     + Team∈{EA,APAC,EMEA,Americas,Global}  → 合法
Function=Finance + Team∈{EA,APAC,EMEA,Americas,Global} → 合法
Function=HR     + Team∈{EA,APAC,EMEA}                  → 合法
Function=Legal  + Team∈{EA,APAC,EMEA,Americas}         → 合法
Function=Procurement + Team∈{Global,APAC,EMEA}         → 合法
```

**触发条件**：`(trim(Function), trim(Team)) 不在上述合法组合中`

**特别注意**：
- R-MAP-FT-001 在 R-WS-ALL-001 触发后也必须报告（因为是跨列规则，用于提示映射也不匹配）

---

### R-REQ-OWNER-001 — Owner 必填

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 所有行（包括 Total 行）|
| 适用列 | Owner |

**触发条件**：`isBlank(trim(Owner))`

---

### R-NUM-001 — 数值列校验

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 数值列（见下方清单）|

**适用列**：
- Actual Headcount Onshore
- Actual Headcount Offshore
- Planned Headcount Onshore
- Planned Headcount Offshore
- Target Headcount Onshore
- Target Headcount Offshore

**校验逻辑**：

```
1. 若 isBlank(trim(RawValue)) → 跳过（允许为空）
2. 若该列已触发 R-WS-ALL-001 → 跳过本规则
3. 否则：尝试将 RawValue 解析为数字
   a. 若解析失败（含文字、特殊字符等）→ Error
      Message：「列 X 的值「abc」不是有效数字。」
   b. 若解析成功但 value < 0 → Error
      Message：「列 X 的值「-1」不能为负数，需 ≥ 0。」
   c. 若解析成功且 value >= 0 → 通过
```

**合法示例**：`10`, `0`, `3.5`, `100`, `` (空)  
**非法示例**：`-1`（负数），`abc`（文字），`1,000`（含逗号），`N/A`（文字）

---

### R-SR-001 — ShoringRatio 格式

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | ShoringRatio 非空（trim 后）且未触发 R-WS-ALL-001 |
| 适用列 | ShoringRatio |

**合法格式**：正则 `^\d+(\.\d+)?%$`，且数值部分 0 ≤ 值 ≤ 100

**合法示例**：`0%`, `25%`, `12.5%`, `100%`  
**非法示例**：
- `25 %`（空格，但先触发 R-WS-ALL-001）
- `0.25`（无 % 符号）
- `25`（无 % 符号）
- `25％`（全角 % 符号）
- `110%`（超过 100）
- `-5%`（负数）

---

### R-SR-REQ-001 — ShoringRatio 非 Total 行必填

| 属性 | 值 |
|------|---|
| 严重级别 | **Error** |
| 适用 | 非 Total 行 |
| 适用列 | ShoringRatio |

**触发条件**：`trim(Function) ≠ "Total" AND isBlank(trim(ShoringRatio))`

**豁免**：Total 行的 ShoringRatio 可以为空。

---

## 3. 规则触发优先级与去重示例

### 示例一：Function 值为 `"GBS "` (末尾有空格)

| 规则 | 触发？| 说明 |
|------|-------|------|
| R-WS-ALL-001 | ✅ 触发 | 末尾有空格 |
| R-FUNC-WL-001 | ❌ 不触发 | 因 R-WS-ALL-001 已触发，同列格式规则去重 |
| R-MAP-FT-001 | ✅ 触发 | 跨列规则，不受去重影响（映射也不匹配）|

### 示例二：YearMonth 值为 `" 202513"` (前置空格+非法月份)

| 规则 | 触发？| 说明 |
|------|-------|------|
| R-WS-ALL-001 | ✅ 触发 | 前置有空格 |
| R-YM-FMT-001 | ❌ 不触发 | 因 R-WS-ALL-001 已触发，格式规则去重 |
| R-YM-001 | ❌ 不触发 | trim 后非空（`"202513"` 非空），不触发必填 |

### 示例三：YearMonth 值为 `"  "` (纯空格)

| 规则 | 触发？| 说明 |
|------|-------|------|
| R-WS-ALL-001 | ✅ 触发 | 含空白字符且 RawValue ≠ trim(RawValue) |
| R-YM-001 | ✅ 触发 | trim 后为空，必填规则触发（必填规则不受去重限制，属于跨类型规则）|

### 示例四：Actual Headcount Onshore 值为 `""` (空)

| 规则 | 触发？| 说明 |
|------|-------|------|
| R-WS-ALL-001 | ❌ | RawValue 为 "" 不触发 |
| R-NUM-001 | ❌ | 空值跳过数值校验 |

---

## 4. RowKey 格式规范

**格式**：
```
RowIndex=<n>|YearMonth=<raw>|CCN=<raw>|Function=<raw>|Team=<raw>
```

**说明**：
- `RowIndex`：行在输入数组中的序号，从 1 开始（不含表头）
- `YearMonth`, `CCN`, `Function`, `Team`：使用 RawValue（原始值，含空格）
- 若某字段为空，则留空，例如 `Team=`

**示例**：
```
RowIndex=3|YearMonth=202501|CCN=CC001|Function=GBS|Team=EA
RowIndex=7|YearMonth=202501|CCN=CC002|Function=Total|Team=
RowIndex=5|YearMonth=202501|CCN=CC001|Function=GBS |Team=EA
```
（第三个示例中 Function 有尾部空格，原样保留在 RowKey 中）
