# POC Headcount Validator — 文件命名规范与 SharePoint 目录结构

---

## 1. 文件命名规范

### 1.1 校验报告文件（输出）

**格式**：

```
ValidationReport_<YYYYMMDD>_<HHmm>_<RunLabel>.<ext>
```

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `<YYYYMMDD>` | 运行日期（UTC） | `20250115` |
| `<HHmm>` | 运行时间（UTC，24 小时制） | `1030` |
| `<RunLabel>` | 手动触发时输入的标签（只含字母、数字、连字符） | `2025Q1` |
| `<ext>` | 文件扩展名 | `xlsx` 或 `pdf` |

**示例**：

```
ValidationReport_20250115_1030_2025Q1.xlsx
ValidationReport_20250115_1030_2025Q1.pdf
```

> **RunLabel 处理规则**：
> - 若未填写，默认使用 `manual`
> - 去除所有空格，替换特殊字符为连字符 `-`（在 Power Automate 中使用 `replace()` + `toLower()`）
> - 最大长度 32 字符，超出截断

### 1.2 输入数据文件（OneDrive → SharePoint 迁移后）

```
tblOffshoring_<YYYYMM>.xlsx
```

例如：`tblOffshoring_202501.xlsx`（存放 2025 年 1 月数据）

> POC 阶段使用固定文件名 `tblOffshoring.xlsx`，通过覆盖更新内容；迁移后建议按月归档。

### 1.3 Excel / Word 模板文件

```
ValidationReport_Template.xlsx
ValidationReport_Template.docx
```

模板文件存放在 SharePoint 的 `Templates/` 目录，**不允许覆盖**，每次生成报告时**复制**后填充。

---

## 2. SharePoint 目录结构

```
<SharePoint 站点>/
└── Documents/                          ← 文档库根目录
    ├── Templates/                      ← 报告模板（只读，不覆盖）
    │   ├── ValidationReport_Template.xlsx
    │   └── ValidationReport_Template.docx
    │
    ├── ValidationReports/              ← 校验报告输出根目录
    │   ├── 2025/                       ← 按年分组
    │   │   ├── 202501/                 ← 按月分组（YYYYMM）
    │   │   │   ├── ValidationReport_20250115_1030_2025Q1.xlsx
    │   │   │   ├── ValidationReport_20250115_1030_2025Q1.pdf
    │   │   │   └── ValidationReport_20250120_0900_manual.xlsx
    │   │   └── 202502/
    │   │       └── ...
    │   └── 2026/
    │       └── ...
    │
    └── InputData/                      ← 输入数据存档（迁移至 SharePoint 后使用）
        ├── 2025/
        │   ├── tblOffshoring_202501.xlsx
        │   └── tblOffshoring_202502.xlsx
        └── 2026/
            └── ...
```

### 2.1 目录说明

| 目录 | 用途 | 写入者 | 读取者 |
|------|------|--------|--------|
| `Templates/` | 存放 Excel/Word 报告模板 | 管理员（手动上传） | Power Automate 流程（只读复制） |
| `ValidationReports/<年>/<年月>/` | 存放生成的 Excel + PDF 报告 | Power Automate 流程 | 所有相关团队成员 |
| `InputData/<年>/` | 存放输入 Excel（迁移后） | 数据维护人员 | Power Automate 流程（只读） |

### 2.2 SharePoint 权限建议

| 角色 | 权限 | 适用目录 |
|------|------|----------|
| 流程服务账号 | 读取 | `Templates/`、`InputData/` |
| 流程服务账号 | 写入/创建 | `ValidationReports/` |
| 数据维护人员 | 读写 | `InputData/` |
| 报告查看者 | 只读 | `ValidationReports/` |
| 管理员 | 完全控制 | 所有目录 |

---

## 3. OneDrive 目录结构（POC 阶段）

```
OneDrive（个人/商业）/
└── Offshoring/
    └── tblOffshoring.xlsx              ← 待校验输入文件（固定路径）
```

> POC 阶段仅使用此一个文件。流程路径在 Power Automate 连接器中硬编码为 `/Offshoring/tblOffshoring.xlsx`。

---

## 4. 迁移至 SharePoint 后的路径对照

| 阶段 | 输入文件位置 | PA 连接器类型 |
|------|-------------|--------------|
| POC | `OneDrive:/Offshoring/tblOffshoring.xlsx` | OneDrive for Business |
| 生产 | `SharePoint:/Documents/InputData/<年>/tblOffshoring_<YYYYMM>.xlsx` | SharePoint（Excel Online） |

迁移时**仅修改 Step 2** 的连接器配置，其余流程步骤无需更改。
