# MapStruct 使用规范 (MapStruct Usage Guide)

> **最后更新**: 2026-04-27
> **维护说明**: 当 AI Agent 因对象映射方式不当（如使用 BeanUtils、映射位置错误、数据丢失）导致 Bug 或架构漂移时，必须立即更新本文档，补充对应的错误案例与修正规则。本规范是 Harness 中“架构约束”护栏的关键部分。

## 一、核心原则

1.  **唯一合法映射工具**：项目内所有 Java Bean 之间的转换（DTO、Command、Query、Entity、PO 等）**必须且仅能**通过 MapStruct 完成。其他任何方式——包括 `BeanUtils.copyProperties`、`ModelMapper`、JSON 序列化中转、手动 setter 赋值——均为**非法**。
2.  **映射代码即基础设施**：MapStruct 接口本身属于架构的一部分，其命名、位置、依赖关系必须严格遵守本文档。
3.  **Domain 层豁免**：领域层（`domain` 包及其子包）内**禁止**出现任何 MapStruct 接口，也不得依赖任何 MapStruct 类库。领域代码保持绝对纯粹。
4.  **测试即验证**：任何新的 Mapper 接口必须伴随对应的 Spock 单元测试（见第 6 节）。

## 二、依赖与配置

### 2.1 Maven 依赖 (pom.xml)
```xml
<properties>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                    </path>
                    <!-- 如果 Lombok 和 MapStruct 一起使用，确保顺序正确 -->
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```
### 2.2 注解约定
+ 所有 Mapper 接口必须使用：
```java
@Mapper(componentModel = "spring")
需要忽略某个字段时，使用 @Mapping(target = "xxx", ignore = true)。
```
+ 需要自定义转换逻辑时，应定义 default 方法或使用 expression，但必须保持简单，复杂逻辑应提取为独立的转换工具类。

## 三、分层 Mapper 职责与命名规范
项目中的 MapStruct 接口按 DDD 层级严格划分职责，各层 Mapper 只能在其合法范围内工作。

### 3.1 Interface 层 Mapper
位置: com.yourcompany.project.interfaces.assembler
命名: {领域上下文}InterfaceMapper, 例如 OrderInterfaceMapper
职责:
+ 将 Request DTO → Application 层的 Command 或 Query 对象。
+ 若需要，也可完成 Response 对象 ← Application 层的 DTO（但通常这个转换发生在 Application 层，Interface 层直接返回 Application DTO）。
模板:
```java
@Mapper(componentModel = "spring")
public interface OrderInterfaceMapper {
    CreateOrderCommand toCommand(OrderRequest request);
}
```

### 3.2 Application 层 Mapper
位置: com.yourcompany.project.application.assembler
命名: {领域上下文}ApplicationMapper, 例如 OrderApplicationMapper
职责:
+ Command → Domain Entity/ValueObject。
+ Domain Entity → Application 层的 DTO（用于返回给 Interface 层）。
模板:
```java
@Mapper(componentModel = "spring")
public interface OrderApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderItems", source = "items")
    Order toEntity(CreateOrderCommand command);

    OrderDTO toDto(Order entity);

    List<OrderDTO> toDtoList(List<Order> entities);
}
```

### 3.3 Infrastructure 层 Mapper
位置: com.yourcompany.project.infrastructure.persistence.assembler
命名: {领域上下文}PersistenceMapper, 例如 OrderPersistenceMapper
职责:
+ Domain Entity → 持久化对象 (PO)。
+ PO → Domain Entity。
模板:
```java
@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {
    OrderPO toPO(Order entity);
    Order toEntity(OrderPO po);
}
```
### 3.4 禁止跨层映射
+ 绝不允许一个 Mapper 同时处理跨两个层级的转换（如 Interface Mapper 直接处理 Entity ↔ PO）。
+ 绝不允许 Mapper 之间相互调用或循环依赖。
## 四、映射关系定义规则
### 4.1 同名字段
+ MapStruct 会自动映射同名字段，无需显式配置。
+ 如果你不希望某个同名字段被映射，必须显式使用 ignore = true。

### 4.2 不同名字段
+ 使用 @Mapping(source = "源字段", target = "目标字段") 显式指定。

例子：@Mapping(target = "orderItems", source = "items")。

### 4.3 类型转换
+ 简单的类型转换（如 int ↔ String，Date ↔ LocalDateTime）MapStruct 可自动处理。
+ 复杂转换（如 枚举 ↔ 数据库码值，或格式化）应通过 expression 或 @Named 方法实现，但必须简单明了。
+ 禁止在 expression 中编写超过 5 行的代码逻辑。超出部分应提取到独立的转换组件（如 EnumConverter），然后通过 uses 属性引入。
```java
@Mapper(componentModel = "spring", uses = {EnumConverter.class})
public interface OrderApplicationMapper { ... }
```

