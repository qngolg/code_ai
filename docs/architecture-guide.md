# 项目 DDD 四层架构指南 (Architecture Guide)

> **最后更新**: 2026-04-26
> **维护说明**: 本文件为 AI Agent 架构约束的权威来源。当 Agent 因架构理解偏差导致失败时，必须立即审查并更新本文档，形成活的反馈循环。

## 一、核心原则

本项目**严格**遵循领域驱动设计(DDD)的四层架构。核心口号：**一切始于领域，一切为了领域**。

- **Domain 层是心脏**：包含全部业务逻辑和规则，不与任何具体技术框架强耦合。
- **层间单向依赖**：上层可以依赖下层，**绝不允许**反向依赖。
- **接口定义与实现分离**：Domain 层定义接口，Infrastructure 层提供实现。

## 二、层依赖关系图 (缰绳)

这是必须刻在 Agent “脑海”里的绝对规则。**任何违反此依赖的代码都将被 CI 拒绝合并。**

```markdown
+-------------------+
| Interface (用户接口) | <-- Controllers, DTOs for API
+---------+---------+
| 依赖
+---------v---------+
| Application (应用层) | <-- Application Services, 编排任务，很薄
+---------+---------+
| 依赖
+---------v---------+
| Domain (领域层) | <-- Entities, Value Objects, Domain Services, Repository Interfaces
+---------+---------+
| 依赖
+---------v---------+
| Infrastructure (基础设施) | <-- Repository Impl, MyBatis-Plus Mapper, 外部服务实现
+-------------------+
```

**依赖规则表 (硬性约束)**:

| 分层                         | 可依赖的分层                           | 绝对禁止依赖                                 |
| :------------------------- | :------------------------------- | :------------------------------------- |
| **Interface (用户接口层)**      | Application                      | Domain, Infrastructure                 |
| **Application (应用层)**      | Domain (接口), Infrastructure (接口) | Infrastructure (具体实现类)                 |
| **Domain (领域层)**           | **无** (仅依赖JDK和必要的通用注解)           | Application, Interface, Infrastructure |
| **Infrastructure (基础设施层)** | Domain (实现其接口)                   | Application, Interface                 |

## 三、各层详细职责与标准实践

### 1. Interface Layer (用户接口层) - `com.yourcompany.project.interfaces`

**包结构示例**: `com.yourcompany.project.interfaces.controller`, `.dto`

- **职责**：
  - 接收HTTP请求，进行基础的参数校验（格式、非空）。
  - 将外部请求通过 `Assembler/Converter` 转换为 Application 层的 DTO/Command 对象。
  - 调用 Application 层的服务。
  - 将返回结果封装为 HTTP 响应。
- **可以做**：
  - 包含 `@RestController`, `@RequestMapping` 等 Spring Web 注解。
  - 使用 `@Valid` 注解进行 DTO 的基础验证。
- **绝对禁止**：
  - 包含任何业务逻辑。例如，判断“用户是否有权限执行此操作”属于应用层或领域层职责。
  - 直接调用 `Domain` 层的 `Repository` 或 `DomainService`。
  - 直接调用 `Infrastructure` 层的 `Mapper` 或具体实现。

### 2. Application Layer (应用层) - `com.yourcompany.project.application`

**包结构示例**: `com.yourcompany.project.application.service`, `.command`, `.query`, `.dto`

- **职责**：
  - 作为系统的“任务协调者”，不包含核心业务规则。
  - 接收来自 Interface 层的命令或查询对象。
  - 从 Domain 层获取领域对象，并调用其方法来执行业务逻辑。
  - 管理事务边界（使用 `@Transactional`）。
  - 处理与安全（如 `@PreAuthorize`）、日志、外部事件等非业务横切关注点相关的工作。
- **可以做**：
  - 包含 `ApplicationService` 或 `TaskCoordinator` 类。
  - 调用 `DomainService`, `Repository` 接口, `Domain` 实体行为。
  - 发布领域事件。
