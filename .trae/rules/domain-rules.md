---
alwaysApply: false
globs: 
  - 'src/main/java/com/code_ai/domain/**'
---

【强制约束 - Domain层结构】
1. domain/aggregate：聚合根
2. domain/entity：普通实体
3. domain/vo：值对象
4. domain/event：领域事件
5. domain/service：领域服务（仅当业务逻辑跨越多个聚合根时使用）

【强制约束 - 领域对象规范】
1. 聚合根负责维护内部一致性，禁止外部对象直接修改聚合根内部状态
2. 值对象必须 immutable（使用@Value注解或手写全参构造函数+无setter）
3. 实体必须有唯一标识（ID），由Domain层生成或由外部系统传入
4. 领域事件用于表达领域模型发生变化，应使用@DomainEvent注解标记
