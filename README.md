# creed-ai-rewrite

OpenRewrite recipes:自动迁移 commons-lang3 3.18+(含 3.20.0)中被 `@Deprecated`
标记的 API。

提供三个 recipe:

| Recipe | 说明 |
|---|---|
| `com.creed.rewrite.CommonsLang3Deprecations` | 聚合入口(推荐),包含下面两个,并在最后移除所有未使用的 import |
| `com.creed.rewrite.CommonsLang3StringUtilsToStrings` | `StringUtils` → `Strings.CS` / `Strings.CI` |
| `com.creed.rewrite.CommonsLang3NumberUtilsMigration` | `NumberUtils.isNumber` → `isCreatable`;`NumberUtils.compare` → JDK `Byte/Short/Integer/Long.compare` |

聚合 recipe 还包含 OpenRewrite 内置的 `org.openrewrite.java.RemoveUnusedImports`,
迁移完成后会顺带清理文件中所有未使用的 import(不限于 commons-lang3 的)。

```java
// 迁移前
StringUtils.equals(a, b);
StringUtils.equalsIgnoreCase(a, b);
StringUtils.removeStartIgnoreCase(name, "user_");
NumberUtils.isNumber(s);
NumberUtils.compare(i1, i2);
NumberUtils.compare(l1, l2);

// 迁移后
Strings.CS.equals(a, b);
Strings.CI.equals(a, b);
Strings.CI.removeStart(name, "user_");
NumberUtils.isCreatable(s);
Integer.compare(i1, i2);
Long.compare(l1, l2);
```

- 区分大小写的方法 → `Strings.CS.xxx(...)`
- `xxxIgnoreCase` 方法 → `Strings.CI.xxx(...)`(去掉 `IgnoreCase` 后缀)
- 自动添加 `import org.apache.commons.lang3.Strings;`
- `StringUtils` 若仍被其他方法(如 `isBlank`)使用,其 import 会保留,否则自动移除
- 静态导入(`import static ...StringUtils.xxx`)同样会被迁移
- 只匹配精确签名,不会误改未弃用的重载,例如 `contains(CharSequence, int)`、
  `remove(String, char)`、`compare(String, String, boolean)`

## 环境要求

- JDK 17(运行 `mvn` 命令所用的 JDK;注意 rewrite-maven-plugin 目前在 JDK 24+ 上不可用)
- Maven 3.x

## 使用方式

### 1. 安装本 recipe 到本地仓库

```bash
cd creed-ai-rewrite
mvn install
```

### 2. 在目标项目中执行迁移(无需修改目标项目 pom)

```bash
cd <你的项目>
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:run \
  -Drewrite.recipeArtifactCoordinates=com.creed:creed-ai-rewrite:1.0.0 \
  -Drewrite.activeRecipes=com.creed.rewrite.CommonsLang3Deprecations
```

先预览不落盘,可用 `dryRun`(diff 输出到 `target/rewrite/rewrite.patch`):

```bash
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.creed:creed-ai-rewrite:1.0.0 \
  -Drewrite.activeRecipes=com.creed.rewrite.CommonsLang3Deprecations
```

只想迁移某一类,把 `activeRecipes` 换成 `...CommonsLang3StringUtilsToStrings`
或 `...CommonsLang3NumberUtilsMigration` 即可。

### 方式二:配置到目标项目 pom 中

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>6.12.0</version>
    <configuration>
        <activeRecipes>
            <recipe>com.creed.rewrite.CommonsLang3Deprecations</recipe>
        </activeRecipes>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.creed</groupId>
            <artifactId>creed-ai-rewrite</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

之后执行 `mvn rewrite:run`。

## 覆盖的方法

### NumberUtils(`org.apache.commons.lang3.math.NumberUtils`)

| NumberUtils(已弃用) | 迁移为 |
|---|---|
| isNumber(String) | NumberUtils.isCreatable(String) |
| compare(byte, byte) | Byte.compare |
| compare(short, short) | Short.compare |
| compare(int, int) | Integer.compare |
| compare(long, long) | Long.compare |

### StringUtils(34 个弃用方法,含重载共 39 个签名)

| StringUtils(已弃用) | 迁移为 |
|---|---|
| equals / equalsIgnoreCase | Strings.CS/CI.equals |
| equalsAny / equalsAnyIgnoreCase | Strings.CS/CI.equalsAny |
| compare / compareIgnoreCase | Strings.CS/CI.compare |
| contains / containsIgnoreCase | Strings.CS/CI.contains |
| containsAny / containsAnyIgnoreCase | Strings.CS/CI.containsAny |
| startsWith / startsWithIgnoreCase / startsWithAny | Strings.CS/CI.startsWith(Any) |
| endsWith / endsWithIgnoreCase / endsWithAny | Strings.CS/CI.endsWith(Any) |
| indexOf / indexOfIgnoreCase(含 int 起点重载) | Strings.CS/CI.indexOf |
| lastIndexOf / lastIndexOfIgnoreCase(含 int 起点重载) | Strings.CS/CI.lastIndexOf |
| remove / removeIgnoreCase | Strings.CS/CI.remove |
| removeStart / removeStartIgnoreCase | Strings.CS/CI.removeStart |
| removeEnd / removeEndIgnoreCase | Strings.CS/CI.removeEnd |
| replace / replaceIgnoreCase(含 max 重载) | Strings.CS/CI.replace |
| replaceOnce / replaceOnceIgnoreCase | Strings.CS/CI.replaceOnce |
| appendIfMissing / appendIfMissingIgnoreCase | Strings.CS/CI.appendIfMissing |
| prependIfMissing / prependIfMissingIgnoreCase | Strings.CS/CI.prependIfMissing |

不在本 recipe 范围内的弃用方法(它们的替代品不是 `Strings`):
`replaceAll` / `replaceFirst` / `replacePattern` / `removeAll` / `removeFirst` /
`removePattern`(→ `RegExUtils`)、`getLevenshteinDistance` / `getJaroWinklerDistance` /
`getFuzzyDistance`(→ commons-text)、`chomp(String,String)`、`defaultString(String,String)`、
`toString(byte[],String)`。

## 其他迁移文档

- [JUnit 4 + PowerMock 迁移到 JUnit 5 + Mockito(mockStatic)](docs/junit4-powermock-to-junit5-mockito.md)
  —— 使用 OpenRewrite 官方 `rewrite-testing-frameworks`,含 dryRun 预览、分步执行等完整命令。
