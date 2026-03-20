# Power Automate × Copilot Studio 排障指南

## 场景：Execute Agent and wait 返回 502 / DNS 解析失败

### 错误现象

在 Power Automate 流中使用 **Copilot Studio → Execute Agent and wait** 动作时，运行历史显示类似下面的错误：

```
Status: 502 Bad Gateway
Message: The remote name could not be resolved:
         'resonap.oc.environment.api.powerplatform.com'
```

或其他包含 `*.environment.api.powerplatform.com` 的 DNS 解析失败信息。

---

## 一、可能原因

| # | 原因 | 说明 |
|---|------|------|
| 1 | **Environment ID 填写错误** | 动作配置中的 `Environment ID` 字段为空或填写了无效值，导致 SDK 拼接出不存在的子域名（如 `resonap.oc.…`） |
| 2 | **环境域名拼写错误** | 自动生成的域名（如 `org12345.crm.dynamics.com`）被手动改错，或 Connector 指向了已删除/改名的环境 |
| 3 | **连接器区域与租户区域不匹配** | Copilot Studio 连接器在创建时选择了错误的地域（如选 US 但实际环境在 Europe），导致请求路由到错误的端点 |
| 4 | **DNS / 网络限制** | 企业防火墙、代理或 Private Link 配置阻止了对 `*.api.powerplatform.com` 的出站解析；或当前网络（如 VPN）不允许该域名通过 |
| 5 | **租户或 Power Platform 服务中断** | Microsoft 侧服务故障，导致该端点暂时不可用（排除以上原因后再考虑此项） |

---

## 二、逐步排查与修复

### 步骤 1：确认 Environment ID

