package com.codeai.controller

import com.codeai.CodeAiApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class HealthControllerSpec extends BaseAppSpec {

    @Autowired
    TestRestTemplate restTemplate

    def "health endpoint should return 200 and OK"() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/health", Map)

        then:
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 200
        response.body.message == "success"
        response.body.data == "OK"
    }
}
