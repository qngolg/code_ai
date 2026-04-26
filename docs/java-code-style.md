# Java 编码规范 (Java Code Style)

> **最后更新**: 2026-04-26
> **维护说明**: 当 AI Agent 因编码风格问题导致可读性下降、产生 Bug 或引发评审返工时，必须立即更新本规范，加入对应的错误案例和修正规则。

本规范是本项目所有 Java 代码（包括领域逻辑、应用服务、基础设施实现）的**唯一风格指南**。AI Agent 产生的任何代码都必须通过本规范的自动检查（Checkstyle / 自定义 Linter），否则 CI 将拒绝合并。

---

## 一、命名规范 —— 让代码说话

### 1.1 包命名
- **全部小写**，使用单数形式。
- **严格**遵循 DDD 分层约定的包名：
  - `com.yourcompany.project.interfaces.controller`
  - `com.yourcompany.project.application.service`
  - `com.yourcompany.project.domain.model.aggregate`
  - `com.yourcompany.project.domain.service`
  - `com.yourcompany.project.domain.repository`
  - `com.yourcompany.project.infrastructure.persistence.mapper`
  - `com.yourcompany.project.infrastructure.persistence.repository`

### 1.2 类与接口命名
- **实体 (Entity)**: 使用业务概念名，不加后缀。如 `Order`, `User`。
- **值对象 (Value Object)**: 不用特殊后缀，名字应体现其业务含义。如 `OrderItem`, `Address`。
- **领域服务 (Domain Service)**: 以 `Service` 结尾，名称体现其职责。如 `PointsDeductionService`。
- **仓库接口 (Repository Interface)**: 以 `Repository` 结尾。如 `OrderRepository`。**必须**定义在 `domain` 层。
- **应用服务 (Application Service)**: 以 `ApplicationService` 结尾。如 `OrderApplicationService`。
- **MyBatis-Plus Mapper**: 以 `Mapper` 结尾，定义在 `infrastructure` 层。如 `OrderMapper`。
- **仓库实现 (Repository Implementation)**: 以 `RepositoryImpl` 结尾，实现 `domain` 层的对应接口。如 `OrderRepositoryImpl`。
- **Controller**: 以 `Controller` 结尾，如 `OrderController`。

### 1.3 方法命名
- **领域实体的方法**必须体现**业务意图 (Ubiquitous Language)**。
  - ✅ `order.pay()`, `user.activate()`
  - ❌ `order.setStatus(OrderStatus.PAID)` (贫血模型恶疾)
- **应用服务方法**体现**用例**。
  - ✅ `createOrder(CreateOrderCommand command)`
  - ❌ `saveOrder(Order order)` (太技术化，非业务)
- **Repository 接口方法**应贴近持久化操作，但避免数据库术语。
  - ✅ `findByUserId(String userId)`
  - ❌ `getOrderByUserIdAndSelectFromTableJoinWithItems(String userId)`

### 1.4 变量与常量
- 变量名使用有意义的业务词汇，避免单字母。
- 常量必须使用 `static final`，并全部大写，以下划线分隔。**禁止魔法值**。
  - ✅ `private static final int MAX_RETRY_TIMES = 3;`
  - ❌ `if (status == 1)` (魔法值)

## 二、DDD 编码铁律 —— 架构活着的保证

### 2.1 领域层：拒绝贫血模型
- 实体必须有行 为。**绝对禁止**只提供 getter/setter，而让外部 Service 修改其状态。
- 实体构造器应尽量**明确和受控**，通过工厂方法或静态构造方法创建。
  ```java
  // ✅
  public static Order create(String userId, List<OrderItem> items) {
      // 验证业务规则
      return new Order(userId, items);
  }
  private Order(String userId, List<OrderItem> items) { ... }
  public void pay() { ... }
  
  // ❌
  public Order() {}
  public void setUserId(String userId) { ... }
  ```
### 2.2 应用层：保持轻盈
+ 应用服务方法体应短小（理想不超过 20 行），仅做编排：
1. 从 Repository 加载实体。
2. 调用实体或领域服务方法。
3. 通过 Repository 保存实体。
4. 发布领域事件（如果有）。

