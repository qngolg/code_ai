# MyBatis-Plus 使用规范 (MyBatis-Plus Usage Guide)

> **最后更新**: 2026-04-27
> **维护说明**: 当 AI Agent 因 MyBatis-Plus 使用不当（如 Mapper 泄露到上层、复杂查询滥用 QueryWrapper、Entity 与 PO 混淆）导致架构污染或性能问题时，必须立即更新本文档，补充错误案例与修正规则。本规范是 Harness 中“架构约束”护栏的关键部分。

## 一、核心原则

1.  **基础设施层专属**：MyBatis-Plus 的所有类（`BaseMapper`、`IService`、`QueryWrapper` 等）**只能**出现在 `infrastructure` 层。其他任何层不得导入、引用或间接依赖这些类。
2.  **PO 隔离**：数据库持久化对象（PO）**仅在** `infrastructure.persistence.dataobject` 包内定义，**绝不**向 Domain 层或 Application 层泄露。
3.  **Entity ↔ PO 转换**：所有实体与 PO 的转换必须通过 MapStruct 的 `PersistenceMapper` 完成，严禁手动赋值。
4.  **Repository 模式**：Domain 层定义 `Repository` 接口，Infrastructure 层通过注入 `BaseMapper` 来实现该接口。

## 二、依赖隔离铁律

### 2.1 各层对 MyBatis-Plus 的可见性

| 分层 | 可否使用 MyBatis-Plus | 说明 |
| :--- | :--- | :--- |
| **Interface** | ❌ 绝对禁止 | Controller 和 DTO 中不得出现任何 MP 类 |
| **Application** | ❌ 绝对禁止 | ApplicationService 中不得注入或使用 MP 类 |
| **Domain** | ❌ 绝对禁止 | 领域层保持纯粹，不依赖任何 ORM 框架 |
| **Infrastructure** | ✅ 唯一合法使用层 | 仅在本层内使用，对外暴露 Domain 接口 |

### 2.2 Maven 依赖范围
虽 MyBatis-Plus 多为全局依赖，但在编码时必须确保其导入是**仅在 infrastructure 层**发生，通过 ArchUnit 自动化检查实施。

## 三、标准目录结构与模板

### 3.1 Infrastructure 层持久化相关结构
```markdown
infrastructure/persistence/
├── mapper
│ └── OrderMapper.java # MyBatis-Plus Mapper 接口
├── dataobject
│ └── OrderPO.java # 持久化对象 (PO)
├── assembler
│ └── OrderPersistenceMapper.java # MapStruct: Entity ↔ PO
└── repository
└── OrderRepositoryImpl.java # 实现 Domain 层的 Repository 接口
```

### 3.2 完整代码模板

#### Domain 层 - 仓库接口
```java
// com.yourcompany.project.domain.repository.OrderRepository
public interface OrderRepository {
    Optional<Order> findById(Long id);
    Order save(Order order);
    void delete(Order order);
    List<Order> findByUserId(String userId);
}
```
#### Infrastructure 层 - 持久化对象 (PO)
```java
// com.yourcompany.project.infrastructure.persistence.dataobject.OrderPO
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@TableName("tb_order")
public class OrderPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String userId;
    // ... 其他字段，使用标准 getter/setter
    // 注意：PO 允许使用 getter/setter，它是纯粹的数据容器
}
```
#### Infrastructure 层 - MyBatis-Plus Mapper
```java
// com.yourcompany.project.infrastructure.persistence.mapper.OrderMapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper // MyBatis 注解，标识这是一个 Mapper Bean
public interface OrderMapper extends BaseMapper<OrderPO> {
    // 只有 BaseMapper 不够用时，才在此处声明自定义方法
    // 复杂查询必须放在 XML 中，见 5.3 节
    List<OrderPO> selectOrdersWithItems(@Param("userId") String userId);
}
```

