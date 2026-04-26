# 集成测试模板 (Controller 层)

> **适用场景**: 测试 REST Controller 层的 HTTP 端点
> **命名规范**: `被测试类名` + `IT`，如 `OrderControllerIT`
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

## Controller 集成测试示例

```groovy
// 文件名: OrderControllerIT.groovy
import spock.lang.Unroll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class OrderControllerIT extends BaseAppSpec {

    @Autowired
    TestRestTemplate restTemplate

    @Unroll
    def "POST /api/orders 当请求体为 #desc 时，应返回 #expectedStatus"() {
        given: "构建请求体"
        def requestBody = [
            productId: productId,
            quantity: quantity
        ]

        when: "发送 POST 请求"
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/orders",
            new HttpEntity<>(requestBody),
            Map
        )

        then: "验证响应状态码和响应体"
        response.statusCode == expectedStatus
        if (response.statusCode == HttpStatus.OK) {
            response.body.id != null
            response.body.status == "CREATED"
        }

        where: "参数化测试数据"
        desc            | productId | quantity || expectedStatus
        "正常下单"       | "P001"    | 2        || HttpStatus.OK
        "商品不存在"     | "INVALID" | 1        || HttpStatus.BAD_REQUEST
        "数量为负数"     | "P001"    | -1       || HttpStatus.BAD_REQUEST
    }

    def "GET /api/orders/{id} 当订单存在时，应返回订单详情"() {
        given: "创建一个订单"
        def createRequest = [
            productId: "P001",
            quantity: 2
        ]
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/orders",
            new HttpEntity<>(createRequest),
            Map
        )
        def orderId = createResponse.body.id

        when: "发送 GET 请求"
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/orders/${orderId}",
            Map
        )

        then: "验证响应状态码和订单详情"
        response.statusCode == HttpStatus.OK
        response.body.id == orderId
        response.body.productId == "P001"
    }

    def "GET /api/orders/{id} 当订单不存在时，应返回 404"() {
        when: "发送 GET 请求获取不存在的订单"
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/orders/NON_EXIST_ID",
            Map
        )

        then: "验证返回 404"
        response.statusCode == HttpStatus.NOT_FOUND
    }
}
```