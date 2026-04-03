# Java 后端知识详解

> Phase 0-1 涉及的 Spring Boot / JPA / Maven 核心知识点。
> 复盘时逐项展开，结合项目代码讲解。

---

## Spring Boot 核心

### 1. 自动配置（Auto-Configuration）

**项目中的体现**：
- `@SpringBootApplication` 一个注解启动整个应用
- `application.yml` 配一下数据库地址，JPA 就自动连上了
- 加了 `spring-boot-starter-web` 依赖，Tomcat 自动内嵌

**复盘要点**：
- [ ] `@SpringBootApplication` 包含哪 3 个注解
- [ ] Spring Boot 怎么知道要配置什么（spring.factories / AutoConfiguration）
- [ ] `application.yml` 的属性怎么映射到 Java 对象的

### 2. 依赖注入（DI）

**项目中的体现**：
```java
@RestController
public class AuthController {
    private final AuthService authService;  // 怎么来的？

    public AuthController(AuthService authService) {
        this.authService = authService;  // Spring 自动注入
    }
}
```

**复盘要点**：
- [ ] `@Service`, `@Repository`, `@Controller` 有什么区别
- [ ] 构造器注入 vs `@Autowired` 字段注入，为什么推荐前者
- [ ] Bean 的生命周期是什么样的

### 3. REST API 设计

**项目中的体现**：
```java
@GetMapping("/api/content/banks")         // 列表
@GetMapping("/api/content/banks/{id}")    // 详情
@PostMapping("/api/content/banks")        // 创建
@PutMapping("/api/content/banks/{id}")    // 更新
@DeleteMapping("/api/content/banks/{id}") // 删除
```

**复盘要点**：
- [ ] RESTful 设计 6 原则
- [ ] HTTP 状态码选择规范（200/201/204/400/401/403/404）
- [ ] 分页参数设计（page/size vs cursor）
- [ ] 统一响应格式 `ApiResponse<T>` 的设计

---

## Spring Data JPA

### 1. Repository 模式

**项目中的体现**：
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    // Spring 自动生成 SQL：SELECT * FROM users WHERE username = ?
}
```

**复盘要点**：
- [ ] JpaRepository 提供了哪些现成方法
- [ ] 方法命名查询的规则（findBy/countBy/deleteBy）
- [ ] `@Query` 自定义 JPQL / Native SQL 的区别
- [ ] 分页 `Pageable` 和 `Page<T>` 的用法

### 2. Entity 映射

**复盘要点**：
- [ ] `@Entity`, `@Table`, `@Column` 的作用
- [ ] 主键策略（`@GeneratedValue` 的几种 strategy）
- [ ] `@OneToMany`, `@ManyToOne` 关联关系
- [ ] 懒加载 vs 急加载，N+1 问题

### 3. 事务管理

**复盘要点**：
- [ ] `@Transactional` 放在哪里合适
- [ ] 事务传播机制（REQUIRED / REQUIRES_NEW）
- [ ] 什么情况下事务会失效（自调用、非 public 方法）

---

## Spring Cloud Gateway

### 1. 路由配置

**项目中的体现**（gateway/application.yml）：
```yaml
spring.cloud.gateway.routes:
  - id: user-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/auth/**, /api/user/**
```

**复盘要点**：
- [ ] Gateway 和 Nginx 反向代理的区别
- [ ] Predicate（断言）有哪些类型
- [ ] Filter（过滤器）的执行顺序
- [ ] 为什么 Gateway 用 WebFlux 而不是 MVC

### 2. JWT 过滤器

**复盘要点**：
- [ ] JWT 的三部分结构（Header.Payload.Signature）
- [ ] 为什么用 JWT 而不是 Session
- [ ] Token 过期怎么处理（刷新 Token 机制）
- [ ] 网关层鉴权 vs 服务层鉴权的优劣

---

## Maven 多模块

**项目结构**：
```
backend/
├── pom.xml          ← 父 POM
├── common/          ← 公共模块
├── gateway/
├── service-user/
├── service-content/
└── service-ai/
```

**复盘要点**：
- [ ] 父 POM 的 `<dependencyManagement>` 和 `<dependencies>` 区别
- [ ] 模块间依赖怎么声明
- [ ] `mvn install -pl common -am` 各参数含义
- [ ] 为什么要抽 common 模块

---

## PostgreSQL + Flyway

### 数据库设计
**复盘要点**：
- [ ] 主键用 BIGSERIAL 还是 UUID
- [ ] 索引设计原则（什么字段该加索引）
- [ ] `VARCHAR(n)` vs `TEXT` 的选择
- [ ] 外键约束 `ON DELETE CASCADE` vs `SET NULL`

### Flyway 迁移
**复盘要点**：
- [ ] 迁移文件命名规则（V1__, V2__）
- [ ] 为什么不能修改已执行的迁移文件
- [ ] 迁移失败怎么回滚
- [ ] 开发环境 vs 生产环境的迁移策略

---

## 待复盘文件清单

Phase 0 完成后，逐个文件复盘：

| 文件 | 涉及知识点 |
|------|-----------|
| `backend/pom.xml` | Maven 父 POM、版本管理 |
| `common/util/JwtUtil.java` | JWT 生成/解析、HMAC 签名 |
| `common/dto/ApiResponse.java` | 泛型、统一响应 |
| `gateway/filter/JwtAuthFilter.java` | WebFlux 过滤器、响应式编程 |
| `gateway/application.yml` | Gateway 路由配置 |
| `service-user/AuthService.java` | BCrypt、事务、业务逻辑 |
| `service-user/AuthController.java` | REST 控制器、参数校验 |
| `service-user/UserRepository.java` | JPA Repository |
| `frontend/src/api/http.ts` | Axios 拦截器、Token 管理 |
| `frontend/src/stores/user.ts` | Pinia 状态管理 |
| `frontend/src/router/index.ts` | 路由守卫、懒加载 |
