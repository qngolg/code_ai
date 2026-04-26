# 标准单元测试模板 (Application/Domain Service)

> **适用场景**: 测试 Application Service 或 Domain Service 类
> **命名规范**: `被测试类名` + `Spec`，如 `OrderApplicationServiceSpec`

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