#### Infrastructure 层 - MapStruct 持久化映射器
```java
// com.yourcompany.project.infrastructure.persistence.assembler.OrderPersistenceMapper
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {
    OrderPO toPO(Order entity);
    Order toEntity(OrderPO po);
    List<Order> toEntityList(List<OrderPO> poList);
}
```
#### Infrastructure 层 - 仓库实现
```java
// com.yourcompany.project.infrastructure.persistence.repository.OrderRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    @Autowired
    private OrderMapper orderMapper;  // MP 的 Mapper

    @Autowired
    private OrderPersistenceMapper persistenceMapper; // MapStruct 映射器

    @Override
    public Optional<Order> findById(Long id) {
        OrderPO po = orderMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(persistenceMapper.toEntity(po));
    }

    @Override
    public Order save(Order order) {
        OrderPO po = persistenceMapper.toPO(order);
        if (order.getId() == null) {
            orderMapper.insert(po);
        } else {
            orderMapper.updateById(po);
        }
        return persistenceMapper.toEntity(po); // 返回更新后的实体（含 ID）
    }

    @Override
    public void delete(Order order) {
        orderMapper.deleteById(order.getId());
    }

    @Override
    public List<Order> findByUserId(String userId) {
        List<OrderPO> poList = orderMapper.selectList(
            new LambdaQueryWrapper<OrderPO>().eq(OrderPO::getUserId, userId)
        );
        return persistenceMapper.toEntityList(poList);
    }
}
```

## 四、BaseMapper 与 IService 使用规则
### 4.1 只使用 BaseMapper，禁止使用 IService
+ 本项目统一使用 BaseMapper<T> 接口，因其更轻量、更贴近原生 MyBatis 语义。
+ 严禁使用 MyBatis-Plus 的 IService<T> 或 ServiceImpl<T>。这会导致业务逻辑误放入“Service”层，破坏 DDD 分层。

### 4.2 条件构造器 (QueryWrapper) 使用规范
+ QueryWrapper 或 LambdaQueryWrapper 只能在 RepositoryImpl 方法内部使用。
+ 构建查询条件时，优先使用 LambdaQueryWrapper，以确保类型安全并避免字段名字符串硬编码。
+ 单个方法内的条件构造不能超过 5 个条件。超过时，说明查询过于复杂，必须放入 XML 或提取为专门的 Query 对象。
```java
// ✅ 正确：简单条件，使用 LambdaQueryWrapper
List<OrderPO> pos = orderMapper.selectList(
    new LambdaQueryWrapper<OrderPO>()
        .eq(OrderPO::getUserId, userId)
        .eq(OrderPO::getStatus, status)
);

// ❌ 错误：条件过多，耦合性强
List<OrderPO> pos = orderMapper.selectList(
    new LambdaQueryWrapper<OrderPO>()
        .eq(OrderPO::getUserId, userId)
        .eq(OrderPO::getStatus, status)
        .ge(OrderPO::getAmount, minAmount)
        .le(OrderPO::getAmount, maxAmount)
        .in(OrderPO::getType, types)
        .orderByDesc(OrderPO::getCreatedAt)
        // ... 这应该放到 XML 中
);
```

## 五、复杂查询与 XML 配置
### 5.1 必须使用 XML 的场景
+ 多表联查（JOIN）。
+ 超过 5 个查询条件。
+ 动态条件组合（<if>, <choose> 等动态 SQL）。
+ 报表、统计类查询。
+ 返回结果不是单一 PO 的映射（如自定义 ResultMap）。

### 5.2 XML 文件位置与命名规范
+ 路径：src/main/resources/mapper/
+ 文件名：与 Mapper 接口的简单类名一致，如 OrderMapper.xml。
+ 命名空间：必须与 Mapper 接口的全限定类名一致。

### 5.3 XML 模板
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.yourcompany.project.infrastructure.persistence.mapper.OrderMapper">
    <resultMap id="OrderWithItemsMap" type="com.yourcompany.project.infrastructure.persistence.dataobject.OrderPO">
        <id property="id" column="order_id"/>
        <!-- 其他字段映射 -->
    </resultMap>
    <select id="selectOrdersWithItems" resultMap="OrderWithItemsMap">
        SELECT o.*, oi.*
        FROM tb_order o
        LEFT JOIN tb_order_item oi ON o.id = oi.order_id
        WHERE o.user_id = #{userId}
        <if test="status != null">
            AND o.status = #{status}
        </if>
    </select>
