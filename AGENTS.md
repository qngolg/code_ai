# AGENTS.md - 项目开发总指南

> **最后更新**: 2026-04-26
> **维护原则**: 当 AI Agent 因文档不清而重复犯错时，人类工程师的职责是**立即更新本文件及相关详细规范**，而不是手动修复代码。本文件是驾驭工程的**入口**，必须保持简洁。

## 1. 架构准则
本项目严格遵循 DDD 四层架构。**所有代码生成前，你必须先阅读并严格遵守**：
- 详细架构规则与包结构：`docs/architecture-guide.md`
- 各层职责与依赖关系图：`docs/architecture-guide.md#层依赖规则`

**特别注意**：层间数据传递必须使用 **MapStruct** 进行对象转换，严禁手动逐字段拷贝或直接传递实体。相关 Mapper 接口统一定义在 `interfaces/assembler` 或 `application/assembler` 包下。

## 2. 技术栈约束
- **Web 框架**：Spring Boot
- **ORM 框架**：MyBatis-Plus (BaseMapper/IService)
- **对象映射**：**MapStruct**。所有跨层对象（DTO ↔ Command ↔ Entity ↔ PO）的转换，必须通过 MapStruct 接口完成。禁止使用 `BeanUtils.copyProperties` 或手动 setter 赋值。
- **测试框架**：**强制使用 Spock + Groovy**。**禁止**生成 JUnit 测试。
- 测试文件命名规则与模板：`docs/testing-guide.spock.md`

## 3. 编码规范
- 通用 Java 编码规范 (命名、注释、异常处理等)：`docs/java-code-style.md`
- 针对 MyBatis-Plus 的特定规范 (禁止在 Service 层拼 SQL，复杂查询走 XML 等)：`docs/mybatis-plus-usage.md`
- **MapStruct 映射规范**：跨层对象转换必须遵循 `docs/mapstruct-usage.md`，其中定义了 DTO/Command/Entity/PO 之间的映射约定，以及禁止循环依赖等铁律。

## 4. 任务模板
- 当你被要求实现一个新功能时，请严格遵循此任务分解与执行规范：`docs/task-template.md`

## 5. 本文件维护方式
本文件作为所有约束的入口，其本身不宜膨胀。当 AI Agent 因文档不清而重复犯错时，正确的修复路径是：
1. 在对应的详细文档（`architecture-guide.md`, `testing-guide.spock.md` 等）中增补规则。
2. 必要时在本文件中增加一条指向新规则的简短说明。