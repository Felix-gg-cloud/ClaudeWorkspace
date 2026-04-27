# POC-Validator — 数据校验自动化流程（POC 阶段）

## 项目概述

本项目使用 **Power Automate（手动触发）+ Copilot Studio（AI-only 校验）** 对 Excel 表格 `tblOffshoring` 进行数据质量校验，并输出 Excel 报告与 PDF 报告，通过 Teams 通知相关人员。

### 关键约定

| 项目 | 决策 |
|------|------|
| 校验表 | Excel `tblOffshoring` |
| 触发方式 | Power Automate 手动触发 |
| 清洗策略 | PA 不做任何 trim / 清洗 / 规范化 |
| 校验执行者 | Copilot Studio（AI-only） |
| YearMonth 必填 | 所有行必须有值（空格视为空 → Error） |
| 前后空格规则 | 全列禁止前后空格 → Error（R-WS-ALL-001） |
| 严重级别 | 仅 `Error` / `Warning` 两级 |
| 输出格式 | JSON（审计）+ Excel（业务修复）+ PDF（阅读/归档） |
| 通知方式 | Microsoft Teams |

---

## OneDrive 目录结构（POC 阶段）

> **POC 阶段所有文件（模板、输入、输出）均存储在个人 OneDrive 下：**
>
> `Documents/PowerbiTest/POC-Validator/`

```
OneDrive（个人）
└── Documents/
    └── PowerbiTest/
        │
        ├── Master Excel_Strategy_OnOffshoring.xlsx   ← 输入文件（原有位置，保持不变）
        │
        └── POC-Validator/
            ├── Templates/
            │   └── ValidationReportTemplate.xlsx     ← Excel 报告模板（手动上传一次）
            │
            ├── Outputs/
            │   ├── Excel/
            │   │   └── ValidationReport_<YearMonth>_<timestamp>.xlsx
            │   ├── PDF/
            │   │   └── ValidationReport_<YearMonth>_<timestamp>.pdf
            │   └── JSON/
            │       └── ValidationResult_<YearMonth>_<timestamp>.json
            └── (README — 可选说明文件)
```

### 路径说明

| 路径 | 说明 |
|------|------|
| `Documents/PowerbiTest/` | 输入文件所在目录（现有，保持不变） |
| `Documents/PowerbiTest/POC-Validator/` | POC 产出物根目录（**固定，不变**） |
| `Documents/PowerbiTest/POC-Validator/Templates/` | 存放 Excel 报告模板 |
| `Documents/PowerbiTest/POC-Validator/Outputs/Excel/` | 生成的 Excel 校验报告 |
| `Documents/PowerbiTest/POC-Validator/Outputs/PDF/` | 生成的 PDF 校验报告 |
| `Documents/PowerbiTest/POC-Validator/Outputs/JSON/` | AI 原始返回 JSON（审计用，可选） |

### 文件命名规则

- **YearMonth**：从数据中提取（如 `202603`）或从触发参数获取
- **timestamp**：`yyyyMMdd-HHmm`（UTC+8 或触发时刻）
- 示例：`ValidationReport_202603_20260320-1001.xlsx`

---

## 后续迁移到团队 SharePoint（正式阶段）

POC 阶段用个人 OneDrive 是为了最快验证逻辑，不依赖团队权限申请。  
正式上线时迁移步骤：

1. 在团队 SharePoint 站点创建对应文档库与目录结构
2. Power Automate 中：
   - 将"读取 Excel"连接器的 **Location** 从 `OneDrive for Business` 改为 `SharePoint`，并填入站点 URL
   - 将所有"创建文件"/"复制文件"动作的 **Location** 与 **Site Address** 改为 SharePoint 站点
3. 模板文件（`ValidationReportTemplate.xlsx`）上传到 SharePoint 对应 `Templates/` 目录
4. 业务逻辑（AI 校验规则、HTML 模板、字段映射）**无需修改**

---

## 目录说明

| 文件 | 内容 |
|------|------|
| `docs/runbook-power-automate.md` | Power Automate 逐动作配置手顺（含字段截图说明） |
| `docs/ai-contract.md` | Copilot Studio AI 输入/输出 JSON 契约 |
| `templates/ExcelTemplate-guide.md` | Excel 报告模板结构说明（Sheet、表名、列名） |
