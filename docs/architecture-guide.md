# 项目 DDD 四层架构指南 (Architecture Guide)

> **最后更新**: 2026-04-26
> **维护说明**: 本文件为 AI Agent 架构约束的权威来源。当 Agent 因架构理解偏差（特别是跨层对象映射方式）导致失败时，必须立即审查并更新本文档，形成活的反馈循环。

## 一、核心原则

本项目**严格**遵循领域驱动设计(DDD)的四层架构。核心口号：**一切始于领域，一切为了领域**。

-   **Domain 层是心脏**：包含全部业务逻辑和规则，不与任何具体技术框架强耦合。
-   **层间单向依赖**：上层可以依赖下层，**绝不允许**反向依赖。
-   **接口定义与实现分离**：Domain 层定义接口，Infrastructure 层提供实现。
-   **跨层对象隔离**：各层拥有自己的数据对象，**严禁**跨层直接传递实体或数据库 PO。必须使用 **MapStruct** 进行对象转换。

## 二、层依赖关系图 (缰绳)

这是必须刻在 Agent “脑海”里的绝对规则。**任何违反此依赖的代码都将被 CI 拒绝合并。**
```markdown
+-------------------+
| Interface (用户接口) | <-- Controllers, DTOs (使用 MapStruct 转换为 Command)
+---------+---------+
| 依赖
+---------v---------+
| Application (应用层) | <-- Application Services, Commands, Queries (使用 MapStruct 转换为 Entity)
+---------+---------+
| 依赖
+---------v---------+
| Domain (领域层) | <-- Entities, Value Objects, Domain Services, Repository Interfaces
+---------+---------+
| 依赖
+---------v---------+
| Infrastructure (基础设施) | <-- Repository Impl, MyBatis-Plus Mapper, 数据对象 (PO) (使用 MapStruct 与 Entity 转换)
+-------------------+
```


**依赖规则表 (硬性约束)**:

| 分层 | 可依赖的分层 | 绝对禁止依赖 | 对象转换职责 (MapStruct) |
| :--- | :--- | :--- | :--- |
| **Interface** | Application (及其 DTO/Command) | Domain, Infrastructure | 将外部请求 DTO 转换为 Application 层的 Command/Query |
| **Application** | Domain (实体、接口) | Infrastructure (具体实现) | 将 Command 转换为 Domain Entity；将 Entity 转换为返回 DTO |
| **Domain** | **无** (仅依赖JDK和通用注解) | Application, Interface, Infrastructure | **禁止**使用 MapStruct，保持纯粹性 |
| **Infrastructure** | Domain (实现其接口) | Application, Interface | 将 MyBatis-Plus PO 与 Domain Entity 互相转换 |

## 三、各层详细职责与标准实践

### 1. Interface Layer (用户接口层) - `com.yourcompany.project.interfaces`

**包结构示例**: `com.yourcompany.project.interfaces.controller`, `.dto`, `.assembler`

-   **职责**：
    -   接收HTTP请求，进行基础的参数校验（格式、非空）。
    -   通过 **MapStruct Mapper 接口**（在本层 `assembler` 包内），将外部请求 DTO 转换为 Application 层的 Command 对象。
    -   调用 Application 层的服务。
    -   将返回结果封装为 HTTP 响应。
-   **可以做**：
    -   包含 `@RestController`, `@RequestMapping` 等 Spring Web 注解。
    -   使用 `@Valid` 注解进行 DTO 的基础验证。
    -   定义 `@Mapper(componentModel = "spring")` 的 MapStruct 接口。
-   **绝对禁止**：
    -   包含任何业务逻辑。
    -   直接调用 `Domain` 层的 `Repository` 或 `DomainService`。
    -   直接调用 `Infrastructure` 层的 `Mapper` 或具体实现。
    -   使用 `BeanUtils.copyProperties` 或手动 setter 进行对象转换。

### 2. Application Layer (应用层) - `com.yourcompany.project.application`

**包结构示例**: `com.yourcompany.project.application.service`, `.command`, `.query`, `.dto`, `.assembler`

-   **职责**：
    -   作为系统的“任务协调者”，不包含核心业务规则。
    -   接收来自 Interface 层的 Command/Query 对象。
    -   通过 **MapStruct Mapper 接口**（在本层 `assembler` 包内），将 Command 转换为 Domain 实体/值对象。
    -   从 Domain 层获取领域对象，并调用其方法来执行业务逻辑。
    -   管理事务边界（使用 `@Transactional`）。
