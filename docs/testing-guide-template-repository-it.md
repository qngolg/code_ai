# 集成测试模板 (Repository 实现)

> **适用场景**: 测试涉及外部依赖（数据库、MQ 等）的 Repository 层实现
> **命名规范**: `被测试类名` + `IT`，如 `OrderRepositoryIT`
> **包路径**: `src/test/groovy/../integration`
> **基类要求**: 所有集成测试必须继承 `BaseAppSpec`

## BaseAppSpec 基类

```groovy
// 文件名: BaseAppSpec.groovy
import spock.lang.Specification
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment

@SpringBootTest(classes = CodeAiApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class BaseAppSpec extends Specification {
    // 集成测试公共基类，提供 Spring Boot 测试环境
}
```

## Repository 集成测试示例

```groovy
// 文件名: OrderRepositoryIT.groovy
// 注意：该类继承 BaseAppSpec 以获得自动回滚等能力
class OrderRepositoryIT extends BaseAppSpec {

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