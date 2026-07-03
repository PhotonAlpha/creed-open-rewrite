# JUnit 4 + PowerMock 迁移到 JUnit 5 + Mockito(mockStatic)

使用 OpenRewrite 官方维护的 [`rewrite-testing-frameworks`](https://docs.openrewrite.org/recipes/java/testing)
(本文档基于 3.11.0)自动完成迁移,无需修改目标项目的 pom,命令行直接执行。

## 前置条件

- 运行 `mvn` 命令的 JDK 为 **17 ~ 21**(rewrite-maven-plugin 在 JDK 24+ 上不可用)
- 目标项目能正常 `mvn compile`(OpenRewrite 需要完整的类型信息)
- 建议在干净的 git 工作区上执行,便于审查 diff 和回滚

## 常用 Recipe 一览

| Recipe | 作用 |
|---|---|
| `org.openrewrite.java.testing.junit5.JUnit4to5Migration` | JUnit 4 → 5:`@Test/@Before/@After/@Ignore` 注解、`Assert` → `Assertions`、`@Test(expected/timeout)` 与 `ExpectedException` → `assertThrows`/`assertTimeout`、`TemporaryFolder` → `@TempDir`、Runner → Extension,并升级依赖与 surefire |
| `org.openrewrite.java.testing.mockito.Mockito1to5Migration` | Mockito 升级到 5.x 的聚合入口,**内含 ReplacePowerMockito**,同时处理 `MockitoJUnitRunner` → `MockitoExtension`、`initMocks` → `openMocks` 等 |
| `org.openrewrite.java.testing.mockito.ReplacePowerMockito` | 仅做 PowerMock → 原生 Mockito:`PowerMockito.mockStatic/when/spy/mock/do*` → `Mockito.*`;`@PrepareForTest` → `MockedStatic` 字段 + setUp/tearDown;`whenNew` → `mockConstruction`;移除 powermock 依赖 |
| `org.openrewrite.java.testing.junit5.JUnit5BestPractices` | 在已完成迁移的代码上应用 JUnit 5 最佳实践(可选的后续清理) |

## 一、先预览(dryRun,不修改任何文件)

diff 会输出到 `target/rewrite/rewrite.patch`:

```bash
cd <你的项目>

mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.testing.junit5.JUnit4to5Migration,org.openrewrite.java.testing.mockito.Mockito1to5Migration
```

审查 patch:

```bash
less target/rewrite/rewrite.patch
```

(如果对 patch 满意,也可以直接应用它:`git apply target/rewrite/rewrite.patch`)

## 二、确认后正式执行(直接改写源码)

```bash
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.testing.junit5.JUnit4to5Migration,org.openrewrite.java.testing.mockito.Mockito1to5Migration
```

执行完后:

```bash
mvn test          # 验证测试是否全部通过
git diff --stat   # 审查改动范围
```

## 三、按需拆分执行(可选)

改动太大想分批提交时,可以一个 recipe 一个 recipe 地跑:

```bash
# 第一步:只迁移 JUnit 4 → 5
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.testing.junit5.JUnit4to5Migration

# 第二步:Mockito 升 5.x 并替换 PowerMock
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.testing.mockito.Mockito1to5Migration

# 只想处理 PowerMock、不动 Mockito 版本时:
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.testing.mockito.ReplacePowerMockito

# 收尾(可选):JUnit 5 最佳实践清理
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.testing.junit5.JUnit5BestPractices
```

每一步都可以先把 `run` 换成 `dryRun` 预览。

## 四、配置到 pom 中(可选,适合多模块反复执行)

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>6.12.0</version>
    <configuration>
        <activeRecipes>
            <recipe>org.openrewrite.java.testing.junit5.JUnit4to5Migration</recipe>
            <recipe>org.openrewrite.java.testing.mockito.Mockito1to5Migration</recipe>
        </activeRecipes>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.openrewrite.recipe</groupId>
            <artifactId>rewrite-testing-frameworks</artifactId>
            <version>3.11.0</version>
        </dependency>
    </dependencies>
</plugin>
```

之后执行 `mvn rewrite:dryRun` 预览、`mvn rewrite:run` 落盘。

## 迁移效果示例

```java
// 迁移前(JUnit 4 + PowerMock)
@RunWith(PowerMockRunner.class)
@PrepareForTest(StringUtils.class)
public class FooTest {
    @Test
    public void test() {
        PowerMockito.mockStatic(StringUtils.class);
        PowerMockito.when(StringUtils.isBlank("x")).thenReturn(true);
    }
}

// 迁移后(JUnit 5 + Mockito mockStatic)
@ExtendWith(MockitoExtension.class)
class FooTest {
    private MockedStatic<StringUtils> mockedStringUtils;

    @BeforeEach
    void setUp() {
        mockedStringUtils = Mockito.mockStatic(StringUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedStringUtils.close();
    }

    @Test
    void test() {
        mockedStringUtils.when(() -> StringUtils.isBlank("x")).thenReturn(true);
    }
}
```

## 无法自动迁移、需要人工处理的场景

自动化通常能完成 80–95% 的机械改动,以下情况需要手工收尾:

- **mock 私有方法、suppress 静态初始化块**:Mockito 没有等价能力,只能重构被测代码
  (例如把私有逻辑提取为可注入的协作对象)
- 复杂的 `whenNew(...).withArguments(...)` 链:`mockConstruction` 语义略有差异,可能需要调整
- 依赖 PowerMock 字节码改写的各种 hack(如修改 final 字段、mock 系统类的深层行为)
- `verifyStatic` 的复杂用法可能需要按 `MockedStatic.verify(...)` 语义微调
- mock final 类/final 方法:Mockito 5 默认 inline mockmaker 已原生支持,一般无需处理;
  若停留在 Mockito 4.x,需额外引入 `mockito-inline` 依赖

## 建议流程

1. `dryRun` → 审查 `rewrite.patch`
2. `run` → `mvn test`
3. 修复无法自动迁移的残留用例(通常是重度 PowerMock 用法)
4. 全绿后从 pom 中删除 powermock / junit4 相关残留依赖(recipe 会移除大部分,做一次人工确认)
5. 分批、按模块提交,方便 review 与回滚