</mapper>
```
六、逻辑删除与自动填充
6.1 逻辑删除
+ 项目必须使用 MyBatis-Plus 的逻辑删除功能，不要手动在 SQL 或代码中添加 deleted = 0 条件。
+ 全局配置逻辑删除字段和值，例如：
```yml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```
+ PO 中定义 isDeleted 字段（通常为 Integer 类型），无需在 Entity 中暴露此技术字段，通过 MapStruct 的 ignore 忽略它。

6.2 自动填充
+ 创建时间 (createdAt)、更新时间 (updatedAt) 等字段，使用 MyBatis-Plus 的 MetaObjectHandler 全局处理。
+ 定义统一填充器：
```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

## 七、分页与批量处理
### 7.1 分页
+ 使用 MyBatis-Plus 的 Page<T> 对象进行分页查询。
+ 分页代码只能在 RepositoryImpl 中编写，对外暴露 Domain 层理解的 PageResult 或 List。
示例：
```java
public List<Order> findByPage(int page, int size) {
    Page<OrderPO> poPage = new Page<>(page, size);
    Page<OrderPO> result = orderMapper.selectPage(poPage, new LambdaQueryWrapper<>());
    return persistenceMapper.toEntityList(result.getRecords());
}
```
### 7.2 批量操作
+ 批量插入、更新等操作，优先使用 MyBatis-Plus 的便捷方法（如 saveBatch 需使用扩展，但请避免使用 ServiceImpl）。
+ 标准的批量插入建议通过注入 SqlSessionTemplate 或直接使用 Mapper 内置方法。
+ 严禁在循环中逐条调用 insert/update，会产生严重性能问题。

## 八、禁止事项清单
+ [红线] 禁止在 Domain 层、Application 层、Interface 层导入任何 MyBatis-Plus 类。
+ [红线] 禁止在 Domain Entity 上使用 MyBatis-Plus 的注解（@TableName, @TableId 等）。 这些注解只在 PO 上使用。
+ [红线] 禁止将 PO 直接返回给 Application 层或 Interface 层。 必须在 RepositoryImpl 中通过 MapStruct 转为实体。
+ [红线] 禁止在 ApplicationService 中注入 BaseMapper 或 IService。
+ [重要] 禁止使用 IService<T> 或 ServiceImpl<T>。
+ [重要] 禁止在 XML 外使用 QueryWrapper 构造超过 5 个条件的查询。
+ [重要] 禁止在循环中进行数据库操作。

九、AI Agent 常见错误与反馈
| 常见错误 | 修正方案 |
| :--- | :--- |
| 在 ApplicationService 中直接注入 OrderMapper | 改为注入 OrderRepository（Domain 接口），由 RepositoryImpl 持有 Mapper |
| 在 Domain Entity 上使用 @TableName 注解 | 分离出独立的 PO 类，去掉 Entity 上的 ORM 注解 |
| 在 Controller 中直接操作 QueryWrapper 并查询 | 查询逻辑必须下沉到 Repository，Controller 只调用 Application 服务 |
| 复杂查询全部用 QueryWrapper 写在 Java 代码中 | 超过 5 个条件或多表联查，必须移入 XML |
| 在循环中逐条 insert | 改用批量插入方法 |
| 将 OrderPO 作为 Application 层方法的参数或返回值 | 在 RepositoryImpl 中完成 PO ↔ Entity 转换，对外只暴露 Entity |
| 手动编写 deleted = 0 过滤条件 | 使用 MP 全局逻辑删除配置，不要手动加此条件 |

本规范是项目 Harness 系统的重要组成部分，与 architecture-guide.md、java-code-style.md、mapstruct-usage.md 共同构成完整的架构约束。所有 AI Agent 生成的数据访问代码必须通过以此规范为基础的自动化规则检验。