1. 打开 [Power Platform 管理中心](https://admin.powerplatform.microsoft.com)
2. 左侧菜单点击 **Environments**
3. 找到并点击你要调用的环境（例如 `POC Validator` 环境）
4. 在 **Details** 页面，复制 **Environment ID**（格式类似 `00000000-0000-0000-0000-000000000000`）
5. 回到 Power Automate 流，打开 **Execute Agent and wait** 动作
6. 将 **Environment ID** 字段粘贴为你复制的值（不要留空，不要手动拼写）

> ⚠️ 注意：如果字段下拉里能直接选环境名称，优先选下拉而不是手填 ID，避免拼写错误。

---

### 步骤 2：验证环境 URL 是否正确

1. 在 [Power Platform 管理中心](https://admin.powerplatform.microsoft.com) → **Environments** → 点击你的环境
2. 查看 **Environment URL**，格式应为：
   ```
   https://org<xxxxxxxx>.crm<N>.dynamics.com
   ```
3. 用浏览器打开该 URL，能正常访问则说明环境存在且 URL 正确
4. 如果 URL 访问失败（404/无法解析），该环境可能已被删除或迁移，需联系管理员确认

---

### 步骤 3：重建 Copilot Studio 连接

旧连接（Connection）可能缓存了错误的区域或凭据。

1. 在 Power Automate 左侧菜单点击 **Data → Connections**
2. 找到 **Microsoft Copilot Studio** 类型的连接，点击 **⋯ → Delete**（删除旧连接）
3. 回到你的流，点击 **Execute Agent and wait** 动作
4. 在 **Connection** 下拉里选 **+ New connection**，按提示登录并授权（选择与你 Agent 同一租户的账号）
5. **重要**：建连接时注意选择正确的地区（如果有提示）

---

### 步骤 4：正确填写动作的各个字段

打开 **Execute Agent and wait** 动作，确认以下字段：

| 字段 | 应填内容 | 常见错误 |
|------|----------|----------|
| **Environment** / Environment ID | 从管理中心复制的正确 ID | 空白或手动拼写出错 |
| **Agent** / Copilot | 下拉选择你的 Agent 名称 | 选了其他环境里的 Agent |
| **Message** | 你要传给 Agent 的输入文本或 JSON 字符串 | 留空 |
| **Conversation ID** | 留空即可（首次调用由 SDK 自动生成）；如需多轮会话再填 | 误填导致上下文错乱 |

---

### 步骤 5：在不同网络环境下测试

1. **关闭 VPN** 后重新触发流，观察是否 DNS 报错消失
2. 在手机热点（4G/5G）下触发流，与办公网络的结果做对比
3. 如果非 VPN 环境下正常运行，说明问题出在企业网络的 DNS/防火墙策略：
   - 联系网络/IT 管理员，确认以下域名已加入白名单：
     ```
     *.api.powerplatform.com
     *.powerva.microsoft.com
     *.crm.dynamics.com
     *.microsoftonline.com
     ```

---

### 步骤 6：检查 Power Platform 服务健康状态

1. 打开 [Microsoft 365 管理中心 → 服务健康](https://admin.microsoft.com/adminportal/home#/servicehealth) 或
   [Power Platform 管理中心 → Service health](https://admin.powerplatform.microsoft.com/servicestatus)
2. 查看 **Power Automate** 和 **Copilot Studio** 是否有正在进行的事故（Incident）或计划维护
3. 如有服务中断，等待 Microsoft 修复后重试；可在该页面订阅邮件通知

---

## 三、捕获输出与关联 ID（用于上报支持）

### 3.1 在 Power Automate 运行历史中获取详情

1. 打开你的流 → 点击顶部 **28 day run history**（或类似入口）
2. 找到失败的运行，点击它
3. 在运行详情页，点击失败的 **Execute Agent and wait** 动作
4. 展开 **Inputs** 和 **Outputs** 面板，复制完整 JSON 内容
5. 特别注意 `error.code`、`error.message`、`statusCode` 字段

### 3.2 获取 Correlation ID（关联 ID）

Correlation ID 是向 Microsoft 支持提单时最重要的信息，可以从两个地方获取：

**方法 A：从流运行详情获取**

在运行详情页右上角，通常有 **Correlation ID** 或 **Request ID** 字段（点击 `···` 或 `i` 图标）。

**方法 B：从浏览器开发者工具获取**

1. 在 Power Automate 网页打开 F12 开发者工具 → **Network** 标签
2. 触发流运行
3. 找到请求 `powerautomate.microsoft.com` 或 `flow.microsoft.com` 的 API 调用
4. 在 Response Headers 中找到 `x-ms-request-id` 或 `client-request-id`，复制其值

**方法 C：在流中主动捕获（推荐用于生产排障）**

在 **Execute Agent and wait** 动作后面加一个 **Compose**，Inputs 填写：

```text
outputs('Execute_Agent_and_wait')
```

运行失败时，此 Compose 会把动作完整的原始输出（含错误详情）写入运行历史，便于取证。

---

### 3.3 捕获模板（可直接加入流）

在流结尾加一个 **Compose – ErrorCapture**，在 *Configure run after* 里勾选 **has failed**，Inputs 填：

```json
{
  "flowRunId": "@{workflow()['run']['name']}",
  "actionOutputs": "@{outputs('Execute_Agent_and_wait')}",
  "timestamp": "@{utcNow()}"
}
```

这样每次失败时，你都能从运行历史里拿到完整的失败上下文。

---

## 四、何时升级问题

### 升级给组织管理员

出现以下情况时，联系你的 **Power Platform 租户管理员**：

- 环境 ID 已确认正确，但仍然 DNS 解析失败（怀疑是环境被删除或迁移）
- 多位用户、多个流都遇到同样的 502 错误（排除个人连接问题）
- 需要在防火墙白名单里添加 `*.api.powerplatform.com` 等域名
- 需要确认 Copilot Studio Agent 是否已发布并且状态正常（Agent 须在 Copilot Studio 管理门户中处于 **Published** 状态）

### 升级给 Microsoft 支持

出现以下情况时，通过 [Microsoft 365 管理中心](https://admin.microsoft.com) 或 [Power Platform 管理中心](https://admin.powerplatform.microsoft.com) 提交支持单：

- 服务健康页面无故障，但 502 持续超过 2 小时
- 重建连接、换网络后仍然失败
- 错误信息指向 Microsoft 侧端点（如 `resonap.oc.environment.api.powerplatform.com` 等自动生成的子域名无法解析）

**提单时请附上以下信息：**

| 信息项 | 如何获取 |
|--------|----------|
| Tenant ID | Azure Portal → Microsoft Entra ID → Properties |
| Environment ID | Power Platform 管理中心 → Environments → 选择环境 → Details |
| Flow Run ID | 运行历史详情页面的 URL 末尾，或通过 `workflow()['run']['name']` 表达式获取 |
| Correlation ID / Request ID | 见上方 3.2 节 |
| 错误截图或完整错误 JSON | 运行详情 → 失败动作 → 展开 Outputs |
| 首次发生时间（UTC） | 运行历史中的时间戳 |

---

## 五、快速自查清单

在提单或联系他人之前，先按以下清单逐项确认：

- [ ] Environment ID 已从管理中心复制，无拼写错误
- [ ] Environment URL 可用浏览器正常打开
- [ ] 已删除旧连接并重建 Copilot Studio 连接
- [ ] Agent 在 Copilot Studio 中处于 **Published**（已发布）状态
- [ ] 已在关闭 VPN / 换用手机热点后重试
- [ ] Power Platform 服务健康页面无当前事故
- [ ] 已用 Compose 捕获完整错误输出（含 Correlation ID）

---

## 参考资料

- [Power Platform 管理中心](https://admin.powerplatform.microsoft.com)
- [Copilot Studio 文档 – 在 Power Automate 中使用 Agent](https://learn.microsoft.com/en-us/microsoft-copilot-studio/advanced-flow)
- [Execute a Copilot Studio agent from a cloud flow](https://learn.microsoft.com/en-us/microsoft-copilot-studio/advanced-use-flow)
- [Power Platform URL 与 IP 范围白名单](https://learn.microsoft.com/en-us/power-platform/admin/online-requirements)
- [Microsoft 服务健康状态（管理中心）](https://admin.microsoft.com/adminportal/home#/servicehealth)
