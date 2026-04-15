# POC-Validator — Power Automate 逐步搭建指南（新手完整版）

> **阅读说明**：本文档为 Power Automate 经验较少的用户提供**逐步点选操作指引**，每个字段填什么、点哪个按钮均有说明。建议按顺序操作，不要跳步。
>
> **新旧设计器说明**：Power Automate 目前有「旧版设计器」和「新版设计器（New Designer）」两套 UI，整体功能相同，但部分按钮位置略有差异。本文以常见的**新版设计器**为主描述，旧版差异会单独标注。

---

## 目录
1. [创建 Flow 并设置触发器](#一创建-flow-并设置触发器)
2. [读取 Excel 输入文件](#二读取-excel-输入文件list-rows-present-in-a-table)
3. [开启 Pagination 分页（处理大数据量）](#三开启-pagination-分页处理大数据量)
4. [初始化变量 RowIndex 和 RowsForAI](#四初始化变量-rowindex-和-rowsforai)
5. [添加 Apply to each 循环并构建行数据](#五添加-apply-to-each-循环并构建行数据)
6. [用 Compose 验证结果](#六用-compose-验证结果)
7. [运行测试与查看运行历史](#七运行测试与查看运行历史)
8. [常见问题与错误排查](#八常见问题与错误排查)

---

## 一、创建 Flow 并设置触发器

### 步骤 1：进入 Power Automate

1. 打开浏览器，访问 [https://make.powerautomate.com](https://make.powerautomate.com)。
2. 用你的工作账号（Microsoft 365）登录。
3. 确认左上角显示的**环境**是你的工作环境（不是默认的个人环境），如有需要，点击右上角环境名称切换。

### 步骤 2：新建 Instant cloud flow（即时云端流程）

1. 左侧菜单点击 **「+ 创建」**（英文：Create）。
2. 在「从空白开始」区域，点击 **「即时云端流程」**（英文：Instant cloud flow）。
   > 注意：不要选「自动化云端流程」（Automated）或「计划云端流程」（Scheduled）。
3. 弹出「生成即时云端流程」对话框：
   - **流程名称**：输入 `POC-Validator-Flow`（或你喜欢的名称）。
   - **选择触发此流程的方式**：选择 **「手动触发流程」**（英文：Manually trigger a flow）。
4. 点击 **「创建」**。
5. 进入 Flow 设计器界面，看到第一个动作框：**「手动触发流程」**。

### 步骤 3：添加触发器输入参数

「手动触发流程」支持设置在运行前让用户填写的参数，我们需要添加两个：

1. 点击触发器框 **「手动触发流程」**，展开它的详情。
2. 点击 **「+ 添加输入」**（英文：+ Add an input）。

#### 添加第一个输入：BatchYearMonth

3. 选择类型 **「文本」**（英文：Text）。
4. 出现两个输入框：
   - 左边输入框（标签/名称）：输入 `BatchYearMonth`
   - 右边输入框（请输入...）：这是运行时的提示文字，可输入 `批次年月，例如 202603`
5. 如果想设置为**必填**：点击这个输入项右侧的 **「…」（更多选项）** → 勾选「必填」（英文：Required）。

#### 添加第二个输入：TopNIssuesInPDF

6. 再次点击 **「+ 添加输入」**。
7. 选择类型 **「数字」**（英文：Number）。
8. 左边输入框：输入 `TopNIssuesInPDF`
9. 右边输入框（默认值提示）：可输入 `200`
   > 在「数字」类型中，如果需要设置默认值，点击该输入项的「…」→「默认值」，输入 `200`。

### 步骤 4：保存当前进度

1. 点击右上角 **「保存」**（英文：Save）。
2. 看到「流程已保存」的提示即成功。

---

## 二、读取 Excel 输入文件（List rows present in a table）

### 步骤 5：添加"List rows present in a table"动作

1. 点击触发器动作框下方的 **「+」号**（新版设计器中是竖线下方的加号），点击 **「添加操作」**（英文：Add an action）。
2. 在搜索框中输入 `List rows present`，等待搜索结果出现。
3. 在结果列表中找到 **「Excel Online (Business)」** 下的 **「List rows present in a table」**，点击选中它。
4. 如果是第一次使用 Excel Online (Business)，可能需要登录授权，点击「登录」并按提示完成。

### 步骤 6：填写 List rows 的字段

进入该动作后，你会看到以下字段：

#### Location（位置）
5. 点击 **Location** 下拉框。
6. 选择 **「SharePoint Site – https://[你的公司].sharepoint.com/sites/[站点名]」**。
   > 如果下拉列表中没有你想要的站点，选择「输入自定义值」然后粘贴站点 URL。

#### Document Library（文档库）
7. 点击 **Document Library** 下拉框。
8. 选择 **「Documents」**（文档）。
   > 注意：这里选的是文档库名称"Documents"，不是某个子文件夹。

#### File（文件）
9. 点击 **File** 字段右侧的 **文件夹图标**（📁）。
10. 在文件浏览器中导航：点击 **「POC-Validator」** → **「Inputs」**。
11. 找到你的输入文件（例如 `offshoring_202603.xlsx`），点击选中它。
12. File 字段将显示文件路径，例如：`/POC-Validator/Inputs/offshoring_202603.xlsx`。

#### Table（表格）
13. 点击 **Table** 下拉框。
14. 等待几秒，Power Automate 会自动读取文件中的 Excel Table 列表。
15. 选择 **「tblOffshoring」**。
    > 如果下拉列表为空或没有 `tblOffshoring`，请先按照《Excel 输入文件准备指南》完成 Excel Table 的创建步骤。

---

## 三、开启 Pagination 分页（处理大数据量）

当 Excel 数据行数超过连接器默认限制（通常 256 行），必须开启 Pagination 才能读取全部数据。

### 步骤 7：进入 List rows 的设置页面

1. 在 **「List rows present in a table」** 动作框右上角，点击 **「…」（省略号 / 更多选项）**。
2. 在弹出菜单中，点击 **「设置」**（英文：Settings）。
   
   > **新版设计器**：点击动作框右上角的 `⋮`（竖排三点）→ 点击「Settings」。  
   > **旧版设计器**：在动作框的标题栏右侧，点击 `…` → 「设置」。

3. 弹出「设置」侧边面板。

### 步骤 8：开启 Pagination 并设置阈值

4. 在设置面板中，找到 **「分页」**（英文：Pagination）区域。
5. 把 Pagination 的开关从**关闭**拨到**开启**（Toggle 变蓝）。
6. 开启后出现 **「阈值」**（英文：Threshold）输入框。
7. 在阈值框中输入一个正整数，例如：
   - `5000`（适合 5000 行以内的数据）
   - `100000`（适合更大数据量，但 Flow 运行时间会变长）
   
   > ⚠️ **常见错误**：如果阈值留空或输入 0，运行时会报错：  
   > `Paging count invalid. Value must be a number greater than 0`  
   > **解决方法**：必须填入大于 0 的正整数，不能留空，不能填 0。

8. 点击 **「完成」**（英文：Done）关闭设置面板。

### 步骤 9：保存

9. 点击右上角 **「保存」**。

---

## 四、初始化变量 RowIndex 和 RowsForAI

在循环之前，需要先初始化两个变量：
- `RowIndex`：整数，用于记录当前处理的行号（从 1 开始递增）
- `RowsForAI`：数组，用于收集每一行的数据，最终传给 AI

### 步骤 10：初始化变量 RowIndex

1. 在 **List rows present in a table** 动作下方，点击 **「+」** → **「添加操作」**。
2. 搜索框输入 `Initialize variable`。
3. 选择 **「变量」** 下的 **「初始化变量」**（英文：Initialize variable）。
4. 填写字段：
   - **名称（Name）**：输入 `RowIndex`
   - **类型（Type）**：下拉选择 **「整数」**（英文：Integer）
   - **值（Value）**：输入 `0`

### 步骤 11：初始化变量 RowsForAI

1. 在 **初始化变量 RowIndex** 动作下方，点击 **「+」** → **「添加操作」**。
2. 搜索 `Initialize variable`，选择同一个动作。
3. 填写字段：
   - **名称（Name）**：输入 `RowsForAI`
   - **类型（Type）**：下拉选择 **「数组」**（英文：Array）
   - **值（Value）**：输入 `[]`（一对空方括号，表示空数组）

### 步骤 12：保存

4. 点击右上角 **「保存」**。

---

## 五、添加 Apply to each 循环并构建行数据

「Apply to each」动作会对一个数组里的每个元素逐条执行一组操作。我们用它来遍历 Excel 的每一行数据。

### 步骤 13：添加 Apply to each

1. 在 **初始化变量 RowsForAI** 下方，点击 **「+」** → **「添加操作」**。
2. 搜索 `Apply to each`（或中文搜索「应用于每一个」）。
3. 选择 **「控件」** 下的 **「Apply to each」**（英文：Apply to each）。

### 步骤 14：设置 Apply to each 的遍历数组

4. 动作框出现 **「从之前的步骤中选择一个输出」**（英文：Select an output from previous steps）字段。
5. 点击该字段，弹出「动态内容」选择器。
   
   > **动态内容选择器说明**：  
   > 这是一个弹出面板，左侧列出之前各步骤的动作名称，右侧列出每个动作可输出的字段。  
   > - 如果看不到想要的字段，在顶部搜索框中输入字段名即可。
   > - 新版设计器中，点击输入框后面板自动弹出；旧版设计器中，点击「添加动态内容」链接。

6. 在动态内容面板中，找到 **「List rows present in a table」** 部分。
7. 点击选择 **「value」**（这就是 Excel 所有行数据组成的数组）。
8. Apply to each 的输入字段现在显示：`value`（或显示为动态标签）。

### 步骤 15：关闭 Apply to each 的并发（防止限流）

> 如果数据量较大或写入操作较多，建议关闭并发，避免因为 SharePoint/Excel 连接器限流而导致某些操作失败或假死。

1. 点击 **Apply to each** 动作框右上角的 **「…」** → **「设置」**。
2. 找到 **「并发控制」**（英文：Concurrency Control）。
3. 打开并发控制开关，把「度」（英文：Degree）设为 **`1`**（串行，一次处理一条）。
4. 点击 **「完成」**。

### 步骤 16：在循环内添加"递增变量 RowIndex"

现在要在 Apply to each 循环**内部**添加动作。

1. 在 Apply to each 框**内部**，点击 **「+」** → **「添加操作」**（注意：是在循环框内，不是循环框下方）。
2. 搜索 `Increment variable`（或「递增变量」）。
3. 选择 **「变量」** 下的 **「递增变量」**（英文：Increment variable）。
4. 填写字段：
   - **名称（Name）**：下拉选择 **`RowIndex`**
   - **值（Value）**：输入 `1`

### 步骤 17：在循环内添加"追加到数组变量 RowsForAI"

1. 在 **递增变量 RowIndex** 下方（循环内），点击 **「+」** → **「添加操作」**。
2. 搜索 `Append to array variable`（或「追加到数组变量」）。
3. 选择 **「变量」** 下的 **「追加到数组变量」**（英文：Append to array variable）。
4. 填写字段：

   - **名称（Name）**：下拉选择 **`RowsForAI`**
   
   - **值（Value）**：这里必须填一个 **对象（Object）**，不是数组。点击「值」输入框，切换到「表达式」或直接填写以下内容（以实际列名为准）：

     **方法 A：手动拼 JSON（推荐新手）**  
     点击 Value 输入框，选择 **「表达式」**（英文：Expression）标签，输入以下表达式：

     ```
     outputs('List_rows_present_in_a_table')?['body/value']
     ```
     
     > 实际上，对于「追加到数组变量」的 Value，更常见的是直接点击 **「动态内容」**，然后选择 Apply to each 当前行的各个字段。
     
     **方法 B：使用动态内容构建对象（标准做法）**
     
     点击 Value 输入框 → 切换到 **「表达式」** 标签 → 输入以下表达式（根据你的列名调整）：
     
     ```
     json(concat('{"RowIndex":', variables('RowIndex'), ',"YearMonth":"', triggerBody()?['text'], '","Cost Center Number":"', items('Apply_to_each')?['Cost Center Number'], '","Function":"', items('Apply_to_each')?['Function'], '","Team":"', items('Apply_to_each')?['Team'], '","Owner":"', items('Apply_to_each')?['Owner'], '"}'))
     ```
     
     > 这个表达式会生成一个 JSON 对象（不是数组），包含：RowIndex + BatchYearMonth + 当前行各列的值。
     
     **方法 C：使用 Compose 先构建对象，再 Append（最直观，推荐新手）**
     
     跳到下一步，先看「使用 Compose 构建对象」的做法。

---

### 推荐做法：用 Compose 构建每行对象，再 Append

对新手来说，**最直观**的方式是先用一个 **Compose** 动作把单行数据拼成 JSON 对象，再把 Compose 的输出 Append 进数组。

#### 步骤 17-A：循环内添加 Compose（构建单行对象）

1. 在 **递增变量 RowIndex** 下方（Apply to each 循环内），点击 **「+」** → **「添加操作」**。
2. 搜索 `Compose`（或「撰写」）。
3. 选择 **「数据操作」** 下的 **「撰写」**（英文：Compose）。
4. 在 **Inputs（输入）** 字段中，点击进入后，**切换到「表达式」标签**，输入：
   
   ```
   {
     "RowIndex": variables('RowIndex'),
     "BatchYearMonth": triggerBody()?['text'],
     "CostCenterNumber": items('Apply_to_each')?['Cost Center Number'],
     "Function": items('Apply_to_each')?['Function'],
     "Team": items('Apply_to_each')?['Team'],
     "Owner": items('Apply_to_each')?['Owner']
   }
   ```
   
   > **重要提示**：`items('Apply_to_each')?['列名']` 中的列名必须和 Excel 表格的列标题**完全一致**（包括大小写和空格）。  
   > 你可以在 Apply to each 的「当前项」（Current item）动态内容中看到你的实际列名。

5. 点击 **「确定」** 或直接点击其他地方收起。

   > **验证 Compose 输出的方法**：先保存并运行一次（只取 2 条数据），在运行历史中点开 Compose，看 Outputs 里显示的 JSON 是否是一个 `{...}` 对象（而不是 `[{...}]` 数组）。  
   > 正确格式示例：
   > ```json
   > {
   >   "RowIndex": 1,
   >   "BatchYearMonth": "202603",
   >   "CostCenterNumber": "CC001",
   >   "Function": "IT",
   >   "Team": "Platform",
   >   "Owner": "John Smith"
   > }
   > ```

#### 步骤 17-B：循环内添加 Append to array variable（把 Compose 输出追加进数组）

1. 在 **Compose** 动作下方（Apply to each 循环内），点击 **「+」** → **「添加操作」**。
2. 搜索 `Append to array variable`，选择 **「追加到数组变量」**。
3. 填写字段：
   - **名称（Name）**：下拉选择 **`RowsForAI`**
   - **值（Value）**：点击输入框 → 切换到 **「动态内容」** 标签 → 找到 **「Compose」** 下的 **「Outputs」**，点击选中。
   
   > ⚠️ **常见错误**：Value 字段不能直接输入 `[{...}]`（数组），只能是单个对象 `{...}`。  
   > 「追加到数组变量」每次只追加一个元素，Power Automate 自动把它加到数组末尾。  
   > 如果你填了 `[{...}]`，最终 RowsForAI 会变成"数组的数组"，格式为 `[[{...}],[{...}]]`，AI 调用时会报错。

### 步骤 18：保存

4. 点击右上角 **「保存」**。

---

## 六、用 Compose 验证结果

在把数据传给 AI 之前，建议在 Apply to each 循环**之后**加一个 Compose，查看 RowsForAI 的完整内容，确认格式正确。

### 步骤 19：在循环外添加验证用 Compose

1. 在 Apply to each 循环框**外部**（循环框下方），点击 **「+」** → **「添加操作」**。
2. 搜索 `Compose`，选择 **「撰写」**。
3. 在 Inputs 字段中，点击输入框 → 切换到 **「动态内容」** → 找到 **「变量」** 区域 → 选择 **`RowsForAI`**。
4. 保存并运行（见下一节）。

#### 预期输出（正确格式）：
```json
[
  {"RowIndex": 1, "BatchYearMonth": "202603", "CostCenterNumber": "CC001", "Function": "IT", "Team": "Platform", "Owner": "John Smith"},
  {"RowIndex": 2, "BatchYearMonth": "202603", "CostCenterNumber": "CC002", "Function": "Finance", "Team": "Reporting", "Owner": "Jane Doe"}
]
```
> 这是一个数组（最外层是 `[...]`），数组里每个元素是一个对象（`{...}`），每个对象代表一行数据。

#### 不正确格式（需要修复）：
```json
[[{"RowIndex": 1}], [{"RowIndex": 2}]]   ← 数组的数组，Append 时 Value 写成了 [{...}]
[{}, {}, {}]                              ← 对象为空，Compose 输入没有映射到列值
```

---

## 七、运行测试与查看运行历史

### 步骤 20：先用少量数据做测试

为了快速验证逻辑，建议先只读取前 20 行数据：

1. 点击 **「List rows present in a table」** 动作展开。
2. 找到底部的 **「高级选项」**（英文：Advanced options）或直接看动作字段列表。
3. 找到 **「Top Count」** 字段（即"最多取多少行"），输入 `20`。
4. 保存。

### 步骤 21：手动触发运行

1. 点击设计器右上角（或顶部）的 **「测试」**（英文：Test）按钮。
2. 选择 **「手动」**（Manually），点击 **「测试」**。
3. 弹出输入框：
   - `BatchYearMonth`：输入 `202603`（或你的实际批次）
   - `TopNIssuesInPDF`：输入 `10`（测试时填小数字）
4. 点击 **「运行流程」**（英文：Run flow）。
5. 等待几秒，看到「已成功触发流程」弹窗，点击 **「完成」**。

### 步骤 22：查看运行历史

1. 左侧菜单点击 **「我的流程」**（英文：My flows）。
2. 找到 `POC-Validator-Flow`，点击进入流程详情页。
3. 在页面下方找到 **「28 天运行历史记录」**（英文：28 day run history）。
4. 点击最新一条记录（状态可能是「成功」绿色、「失败」红色、「正在运行」旋转图标）。
5. 进入运行详情页：
   - 每个动作框旁边显示执行时间和状态（✅ 成功 / ❌ 失败 / ⏭ 跳过）。
   - 点击任意动作框可以展开查看：**输入（Inputs）** 和 **输出（Outputs）**。
   - 点开 **Compose（验证）** 动作，查看 Outputs 里的 RowsForAI 内容。
   - 点开 **Apply to each**，查看循环了多少次（迭代数）。

### 步骤 23：确认后去掉 Top Count

验证结果正确后：
1. 回到设计器，把 **List rows 的 Top Count** 字段清空（删除 `20`）。
2. 保存。
3. 再次运行，这次会读取全部数据（可能需要等几分钟，取决于数据量）。

---

## 八、常见问题与错误排查

### 错误 1：`Paging count invalid. Value must be a number greater than 0`

**出现位置**：List rows present in a table 运行时报错。  
**原因**：Pagination 已开启，但 Threshold（阈值）字段为空或填了 0。  
**解决**：按照[第三节步骤 8](#步骤-8开启-pagination-并设置阈值)，在 Threshold 填入正整数（如 `5000`）。

---

### 错误 2：Apply to each 一直在跑，感觉停不了

**诊断步骤**：
1. 打开运行历史，进入该次运行详情。
2. 点开 Apply to each，查看**迭代数**（循环了多少次）——这就是实际数据行数。
3. 如果是 5000 次，那就确实要跑 5000 次，属于正常，不是卡死。
4. 查看每次迭代的执行时间，如果某次卡在"Running"很久，可能是 SharePoint/Excel 限流在重试。

**临时解决**：在运行详情页右上角点击 **「取消运行」**（Cancel run），结束本次运行。  
**根本解决**：先用 Top Count=20 验证逻辑，再放开全量数据。

---

### 错误 3：动态内容选择器里找不到 List rows 的 value 字段

**原因**：Apply to each 的输入框没有选中正确步骤的输出。  
**解决**：
1. 在动态内容面板顶部搜索框输入 `value`。
2. 确认选中的是 **「List rows present in a table」** 下的 `value`，而不是其他步骤的字段。
3. 如果仍找不到，检查 List rows 步骤是否有报错（运行报错的步骤不输出数据）。

---

### 错误 4：Compose 输出是 `{}` 空对象

**原因**：Compose 的 Inputs 里没有正确引用 Apply to each 当前行的列值。  
**解决**：
1. 在 Compose Inputs 里，确认使用 `items('Apply_to_each')?['列名']` 来引用当前行字段。
2. 动作名有空格时（如 Apply to each 动作名自动变成 `Apply_to_each_1`），需要相应调整。
3. 确认列名和 Excel 表格标题完全一致（大小写、空格）。

---

### 错误 5：追加到数组后，RowsForAI 变成数组的数组 `[[...],[...]]`

**原因**：「追加到数组变量」的 Value 写成了 `[{...}]`（数组），而不是 `{...}`（对象）。  
**解决**：Value 必须是单个对象。检查 Compose 的输出是否是 `{...}` 格式；如果 Compose 输出是 `[{...}]`，说明 Compose 的 Inputs 本身写错了，需要去掉外层方括号。

---

### 提示：新版设计器中的常见 UI 差异

| 功能 | 旧版设计器 | 新版设计器（New Designer） |
|---|---|---|
| 添加动作 | 点击「+ 新建步骤」 | 点击动作之间的「+」号 |
| 设置（Settings） | 动作标题栏右侧 `…` | 动作框右上角 `⋮` 或 `…` |
| 动态内容 | 点击「添加动态内容」链接 | 点击输入框后自动弹出面板 |
| Apply to each 并发 | 设置面板 → 并发控制 | 设置面板 → Concurrency Control |
| 运行测试 | 右上角「测试」 | 右上角「测试」（位置基本相同） |

---

*最后更新：2026-03*
