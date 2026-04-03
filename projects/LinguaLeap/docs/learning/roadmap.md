# LinguaLeap 技术学习路线

> 以项目实践为主线，做完再复盘，把用到的知识吃透。

---

## 学习理念

```
做项目（先跑通）→ 复盘代码（为什么这么写）→ 深入原理（底层怎么运作）→ 举一反三（还能怎么用）
```

---

## 知识全景图

```mermaid
mindmap
  root((LinguaLeap<br/>技术栈))
    Java 后端
      Spring Boot
        自动配置原理
        Starter 机制
        条件注解
      Spring Cloud
        Gateway 网关
        OpenFeign 远程调用
        负载均衡
      Spring Data JPA
        Repository 模式
        JPQL / Native Query
        事务管理
      Spring Security
        JWT 鉴权
        过滤器链
        CORS 配置
      Maven
        多模块管理
        依赖传递
        BOM 版本管理
    数据层
      PostgreSQL
        SQL 基础
        索引优化
        JSON 类型
      Flyway
        版本化迁移
        回滚策略
      MinIO
        S3 协议
        对象存储
        Bucket 策略
    前端
      Vue 3
        Composition API
        响应式原理 ref/reactive
        生命周期
      TypeScript
        类型系统
        接口与泛型
        类型推断
      Pinia
        状态管理
        持久化
      Vue Router
        路由守卫
        动态路由
        懒加载
      Vite
        ESM 构建
        HMR 热更新
      SCSS
        变量与Mixin
        嵌套与模块化
    AI 应用
      Prompt Engineering
        System与User prompt
        Few-shot 示例
        输出格式控制
      Spring AI
        ChatClient
        Tool Calling
        Structured Output
      RAG
        文档解析
        文本分块
        上下文管理
      Agent
        ReAct 模式
        工具编排
        多轮对话
      多模态
        TTS 语音合成
        STT 语音识别
        PDF 多模态解析
    工程化
      Docker
        容器化
        Docker Compose
        网络与卷
      Git
        分支策略
        提交规范
      REST API
        设计规范
        状态码
        分页与筛选
      测试
        单元测试
        集成测试
        API 测试
```

---

## Phase × 知识点对照表

```mermaid
graph LR
    subgraph P0["Phase 0 ✅ 已完成"]
        P0A["Maven 多模块"]
        P0B["Spring Boot 启动"]
        P0C["Gateway + JWT"]
        P0D["JPA + Flyway"]
        P0E["Vue 3 + Router + Pinia"]
        P0F["Axios + 拦截器"]
        P0G["SCSS + 响应式布局"]
    end

    subgraph P1["Phase 1 ⬜ 题库+练习"]
        P1A["JPA 复杂查询<br/>分页/筛选"]
        P1B["REST API 设计<br/>CRUD 最佳实践"]
        P1C["业务逻辑设计<br/>出题引擎/SRS算法"]
        P1D["MinIO 文件存储"]
        P1E["PDFBox 文档解析"]
        P1F["Vue 组件化<br/>题型组件"]
        P1G["Chart.js 图表"]
        P1H["复杂状态管理<br/>练习流程"]
    end

    subgraph P2["Phase 2 ⬜ AI 智能"]
        P2A["Spring AI + Groq"]
        P2B["Prompt 模板系统"]
        P2C["PDF AI 分析<br/>RAG 模式"]
        P2D["AI 出题 + 质量校验"]
        P2E["缓存 + 限流"]
        P2F["Agent + Tool"]
        P2G["多轮对话"]
    end

    subgraph P3["Phase 3 ⬜ 语音"]
        P3A["Web Speech API"]
        P3B["TTS 朗读"]
        P3C["STT 识别"]
        P3D["AI 口语对话"]
    end

    P0 --> P1 --> P2 --> P3

    style P0 fill:#ECFDF5,stroke:#10B981
    style P1 fill:#EFF6FF,stroke:#3B82F6
    style P2 fill:#FDF2F8,stroke:#EC4899
    style P3 fill:#FEF3C7,stroke:#F59E0B
```

---

## 复盘计划

每个 Phase 完成后，按以下流程逐模块复盘：

```mermaid
flowchart TD
    A["选一个模块<br/>比如: JWT 鉴权"] --> B["读代码<br/>逐行讲解"]
    B --> C["画流程图<br/>请求怎么走的"]
    C --> D["讲原理<br/>Spring 底层做了什么"]
    D --> E["提问题<br/>如果改成XXX会怎样"]
    E --> F["做练习<br/>你自己尝试改一个功能"]
    F --> G["总结笔记<br/>写入学习文档"]

    style A fill:#EEF2FF,stroke:#6366F1
    style D fill:#FDF2F8,stroke:#EC4899
    style F fill:#ECFDF5,stroke:#10B981
    style G fill:#FEF3C7,stroke:#F59E0B
```
