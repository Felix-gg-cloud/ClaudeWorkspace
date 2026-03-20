# Power Automate × Excel 操作手顺（Runbook）

> **适用范围**：使用 Power Automate「手动触发」流程读取存放在 OneDrive/SharePoint 上的 Excel 文件，并进行数据校验与报告输出。
>
> **本文件用途**：帮助开发者和运维人员配置 Power Automate 流程，使其能正确定位并读取指定的 Excel 文件。

---

## 目录

1. [目标 Excel 文件说明](#1-目标-excel-文件说明)
2. [OneDrive 个人站点 URL 与团队 SharePoint 文档库的区别](#2-onedrive-个人站点-url-与团队-sharepoint-文档库的区别)
3. [如何在 Power Automate 中定位并选择文件](#3-如何在-power-automate-中定位并选择文件)
4. [手动触发流程配置步骤](#4-手动触发流程配置步骤)
5. [报告输出格式（Excel / PDF）](#5-报告输出格式excel--pdf)
6. [迁移至团队 SharePoint 文档库（生产环境推荐）](#6-迁移至团队-sharepoint-文档库生产环境推荐)
7. [常见问题与排错](#7-常见问题与排错)

---

## 1. 目标 Excel 文件说明

当前开发/测试阶段使用的 Excel 文件位于 **个人 OneDrive**（IBM 企业账号），链接如下：

```
https://ibm-my.sharepoint.com/:x:/r/personal/guozg_cn_ibm_com/Documents/PowerbiTest/Master%20Excel_Strategy_OnOffshoring.xlsx?d=wcc0205562ce44e8c944e63e024cceba8&csf=1&web=1&e=VkomkR
```

> ⚠️ **注意**：上述链接为用户提供的**示例输入**，仅供开发/测试阶段参考。链接中的 `d=` 参数为 OneDrive 文件项 ID（非访问凭据），`e=` 参数为临时共享令牌——请勿将该链接或任何访问令牌硬编码进 Power Automate 流程配置或源代码。生产环境中应迁移至团队 SharePoint 文档库（详见 [第 6 节](#6-迁移至团队-sharepoint-文档库生产环境推荐)）。

**文件关键信息：**

| 属性 | 值 |
|------|-----|
| 文件名 | `Master Excel_Strategy_OnOffshoring.xlsx` |
| 所在路径（OneDrive 相对路径） | `/Documents/PowerbiTest/` |
| 所有者账号 | `guozg_cn_ibm_com`（个人 OneDrive） |
| SharePoint 主机 | `ibm-my.sharepoint.com` |
| 数据表名（参考） | `tblOffshoring`（根据实际工作簿内 Table 名称确认） |

---

## 2. OneDrive 个人站点 URL 与团队 SharePoint 文档库的区别

理解两者的差异对正确配置 Power Automate 连接器至关重要。

### 2.1 个人 OneDrive（ibm-my.sharepoint.com）

- **URL 特征**：域名包含 `-my`，路径包含 `/personal/<用户名>/`
- **示例**：`https://ibm-my.sharepoint.com/personal/guozg_cn_ibm_com/Documents/...`
- **访问控制**：文件默认归属个人账号，分享给他人需单独授权
- **Power Automate 连接器**：使用 **OneDrive for Business** 连接器
- **适用场景**：个人开发测试、临时文件存取
- **局限性**：
  - 账号离职/禁用后文件即不可访问
  - 跨团队自动化流程访问权限难以统一管理
  - 不适合作为生产环境的稳定数据源

### 2.2 团队 SharePoint 文档库

- **URL 特征**：域名为 `<tenant>.sharepoint.com`，路径包含站点名与文档库名
- **示例**：`https://ibm.sharepoint.com/sites/MyTeamSite/Shared%20Documents/...`
- **访问控制**：由团队管理员统一管理权限，支持角色/组授权
- **Power Automate 连接器**：使用 **SharePoint** 连接器
- **适用场景**：生产环境、多人协作、自动化流程的稳定数据源
- **优势**：
  - 不依赖特定个人账号
  - 权限可精细控制（读取/编辑/删除）
  - 支持版本历史、审计日志

### 2.3 选择建议

```
开发/测试阶段  →  个人 OneDrive（OneDrive for Business 连接器）
生产环境       →  团队 SharePoint 文档库（SharePoint 连接器）
```

---

## 3. 如何在 Power Automate 中定位并选择文件

### 3.1 通过分享链接找到文件路径

1. 在浏览器中打开目标文件的分享链接：
   ```
   https://ibm-my.sharepoint.com/:x:/r/personal/guozg_cn_ibm_com/Documents/PowerbiTest/Master%20Excel_Strategy_OnOffshoring.xlsx?d=wcc0205562ce44e8c944e63e024cceba8&csf=1&web=1&e=VkomkR
   ```
2. 文件在 OneDrive 中打开后，点击顶部「打开」→「在 OneDrive 中查看」（或直接从 OneDrive 网页界面导航）
3. 记录文件的相对路径：`/Documents/PowerbiTest/Master Excel_Strategy_OnOffshoring.xlsx`

### 3.2 在 Power Automate 文件选择器中定位文件

当 Power Automate 操作需要选择文件时（例如 "Excel Online (Business)" 连接器的 "File" 参数）：

1. 在 **Location（位置）** 下拉框中选择：
   - 个人文件选 `OneDrive for Business`
   - 团队文件选对应的 SharePoint 站点
2. 在 **Document Library（文档库）** 中选择：
   - 个人 OneDrive 对应 `OneDrive`
3. 在 **File（文件）** 文件选择器中，导航到：
   ```
   /Documents/PowerbiTest/Master Excel_Strategy_OnOffshoring.xlsx
   ```
4. 选择文件后，Power Automate 会自动填充文件的内部 ID（`driveItem` ID），无需手动粘贴分享链接

> 💡 **提示**：Power Automate 使用文件的内部 Graph API ID，而非网页分享链接。分享链接仅用于在浏览器中**定位和确认**文件位置，之后通过文件选择器完成配置。

---

## 4. 手动触发流程配置步骤

以下步骤演示如何创建一个「手动触发」的 Power Automate 流程来读取上述 Excel 文件。

### 4.1 创建流程

1. 登录 [Power Automate](https://make.powerautomate.com/)
2. 点击「创建」→「即时云端流」（Instant cloud flow）
3. 触发器选择「手动触发流」（Manually trigger a flow）
4. 为流程命名，例如：`Excel_Validation_ManualTrigger`

### 4.2 添加「列出表中存在的行」步骤

1. 点击「新建步骤」
2. 搜索并选择 **Excel Online (Business)**
3. 选择操作：**列出表中存在的行**（List rows present in a table）
4. 配置参数：

   | 参数 | 配置值 |
   |------|--------|
   | **Location** | `OneDrive for Business` |
   | **Document Library** | `OneDrive` |
   | **File** | 通过文件选择器导航至 `/Documents/PowerbiTest/Master Excel_Strategy_OnOffshoring.xlsx` |
   | **Table** | 选择工作簿内的 Table 名称（如 `tblOffshoring`） |

5. 可选：在「显示高级选项」中设置分页（`$top` / `$skip`）以处理大数据量

### 4.3 添加数据校验步骤

参考 Copilot Studio 提示词中定义的校验规则（见 `prompts/` 目录），添加对应的条件判断和错误记录步骤。

**关键校验规则提醒：**
- **R-WS-ALL-001（Error）**：所有列的值不允许存在前后空格（leading/trailing whitespace）
- 必填字段（YearMonth、Owner 等）不允许为空或仅含空格
- ShoringRatio 必须为合法的百分比格式

### 4.4 配置报告输出

根据用户需求，报告以 **Excel（.xlsx）** 或 **PDF** 格式输出（详见 [第 5 节](#5-报告输出格式excel--pdf)），避免使用 Markdown 格式。

### 4.5 保存并测试

1. 点击「保存」
2. 点击「测试」→「手动」→「运行流」
3. 检查运行历史，确认每一步的输入/输出正确

---

## 5. 报告输出格式（Excel / PDF）

> 背景：用户要求不使用 Markdown 作为交付报告格式，因为普通用户可能没有对应的查看工具。应改用 **Excel（.xlsx）** 或 **PDF** 等通用办公格式。

### 5.1 方案 A（推荐）：Excel 报告

**适用场景**：需要进一步筛选、排序或处理校验结果的场景。

**输出文件命名建议**：
```
ValidationReport_<YYYYMMDD>_<HHMMSS>.xlsx
```

**工作表结构建议**：

| 工作表名 | 内容 |
|---------|------|
| `Summary` | 校验汇总：总行数、Error 数、Warning 数、通过率 |
| `ErrorDetail` | 每条 Error 的行号、列名、原始值、错误规则、修复建议 |
| `WarningDetail` | 每条 Warning 的行号、列名、原始值、警告规则 |
| `RawData` | 原始导入数据（可选，便于对照） |

**Power Automate 实现方式**：
- 使用 **Excel Online (Business)** 连接器的「将行添加到表中」操作
- 或使用 OneDrive 连接器上传由 Office Script / Python 生成的 .xlsx 文件

### 5.2 方案 B：PDF 报告

**适用场景**：需要打印或正式分发校验报告的场景。

**实现方式**：
1. 先生成 HTML 报告内容（在 Power Automate 中用「撰写」步骤构建 HTML 字符串）
2. 使用 **OneDrive for Business** 连接器将 HTML 保存为临时文件
3. 使用 **Word Online** 或 **PDF 转换**服务生成 PDF
4. 将 PDF 上传至 SharePoint/OneDrive 并通过邮件/Teams 通知发送链接

---

## 6. 迁移至团队 SharePoint 文档库（生产环境推荐）

### 6.1 为什么要迁移

| 问题 | 个人 OneDrive | 团队 SharePoint |
|------|--------------|----------------|
| 文件可用性 | 依赖个人账号状态 | 独立于个人账号 |
| 权限管理 | 需逐个分享 | 统一角色/组管理 |
| 审计日志 | 有限 | 完整 |
| 自动化稳定性 | 低（账号变更即失效） | 高 |

### 6.2 迁移步骤

1. **在团队 SharePoint 站点创建目标文档库**
   - 进入团队 SharePoint 站点（例如 `https://ibm.sharepoint.com/sites/<YourTeamSite>`）
   - 创建文档库，例如命名为 `DataSources`

2. **迁移文件**
   - 从个人 OneDrive 下载文件：`Master Excel_Strategy_OnOffshoring.xlsx`
   - 上传至团队 SharePoint 文档库的指定路径，例如：
     ```
     /sites/<YourTeamSite>/DataSources/PowerbiTest/Master Excel_Strategy_OnOffshoring.xlsx
     ```

3. **更新 Power Automate 流程中的文件引用**
   - 将 Excel Online (Business) 连接器的 **Location** 从 `OneDrive for Business` 改为对应的 SharePoint 站点
   - 将 **Document Library** 改为 `DataSources`（或实际库名）
   - 重新用文件选择器定位文件

4. **验证权限**
   - 确认流程运行账号对目标文档库拥有至少**读取**权限
   - 如需写入报告，需要**贡献**权限

5. **更新此 Runbook**
   - 将本节中的示例路径替换为实际生产路径
   - 更新团队 SharePoint URL 和文档库名称

### 6.3 迁移后的 Power Automate 配置参考

| 参数 | 生产环境配置值（示例） |
|------|----------------------|
| **Location** | `https://ibm.sharepoint.com/sites/<YourTeamSite>` |
| **Document Library** | `DataSources` |
| **File** | `/PowerbiTest/Master Excel_Strategy_OnOffshoring.xlsx` |
| **Table** | `tblOffshoring` |

---

## 7. 常见问题与排错

### Q1：Power Automate 报错「找不到文件」

**可能原因**：
- 文件已被移动或重命名
- 连接账号对文件没有访问权限

**解决方法**：
1. 确认文件仍在 `/Documents/PowerbiTest/` 路径下
2. 在 OneDrive 网页界面用分享链接验证文件可正常打开
3. 检查 Power Automate 连接使用的账号是否与文件所有者相同（或已获授权）

### Q2：分享链接无法在 Power Automate 中直接使用

**说明**：Power Automate 的 Excel Online 连接器**不接受网页分享链接**作为文件参数。分享链接（`https://ibm-my.sharepoint.com/:x:/r/...`）仅用于浏览器访问，连接器需要通过**文件选择器**（Graph API driveItem ID）来引用文件。

**解决方法**：在连接器配置中使用文件选择器 UI 导航选择文件，而非手动粘贴链接。

### Q3：读取到的数据行数与 Excel 不符

**可能原因**：
- 「列出表中存在的行」默认最多返回 256 行（旧版）或受分页限制
- 工作簿中 Table 范围定义不正确

**解决方法**：
1. 在步骤的「显示高级选项」中启用分页：设置 `$top` 为较大值（如 5000）
2. 或在 Excel 文件中确认 Table 的命名范围覆盖所有数据行

### Q4：如何确认 Excel 文件中的 Table 名称

1. 在 Excel 中打开文件
2. 点击数据区域内任意单元格
3. 在「表设计」（Table Design）选项卡中查看「表名称」字段
4. 将该名称填入 Power Automate 配置的 **Table** 参数

---

## 附录：相关资源

- [Power Automate 官方文档](https://learn.microsoft.com/zh-cn/power-automate/)
- [Excel Online (Business) 连接器参考](https://learn.microsoft.com/zh-cn/connectors/excelonlinebusiness/)
- [OneDrive for Business 连接器参考](https://learn.microsoft.com/zh-cn/connectors/onedriveforbusiness/)
- [SharePoint 连接器参考](https://learn.microsoft.com/zh-cn/connectors/sharepointonline/)
- [Microsoft Graph API - DriveItem 参考](https://learn.microsoft.com/zh-cn/graph/api/resources/driveitem)
