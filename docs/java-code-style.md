# Java 编码规范 (Java Code Style)

> **最后更新**: 2026-04-27
> **维护说明**: 当 AI Agent 因编码风格问题导致可读性下降、产生 Bug 或引发评审返工时，必须立即更新本规范，加入对应的错误案例和修正规则。新增 MapStruct 后，所有跨层对象转换必须遵循本规范第 7 节。

本规范是本项目所有 Java 代码（包括领域逻辑、应用服务、基础设施实现）的**唯一风格指南**。AI Agent 产生的任何代码都必须通过本规范的自动检查，否则 CI 将拒绝合并。

---

## 一、命名规范 —— 让代码说话

### 1.1 包命名
- **全部小写**，使用单数形式。
- **严格**遵循 DDD 分层约定的包名：
  - `com.yourcompany.project.interfaces.controller`
  - `com.yourcompany.project.interfaces.assembler`
  - `com.yourcompany.project.application.service`
  - `com.yourcompany.project.application.assembler`
  - `com.yourcompany.project.domain.model.aggregate`
  - `com.yourcompany.project.domain.service`
  - `com.yourcompany.project.domain.repository`
  - `com.yourcompany.project.infrastructure.persistence.mapper`
  - `com.yourcompany.project.infrastructure.persistence.assembler`
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
- **MapStruct 映射接口**: 以层名为前缀，以 `Mapper` 结尾，清晰表达转换方向。
  - Interface 层: `OrderInterfaceMapper`
  - Application 层: `OrderApplicationMapper`
  - Infrastructure 层: `OrderPersistenceMapper`

### 1.3 方法命名
- **MapStruct 映射方法**应直观体现源类型和目标类型，无需添加前缀。
  - ✅ `Order toEntity(CreateOrderCommand command);`
  - ✅ `OrderDTO toDto(Order entity);`
  - ❌ `Order convertCommandToEntity(CreateOrderCommand command);` (冗余)

### 1.4 变量与常量
- 变量名使用有意义的业务词汇，避免单字母。
- 常量必须使用 `static final`，并全部大写，以下划线分隔。**禁止魔法值**。

## 二、DDD 编码铁律 —— 架构活着的保证

### 2.1 领域层：拒绝贫血模型
- 实体必须有行为。**绝对禁止**只提供 getter/setter。
- 实体构造器应尽量**明确和受控**，通过工厂方法或静态构造方法创建。

### 2.2 应用层：保持轻盈
- 应用服务方法体应短小（理想不超过 20 行），仅做编排：
  1. 使用 MapStruct 将 Command 转换为领域实体。
  2. 调用实体或领域服务方法。
  3. 通过 Repository 保存实体。
  4. 使用 MapStruct 将实体转换为返回 DTO。
- **严禁**在应用层中出现表达业务规则的 `if-else`。

### 2.3 基础设施层：干净的实现
- **Repository 实现**必须注入 MyBatis-Plus 的 `BaseMapper` 和本层的 **MapStruct Mapper**，在内部完成 Entity ↔ PO 转换。

## 三、MyBatis-Plus 使用规范 —— 安全的桥梁

### 3.1 Mapper 的定义与使用
- 所有 `Mapper` 接口必须放在 `infrastructure.persistence.mapper` 包下，并使用 `@Mapper` 注解。
- 仅在 `RepositoryImpl` 中注入和调用 `Mapper`。

### 3.2 复杂查询
- 复杂查询必须在 XML 文件中定义 SQL。严禁在 Java 代码中通过 `QueryWrapper` 嵌套大量条件。

## 四、异常处理与日志 —— 透明的根基

### 4.1 异常
- **领域异常**：定义在 `domain` 层，继承 `RuntimeException`。
- **禁止吞异常**：catch 块必须做处理。

### 4.2 日志
- 使用 Lombok 的 `@Slf4j` 注解。
- 日志级别规则：error/warn/info/debug，对关键节点（如调用 MapStruct 转换失败时）记录异常详情。

## 五、Lombok 与通用工具

- 领域实体**不要使用 `@Setter`**。
- MapStruct 生成的实现类无需手动编写，确保 `componentModel = "spring"`。

