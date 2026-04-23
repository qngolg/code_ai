---
alwaysApply: false
globs:
  - 'src/main/java/com/codeai/application/**'
---

【强制约束 - Application层核心职责】
1. 只负责业务流程编排、事务控制、聚合调用协调、多领域流程组装
2. 只负责调度：调用Domain领域方法 + 调用Repository存储
3. 负责DTO与领域对象转换调用MapStruct，自身不做转换逻辑

【强制约束 - 严格禁止】
1. 禁止编写任何业务规则、业务校验、核心计算
2. 禁止直接操作数据库、禁止写数据持久化逻辑
3. 禁止修改领域对象内部状态
4. 禁止写任何业务判断if/else

【强制约束 - 事务规范】
1. @Transactional注解统一加在Application服务层方法上
2. readOnly=true用于查询操作，readOnly=false用于写操作
3. 事务传播行为默认REQUIRED，需特殊场景显式指定

【强制约束 - 方法编写规范】
1. 方法体不超过20行，复杂流程拆分为私有辅助方法
2. 一个方法只做一件事：查询调用 → 领域逻辑 → 持久化
3. 禁止在Application层直接new聚合根或实体，通过Repository获取

【强制约束 - 异常处理】
1. 业务异常统一抛出BusinessException，由上层Controller的GlobalExceptionHandler处理
2. 禁止catch业务异常后自行处理
3. 技术异常（如数据库连接失败）可捕获记录日志后重新抛出

【强制约束 - 依赖注入规范】
1. 只注入Repository接口和Domain服务，禁止注入第三方工具类
2. 领域逻辑全部下沉Domain层，Application只做编排