- **绝对禁止**：
  - 包含任何核心业务规则。例如，订单金额的计算规则必须在 Domain 层的实体或值对象中。
  - 直接注入 MyBatis-Plus 的 `BaseMapper`。
  - “泻药式”的代码，即 Application 层本身不厚，但充斥大量 if-else 代码。

### 3. Domain Layer (领域层) - `com.yourcompany.project.domain`

**包结构示例**: `com.yourcompany.project.domain.model.aggregate`, `.model.entity`, `.model.valueobject`, `.service`, `.repository`

- **职责**：
  - 这是项目的核心。包含所有业务概念、规则和逻辑。
  - **实体 (Entity)**：拥有唯一标识和生命周期的业务对象。
  - **值对象 (Value Object)**：无唯一标识，通过属性值来定义的不可变对象。
  - **聚合根 (Aggregate Root)**：一个聚合的入口，外部只能通过它修改聚合内的实体。
  - **领域服务 (Domain Service)**：当某个业务逻辑不属于单个实体或值对象时，放在这里。
  - **仓库接口 (Repository Interface)**：定义聚合根的持久化契约，与实现无关。
- **可以做**：
  - 拥有丰富的、体现业务意图的方法（如 `order.pay()`, `user.activate()`）。
  - 抛出特定于领域的异常。
  - 使用标准的 JDK 和一些通用注解。
- **绝对禁止**：
  - 导入任何 MyBatis-Plus、Spring Web、或其他基础设施层的类。
  - 直接依赖 `BaseMapper`, `ServiceImpl` 等。
  - **贫血模型**：实体只有一个空的构造器和一堆 getter/setter，而没有行为方法。这是最大的禁忌。

### 4. Infrastructure Layer (基础设施层) - `com.yourcompany.project.infrastructure`

**包结构示例**: `com.yourcompany.project.infrastructure.persistence.mapper`, `.persistence.repository`, `.external`

- **职责**：
  - 为其他层提供技术能力的支撑。
  - 为 Domain 层定义的 `Repository` 接口提供具体实现。
  - 封装对数据库、消息队列、第三方 API 等的访问。
- **可以做**：
  - 包含 MyBatis-Plus 的 `Mapper` 接口和 XML。
  - 包含 `RepositoryImpl` 类，该类实现 `Domain` 层的 `Repository` 接口，并注入 MyBatis-Plus 的 `BaseMapper`。
  - 包含数据对象 (PO)，如果与领域实体差异大，需在实现类中进行转换。
- **绝对禁止**：
  - 包含业务逻辑。
  - 让 Mapper 或 RepositoryImpl 类绕过 Domain 层，直接被 Interface 或 Application 层调用。

## 四、模块与包结构映射示例

假设有一个“订单”上下文，其代码结构应如下所示：

```markdown
com.yourcompany.project
├── interface
│ ├── controller
│ │ └── OrderController.java
│ └── dto
│ ├── OrderRequest.java
│ └── OrderResponse.java
├── application
│ ├── service
│ │ └── OrderApplicationService.java
│ └── command
│ └── CreateOrderCommand.java
├── domain
│ ├── model
│ │ ├── aggregate
│ │ │ └── Order.java // 聚合根
│ │ └── valueobject
│ │ └── OrderItem.java // 值对象
│ ├── service
│ │ └── OrderDomainService.java // 领域服务
│ └── repository
│ └── OrderRepository.java // 仓库接口
└── infrastructure
├── persistence
│ ├── mapper
│ │ └── OrderMapper.java // MyBatis-Plus Mapper
│ ├── repository
│ │ └── OrderRepositoryImpl.java // 仓库实现
│ └── dataobject
│ └── OrderPO.java // 持久化对象(可选)
└── external
└── PaymentGatewayImpl.java // 外部服务实现
```
此文档是项目的活宪法。AI Agent 生成的任何代码、提交的任何 PR，都必须经过 `arch-unit-constraints-test.groovy` 的验证，以确保不偏离此架构。

