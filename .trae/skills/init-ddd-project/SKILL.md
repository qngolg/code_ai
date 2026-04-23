---
name: /init-ddd-project
description: 初始化完整企业级项目目录结构、DDD 分层包结构、统一全局配置
---

你现在严格按照预设项目Rules约束执行：
1. 生成全新纯净 Spring Boot 可启动Web后端项目完整目录结构
2. 采用标准DDD四层分层架构：Controller / Application / Domain / Repository
3. 内置统一全局返回体Result、全局异常处理器、链路追踪Filter
4. 引入依赖：Spring Web、MyBatis、Lombok、MapStruct、Spring Test、Groovy(Spock测试框架)
5. pom.xml / build.gradle 完整可编译无报错
6. 只搭建骨架结构，不写任何复杂业务代码
7. 代码整洁、职责隔离、架构统一，保证后续扩展架构一致
8. 生成标准.gitignore文件，忽略.idea、.vscode、target、*.class等与项目无关的文件
9. pom.xml中Spock版本为2.4-groovy-5.0，Groovy版本为5.0.0
10. 必须包含gmaven-plugin用于编译Groovy
11. 必须配置maven-surefire-plugin识别**/*Spec.groovy文件
输出：完整项目文件夹结构 + pom依赖 + .gitignore + 基础启动类