### 4.4 忽略字段
+ 需要忽略的字段一定是在当前上下文中安全且不可设置的字段（如数据库自增 ID、创建时间等）。
+ 必须为每个忽略的字段添加注释说明原因。
```java
@Mapping(target = "id", ignore = true) // 自增主键，由数据库生成
@Mapping(target = "createdAt", ignore = true) // 自动填充
```

## 五、禁止事项清单
+ [红线] 禁止在 Domain 层导入或使用 MapStruct 的任何类。
+ [红线] 禁止使用 BeanUtils.copyProperties 或类似的反射工具进行对象拷贝。
+ [红线] 禁止在 Mapper 接口中编写任何业务逻辑（如 if-else 判断业务状态）。映射仅做数据搬运。
+ [红线] 禁止在 expression 中写长表达式或进行数据库调用、远程服务调用。    
+ [红线] 禁止在实体中为映射而公开 setter 或破坏封装的方法。 如果某个字段必须设置，请通过构造器或静态工厂方法；若无法避免，在 Mapper 中标记为 ignore 并手写转换。
+ [红线] 禁止在 expression 中写长表达式或进行数据库调用、远程服务调用。
+ [红线] 禁止在实体中为映射而公开 setter 或破坏封装的方法。 如果某个字段必须设置，请通过构造器或静态工厂方法；若无法避免，在 Mapper 中标记为 ignore 并手写转换。
+ [重要] 禁止在 Controller 或 ApplicationService 中直接注入 Infrastructure 层的 Mapper。Interface 层只能注入自己层的 Mapper，Application 层只能注入自己层的 Mapper，Infrastructure 层的 Mapper 只能注入到 RepositoryImpl 中。

## 六、测试要求
每个自定义的 Mapper 接口必须有对应的 Spock 单元测试，验证映射的正确性，特别是不同名字段、类型转换、忽略字段。
测试类命名: {Mapper接口名}Spec，如 OrderApplicationMapperSpec。
位置: 与接口同在测试源集下镜像包结构，如 src/test/groovy/com/yourcompany/project/application/assembler/OrderApplicationMapperSpec.groovy。
测试模板:
```groovy
class OrderApplicationMapperSpec extends Specification {

    def mapper = new OrderApplicationMapperImpl() // MapStruct 生成的实现

    def "将 CreateOrderCommand 正确映射为 Order 实体"() {
        given: "一个完整的 Command 对象"
        def command = new CreateOrderCommand()
        command.setItems(/* ... */)

        when: "执行映射"
        def order = mapper.toEntity(command)

        then: "字段正确转换"
        order.id == null  // 忽略
        order.orderItems.size() == command.items.size()
    }

    def "将 Order 实体正确映射为 OrderDTO"() {
        given: "一个完整的实体对象"
        def entity = new Order(/* ... */)

        when: "执行映射"
        def dto = mapper.toDto(entity)

        then: "DTO 包含所有必要信息"
        dto.orderId == entity.id
    }
}
```

## 七、性能与安全注意事项
+ MapStruct 在编译期生成实现，所以运行时性能与手写代码相同，务必确保编译配置正确。

+ 所有 Mapper 是 Spring 单例 Bean，必须是线程安全的。不要在 Mapper 接口中持有任何状态变量。

+ 当转换逻辑中需要依赖外部服务时（如加解密字段），绝对不要把服务注入到 Mapper 中。应该在调用 Mapper 之前或之后在服务层手动处理该特殊字段，然后用 @Mapping(target = "xxx", ignore = true) 忽略它。

## 八、AI Agent 常见错误与反馈
如果 Agent 在生成代码时违反本规范，你将会在 CI 或代码审查中看到如下典型错误模式：

| 常见错误 | 修正方案 |
| :--- | :--- |
| 在 Domain 实体上使用 @Builder 或 @Setter 只是为了 MapStruct 能映射 | 让实体通过工厂方法创建，在 Mapper 中标记那些需要外部设置的字段为 ignore，然后手动实现转换 |
| 在 Controller 中注入 OrderApplicationMapper 和 OrderPersistenceMapper 混合使用 | 严格分层，Controller 只应使用 Interface 层 Mapper |
| 在 ApplicationService 中直接使用 BeanUtils.copyProperties | 替换为注入的 Application 层 Mapper |
| MapStruct 接口定义在 domain 包中 | 立即移动到正确的 assembler 包 |

本规范是项目 Harness 系统的重要组成部分，与 architecture-guide.md、java-code-style.md 互为补充。所有 AI Agent 生成的映射代码必须通过以此规范为基础的自动化规则检验。