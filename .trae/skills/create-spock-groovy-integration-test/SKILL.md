---
name: /create-spock-groovy-integration-test
description: 自动生成 Spock + Groovy 风格 集成自动化测试用例
---

要求严格生成Groovy后缀测试文件：
1. 使用 SpringBootTest 集成测试注解
2. 基于Spock框架语法：given-when-then标准三段式结构
3. 对上面 /api/hello GET接口做完整HTTP请求集成测试
4. 校验接口调用成功、返回状态码、返回文案正确性
5. 测试目录规范放置groovy测试类
6. 测试可直接运行通过、无依赖缺失
只做接口连通性集成验证，不写复杂场景

【重要依赖配置 - 必须包含】
项目pom.xml中必须包含以下配置才能使Spock测试正常运行：

1. Spock版本必须使用：2.4-groovy-5.0，Groovy版本必须使用：5.0.0
```xml
<properties>
    <spock.version>2.4-groovy-5.0</spock.version>
    <groovy.version>5.0.0</groovy.version>
</properties>
```

2. 必须包含spock-core和spock-spring依赖：
```xml
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-core</artifactId>
    <version>${spock.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-spring</artifactId>
    <version>${spock.version}</version>
    <scope>test</scope>
</dependency>
```

3. 必须包含gmaven-plugin用于编译Groovy：
```xml
<plugin>
    <groupId>org.codehaus.gmaven</groupId>
    <artifactId>gmaven-plugin</artifactId>
    <version>1.5</version>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>testCompile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

4. 必须配置maven-surefire-plugin识别groovy文件：
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>
    <configuration>
        <includes>
            <include>**/*Spec.groovy</include>
            <include>**/*Test.java</include>
        </includes>
    </configuration>
</plugin>
```

5. groovy-sql依赖groupId必须为org.apache.groovy：
```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy-sql</artifactId>
    <version>${groovy.version}</version>
    <scope>test</scope>
</dependency>
```
