// 文件名: ArchUnitConstraintsTest.groovy
// 位置: src/test/groovy/com/yourcompany/project/architecture/ArchUnitConstraintsTest.groovy
// 说明: 本文件基于 Spock 框架，为 AI Agent 架构约束的自动化验证。任何违反的代码都将导致 CI 失败。

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import spock.lang.Shared
import spock.lang.Specification

class ArchUnitConstraintsTest extends Specification {

    @Shared
    JavaClasses importedClasses

    def setupSpec() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.yourcompany.project")
    }

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

        expect: "Domain 层保持纯粹，不违反分层依赖"
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

    def "Interface 层只能依赖 Application 层及其他合法基础"() {
        given: "定义 Interface 层的合法依赖范围"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..interfaces..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..interfaces..",
                        "java..",
                        "org.springframework..",
                        "jakarta..",
                        "org.mapstruct.."
                )

        expect: "Interface 层依赖合法"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 二、Repository 接口与实现分离约束
    // 来源: architecture-guide.md 第三章
    // =========================================================================

    def "Repository 接口必须在 Domain 层定义"() {
        given: "所有以 Repository 结尾的接口"
        def rule = ArchRuleDefinition.classes()
                .that().haveNameMatching(".*Repository")
                .and().areInterfaces()
                .should().resideInAPackage("..domain.repository..")

        expect: "所有 Repository 接口位于 domain.repository 包"
        rule.check(importedClasses)
    }

    def "Repository 实现必须在 Infrastructure 层"() {
        given: "所有以 RepositoryImpl 结尾的类"
        def rule = ArchRuleDefinition.classes()
                .that().haveNameMatching(".*RepositoryImpl")
                .should().resideInAPackage("..infrastructure.persistence.repository..")

        expect: "所有 RepositoryImpl 位于 infrastructure 包"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 三、MyBatis-Plus 隔离约束
    // 来源: mybatis-plus-usage.md 第二章“依赖隔离铁律”
    // =========================================================================

    def "Domain 层禁止使用任何 MyBatis-Plus 类"() {
        given: "禁止 Domain 层依赖 MyBatis-Plus 相关包"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus..")

        expect: "Domain 层不包含 MyBatis-Plus 引用"
        rule.check(importedClasses)
    }

    def "Application 层禁止使用任何 MyBatis-Plus 类"() {
        given: "禁止 Application 层依赖 MyBatis-Plus 相关包"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus..")

        expect: "Application 层不包含 MyBatis-Plus 引用"
        rule.check(importedClasses)
    }

    def "Interface 层禁止使用任何 MyBatis-Plus 类"() {
        given: "禁止 Interface 层依赖 MyBatis-Plus 相关包"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.baomidou.mybatisplus..")

        expect: "Interface 层不包含 MyBatis-Plus 引用"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 四、MapStruct 隔离约束
    // 来源: mapstruct-usage.md 第一章与第五章
    // =========================================================================

    def "Domain 层禁止使用任何 MapStruct 类"() {
        given: "禁止 Domain 层依赖 MapStruct"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.mapstruct..")

        expect: "Domain 层不包含 MapStruct 引用"
        rule.check(importedClasses)
    }

    def "MapStruct Mapper 必须位于正确的 assembler 包"() {
        given: "所有被 @Mapper 注解的接口"
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

    def "Interface 层 Mapper 只能转换本层及 Application 层对象"() {
        given: "Interface 层 Mapper 的合法依赖范围"
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
        given: "禁止 Application 的 Mapper 引用 Infrastructure 的 dataobject"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..application.assembler")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.dataobject..")

        expect: "Application 层 Mapper 不直接使用 PO"
        rule.check(importedClasses)
    }

    def "Infrastructure 层 Mapper 禁止被上层直接注入"() {
        given: "禁止接口层和应用层直接依赖 Infrastructure 的 Mapper"
        def rule = ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage("..interfaces..", "..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.assembler")

        expect: "上层不直接使用 Infrastructure 层 Mapper"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 五、命名规范与代码风格约束
    // 来源: java-code-style.md 第一章
    // =========================================================================

    def "应用服务类名称必须以 ApplicationService 结尾"() {
        given: "Application 层 service 包下的类"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..application.service..")
                .should().haveSimpleNameEndingWith("ApplicationService")

        expect: "应用服务命名规范"
        rule.check(importedClasses)
    }

    def "Controller 类名称必须以 Controller 结尾"() {
        given: "Interface 层 controller 包下的类"
        def rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..interfaces.controller..")
                .should().haveSimpleNameEndingWith("Controller")

        expect: "Controller 命名规范"
        rule.check(importedClasses)
    }

    def "PO 类名称必须以 PO 结尾且位于 dataobject 包"() {
        given: "所有以 PO 结尾的类"
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
        given: "按层切片并检查循环"
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
        given: "检查所有类对 Spring BeanUtils 的依赖"
        def rule = ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.beans.BeanUtils")

        expect: "没有类直接依赖 Spring BeanUtils"
        rule.check(importedClasses)
    }

    // =========================================================================
    // 八、IService / ServiceImpl 禁用检查
    // 来源: mybatis-plus-usage.md 第四章
    // =========================================================================

    def "项目中禁止使用 MyBatis-Plus 的 IService 接口"() {
        given: "检查所有类对 IService 的依赖"
        def rule = ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.baomidou.mybatisplus.extension.service.IService")

        expect: "没有类依赖 IService"
        rule.check(importedClasses)
    }

    def "项目中禁止使用 MyBatis-Plus 的 ServiceImpl 类"() {
        given: "检查所有类对 ServiceImpl 的依赖"
        def rule = ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl")

        expect: "没有类依赖 ServiceImpl"
        rule.check(importedClasses)
    }
}