## 六、MapStruct 映射规范 —— 安全转换的生命线

### 6.1 核心铁律
- **所有跨层对象转换必须通过 MapStruct Mapper 接口完成**。
- **绝对禁止**使用 `BeanUtils.copyProperties`、`ModelMapper` 或手动逐字段赋值。
- 每个 MapStruct 接口必须使用 `@Mapper(componentModel = "spring")` 注解，确保被 Spring 管理。

### 6.2 各层 Mapper 的职责与存放位置

| 层 | Mapper 接口位置 | 职责 | 注入者 |
| :--- | :--- | :--- | :--- |
| **Interface** | `interfaces/assembler/` | Request DTO ↔ Application Command/Query | Controller |
| **Application** | `application/assembler/` | Command ↔ Domain Entity<br/>Domain Entity ↔ Application DTO | ApplicationService |
| **Infrastructure** | `infrastructure/persistence/assembler/` | Domain Entity ↔ Persistence PO | RepositoryImpl |

### 6.3 标准代码模板

#### Interface 层 Mapper 示例
```java
package com.yourcompany.project.interfaces.assembler;

import com.yourcompany.project.interfaces.dto.OrderRequest;
import com.yourcompany.project.application.command.CreateOrderCommand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderInterfaceMapper {
    CreateOrderCommand toCommand(OrderRequest request);
}
```

#### Application 层 Mapper 示例
```java
package com.yourcompany.project.application.assembler;

import com.yourcompany.project.application.command.CreateOrderCommand;
import com.yourcompany.project.application.dto.OrderDTO;
import com.yourcompany.project.domain.model.aggregate.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderApplicationMapper {
    @Mapping(target = "id", ignore = true) // ID 由数据库生成
    @Mapping(target = "items", source = "items")
    Order toEntity(CreateOrderCommand command);

    OrderDTO toDto(Order entity);
}
```
#### Infrastructure 层 Mapper 示例

```java
package com.yourcompany.project.infrastructure.persistence.assembler;

import com.yourcompany.project.domain.model.aggregate.Order;
import com.yourcompany.project.infrastructure.persistence.dataobject.OrderPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {
    OrderPO toPO(Order entity);
    Order toEntity(OrderPO po);
}
```

### 6.4 注入与使用规范

+ 在需要转换的类中，直接注入对应的 MapStruct Mapper。
```java
// ✅ 正确注入
@Autowired
private OrderApplicationMapper orderApplicationMapper;
```
+ 严禁通过 Mappers.getMapper() 获取实例，失去 Spring 管理可能导致依赖问题。
+ 转换表达式尽量使用 MapStruct 内置的 source 和 expression，仅在极特殊情况下才使用 qualifiedByName。

### 6.5 禁止事项
+ 禁止在 Domain 层引入 MapStruct 或任何转换接口。Domain 层保持纯粹，不接受外部对象转换。
+ 禁止 Mapper 接口之间互相调用或循环依赖。  
+ 禁止 为了映射强行暴露实体的内部结构（如公开 setter）。如需映射特定字段，应使用 MapStruct 的 @Mapping 注解精确指定，同时保持实体封装。

### 七、自动化检查清单 (AI Agent 提交前自查)
在提交代码前，你必须逐项确认：

+ 没有魔法值，常量已定义。
+ 领域实体没有公开的 setter，行为方法命名体现业务。
+ 应用服务中没有 if-else 业务判断。
+ 没有在 domain 层导入 MapStruct、MyBatis-Plus 或 Spring MVC 的类。
+ 所有跨层对象转换都使用了 MapStruct，且 Mapper 所在层正确。
+ 所有 catch 块都有意义。
+ Mapper 未被 domain 层或 application 层直接引用。
+ 复杂 SQL 已放入 XML。
+ 日志级别正确，内容简洁但包含关键上下文。

本规范归入 Harness Engineering 的“架构约束”护栏，将以 Checkstyle 配置和自定义 Linter 形式强制执行。如果 Agent 违反了本规范中的任何一条，请不要手动修正代码，而应更新本规范或 CI 检查规则，以从根本上防止该错误重现。