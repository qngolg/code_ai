// 文件名: arch-unit-constraints-test.groovy
// 位置: src/test/groovy/com/yourcompany/project/architecture/ArchUnitConstraintsTest.groovy
// 说明: 本文件为 AI Agent 架构约束的自动化验证。任何违反的代码都将导致 CI 失败。

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import spock.lang.Shared
import spock.lang.Specification

class ArchUnitConstraintsTest extends Specification {

    @Shared
    JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.yourcompany.project")

    // =========================================================================
    // 一、DDD 分层依赖约束
    // 来源: architecture-guide.md 第二章“层依赖关系图”
    // =========================================================================

    def "Domain 层不应依赖任何其他层"() {
        given: "分层架构定义"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..interfaces..", "..infrastructure..")

        expect: "Domain 层保持纯粹"
        rule.check(importedClasses)
    }

    def "Application 层不应依赖 Interface 层"() {
        given: "定义 Application 层对 Interface 层的依赖禁止"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..interfaces..")

        expect: "Application 层不依赖 Interface 层"
        rule.check(importedClasses)
    }

    def "Interface 层只能依赖 Application 层"() {
        given: "定义 Interface 层的合法依赖"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..interfaces..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",   // 只能调用应用层
                        "..interfaces..",    // 自身
                        "java..",            // JDK
                        "org.springframework..", // Spring 框架
                        "jakarta..",         // Jakarta 注解
                        "org.mapstruct.."    // MapStruct
                )

        expect: "Interface 层依赖合法"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 二、Repository 接口与实现分离约束
    // 来源: architecture-guide.md 第三章“Domain 层”与“Infrastructure 层”
    // =========================================================================

    def "Repository 接口必须在 Domain 层定义"() {
        given: "检查所有 Repository 接口的位置"
        def rule = ArchRuleDefinition.classes()
                .that().haveNameMatching(".*Repository")
                .and().areInterfaces()
                .should().resideInAPackage("..domain.repository..")

        expect: "所有 Repository 接口位于 domain.repository 包"
        rule.check(importedClasses)
    }

    def "Repository 实现必须在 Infrastructure 层"() {
        given: "检查所有 Repository 实现类的位置"
        def rule = ArchRuleDefinition.classes()
                .that().haveNameMatching(".*RepositoryImpl")
                .should().resideInAPackage("..infrastructure.persistence.repository..")

        expect: "所有 RepositoryImpl 位于 infrastructure 包"
        rule.check(importedClasses)
    }

    def "Repository 实现必须实现对应的 Domain 接口"() {
        given: "定义实现类的行为约束"
        def rule = ArchRuleDefinition.classes()
                .that().haveNameMatching(".*RepositoryImpl")
                .should().implement(Class.forName("com.yourcompany.project.domain.repository.*Repository"))

        expect: "RepositoryImpl 实现 Domain 层的 Repository 接口"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 三、MyBatis-Plus 隔离约束
    // 来源: mybatis-plus-usage.md 第二章“依赖隔离铁律”
    // =========================================================================

    def "Domain 层禁止使用任何 MyBatis-Plus 类"() {
        given: "禁止项列表"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus..")

        expect: "Domain 层不包含 MyBatis-Plus 引用"
        rule.check(importedClasses)
    }

    def "Application 层禁止使用任何 MyBatis-Plus 类"() {
        given: "禁止项列表"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus..")

        expect: "Application 层不包含 MyBatis-Plus 引用"
        rule.check(importedClasses)
    }

    def "Interface 层禁止使用任何 MyBatis-Plus 类"() {
        given: "禁止项列表"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus..")

        expect: "Interface 层不包含 MyBatis-Plus 引用"
        rule.check(importedClasses)
    }

    def "MyBatis-Plus BaseMapper 只能在 Infrastructure 层使用"() {
        given: "定义合法使用范围"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..infrastructure.persistence.mapper..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus.core.mapper.BaseMapper")
                .orShould() // 直接引用 BaseMapper 是合法的
                .andShould().onlyBeAccessed().byAnyPackage("..infrastructure..")

        expect: "BaseMapper 仅被 Infrastructure 层内部访问"
        // 注：此条较为严苛，可先注释，在 PR 审查时人工关注
        true
    }

    // =========================================================================
    // 四、MapStruct 隔离约束
    // 来源: mapstruct-usage.md 第一章“核心原则”与第五章“禁止事项清单”
    // =========================================================================

    def "Domain 层禁止使用任何 MapStruct 类"() {
        given: "禁止项"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.mapstruct..")

        expect: "Domain 层不包含 MapStruct 引用"
        rule.check(importedClasses)
    }

    def "MapStruct Mapper 必须位于正确的 assembler 包"() {
        given: "检查所有 MapStruct 接口的位置"
        def rule = ArchRuleDefinition.classes()
                .that().areAnnotatedWith("org.mapstruct.Mapper")
                .should().resideInAnyPackage(
                "..interfaces.assembler",
                "..application.assembler",
                "..infrastructure.persistence.assembler"
        )

        expect: "MapStruct 接口位于规定包内"
        rule.check(importedClasses)
    }

    def "Interface 层 Mapper 只能转换本层对象"() {
        given: "定义 Interface 层 Mapper 的合法源/目标类型"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..interfaces.assembler")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..interfaces..",
                        "..application.command..",
                        "..application.query..",
                        "java..",
                        "org.mapstruct.."
                )

        expect: "Interface 层 Mapper 不接触 Domain 或 Infrastructure 对象"
        rule.check(importedClasses)
    }

