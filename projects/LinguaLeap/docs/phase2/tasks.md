# Phase 2：AI 智能功能 ✅

> 目标：接入 AI API，实现 AI 智能出题、Prompt 模板、缓存/限流/日志，前端 AI 出题页面。
> 对应学习路线：第一层（AI API 调用）+ 第二层（Prompt 工程）+ 第六层（AI 工程化基础）
> **注意**：本文档编写时曾考虑 Groq API，实际已切换为 GitHub Models GPT-4o（`models.inference.ai.azure.com`），以下内容已同步更新。

---

## 任务总览

| # | 任务 | 类型 | 依赖 | 状态 |
|---|------|------|------|------|
| 2.1 | [Spring AI + GitHub Models 集成](#21-spring-ai--github-models-集成) | 后端 | — | ✅ |
| 2.2 | [Prompt 模板系统](#22-prompt-模板系统) | 后端 | 2.1 | ✅ |
| 2.3 | [AI 智能出题 API](#23-ai-智能出题-api) | 后端 | 2.2 | ✅ |
| 2.4 | [AI 出题质量校验](#24-ai-出题质量校验) | 后端 | 2.3 | ✅ |
| 2.5 | [缓存 + 调用日志 + 限流](#25-缓存--调用日志--限流) | 后端 | 2.1 | ✅ |
| 2.6 | [AI 知识点解析 API](#26-ai-知识点解析-api) | 后端 | 2.2 | ✅ |
| 2.7 | [降级策略：AI 不可用 → 模板出题](#27-降级策略ai-不可用--模板出题) | 后端 | 2.3 | ✅ |
| 2.8 | [前端：AI 出题入口](#28-前端ai-出题入口) | 前端 | 2.3 | ✅ |
| 2.9 | [前端：AI 分析等待/结果展示](#29-前端ai-分析等待结果展示) | 前端 | 2.6 | ✅ |
| 2.10 | [联调 + 端到端测试](#210-联调--端到端测试) | 测试 | ALL | ✅ |

> **注意**：Agent 模式（2.8-2.10 原需求）、PDF AI 分析（RAG）、AI 学习计划 属于 Phase 2 后半段。
> 本规划先聚焦 Phase 2 前半段：**AI 出题能力** —— 这是最核心的价值点。
> 后半段（Agent + RAG + 学习计划）视进度再拆分。

---

## 建议开发顺序

```
2.1 Spring AI 集成 → 2.2 Prompt 模板 → 2.5 缓存/日志/限流
  ↓
2.3 AI 出题 API → 2.4 质量校验 → 2.7 降级策略
  ↓
2.6 AI 知识点解析
  ↓
2.8 前端 AI 出题入口 → 2.9 前端 AI 分析展示
  ↓
2.10 联调测试
```

---

## 技术选型

| 项 | 选择 | 理由 |
|----|------|------|
| AI 框架 | Spring AI 1.0.0 | Spring 官方，与 Boot 深度集成 |
| AI 模型 | GitHub Models GPT-4o | 100次/天免费额度，智能度高，支持 JSON mode |
| Prompt 管理 | Java 类 + 模板字符串 | 简单直接，版本可控 |
| 缓存 | 数据库 (ai_analysis_cache) | 已有表，无需 Redis |
| 限流 | 内存计数器 | 单实例足够，无需分布式 |

---

## GitHub Models API 信息

- **Endpoint**: `https://models.inference.ai.azure.com`
- **模型**: `gpt-4o`
- **免费额度**: 100 req/day, 10 RPM, 40k tokens/min
- **特点**: OpenAI 兼容协议，Spring AI 原生支持
- **API Key**: 通过环境变量 `GITHUB_TOKEN` 注入（不硬编码）

---

## 2.1 Spring AI + GitHub Models 集成

**目标**：service-ai 引入 Spring AI，配置 GitHub Models GPT-4o ChatClient，验证基本调用。

**实现要点**：

1. **pom.xml 新增依赖**：
```xml
<!-- Spring AI BOM -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```
> Spring AI 的 OpenAI starter 兼容 GitHub Models（因为 GitHub Models 用 OpenAI 协议）

2. **application.yml 配置**：
```yaml
spring:
  ai:
    openai:
      api-key: ${GITHUB_TOKEN}
      base-url: https://models.inference.ai.azure.com
      chat:
        options:
          model: gpt-4o
          temperature: 0.3
```

3. **ChatService 封装**：
```java
@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();
    }
}
```

4. **Health/Test 端点**：`GET /api/ai/health`（验证 GitHub Models 连通性）

5. **UserContextFilter**：从 Gateway 接收 X-User-Id

**验收标准**：
- [ ] Maven 编译通过
- [ ] 启动时能连接 GitHub Models API
- [ ] `/api/ai/test?prompt=hello` 返回 AI 回复
- [ ] API Key 通过环境变量注入，不在代码中硬编码

---

## 2.2 Prompt 模板系统

**目标**：建立结构化 Prompt 模板，支持变量替换，为 AI 出题提供高质量指令。

**Prompt 模板设计**：

| 模板 | 用途 | 关键变量 |
|------|------|---------|
| `question_choice` | 英译中/中译英选择题 | grade, word, phonetic, meaning, direction |
| `question_fill` | 填空题 | grade, word, meaning, exampleSentence |
| `question_translate` | 翻译题 | grade, word, meaning, sentence |
| `question_batch` | 批量出题（多个词） | grade, words[], types[] |
| `kp_analyze` | 从文本提取知识点 | grade, text |

**Prompt 结构（以选择题为例）**：
```
[System]
你是一位专业英语教师，为{grade}年级学生出题。
规则：
1. 难度适合{grade}年级学生
2. 干扰项必须是同词性、相近难度的词，不能明显荒谬
3. 题干简洁清晰
4. 严格按指定 JSON 格式输出，不要输出其他内容

[User]
为以下单词出一道{direction}四选一选择题：
单词：{word}
音标：{phonetic}
释义：{meaning}

JSON 格式：
{"stem":"题干","options":["A","B","C","D"],"answer":"正确选项文本","explanation":"简要解析"}
```

**实现方式**：
- `PromptTemplates` 类：静态方法返回格式化 Prompt
- 变量用 `{placeholder}` 占位，Java `String.replace()` 替换
- 每个模板对应一个方法，入参为 Map 或专用 DTO

**验收标准**：
- [ ] 至少 4 个题型 Prompt 模板
- [ ] 变量替换正确
- [ ] Prompt 输出可直接发给 ChatClient

---

## 2.3 AI 智能出题 API

**目标**：基于知识点，调用 AI 生成高质量题目。

**API 设计**：

| 方法 | Path | 说明 |
|------|------|------|
| POST | `/api/ai/generate/question` | 为单个知识点出题 |
| POST | `/api/ai/generate/batch` | 批量出题（多知识点） |

**请求参数（单题）**：
```json
{
  "kpId": 1,
  "questionType": "en2zh_choice",
  "grade": "junior"
}
```

**请求参数（批量）**：
```json
{
  "bankId": 1,
  "questionTypes": ["en2zh_choice", "fill_blank"],
  "count": 10
}
```

**处理流程**：
```
1. 接收请求 → 查询知识点信息
2. 选择 Prompt 模板 → 填入变量
3. 调用 ChatService → 获取 AI 响应
4. JSON 解析 → 校验格式
5. 写入 question 表（created_by = 'ai'）
6. 记录调用日志（ai_call_log）
7. 返回生成的题目
```

**与现有 QuestionService 的关系**：
- 现有 `QuestionService.generate()` 是模板出题（Phase 1）
- AI 出题是 service-ai 中的新能力
- AI 生成的题目存入 service-content 的 question 表
- service-ai 通过 HTTP 调用 service-content 的内部接口保存题目
- 或：service-ai 直接操作 ll_content 数据库（跨库简单方案）

**推荐方案**：service-ai 通过 HTTP 调用 service-content 保存
- `POST /api/content/questions/internal` —— 内部接口，不经 Gateway JWT 校验
- 或在 service-content 增加一个接受 X-User-Id + question data 的保存接口

**验收标准**：
- [ ] 单题生成返回合理的题目
- [ ] 批量生成 10 题正常完成（不超时）
- [ ] created_by = 'ai' 标识 AI 生成题目
- [ ] 错误时返回友好提示（不暴露 API key/内部信息）

---

## 2.4 AI 出题质量校验

**目标**：对 AI 返回的 JSON 进行格式和内容校验。

**校验规则**：

| 项 | 规则 | 处理 |
|----|------|------|
| JSON 格式 | 必须可解析 | 重试 1 次，失败则报错 |
| 必要字段 | stem, answer 不能为空 | 丢弃该题 |
| 选择题选项 | 必须 4 个，answer 在 options 中 | 丢弃该题 |
| 长度 | stem 不超 200 字，option 不超 50 字 | 截断或丢弃 |
| 重复检测 | answer 不能和题干重复 | 丢弃该题 |

**实现**：
- `AiQuestionValidator` 类
- 入参：AI 返回的 JSON 字符串
- 出参：`QuestionDTO`（校验通过）或 `null`（校验失败）
- 校验失败时记录日志

**验收标准**：
- [ ] 格式错误的 JSON 能正确处理（不崩溃）
- [ ] 缺少字段的题目被过滤
- [ ] 选项数量不对的被过滤

---

## 2.5 缓存 + 调用日志 + 限流

**目标**：控制成本，记录调用，避免滥用。

### 调用日志（ai_call_log）

每次 AI 调用记录：
- user_id, api_provider（github_models）, call_type（generate_question 等）
- tokens_in, tokens_out（从 AI 响应中提取）
- latency_ms, status（success/error）

### 缓存（ai_analysis_cache）

- 对于相同输入（如同一知识点 + 同一题型），缓存 AI 结果
- Key：`SHA256(kpId + questionType + grade)`
- 缓存有效期：7 天
- 命中缓存时直接返回，不调用 AI

### 限流

- 用户级：每用户每分钟 10 次 AI 调用
- 全局级：每分钟 50 次 AI 调用（保护免费额度）
- 超限返回友好提示："AI 正忙，请稍后再试"
- 实现：ConcurrentHashMap + 滑动窗口计数

**Entity 类**：使用已有的 `ai_analysis_cache` / `ai_call_log` 表

**验收标准**：
- [ ] 每次调用写入日志
- [ ] 相同请求命中缓存
- [ ] 超过限流阈值时返回 429

---

## 2.6 AI 知识点解析 API

**目标**：给一段文本，AI 自动提取知识点（单词/短语）。为后续 PDF 分析铺路。

**API 设计**：

| 方法 | Path | 说明 |
|------|------|------|
| POST | `/api/ai/analyze/text` | 从文本提取知识点 |

**请求参数**：
```json
{
  "text": "Hello, my name is Tom. I like apples and bananas.",
  "grade": "primary",
  "bankId": 1
}
```

**AI 返回**：
```json
{
  "knowledgePoints": [
    {"content": "hello", "meaningZh": "你好", "type": "word", "difficulty": 1},
    {"content": "apple", "meaningZh": "苹果", "type": "word", "difficulty": 1},
    {"content": "banana", "meaningZh": "香蕉", "type": "word", "difficulty": 1}
  ]
}
```

**处理流程**：
```
1. 接收文本 → 计算 content_hash → 查缓存
2. 未命中 → 构造 Prompt → 调用 AI
3. 解析返回 → 校验格式
4. 通过 HTTP 调用 service-content 批量创建知识点
5. 缓存结果
6. 返回提取的知识点列表
```

**验收标准**：
- [ ] 输入英文短文能正确提取单词
- [ ] 知识点写入 knowledge_point 表
- [ ] 相同文本第二次调用命中缓存

---

## 2.7 降级策略：AI 不可用 → 模板出题

**目标**：当 AI API 不可用（超时/报错/限流耗尽）时，自动降级为模板出题。

**降级逻辑**：
```
try {
    return aiGenerate(kpId, type);  // AI 出题
} catch (Exception e) {
    log.warn("AI 出题失败，降级为模板出题", e);
    return templateGenerate(kpId, type);  // 调用 service-content 的模板引擎
}
```

**降级场景**：
- AI API 超时（>10s）
- API 返回 429（限流）
- API 返回 5xx
- AI 返回内容校验失败（重试后仍失败）

**验收标准**：
- [ ] 断网/API 不可用时仍能出题
- [ ] 降级日志正确记录
- [ ] 降级对用户透明（前端不感知）

---

## 2.8 前端：AI 出题入口

**目标**：在题库详情页增加"AI 出题"按钮，选择题型和数量后调用 AI。

**UI 要素**：
- BankDetailView 新增"AI 出题"按钮（带 sparkles 图标）
- 弹窗/抽屉：选择题型（多选）+ 数量 + 年级
- 点击后显示加载动画（"AI 正在出题..."）
- 完成后显示生成结果：成功 N 题 / 失败 M 题
- 可查看生成的题目列表

**验收标准**：
- [ ] AI 出题按钮可点击
- [ ] 加载状态友好
- [ ] 生成结果正确展示
- [ ] 错误状态有提示

---

## 2.9 前端：AI 分析等待/结果展示

**目标**：文本分析的等待页面和结果展示。

**UI 要素**：
- 输入区：粘贴英文文本（textarea）
- 分析按钮（带加载状态）
- 结果：提取的知识点列表（可勾选确认/删除）
- 确认后批量导入到指定题库

**验收标准**：
- [ ] 文本输入 + 分析流程可用
- [ ] 等待动画流畅
- [ ] 结果可编辑再确认
- [ ] 导入成功有反馈

---

## 2.10 联调 + 端到端测试

**目标**：完整跑通 AI 出题 → 练习 → 反馈流程。

**测试场景**：
1. 用户进入题库详情 → 点击 AI 出题 → 选择英译中 × 5 题 → AI 生成 → 进入练习
2. 粘贴一段文本 → AI 提取知识点 → 确认导入 → 为新知识点 AI 出题
3. AI 不可用时 → 自动降级模板出题 → 用户无感知
4. 连续调用超过限流 → 显示"请稍后再试"

**验收标准**：
- [ ] 场景 1-4 全部通过
- [ ] ai_call_log 记录正确
- [ ] 缓存命中率 > 0（重复请求）

---

## 跨服务调用方案

**service-ai 调用 service-content**：
- service-ai 需要读取知识点信息 → HTTP GET `http://localhost:8082/api/content/kps/{id}`
- service-ai 需要保存 AI 生成的题目 → HTTP POST `http://localhost:8082/api/content/questions/ai-save`
- service-ai 需要保存 AI 提取的知识点 → HTTP POST `http://localhost:8082/api/content/kps/ai-batch`
- 调用时携带内部标识（X-Internal-Call: true）或直接用 X-User-Id

**service-content 新增内部接口**：
- `POST /api/content/questions/ai-save` — 保存 AI 生成的题目（入参：question 数据 + userId）
- `POST /api/content/kps/ai-batch` — 批量创建知识点（入参：kp 数组 + bankId + userId）

---

## 数据库迁移

**无需新迁移** — `ai_analysis_cache` 和 `ai_call_log` 在 V1__init_ai.sql 中已创建。

---

## 依赖项新增

**service-ai/pom.xml**：
```xml
<!-- Spring AI - OpenAI compatible (for GitHub Models GPT-4o) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

**父 pom.xml 新增 Spring AI BOM**：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

---

## 环境准备

1. 获取 GitHub Token：https://github.com/settings/tokens
2. 确保 Token 有 `models:inference` 权限
3. 设置环境变量：`export GITHUB_TOKEN=ghp_xxxxx`
4. 确认 ll_ai 数据库存在且迁移已跑
