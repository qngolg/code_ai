---
alwaysApply: false
globs:
  - 'src/main/java/com/codeai/controller/**'
---

【强制约束 - Controller层核心职责】
1. 只负责接收HTTP请求、参数基础校验、调用Application服务、统一结果返回
2. Controller方法只做一行代码：调用Application服务方法

【强制约束 - 严格禁止】
1. 禁止任何业务逻辑、状态判断、计算逻辑
2. 禁止直接调用Domain、Repository、基础设施层
3. 禁止写SQL、数据查询、数据处理
4. 禁止返回原始实体类

【强制约束 - RESTful规范】
1. 接口全部RESTful风格，GET/POST/PUT/DELETE语义严格匹配
2. GET用于查询，POST用于创建，PUT用于更新，DELETE用于删除
3. 路径命名使用名词复数形式（如/users、/orders）

【强制约束 - 参数规范】
1. 入参统一使用DTO，禁止Domain、DO直接接收前端参数
2. 出参统一包装Result，禁止返回原始实体类
3. 参数校验仅做基础非空、格式校验（如@NotBlank、@NotNull、@Email）
4. 复杂业务校验全部下沉领域层

【强制约束 - 方法编写规范】
1. 方法体不超过3行（参数校验1行 + 调用服务1行 + 返回结果1行）
2. 禁止在Controller中进行数据转换，使用MapStruct在Application层转换
3. 禁止try-catch业务异常，由GlobalExceptionHandler统一处理

【强制约束 - 异常处理】
1. 禁止catch后手动处理业务异常
2. 业务异常通过BusinessException抛出，由GlobalExceptionHandler统一拦截返回
3. 仅允许处理技术性异常（如连接超时），且需记录日志后重新抛出
