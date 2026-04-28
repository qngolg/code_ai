name: /init-ddd-project
description: 初始化完整企业级项目目录结构、DDD 分层包结构、统一全局配置

你现在严格按照预设项目Rules约束执行：

## 一、项目结构要求
1. 生成全新纯净 Spring Boot 可启动Web后端项目完整目录结构
2. 采用标准DDD四层分层架构：Controller / Application / Domain / Repository
3. 内置统一全局返回体Result、全局异常处理器、链路追踪Filter

## 二、依赖配置要求
4. 引入依赖：Spring Web、MyBatis、Lombok、MapStruct、Spring Test、Groovy(Spock测试框架)

## 三、版本约定
5. pom.xml中必须使用以下版本：
   - Spring Boot: 3.2.0
   - Java: 17
   - MapStruct: 1.5.5.Final
   - Lombok: 1.18.30
   - Spock: 2.4-groovy-5.0
   - Groovy: 5.0.0
   - gmavenplus-plugin: 3.0.2

## 四、pom.xml 必须包含的完整配置

### 1. properties 中定义版本
```xml
<properties>
    <java.version>17</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <lombok.version>1.18.30</lombok.version>
    <groovy.version>5.0.0</groovy.version>
    <spock.version>2.4-groovy-5.0</spock.version>
</properties>
```

### 2. dependencies 中必须包含的依赖
```xml
<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- Lombok (仅用@Getter/@ToString) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <scope>provided</scope>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>

<!-- Groovy (注意：groupId必须是org.apache.groovy，不是org.codehaus.groovy) -->
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>${groovy.version}</version>
</dependency>

<!-- Spock Core -->
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-core</artifactId>
    <version>${spock.version}</version>
    <scope>test</scope>
</dependency>

<!-- Spock Spring -->
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-spring</artifactId>
    <version>${spock.version}</version>
    <scope>test</scope>
</dependency>
```

### 3. build.plugins 中必须包含的配置
```xml
<!-- gmavenplus-plugin 用于编译 Groovy (注意：是gmavenplus-plugin，不是gmaven-plugin) -->
<plugin>
    <groupId>org.codehaus.gmavenplus</groupId>
    <artifactId>gmavenplus-plugin</artifactId>
    <version>3.0.2</version>
    <executions>
        <execution>
            <goals>
                <goal>addSources</goal>
                <goal>addTestSources</goal>
                <goal>compile</goal>
                <goal>compileTests</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- maven-surefire-plugin 必须识别groovy文件 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.2</version>
    <configuration>
        <includes>
            <include>**/*Spec.groovy</include>
            <include>**/*Test.java</include>
        </includes>
    </configuration>
</plugin>
```

## 五、其他要求
6. pom.xml / build.gradle 完整可编译无报错
7. 只搭建骨架结构，不写任何复杂业务代码
8. 代码整洁、职责隔离、架构统一，保证后续扩展架构一致
9. 生成标准.gitignore文件，忽略.idea、.vscode、target、*.class等与项目无关的文件

## 六、输出内容
10. 输出：完整项目文件夹结构 + pom依赖 + .gitignore + 基础启动类
