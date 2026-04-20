/**
 * Office Scripts: bulkInsertIssues.ts
 *
 * 用途：在 Power Automate 中通过 "Run script"（Excel Online Business）动作
 *       将 issues[] 数组一次性批量写入 tblIssues，替代逐行 "Add a row into a table" 循环，
 *       大幅提升大批量数据写入的速度（减少 API 调用次数）。
 *
 * 使用方法：
 *   1. 在 Excel 网页版中打开任意文件
 *   2. 自动化（Automate）→ 新建脚本（New Script）
 *   3. 粘贴本脚本内容，保存
 *   4. 在 Power Automate 的 "Run script" 动作中：
 *      - File: 指向刚创建的报告文件（由前一步骤生成）
 *      - Script: 选择本脚本
 *      - issuesJson: 传入 JSON.stringify(report_model.issues) 的结果
 *
 * 注意：
 *   - Script 必须保存在与目标文件同一 SharePoint 站点可访问的位置
 *   - issuesJson 是 JSON 字符串，脚本内部会解析
 */

function main(workbook: ExcelScript.Workbook, issuesJson: string): void {
  // 解析传入的 issues 数组
  const issues: IssueRow[] = JSON.parse(issuesJson);

  if (!issues || issues.length === 0) {
    console.log("No issues to insert.");
    return;
  }

  // 获取 Issues Sheet 上的 tblIssues 表
  const sheet = workbook.getWorksheet("Issues");
  if (!sheet) {
    throw new Error("Sheet 'Issues' not found. Please verify the template.");
  }

  const table = sheet.getTable("tblIssues");
  if (!table) {
    throw new Error("Table 'tblIssues' not found. Please verify the template.");
  }

  // 将 issues 转换为二维数组（行 × 列），列顺序与 tblIssues 一致
  const rows: (string | number)[][] = issues.map(issue => [
    issue.severity,
    issue.rule_id,
    issue.row_key,
    issue.row_index,
    issue.year_month,
    issue.cost_center_number,
    issue.function,
    issue.team,
    issue.owner,
    issue.column,
    issue.raw_value,
    issue.message,
    issue.fix_suggestion
  ]);

  // 批量追加行（一次 API 调用）
  table.addRows(-1, rows);

  console.log(`Successfully inserted ${rows.length} rows into tblIssues.`);
}

// TypeScript 接口定义（与 report_model.issues[] 结构一致）
interface IssueRow {
  severity: string;
  rule_id: string;
  row_key: string;
  row_index: number;
  year_month: string;
  cost_center_number: string;
  function: string;
  team: string;
  owner: string;
  column: string;
  raw_value: string;
  message: string;
  fix_suggestion: string;
}
