---
alwaysApply: false
globs:
  - 'src/main/java/com/codeai/infrastructure/**'
  - 'src/main/java/com/codeai/repository/**'
---

【强制约束 - Infrastructure层核心职责】
1. 只负责：数据库持久化、外部中间件调用、三方SDK调用、数据映射
2. 所有外部工具、中间件、SDK调用全部收敛在此层
3. 领域对象与DO转换统一在基础设施层完成

【强制约束 - Repository层核心职责】
1. 只提供数据存取能力，不做业务判断
2. 只做单表CRUD操作，禁止多表复杂业务联表逻辑
3. 多表查询通过MyBatis XML实现，业务逻辑在Domain层

【强制约束 - 严格禁止】
1. 禁止任何业务逻辑、业务规则、流程判断
2. 禁止在基础设施层编写领域知识
3. 禁止Repository做业务联表逻辑（超过简单JOIN的查询下沉Domain服务）
4. 禁止DO参与业务逻辑，DO仅用于持久化映射

【强制约束 - DO编写规范】
1. DO数据库实体只用于与数据库表映射，不参与业务逻辑
2. DO字段与数据库列一一对应，使用下划线命名
3. DO通过MyBatis Mapper映射，不直接返回给上层

【强制约束 - Repository编写规范】
1. Repository接口定义在domain层，实现类在infrastructure层（依赖倒置）
2. Repository方法命名遵循：save/findById/deleteById等标准命名
3. 复杂查询通过MyBatis XML实现，禁止在Repository接口写@Query

【强制约束 - 数据转换规范】
1. DO → Domain转换：使用MapStruct，在Infrastructure层或Application层调用
2. 禁止手写BeanUtils或反射转换，统一使用MapStruct
3. 转换发生在Application层，Infrastructure层只返回DO

【强制约束 - 外部服务调用规范】
1. 外部API/SDK调用封装在Infrastructure层，统一使用Feign或RestTemplate
2. 外部服务调用异常统一转换为BusinessException后向上抛出
3. 禁止外部调用绕过Domain层直接返回给Controller
