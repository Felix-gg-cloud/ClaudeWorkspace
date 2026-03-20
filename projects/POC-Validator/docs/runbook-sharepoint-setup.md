# POC-Validator：从个人 OneDrive 迁移到 SharePoint 共享账户 Runbook

> **版本**：v1.0  
> **状态**：生效中  
> **适用对象**：负责搭建/维护 POC-Validator Power Automate Flow 的管理员

---

## 目录

1. [背景与目标](#1-背景与目标)
2. [SharePoint 站点与文档库布局](#2-sharepoint-站点与文档库布局)
3. [创建目录结构（手动操作步骤）](#3-创建目录结构手动操作步骤)
4. [上传模板文件](#4-上传模板文件)
5. [Excel 模板规格：ValidationReportTemplate.xlsx](#5-excel-模板规格-validationreporttemplatexlsx)
6. [Power Automate：共享账户连接配置](#6-power-automate共享账户连接配置)
7. [Flow 步骤更新（从 OneDrive → SharePoint）](#7-flow-步骤更新从-onedrive--sharepoint)
8. [权限配置（最小权限原则）](#8-权限配置最小权限原则)
9. [Teams 通知配置](#9-teams-通知配置)
10. [迁移注意事项（从个人 OneDrive 路径迁移）](#10-迁移注意事项从个人-onedrive-路径迁移)
11. [常见问题排查](#11-常见问题排查)

---

## 1. 背景与目标

### 当前状态（已废弃）

POC 阶段使用**个人 OneDrive** 存储所有输入/输出文件，路径为：

```
Documents/PowerbiTest/POC-Validator/
├── Templates/ValidationReportTemplate.xlsx
├── Outputs/Excel/
├── Outputs/PDF/
└── Outputs/JSON/
```

**问题**：

- 个人账户离职/换岗后，Flow 连接失效
- 无法多人协作、无法审计访问记录
- 个人存储配额有限

### 目标状态

切换到**共享服务账户**（Shared/Service Account），使用 **SharePoint** 作为统一存储：

- 文件与账户解耦，运维连续性有保障
- 权限可集中管控（最小权限原则）
- 支持多人访问、审计日志完整

---

## 2. SharePoint 站点与文档库布局

### 2.1 推荐站点结构

```
SharePoint 站点（Team Site）
└── 文档库：Shared Documents
    └── POC-Validator/                    ← 项目根目录
        ├── Inputs/                       ← 输入 Excel 文件（含 tblOffshoring）
        ├── Templates/                    ← Excel 报告模板（只读，勿轻易修改）
        └── Outputs/
            ├── Excel/                    ← 生成的 Excel 报告
            ├── PDF/                      ← 生成的 PDF 报告
            └── JSON/                     ← （可选）结构化 JSON 输出
```

### 2.2 路径变量约定

在后续文档中，用以下占位符指代实际路径：

| 占位符 | 含义 | 示例值 |
|--------|------|--------|
| `{SITE_URL}` | SharePoint 站点 URL | `https://contoso.sharepoint.com/sites/TeamSite` |
| `{LIBRARY}` | 文档库名称 | `Shared Documents` |
| `{ROOT}` | 项目根目录（相对文档库） | `POC-Validator` |

> **建议**：请在本组织的 SharePoint admin 中确认站点 URL 和文档库名称，并将本文中的占位符替换为实际值后存档。

---

## 3. 创建目录结构（手动操作步骤）

使用**共享服务账户**登录 Microsoft 365，然后按以下步骤操作：

### 步骤 1：打开 SharePoint 文档库

1. 访问 `{SITE_URL}`
2. 点击左侧导航 **Documents**（或对应文档库名）
3. 确认当前文档库为 `{LIBRARY}`

### 步骤 2：创建项目根目录

1. 点击 **+ New → Folder**
2. 文件夹名称：`POC-Validator`
3. 点击 **Create**

### 步骤 3：创建子目录

进入 `POC-Validator/` 后，依次创建以下文件夹（每次 **+ New → Folder**）：

| 文件夹名称 | 用途 |
|-----------|------|
| `Inputs` | 放置输入 Excel 文件 |
| `Templates` | 放置报告模板（建议设只读权限） |
| `Outputs` | 输出根目录 |

进入 `Outputs/` 后，再创建：

| 文件夹名称 | 用途 |
|-----------|------|
| `Excel` | Excel 格式验证报告 |
| `PDF` | PDF 格式验证报告 |
| `JSON` | JSON 格式输出（可选） |

### 步骤 4：验证目录结构

完成后，文档库中应看到如下结构：

```
Shared Documents/
└── POC-Validator/
    ├── Inputs/
    ├── Templates/
    └── Outputs/
        ├── Excel/
        ├── PDF/
        └── JSON/
```

---

## 4. 上传模板文件

### 4.1 上传 ValidationReportTemplate.xlsx

1. 在 SharePoint 中进入 `POC-Validator/Templates/`
2. 点击 **Upload → Files**
3. 选择本地的 `ValidationReportTemplate.xlsx`
4. 上传完成后确认文件出现在目录中

> **模板要求**：请参考 [第 5 节](#5-excel-模板规格-validationreporttemplatexlsx) 确保模板满足 Power Automate 所需规格。

### 4.2 上传输入 Excel

1. 进入 `POC-Validator/Inputs/`
2. 上传包含 `tblOffshoring` 表的输入 Excel 文件
3. 建议文件名固定（如 `Master_Excel_Strategy_OnOffshoring.xlsx`），避免每次 Flow 都要手动改路径

---

## 5. Excel 模板规格：ValidationReportTemplate.xlsx

> **这是非二进制规格说明**。实际的 `.xlsx` 文件需在 Excel 桌面版或 Excel 网页版中按以下规格手动创建，然后上传至 SharePoint。

### 5.1 工作表（Sheets）

| Sheet 名称 | 用途 |
|-----------|------|
| `Summary` | 汇总指标（总行数、Error/Warning 数量、通过率、生成时间等） |
| `Issues` | 详细问题清单（Excel Table：`tblIssues`） |

### 5.2 Issues Sheet：tblIssues 表结构

在 `Issues` Sheet 的 A1 单元格开始，按以下顺序填入列头（**大小写与空格必须完全一致**）：

| 列序 | 列名 | 说明 |
|-----|------|------|
| A | `Severity` | 严重级别：`Error` / `Warning` / `Info` |
| B | `RuleId` | 规则编号 |
| C | `RowKey` | 数据行唯一标识 |
| D | `RowIndex` | 源文件中的行号 |
| E | `YearMonth` | 年月，格式 `YYYY-MM` |
| F | `Cost Center Number` | 成本中心编号（含空格） |
| G | `Function` | 职能部门 |
| H | `Team` | 团队 |
| I | `Owner` | 负责人 |
| J | `Column` | 触发问题的列名 |
| K | `RawValue` | 原始值 |
| L | `Message` | 问题描述 |
| M | `FixSuggestion` | 修复建议 |

### 5.3 创建 Excel Table（tblIssues）步骤

1. 在 `Issues` Sheet 的 A1:M1 填入上述列头
2. 选中 A1:M1
3. 点击 **插入（Insert） → 表格（Table）**
4. 勾选 **我的表具有标题（My table has headers）**，点击 **确定**
5. 选中表中任意单元格
6. 在 **表设计（Table Design）** 选项卡中，将 **表名称（Table Name）** 改为：`tblIssues`

> ⚠️ 表名必须为 `tblIssues`，Power Automate 的 **Add a row into a table** 动作按此名称定位。

### 5.4 Summary Sheet 建议预留格

| 单元格 | 内容 |
|--------|------|
| A1 | `生成时间` |
| B1 | （由 Flow 写入：ISO 8601 时间戳） |
| A2 | `输入文件` |
| B2 | （由 Flow 写入：输入文件名） |
| A3 | `批次年月` |
| B3 | （由 Flow 写入：YearMonth） |
| A4 | `总行数` |
| B4 | （由 Flow 写入：total_rows） |
| A5 | `Error 数` |
| B5 | （由 Flow 写入：error_count） |
| A6 | `Warning 数` |
| B6 | （由 Flow 写入：warning_count） |
| A7 | `通过率` |
| B7 | （由 Flow 写入：pass_rate，如 `98.5%`） |

### 5.5 可选格式建议

- **Issues Sheet**：条件格式（Severity = `Error` → 浅红色背景；`Warning` → 浅黄色背景）
- **Issues Sheet**：冻结首行（View → Freeze Panes → Freeze Top Row）
- 保存文件后上传至 `POC-Validator/Templates/`

---

## 6. Power Automate：共享账户连接配置

### 6.1 账户要求

| 项目 | 要求 |
|------|------|
| 账户类型 | 共享服务账户（非个人账户） |
| 授权方式 | 使用共享账户凭据在 Power Automate 中创建连接 |
| MFA | 建议为服务账户配置条件访问（免 MFA 豁免，仅限受信任设备/IP） |

### 6.2 在 Power Automate 中创建连接

1. 以**共享服务账户**登录 [Power Automate](https://make.powerautomate.com)
2. 点击左侧 **Data → Connections**
3. 依次创建以下连接（每个都要用共享账户登录授权）：

| 连接名称 | 连接器 | 用途 |
|---------|--------|------|
| SharePoint（共享账户） | SharePoint | 读写文件 |
| Excel Online (Business)（共享账户） | Excel Online (Business) | 读写 Excel |
| Microsoft Teams（共享账户） | Microsoft Teams | 发通知 |

4. 创建完成后，在每个连接的详情页确认状态为 **Connected**

### 6.3 导入/编辑 Flow 时替换连接

如果从旧 Flow 导入：
1. 打开 Flow 后，系统会提示 **"Fix connections"**
2. 将每个连接器对应的连接从**个人账户**改为**共享账户**对应的连接
3. 保存并测试

---

## 7. Flow 步骤更新（从 OneDrive → SharePoint）

以下是手动触发 Flow 的完整动作配置，按步骤逐一说明。

### 7.1 触发器

| 参数 | 值 |
|-----|-----|
| 触发器类型 | **Manually trigger a flow**（手动触发） |
| 可选输入 | `BatchYearMonth`（格式 `YYYY-MM`，用于文件命名） |

### 7.2 步骤 1：读取输入 Excel（SharePoint）

**动作**：`List rows present in a table`（Excel Online Business）

| 参数 | 值 |
|-----|-----|
| Location | `{SITE_URL}` |
| Document Library | `{LIBRARY}` |
| File | `/POC-Validator/Inputs/Master_Excel_Strategy_OnOffshoring.xlsx` |
| Table | `tblOffshoring` |
| Pagination（高级） | 开启，Threshold 设为 `100000` |

> 旧路径（已废弃）：`/Documents/PowerbiTest/POC-Validator/Inputs/...`（OneDrive）

### 7.3 步骤 2：获取模板内容（SharePoint）

**动作**：`Get file content using path`（SharePoint）

| 参数 | 值 |
|-----|-----|
| Site Address | `{SITE_URL}` |
| File Path | `/Shared Documents/POC-Validator/Templates/ValidationReportTemplate.xlsx` |

输出：`File Content`（用于下一步创建报告文件）

> 旧路径（已废弃）：`/Documents/PowerbiTest/POC-Validator/Templates/...`（OneDrive）

### 7.4 步骤 3：数据验证（业务逻辑，无需改动）

在此步骤中执行数据验证规则，生成 `report_model`，包含：

```json
{
  "metrics": {
    "total_rows": 0,
    "error_count": 0,
    "warning_count": 0,
    "pass_rate": "100%"
  },
  "issues": []
}
```

### 7.5 步骤 4：创建 Excel 报告文件（SharePoint）

**动作**：`Create file`（SharePoint）

| 参数 | 值 |
|-----|-----|
| Site Address | `{SITE_URL}` |
| Folder Path | `/Shared Documents/POC-Validator/Outputs/Excel/` |
| File Name | `ValidationReport_@{triggerBody()['BatchYearMonth']}_@{formatDateTime(utcNow(),'yyyyMMdd-HHmm')}.xlsx` |
| File Content | 上一步（Get file content）的 `File Content` |

输出：新建文件的 `ID`（用于后续 Excel 写入动作）

> 旧路径（已废弃）：`/Documents/PowerbiTest/POC-Validator/Outputs/Excel/`（OneDrive）

### 7.6 步骤 5：写入 Summary Sheet

**动作**：`Update a row`（Excel Online Business）或 `Run script`

| 参数 | 值 |
|-----|-----|
| Location | `{SITE_URL}` |
| Document Library | `{LIBRARY}` |
| File | 使用步骤 4 的 **File Identifier** |
| 写入单元格 | B1=生成时间，B2=输入文件名，B3=BatchYearMonth，B4=total_rows，B5=error_count，B6=warning_count，B7=pass_rate |

> 建议使用 **Office Scripts（Run script）** 动作批量写入，效率更高。

### 7.7 步骤 6：写入 Issues 明细（tblIssues）

**动作**：`Apply to each` → `Add a row into a table`（Excel Online Business）

外层：Apply to each，遍历 `report_model.issues[]`

| 参数（Add a row into a table） | 值 |
|------|-----|
| Location | `{SITE_URL}` |
| Document Library | `{LIBRARY}` |
| File | 步骤 4 的 **File Identifier** |
| Table | `tblIssues` |
| Severity | `@{items('Apply_to_each')['severity']}` |
| RuleId | `@{items('Apply_to_each')['rule_id']}` |
| RowKey | `@{items('Apply_to_each')['row_key']}` |
| RowIndex | `@{items('Apply_to_each')['row_index']}` |
| YearMonth | `@{items('Apply_to_each')['year_month']}` |
| Cost Center Number | `@{items('Apply_to_each')['cost_center_number']}` |
| Function | `@{items('Apply_to_each')['function']}` |
| Team | `@{items('Apply_to_each')['team']}` |
| Owner | `@{items('Apply_to_each')['owner']}` |
| Column | `@{items('Apply_to_each')['column']}` |
| RawValue | `@{items('Apply_to_each')['raw_value']}` |
| Message | `@{items('Apply_to_each')['message']}` |
| FixSuggestion | `@{items('Apply_to_each')['fix_suggestion']}` |

> ⚠️ Issues 数量超过 ~200 条时，循环写入会很慢。建议改用 Office Scripts 批量写入，详见 [常见问题排查](#11-常见问题排查)。

### 7.8 步骤 7：生成 PDF 报告（SharePoint）

**方案 A**：HTML → Word/PDF（需要 Plumsail 或其他 PDF 转换连接器）

**方案 B（推荐，无需第三方）**：

1. 使用 Office Scripts 将 Excel 另存为 PDF（需 Excel Online 支持）
2. 或使用 **Convert file**（OneDrive/SharePoint 连接器，支持 docx→pdf）

**动作**：`Create file`（SharePoint）写入 PDF

| 参数 | 值 |
|-----|-----|
| Site Address | `{SITE_URL}` |
| Folder Path | `/Shared Documents/POC-Validator/Outputs/PDF/` |
| File Name | `ValidationReport_@{triggerBody()['BatchYearMonth']}_@{formatDateTime(utcNow(),'yyyyMMdd-HHmm')}.pdf` |
| File Content | PDF 二进制内容 |

> 旧路径（已废弃）：`/Documents/PowerbiTest/POC-Validator/Outputs/PDF/`（OneDrive）

### 7.9 步骤 8：（可选）保存 JSON

**动作**：`Create file`（SharePoint）

| 参数 | 值 |
|-----|-----|
| Folder Path | `/Shared Documents/POC-Validator/Outputs/JSON/` |
| File Name | `ValidationReport_@{triggerBody()['BatchYearMonth']}_@{formatDateTime(utcNow(),'yyyyMMdd-HHmm')}.json` |
| File Content | JSON 序列化的 `report_model` |

### 7.10 步骤 9：Teams 通知

**动作**：`Post a message in a chat or channel`（Microsoft Teams）

| 参数 | 值 |
|-----|-----|
| Post as | **Flow bot** |
| Post in | **Channel** |
| Team | （选择目标 Team） |
| Channel | `poc-validator`（建议专用频道） |
| Message | 见下方模板 |

**消息模板**：

```
✅ POC Validator 报告已生成

📅 批次年月：@{triggerBody()['BatchYearMonth']}
🕐 生成时间：@{formatDateTime(utcNow(), 'yyyy-MM-dd HH:mm')} UTC

📊 汇总指标：
- 总行数：@{body('report_model')?['metrics']?['total_rows']}
- Error 数：@{body('report_model')?['metrics']?['error_count']}
- Warning 数：@{body('report_model')?['metrics']?['warning_count']}
- 通过率：@{body('report_model')?['metrics']?['pass_rate']}

📎 报告文件：
- 📊 Excel：[打开]({SITE_URL}/Shared Documents/POC-Validator/Outputs/Excel/)
- 📄 PDF：[打开]({SITE_URL}/Shared Documents/POC-Validator/Outputs/PDF/)
```

---

## 8. 权限配置（最小权限原则）

### 8.1 SharePoint 权限

| 对象 | 账户 | 权限级别 | 说明 |
|------|------|--------|------|
| `POC-Validator/Inputs/` | 共享服务账户 | **Edit** | 能读取输入文件 |
| `POC-Validator/Templates/` | 共享服务账户 | **Read** | 只需读取模板，不需写入 |
| `POC-Validator/Outputs/` | 共享服务账户 | **Edit** | 需要创建/写入报告文件 |
| `POC-Validator/Templates/` | 其他维护人员 | **Edit** | 更新模板时才需要 |

### 8.2 配置步骤

1. 在 SharePoint 中，右键 `POC-Validator/Templates/` → **Manage access**
2. 取消继承权限（Stop inheriting permissions）
3. 为共享服务账户设置 **Read** 权限
4. `Outputs/` 和 `Inputs/` 继承父目录权限（共享账户 **Edit** 即可）

### 8.3 Power Automate 环境权限

| 权限 | 说明 |
|------|------|
| Flow 所有者 | 设为团队 DL 或共享账户 |
| 运行历史 | 允许管理员查看（Power Platform Admin Center） |
| 连接共享 | 将共享账户的连接共享给其他 Flow 编辑者（Data → Connections → Share） |

---

## 9. Teams 通知配置

### 9.1 创建专用频道

建议在目标 Team 中创建专用频道 `poc-validator`，便于审计和通知管理。

### 9.2 用共享账户发消息

在 Teams 连接器中，选择 **Post as: Flow bot**（推荐），这样消息发送者显示为 "Power Automate"，不依赖任何个人账户。

如果需要以共享账户身份发消息，在连接配置时选择对应的 Teams 连接（共享账户已加入该 Team）。

---

## 10. 迁移注意事项（从个人 OneDrive 路径迁移）

### 10.1 路径映射对照表

| 旧路径（OneDrive，已废弃） | 新路径（SharePoint） |
|---------------------------|---------------------|
| `/Documents/PowerbiTest/POC-Validator/Templates/ValidationReportTemplate.xlsx` | `/Shared Documents/POC-Validator/Templates/ValidationReportTemplate.xlsx` |
| `/Documents/PowerbiTest/POC-Validator/Inputs/<filename>.xlsx` | `/Shared Documents/POC-Validator/Inputs/<filename>.xlsx` |
| `/Documents/PowerbiTest/POC-Validator/Outputs/Excel/` | `/Shared Documents/POC-Validator/Outputs/Excel/` |
| `/Documents/PowerbiTest/POC-Validator/Outputs/PDF/` | `/Shared Documents/POC-Validator/Outputs/PDF/` |
| `/Documents/PowerbiTest/POC-Validator/Outputs/JSON/` | `/Shared Documents/POC-Validator/Outputs/JSON/` |

### 10.2 连接器替换

| 旧连接器 | 新连接器 | 动作变化 |
|---------|---------|---------|
| OneDrive for Business | **SharePoint** | 动作名称略有不同（如 `Get file content using path` 参数从 `Path` 改为 `Site Address + File Path`） |
| Excel Online (Business)（个人账户） | **Excel Online (Business)**（共享账户连接） | 动作相同，更换 Location 为 SharePoint Site |

### 10.3 迁移检查清单

- [ ] 已在 SharePoint 创建完整目录结构（Inputs/Templates/Outputs/Excel/PDF/JSON）
- [ ] 已上传 `ValidationReportTemplate.xlsx`（含 `tblIssues` 表）至 `Templates/`
- [ ] 已上传输入 Excel（含 `tblOffshoring` 表）至 `Inputs/`
- [ ] 已用共享账户在 Power Automate 创建 SharePoint/Excel/Teams 连接
- [ ] Flow 中所有 OneDrive 动作已替换为 SharePoint 动作
- [ ] Flow 中所有路径已按对照表更新
- [ ] 已验证共享账户有 SharePoint 目录的 Edit 权限
- [ ] 已测试运行 Flow，确认报告文件正确写入 SharePoint
- [ ] 已通知相关团队使用新的 SharePoint 路径访问报告

---

## 11. 常见问题排查

### Q1：Flow 运行时提示 "File not found"

**原因**：路径填写有误，或文件不存在。

**排查步骤**：
1. 在 SharePoint 浏览器中手动确认文件路径
2. 检查 Flow 动作中的 Site Address 是否与实际站点 URL 完全匹配
3. 检查文件名大小写（SharePoint 路径区分大小写）
4. 确认共享账户有该目录的访问权限

### Q2：Excel 写入时提示 "Table not found"

**原因**：模板中的表名与 Flow 中配置的表名不一致。

**排查步骤**：
1. 在 Excel 中打开模板，确认 `Issues` Sheet 中的表名为 `tblIssues`（区分大小写）
2. 在 Flow 的 `Add a row into a table` 动作中，Table 参数选择或输入 `tblIssues`

### Q3：Issues 写入太慢（循环超时）

**原因**：循环写入每行都是一次 API 调用，数据量大时耗时较长。

**解决方案**：使用 **Office Scripts** 批量写入：
1. 在 Flow 中添加 `Run script`（Excel Online Business）动作
2. 脚本接受 JSON 数组，一次性将所有 issues 写入 `tblIssues`
3. 示例脚本见项目 `templates/` 目录的 `office-scripts/bulkInsertIssues.ts`（如已提供）

### Q4：连接提示 "Invalid connection"

**原因**：共享账户的连接已失效（令牌过期或密码更改）。

**排查步骤**：
1. 在 Power Automate → Data → Connections 中找到对应连接
2. 点击 **Fix connection** 或 **Edit**，用共享账户重新授权
3. 确保共享账户的 MFA 配置不影响非交互式登录

### Q5：Teams 消息无法发送

**原因**：共享账户未加入目标 Team，或 Teams 连接权限不足。

**排查步骤**：
1. 确认共享账户已加入目标 Team
2. 如果使用 **Post as: Flow bot**，则无需加入 Team，但需要 Teams 连接器正常授权
3. 检查 Flow 的 Teams 连接是否为共享账户

---

*文档维护：如需更新本 Runbook，请提交 PR 并更新版本号。*
