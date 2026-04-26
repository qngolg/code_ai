# AGENTS.md - 项目开发总指南

> **最后更新**: 2026-04-27
> **维护原则**: 当 AI Agent 因文档不清而重复犯错时，人类工程师的职责是**立即更新本文件及相关详细规范**，而不是手动修复代码。本文件是驾驭工程的**入口**，必须保持简洁。

## 1. 架构准则
本项目严格遵循 DDD 四层架构。**所有代码生成前，你必须先阅读并严格遵守**：
- 详细架构规则与包结构：`docs/architecture-guide.md`
- 各层职责与依赖关系图：`docs/architecture-guide.md#层依赖规则`

**特别注意**：层间数据传递必须使用 **MapStruct** 进行对象转换，严禁手动逐字段拷贝或直接传递实体。相关 Mapper 接口统一定义在 `interfaces/assembler` 或 `application/assembler` 包下。

## 2. 技术栈约束
- **Web 框架**：Spring Boot
- **ORM 框架**：MyBatis-Plus (BaseMapper)
- **对象映射**：**MapStruct**。所有跨层对象转换必须通过 MapStruct 完成。详见 `docs/mapstruct-usage.md`
- **测试框架**：**强制使用 Spock + Groovy**。**禁止**生成 JUnit 测试。详见 `docs/testing-guide.spock.md`

## 3. 编码规范
- 通用 Java 编码规范：`docs/java-code-style.md`
- MyBatis-Plus 使用规范：`docs/mybatis-plus-usage.md`
- MapStruct 映射规范：`docs/mapstruct-usage.md`

## 4. 任务模板
严格遵循 `docs/task-template.md` 中的 6 阶段模型执行开发任务。

## 5. 提交前强制检查
**提交代码前，必须严格按照 `docs/git-hooks.md` 执行本地检查，并遵守其中的提交信息格式要求。** 简要概括：
- 本地通过架构约束测试、全量单元测试、AI 审查脚本
- 提交信息必须遵循规定格式，测试与实现必须拆分提交
- CI 失败后自行修正，不得要求人类修改规则

## 6. 本文件维护方式
当 AI Agent 因文档不清而重复犯错时，正确路径是：
1. 在对应的详细文档中增补规则。
2. 必要时在本文件中增加一条指向新规则的简短说明。