package com.codeai.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Unroll

class OrderControllerIT extends BaseAppSpec {

    @Autowired
    TestRestTemplate restTemplate

    @Unroll
    def "POST /api/v1/orders 当请求体为 #desc 时，应返回 #expectedStatus"() {
        given: "构建请求体"
        def requestBody = [
            orderName: orderName,
            price: price
        ]

        when: "发送 POST 请求"
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/v1/orders",
            new HttpEntity<>(requestBody),
            Map
        )

        then: "验证响应状态码和响应体"
        response.statusCode == expectedStatus
        if (response.statusCode == HttpStatus.CREATED) {
            response.body != null
            response.body.code == 200
            response.body.data.id != null
            response.body.data.orderName == orderName
            response.body.data.price == price
        }

        where: "参数化测试数据"
        desc            | orderName       | price  || expectedStatus
        "正常下单"       | "测试订单"      | 99.99  || HttpStatus.CREATED
        "订单名称为空"   | ""              | 99.99  || HttpStatus.BAD_REQUEST
        "价格为负数"     | "测试订单"      | -10.00 || HttpStatus.BAD_REQUEST
    }

    def "GET /api/v1/orders/{id} 当订单存在时，应返回订单详情"() {
        given: "创建一个订单"
        def createRequest = [
            orderName: "查询测试订单",
            price: 88.88
        ]
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            "/api/v1/orders",
            new HttpEntity<>(createRequest),
            Map
        )
        def orderId = createResponse.body.data.id

        when: "发送 GET 请求"
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/v1/orders/${orderId}",
            Map
        )

        then: "验证响应状态码和订单详情"
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.code == 200
        response.body.data.id == orderId
        response.body.data.orderName == "查询测试订单"
        response.body.data.price == 88.88
    }

    def "GET /api/v1/orders/{id} 当订单不存在时，应返回 404"() {
        when: "发送 GET 请求获取不存在的订单"
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/v1/orders/999999",
            Map
        )

        then: "验证返回 404"
        response.statusCode == HttpStatus.NOT_FOUND
        response.body != null
        response.body.code == 404
    }
}