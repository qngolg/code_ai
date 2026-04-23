
【强制约束 - 架构分层】
1. 严格遵循DDD四层架构：Controller → ApplicationService → Domain → Repository
2. 禁止跨层调用：Controller只能调用ApplicationService，Domain不依赖外部
3. 聚合根必须放在domain/aggregate包，禁止与普通实体混合

【强制约束 - 技术栈】
1. 框架：Spring Boot 3.x + MyBatis + MapStruct + Lombok（仅用@Getter/@ToString）
2. 工具：用MapStruct做DTO↔Domain转换，禁止手写BeanUtils
3. 异常：统一使用GlobalExceptionHandler，禁止在业务层抛原始异常
4. 日志：使用@Slf4j，禁止直接使用System.out/err
5. 事务：@Transactional默认使用readOnly=true，需要写操作明确指定readOnly=false

【强制约束 - 代码规范】
1. 类名/方法名必须体现业务语义（如OrderPaymentService而非OrderService）
2. 方法体不超过50行，复杂逻辑拆分为私有辅助方法
3. 所有领域方法必须有单元测试（用JUnit 5）
4. 异常规范：
   - 禁止在业务层抛原始异常（RuntimeException/SQLException等）
   - 统一转换为业务异常：使用自定义异常（如BusinessException）+ 错误码
   - 禁止catch后吞掉异常（至少要log.warn）
5. 安全规范：
   - 禁止SQL拼接，所有查询必须使用参数化查询或MyBatis的#{}语法
   - 禁止在日志中打印敏感信息（密码、密钥、手机号、身份证等）
   - 禁止在错误响应中暴露内部实现细节