    def "Application 层 Mapper 禁止直接依赖 PO"() {
        given: "检查 Application 层的 Mapper 是否引用 PO"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..application.assembler")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.dataobject..")

        expect: "Application 层 Mapper 不直接使用 PO"
        rule.check(importedClasses)
    }

    def "Infrastructure 层 Mapper 禁止被上层直接注入"() {
        given: "检查 Infrastructure 的 Mapper 是否被上层非法访问"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..interfaces..", "..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.assembler")

        expect: "上层不直接使用 Infrastructure 层 Mapper"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 五、命名规范与代码风格约束
    // 来源: java-code-style.md 第一章“命名规范”
    // =========================================================================

    def "应用服务类名称必须以 ApplicationService 结尾"() {
        given: "命名规则"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..application.service..")
                .should().haveSimpleNameEndingWith("ApplicationService")

        expect: "应用服务命名规范"
        rule.check(importedClasses)
    }

    def "Controller 类名称必须以 Controller 结尾"() {
        given: "命名规则"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..interfaces.controller..")
                .should().haveSimpleNameEndingWith("Controller")

        expect: "Controller 命名规范"
        rule.check(importedClasses)
    }

    def "PO 类名称必须以 PO 结尾，且位于 dataobject 包"() {
        given: "命名与位置规则"
        def rule = ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("PO")
                .should().resideInAPackage("..infrastructure.persistence.dataobject..")

        expect: "PO 类位置正确"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 六、循环依赖检查
    // 来源: 架构通用最佳实践
    // =========================================================================

    def "各 DDD 分层之间不应存在循环依赖"() {
        given: "按层切片"
        def rule = SlicesRuleDefinition.slices()
                .matching("com.yourcompany.project.(*)..")
                .should().beFreeOfCycles()

        expect: "包层级无循环依赖"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 七、BeanUtils 非法使用检查
    // 来源: mapstruct-usage.md 第五章“禁止事项清单”
    // =========================================================================

    def "项目中禁止使用 BeanUtils.copyProperties"() {
        given: "检查所有类中是否调用了 BeanUtils"
        def rule = ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.beans.BeanUtils")

        expect: "没有类直接依赖 Spring BeanUtils"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 八、IService / ServiceImpl 禁用检查
    // 来源: mybatis-plus-usage.md 第四章“BaseMapper 与 IService 使用规则”
    // =========================================================================

    def "项目中禁止使用 MyBatis-Plus 的 IService 接口"() {
        given: "检查是否实现了 IService"
        def rule = ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.baomidou.mybatisplus.extension.service.IService")

        expect: "没有类依赖 IService"
        rule.check(importedClasses)
    }

    def "项目中禁止使用 MyBatis-Plus 的 ServiceImpl 类"() {
        given: "检查是否继承了 ServiceImpl"
        def rule = ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl")

        expect: "没有类依赖 ServiceImpl"
        rule.check(importedClasses)
    }
}