# OneDrive 与 SharePoint 的差异说明

## 1. 基本概念对比

| 维度 | OneDrive | SharePoint |
|------|----------|------------|
| **定位** | 个人云存储（类似"我的文件夹"） | 团队协作平台（类似"共享文件服务器"） |
| **访问范围** | 默认仅文件拥有者；可手动共享 | 默认对站点成员可见；有精细权限管理 |
| **版本控制** | 支持基本版本历史 | 支持完整版本历史 + 审批流 |
| **适合场景** | 个人起草、临时存储、简单共享 | 正式文件归档、团队协作、流程集成 |
| **Power Automate 连接器** | `OneDrive for Business` | `SharePoint` |
| **URL 特征** | `https://<tenant>-my.sharepoint.com/personal/<user>/` | `https://<tenant>.sharepoint.com/sites/<site>/` |

---

## 2. 本 POC 的使用方式

```
┌─────────────────────────────────────────────────────────────────┐
│                        POC 数据流                                │
│                                                                 │
│  填表人                 Power Automate             团队成员      │
│  ┌──────┐              ┌───────────┐              ┌──────────┐  │
│  │      │  手动填写     │ 手动触发  │  校验报告     │          │  │
│  │ 用户 │──────────→   │  PA 流程  │─────────────→│SharePoint│  │
│  │      │              │           │              │ /Reports │  │
│  └──────┘              └─────┬─────┘              └──────────┘  │
│                              │ 读取原始行                        │
│                              ↓                                  │
│                    ┌─────────────────┐                         │
│                    │    OneDrive     │                          │
│                    │ tblOffshoring   │                          │
│                    │    .xlsx        │                          │
│                    └─────────────────┘                         │
└─────────────────────────────────────────────────────────────────┘
```

### 数据来源（OneDrive）
- **位置**：OneDrive for Business，路径 `/Offshoring/tblOffshoring.xlsx`  
  （或共享 OneDrive 文件夹，由团队管理员配置）
- **理由**：填表人可以直接用 Excel Online 在线编辑，操作门槛低
- **注意**：Power Automate 使用 `OneDrive for Business` 连接器读取此文件

### 报告输出（SharePoint）
- **位置**：SharePoint 站点 → 文档库 `/Reports/Offshoring/`
- **理由**：
  - 团队成员均可访问，不依赖特定人员的 OneDrive 权限
  - 支持版本控制，方便追溯历史报告
  - 可与 Teams 频道集成，自动发送链接通知

---

## 3. 文件路径约定

| 文件 | 存放位置 | 完整路径示例 |
|------|----------|-------------|
| `tblOffshoring.xlsx` | OneDrive | `/Offshoring/tblOffshoring.xlsx` |
| `ValidationReport_202501_20250115T0830.xlsx` | SharePoint | `/sites/<SiteName>/Reports/Offshoring/ValidationReport_202501_20250115T0830.xlsx` |
| `ValidationReport_202501_20250115T0830.pdf` | SharePoint | `/sites/<SiteName>/Reports/Offshoring/ValidationReport_202501_20250115T0830.pdf` |

> `<SiteName>` 由各团队在 SharePoint 管理员处确认。

---

## 4. 迁移路径（OneDrive → SharePoint）

POC 阶段允许将 `tblOffshoring.xlsx` 存放在 OneDrive；**正式上线后建议将数据源也迁移到 SharePoint**，原因如下：
- 避免单点依赖（OneDrive 文件拥有者离职/变更时访问中断）
- 统一权限管理，IT 部门维护更便利
- Power Automate 流程不需要修改大量逻辑（SharePoint 连接器同样支持读取 Excel）

### 迁移步骤

| 步骤 | 操作 | 负责人 |
|------|------|-------|
| 1 | 在 SharePoint 站点下创建 `/Data/Offshoring/` 文件夹 | SharePoint 管理员 |
| 2 | 将 `tblOffshoring.xlsx` 从 OneDrive 移动到 SharePoint | 流程负责人 |
| 3 | 在 Power Automate 中把 `OneDrive for Business - 获取文件内容` 步骤改为 `SharePoint - 获取文件内容` | 流程开发者 |
| 4 | 更新 `List rows present in a table` 步骤的连接器（OneDrive → SharePoint） | 流程开发者 |
| 5 | 通知填表人新的文件地址 | 项目负责人 |
| 6 | 验证流程可正常读取数据并产出报告 | 流程开发者 |

> 迁移后 Power Automate 流程的主要变更：将数据来源步骤的连接器从 `OneDrive for Business` 改为 `SharePoint`，其余步骤不变。

---

## 5. 常见问题

**Q：POC 阶段 OneDrive 文件是"个人 OneDrive"还是"共享 OneDrive"？**  
A：推荐使用流程负责人的"OneDrive for Business（工作账号）"，并将文件夹共享给 Power Automate 运行账号；避免使用个人 Microsoft 账号的 OneDrive（不支持工作账号连接器）。

**Q：Power Automate 可以直接读取 OneDrive 的 Excel 吗？**  
A：可以，使用 `OneDrive for Business` 连接器 → `List rows present in a table` 动作即可读取命名表 `tblOffshoring`。

**Q：输出的报告为什么不放 OneDrive？**  
A：可以放，但 SharePoint 对团队共享更友好。如果 POC 阶段 SharePoint 权限申请较慢，可临时将报告写入共享 OneDrive 文件夹，正式上线前再迁移。
