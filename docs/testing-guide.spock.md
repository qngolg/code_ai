# Spock 测试指南 (Testing Guide with Spock)

> **最后更新**: 2026-04-26
> **维护说明**: 当 AI Agent 因测试逻辑疏漏、测试覆盖不足或误用测试框架而出现缺陷漏测时，必须立即更新本指南，加入对应的反例和正确模板。

本指南为本项目**唯一**认可的测试规范。遵循它，是代码通过 CI 的先决条件。

## 一、强硬原则

1.  **测试即规范**: 测试文件是业务行为活文档，必须清晰表达 Given-When-Then 语义。
2.  **强制使用 Spock + Groovy**: 项目内**禁止**出现任何 JUnit 4/5 测试类。所有测试文件名必须以 `Spec` 结尾（如 `OrderApplicationServiceSpec.groovy`）。
3.  **测试先行 (TDD)**: 实现功能前，必须先提交一个**仅包含测试且会失败**的 commit。这是防止 Agent “过早标记功能完成”的关键环节。
4.  **白盒与黑盒结合**: 单元测试侧重交互验证（mock 行为），集成测试侧重结果验证（真实结果）。

## 二、文件组织与命名规范

*   **代码位置**: 测试类应放在 `src/test/groovy` 目录下，镜像源代码包结构。
*   **测试类命名**: `被测试类名` + `Spec`。例如：
    *   `OrderApplicationService` -> `OrderApplicationServiceSpec`
    *   `Order` (Domain Entity) -> `OrderSpec`
*   **集成测试**: 如果测试涉及外部依赖（数据库、MQ 等），类名尾部加 `IT`，并放入 `src/test/groovy/../integration` 包，如 `OrderRepositoryIT.groovy`。

## 三、强制测试模板

以下模板是 AI Agent **必须遵循**的唯一格式。任何偏离都将被认定为无效测试。

### 1. 标准单元测试模板 (针对 Application/Domain Service)

```groovy
// 文件名: OrderApplicationServiceSpec.groovy
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll
import spock.lang.Shared

class OrderApplicationServiceSpec extends Specification {

    // 被测试对象
    @Subject
    def orderApplicationService = new OrderApplicationService()

    // 声明依赖的 mock
    def orderRepository = Mock(OrderRepository)
    def paymentGateway = Mock(PaymentGateway)

    def setup() {
        // 初始化被测试对象，注入 mock
        orderApplicationService.orderRepository = orderRepository
        orderApplicationService.paymentGateway = paymentGateway
    }

    @Unroll
    def "当 #desc 时，应 #expectedBehavior"() {
        given: "准备测试数据与 mock 行为"
        // 1. 设置输入参数
        def command = new CreateOrderCommand()

        and: "mock 外部依赖的预期行为"
        orderRepository.save(_ as Order) >> { Order order -> order }

        when: "调用被测试方法"
        orderApplicationService.createOrder(command)

        then: "验证交互次数与状态"
        // 2. 验证 mock 对象方法被正确调用的交互
        1 * orderRepository.save(_ as Order)
        0 * paymentGateway.charge(_)  // 未发生支付

        and: "验证返回结果或异常"
        // 若方法有返回值，可继续验证
        // result.status == OrderStatus.CREATED

        where: "参数化测试数据"
        desc           | expectedBehavior
        "订单商品正常"   | "仅保存订单，不触发支付"
        "订单为预售商品" | "保存订单并冻结库存"
    }

    def "当库存不足时，应抛出 InsufficientStockException"() {
        given: "设置库存不足的环境"
        // ...

        when: "调用创建订单"
        orderApplicationService.createOrder(command)

        then: "验证异常抛出和原因"
        def exception = thrown(InsufficientStockException)
        exception.message == "商品库存不足"
        // 并且验证没有进行任何保存操作
        0 * orderRepository.save(_)
    }
}
```

### 2. 标准领域实体测试模板
```groovy
// 文件名: OrderSpec.groovy
class OrderSpec extends Specification {

    @Unroll
    def "当订单状态为 #currentStatus 时调用pay方法，expectedStatus 为 #expectedStatus" () {
        given: "一个处于特定状态的订单"
        def order = new Order()
        order.status = currentStatus

        when: "执行支付操作"
        order.pay()

        then: "验证最终状态或异常"
        order.status == expectedStatus

        where:
        currentStatus     || expectedStatus
        OrderStatus.DRAFT || OrderStatus.PAID
    }

    def "当订单已支付时调用 pay()，应抛出异常" () {
        given: "一个已支付订单"
        def order = new Order()
        order.status = OrderStatus.PAID

        when: "再次支付"
        order.pay()

        then: "抛出特定领域异常"
        thrown(OrderAlreadyPaidException)
    }
}
```

### 集成测试模板 (MyBatis-Plus Repository 实现)
```groovy
// 文件名: OrderRepositoryIT.groovy
// 注意：该类可能继承自项目基类，以获得自动回滚等能力
class OrderRepositoryIT extends Specification {

    def orderRepository // 注入真实实现

    def "应正确保存和查询订单"() {
        given: "一个待保存的订单领域对象"
        def order = Order.create(/* ... */)

        when: "调用 save 方法"
        def savedOrder = orderRepository.save(order)

        then: "存在 ID 并能通过 findById 查询到"
        savedOrder.id != null
        def foundOrder = orderRepository.findById(savedOrder.id)
        foundOrder.isPresent()
        foundOrder.get().id == savedOrder.id
    }
}
```
### 四、AI Agent 常见测试错误与应对规则
为避免“过早标记功能完成”和“无效测试”，以下规则强制执行：

1. 必须包含负面测试：每个公开方法至少测试以下场景：
+ 参数为 null 或空值。
+ 外部依赖返回异常.
+ 业务规则拒绝操作。

2. Mock 验证必须具体：禁止仅验证返回值。必须验证关键交互：
+ 1 * repository.save(_) —— 验证只保存一次。
+ (1.._) * auditLogger.log(_) —— 至少记录一次审计日志。
+ 0 * riskyService.delete(_) —— 确认没有发生危险操作。

3. 禁止逻辑空洞：测试不能只是“调用方法，期望为 true”。必须验证一个具体的业务规则。
4. 测试失败才是开始：AI Agent 生成的测试，第一次运行必须失败（因缺少实现）。如果测试提交后意外通过，CI 应标记为失败，因为它证明测试未覆盖真实业务。

### 五、测试与 Harness 的集成
本测试规范通过如下方式与整个驾驭工程结合：
+ 反馈循环钩子: 当 AI Agent 推送代码时，CI 会执行 gradle test。失败时，Agent 将收到带完整错误堆栈的反馈，必须自行修正测试或实现。
+ 智能体审智能体: 除了编译和测试，CI 还会运行一个本地审查脚本 review.sh，它会使用另一 AI 模型检查测试中是否存在“无断言”或“被 mock 的静态方法”等缺陷，并在 Agent 提交 PR 前阻断。
+ 测试即文档: 当领域行为发生重大变更时，提交信息中必须说明与 ./docs/testing-guide.spock.md 的符合性。

**记住**: 如果一个 Bug 漏测，不是 AI 能力不足，而是测试规范不够完善。请立即更新本指南，加入对应的测试模板。