+ 严禁在应用层中出现表达业务规则的 if-else。例如：
  ```java
    // ❌ 这段逻辑属于领域层
    if (order.getTotalAmount() > 1000) {
        discount = 0.1;
    }
  ```
### 2.3 基础设施层：干净的实现
+ Repository 实现必须注入 MyBatis-Plus 的 BaseMapper，绝不泄露给其他层。
+ 如果数据对象 (PO) 与领域实体差别很大，必须在 RepositoryImpl 中完成转换，而不是让实体适应数据库结构。

## 三、MyBatis-Plus 使用规范 —— 安全的桥梁
3.1 Mapper 的定义与使用
+ 所有 Mapper 接口必须放在 infrastructure.persistence.mapper 包下，并使用 @Mapper 注解。
+ 仅在 RepositoryImpl 中注入和调用 Mapper。其他任何层不得引用 Mapper。
```java
// ✅
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderMapper orderMapper;
    // 实现方法
}
```

### 3.2 复杂查询
+ 简单的 CRUD 可直接使用 MyBatis-Plus 提供的方法。

+ 任何涉及多表联查、复杂条件、报表类查询，必须在 XML 文件中定义 SQL。严禁在 Java 代码中通过 QueryWrapper 嵌套大量条件，那会形成难以阅读的技术债务。

+ XML 文件必须放在 src/main/resources/mapper/ 下，与 Mapper 接口全路径对应。

### 3.3 逻辑删除与填充
+ 项目中必须配置统一的逻辑删除字段和自动填充策略，Agent 不要在每个实体中重复定义删除标志或更新时间填充，应使用基类或配置。

## 四、异常处理与日志 —— 透明的根基
### 4.1 异常
+ 领域异常：定义在 domain 层，继承 RuntimeException，使用业务化命名。
  + InsufficientStockException, OrderAlreadyPaidException
+ 应用层异常：用于非业务的问题（如参数校验失败），也继承 RuntimeException。
+ 禁止吞异常：catch 块必须做处理（记录日志并抛出新业务异常），绝不允许空的 catch 块。
+ 所有 API 异常应由全局异常处理器 (@ControllerAdvice) 转换为统一的 HTTP 响应，不要在 Controller 中手动 try-catch。

### 4.2 日志
+ 使用 Lombok 的 @Slf4j 注解获取日志对象，保持日志声明简单统一。
+ 关键点必须记录日志：
  + 领域服务中的重要状态变更（如订单支付成功）。
  + 应用层中外部服务调用失败（记录请求和响应概要）。
  + 基础设施层中数据库操作异常。

+ 日志级别规则：
  + error: 系统错误，需人工介入。
  + warn: 业务异常但可恢复（如用户优惠券已失效）。
  + info: 关键节点（如订单创建）。
  + debug: 调试信息，输出时使用参数化，避免字符串拼接。

### 五、Lombok 与通用工具
+ 使用 @Getter 注解，但领域实体不要使用 @Setter，确保状态只能通过业务方法修改。
+ @Builder 谨慎使用，尤其对含有业务规则的实体，改用自定义工厂方法或构造器。
+ 值对象可以使用 @Value 使其不可变。

### 六、自动化检查清单 (AI Agent 提交前自查)
在提交代码前，你必须逐项确认：
+ 没有魔法值，常量已定义。
+ 领域实体没有公开的 setter，行为方法命名体现业务。
+ 应用服务中没有 if-else 业务判断，都被委托给领域层。
+ 没有在 domain 层导入 MyBatis-Plus 或 Spring MVC 的类。
+ 所有 catch 块都有意义，没有吞异常。
+ Mapper 未被 domain 层或 application 层直接引用。
+ 复杂 SQL 已放入 XML，而非 QueryWrapper 长链。
+ 日志级别正确，内容简洁但包含关键上下文。

本规范归入 Harness Engineering 的“架构约束”护栏，将以 Checkstyle 配置和自定义 Linter 形式强制执行。如果 Agent 违反了本规范中的任何一条，请不要手动修正代码，而应更新本规范或 CI 检查规则，以从根本上防止该错误重现。
