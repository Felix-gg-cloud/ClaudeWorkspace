# 03 — 安全操作与回滚方案

> **原则**：POC 阶段绝对不在生产报表正在使用的文件上直接操作，通过副本文件验证后再决定是否迁移。

---

## 一、POC 阶段推荐做法：使用 SharePoint 副本

### 1.1 创建 POC 副本文件

1. 打开 SharePoint，找到 `headcount_analysis.xlsx` 所在的文档库
2. 右键单击文件 → **复制到（Copy to）** → 选择 POC 专用文件夹（例如 `/POC/`）  
   **或** 直接下载 → 另存为 `headcount_analysis_poc.xlsx` → 上传到 POC 文件夹
3. 命名建议：`headcount_analysis_poc.xlsx`（加 `_poc` 后缀，防止混淆）

### 1.2 操作范围隔离

| 操作 | 对象 | 说明 |
|---|---|---|
| 创建 `tblOffshoring` | `headcount_analysis_poc.xlsx` | 只动 POC 副本 |
| Power Automate Flow | `headcount_analysis_poc.xlsx` | Flow 只连接 POC 副本 |
| Copilot Studio 测试 | `headcount_analysis_poc.xlsx` | Prompt 测试用 POC 副本数据 |
| Power BI 生产报表 | `headcount_analysis.xlsx`（原文件） | 保持不变，不受 POC 影响 |

### 1.3 POC 验证通过后的迁移决策

完成 POC 验证（Flow 能正确读取数据并输出 issues）后，再决定：
- **方案 A**：将 `tblOffshoring` 迁移到原文件（按 `04-powerbi-compatibility.md` 操作，先验证 Power BI 刷新）
- **方案 B**：保持 POC 副本文件作为校验专用文件，与生产 Power BI 文件分开维护

---

## 二、如果必须在原文件上操作（最低风险守则）

如果你决定不创建副本、直接在 `headcount_analysis.xlsx` 上建 Table，请严格遵守以下规则：

### 2.1 操作前必做

- [ ] 在本地下载一份 **手动备份**（另存为带日期的文件名，例如 `headcount_analysis_backup_20250115.xlsx`）
- [ ] 记录当前 Power BI 报表的"上次成功刷新时间"（作为基准）
- [ ] 记录 Excel 当前的 Sheet 名称列表，确认不会改变

### 2.2 操作中的约束

- ❌ **不要**改变 Sheet 名称（`Offshoring` 保持不变）
- ❌ **不要**改变任何列名（包括大小写、空格）
- ❌ **不要**在第 4 行（列名行）的上方插入新行
- ❌ **不要**合并或拆分列名行的单元格
- ✅ Table 名称固定为 `tblOffshoring`，后续不要改名
- ✅ 转成 Table 后，保存前先在本地 Excel Desktop 确认数据内容没有变化

### 2.3 操作后立即验证

建立 Table 并保存后，立即执行以下验证：

1. **Power BI Desktop 验证**：
   - 打开 Power BI Desktop
   - 选择"刷新（Refresh）"
   - 确认刷新成功，数据行数与之前一致
   - 若报错，立即按下方回滚步骤操作

2. **数据行数比对**：
   - 在 Excel 中，Table 底部状态栏会显示"记录数"
   - 与操作前的数据行数对比，确保一致

---

## 三、Power BI 刷新验证步骤

### 3.1 在 Power BI Desktop 中验证

1. 打开 Power BI Desktop，加载连接 SharePoint Excel 文件的报表
2. 点击 **主页 → 刷新（Refresh）**
3. 观察以下指标：
   - **刷新是否成功**（无错误提示）
   - **数据行数**：与历史一致（可与上次报告对比）
   - **关键度量值**：对比几个核心指标（如总 Headcount、Offshore 比例）是否与历史报告吻合

### 3.2 在 Power BI Service（云端）验证

1. 登录 Power BI Service（`app.powerbi.com`）
2. 找到对应数据集 → 点击 **"立即刷新（Refresh now）"**
3. 刷新完成后，检查报告视觉效果是否正常

### 3.3 Power Query 连接方式检查

如果刷新报错，需要检查 Power BI 的 Power Query 连接方式：

1. 在 Power BI Desktop：**主页 → 转换数据（Transform data）**
2. 找到连接 `headcount_analysis.xlsx` 的查询
3. 点击 **高级编辑器（Advanced Editor）**，查看 M 代码开头

**常见连接方式及对应影响：**

| 连接方式（M 代码关键词） | Ctrl+T 建 Table 的影响 |
|---|---|
| `Excel.Workbook(...){[Item="tblOffshoring"]}` | 直接读 Table，影响最小，通常无问题 |
| `Excel.Workbook(...){[Item="Offshoring",Kind="Sheet"]}` | 读整个 Sheet，建 Table 一般不影响；若 PQ 内有"跳过行/提升标题"等步骤需确认 |
| `Folder.Files(...)` | 文件夹读取，进而展开 Workbook/Sheet，影响类似上一行 |

---

## 四、回滚方案

### 4.1 回滚场景 A：仅删除 Table（保留数据）

如果你建了 Table，但 Power BI 刷新失败，可以把 Table 转回普通区域（不删除数据）：

1. 点击 `tblOffshoring` Table 内任意单元格
2. 顶部选择 **"表设计（Table Design）"** 选项卡
3. 点击 **"转换为区域（Convert to Range）"**
4. 弹出确认框，点击 **"是"**
5. 保存文件，重新在 Power BI Desktop 刷新验证

> 转换为区域后，数据内容完全不变，只是"Table 对象"被移除。筛选箭头和表格样式也会消失（你也可以在之前手动清除样式）。

### 4.2 回滚场景 B：从备份文件恢复

如果操作导致文件内容损坏或数据丢失：

1. 找到操作前下载的备份文件（`headcount_analysis_backup_YYYYMMDD.xlsx`）
2. 上传到 SharePoint，覆盖现有文件  
   （或重命名现有文件，再把备份文件以原名上传）
3. 在 Power BI Desktop 刷新验证

### 4.3 回滚场景 C：SharePoint 版本历史恢复

SharePoint 自动保存文件版本历史：

1. 在 SharePoint 找到该文件
2. 右键 → **"版本历史记录（Version history）"**
3. 找到操作前的版本（通过时间戳确认）
4. 点击该版本 → **"还原（Restore）"**

---

## 五、紧急联系与升级

如果 POC 操作导致生产 Power BI 报表数据异常：

1. 立即停止操作，不要继续保存任何修改
2. 按"回滚场景 C"使用 SharePoint 版本历史恢复到操作前状态
3. 在 Power BI Desktop 验证恢复成功
4. 记录问题现象（截图 / 错误信息）供后续分析
5. 通知相关利益方说明情况
