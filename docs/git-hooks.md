# Git 提交钩子与检查规范 (Git Hooks & Commit Checks)

> **最后更新**: 2026-04-27
> **维护说明**: 当 AI Agent 因提交不规范或绕过检查导致问题代码合入时，必须立即更新本文档，收紧检查规则。

## 一、提交前强制检查流程

每次执行 `git commit` 前，你**必须**按顺序完成以下检查，全部通过才能提交。任何一项失败都应阻止提交，并返回修正。

### 1.1 架构约束测试
验证代码是否符合 DDD 分层、框架隔离、命名规范等硬性约束。

```bash
mvn test -Dtest="ArchUnitConstraintsTest" -pl <your-module>
```
要求：必须全部通过。失败时仔细阅读测试输出，它包含违规位置与正确做法的说明。

### 1.2 全量单元测试
运行项目中所有 Spock 测试，确保未破坏现有功能。
```bash
mvn test
```
要求：必须全部通过。失败时仔细阅读测试输出，它包含失败位置与正确做法的说明。

### 1.3 AI 本地自我审查
调用审查脚本，让 AI 对本次变更进行交叉检查。
```bash
./scripts/review.sh
```
要求：通过后方可提交。如果脚本不可用，至少手动检查以下事项：
+ 没有 BeanUtils.copyProperties
+ 没有 Domain 层引用 MapStruct 或 MyBatis-Plus
+ 没有 Application 层直接注入 BaseMapper
+ 没有实体类使用 @Setter

## 二、提交信息格式规范
### 2.1 阶段化提交
每个子任务至少拆分为三个独立 commit，禁止一个 commit 混入测试和实现：
| 顺序 | 提交信息格式 | 内容说明 |
| :--- | :--- | :--- |
| 1 | FAILING TEST: [子任务描述] | 仅包含测试代码，不含任何实现 |
| 2 | IMPLEMENT: [子任务描述] | 仅包含使测试通过的最小实现代码 |
| 3 | REFACTOR: [重构说明] | 如有重构，单独提交；无重构则跳过 |
示例：
```text
FAILING TEST: Add points deduction domain logic
IMPLEMENT: Points deduction domain service
REFACTOR: Extract PointsValidator from PointsDeductionService
```

2.2 禁止的提交模式
+ ❌ 一个 commit 同时包含 .groovy 测试文件和 .java 实现文件
+ ❌ 提交信息用 fix bug、update code 等无意义描述
+ ❌ 在 IMPLEMENT commit 中夹带无关的“顺手”改动

## 三、CI 阻断与反馈规则
代码推送后，CI 流水线会自动运行与本地相同的检查。以下规则必须牢记：

### 3.1 CI 失败处理
+ CI 中 ArchUnitConstraintsTest 失败 → 任务中断，根据日志自行修正后重新提交。
+ CI 中单元测试失败 → 检查是否引入了回归，修正后重新提交。
+ CI 发现测试通过但功能未正确实现（“过早标记功能完成”） → 任务视为失败，必须从 task-template.md 的阶段 2 重新开始。

### 3.2 红线
+ 不得要求人类修改 CI 规则来让你的代码通过。
+ 不得在 CI 失败后仅修改日志输出或测试断言来“蒙混过关”。
+ 不得绕过本地检查直接推送代码。

四、审查脚本模板 (review.sh)
如果 scripts/review.sh 尚未存在，可按此模板创建：
```bash
#!/bin/bash
echo "=== AI Agent 代码自查清单 ==="

# 检查是否有 BeanUtils 引用
if grep -r "BeanUtils" src/main/java/; then
    echo "❌ 发现 BeanUtils 引用，必须改用 MapStruct"
    exit 1
fi

# 检查 Domain 层是否引用了 MapStruct
if grep -r "org.mapstruct" src/main/java/com/yourcompany/project/domain/; then
    echo "❌ Domain 层引用了 MapStruct，违反架构约束"
    exit 1
fi

# 检查 Domain 层是否引用了 MyBatis-Plus
if grep -r "com.baomidou.mybatisplus" src/main/java/com/yourcompany/project/domain/; then
    echo "❌ Domain 层引用了 MyBatis-Plus，违反架构约束"
    exit 1
fi

# 检查是否有 @Setter 在 domain 包
if grep -r "@Setter" src/main/java/com/yourcompany/project/domain/; then
    echo "❌ Domain 层实体使用了 @Setter，必须通过业务方法修改状态"
    exit 1
fi

echo "✅ 基础架构检查通过"
exit 0
```
赋予执行权限：chmod +x scripts/review.sh

## 五、完整提交流程示例
以“添加用户积分扣减功能”为例，正确的提交流程：
```bash
# 阶段 2：编写失败测试
git add src/test/groovy/.../PointsDeductionServiceSpec.groovy
git commit -m "FAILING TEST: Add points deduction domain logic"
mvn test -Dtest="ArchUnitConstraintsTest"  # 架构检查
mvn test                                     # 确认测试失败

# 阶段 3：最小实现
git add src/main/java/.../PointsDeductionService.java
git commit -m "IMPLEMENT: Points deduction domain service"
mvn test                                     # 确认测试通过

# 阶段 4：重构
git add src/main/java/.../PointsValidator.java
git commit -m "REFACTOR: Extract PointsValidator from PointsDeductionService"
mvn test                                     # 确认测试仍通过

# 阶段 5：运行集成测试后推送
mvn test
git push
```

记住：这些检查不是麻烦，是让你跑得又快又稳的护栏。遵守它们，你的每次提交都会是可信的。