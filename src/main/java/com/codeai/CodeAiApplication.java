package com.codeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class CodeAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAiApplication.class, args);
    }
}