-   **可以做**：
    -   包含 `ApplicationService` 类。
    -   调用 `DomainService`, `Repository` 接口, `Domain` 实体行为。
    -   定义 MapStruct 接口用于 Command ↔ Entity 转换。
-   **绝对禁止**：
    -   包含任何核心业务规则。
    -   直接注入 MyBatis-Plus 的 `BaseMapper`。
    -   将 Domain Entity 直接返回给 Interface 层（必须转换为本层 DTO）。
    -   使用手动赋值进行对象转换。

### 3. Domain Layer (领域层) - `com.yourcompany.project.domain`

**包结构示例**: `com.yourcompany.project.domain.model.aggregate`, `.model.entity`, `.model.valueobject`, `.service`, `.repository`

-   **职责**：
    -   这是项目的核心。包含所有业务概念、规则和逻辑。
-   **可以做**：
    -   拥有丰富的、体现业务意图的方法。
    -   抛出特定于领域的异常。
-   **绝对禁止**：
    -   导入任何 MapStruct、MyBatis-Plus、Spring Web 的类。
    -   包含任何对象转换逻辑（那是外层 MapStruct 的工作）。
    -   **贫血模型**：实体只有 getter/setter。

### 4. Infrastructure Layer (基础设施层) - `com.yourcompany.project.infrastructure`

**包结构示例**: `com.yourcompany.project.infrastructure.persistence.mapper`, `.persistence.repository`, `.persistence.assembler`, `.persistence.dataobject`

-   **职责**：
    -   封装对数据库、第三方 API 等的访问。
    -   通过 **MapStruct Mapper 接口**（在本层 `persistence.assembler` 包内），将持久化对象 (PO) 与 Domain Entity 互相转换。
-   **可以做**：
    -   包含 MyBatis-Plus 的 `Mapper` 接口和 XML。
    -   包含 `RepositoryImpl` 类，注入 `BaseMapper` 并完成 Entity ↔ PO 转换。
-   **绝对禁止**：
    -   包含业务逻辑。
    -   让 Mapper 或 RepositoryImpl 类绕过 Domain 层，直接被 Interface 或 Application 层调用。
    -   将 PO 直接泄露到 Domain 层或 Application 层。

## 四、模块与包结构映射示例 (含 MapStruct)

假设有一个“订单”上下文，其代码结构应如下所示：
```markdown
com.yourcompany.project
├── interface
│ ├── controller
│ │ └── OrderController.java
│ ├── dto
│ │ ├── OrderRequest.java // 请求 DTO
│ │ └── OrderResponse.java // 响应 DTO
│ └── assembler
│ └── OrderInterfaceMapper.java // MapStruct: Request DTO ↔ Application Command
├── application
│ ├── service
│ │ └── OrderApplicationService.java
│ ├── command
│ │ └── CreateOrderCommand.java // 应用层命令对象
│ ├── dto
│ │ └── OrderDTO.java // 应用层返回的 DTO
│ └── assembler
│ └── OrderApplicationMapper.java // MapStruct: Command ↔ Domain Entity, Entity ↔ DTO
├── domain
│ ├── model
│ │ ├── aggregate
│ │ │ └── Order.java // 聚合根 (禁止 MapStruct)
│ │ └── valueobject
│ │ └── OrderItem.java // 值对象
│ ├── service
│ │ └── OrderDomainService.java
│ └── repository
│ └── OrderRepository.java // 仓库接口
└── infrastructure
├── persistence
│ ├── mapper
│ │ └── OrderMapper.java // MyBatis-Plus Mapper
│ ├── dataobject
│ │ └── OrderPO.java // 持久化对象
│ ├── assembler
│ │ └── OrderPersistenceMapper.java // MapStruct: Entity ↔ PO
│ └── repository
│ └── OrderRepositoryImpl.java
└── external
└── PaymentGatewayImpl.java
```

**MapStruct 接口定义铁律**：
-   每个层的 `assembler` 包下的 Mapper 接口，**只能**处理本层与其直接依赖层之间的对象转换。
-   例如，Interface 层的 Mapper 不能处理 Entity ↔ PO 的转换。
-   所有 MapStruct 接口必须使用 `@Mapper(componentModel = "spring")` 注解。

---

此文档是项目的活宪法。AI Agent 生成的任何代码、提交的任何 PR，都必须经过 `arch-unit-constraints-test.groovy` 的验证，以确保不偏离此架构。