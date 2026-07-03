# creed-ai-rewrite

OpenRewrite recipe:将 commons-lang3 3.18+(含 3.20.0)中被 `@Deprecated` 标记的
`StringUtils` 静态方法,自动迁移为 `Strings.CS` / `Strings.CI` 的等价调用,
并自动维护 `org.apache.commons.lang3.Strings` 的 import。

```java
// 迁移前
StringUtils.equals(a, b);
StringUtils.equalsIgnoreCase(a, b);
StringUtils.removeStartIgnoreCase(name, "user_");

// 迁移后
Strings.CS.equals(a, b);
Strings.CI.equals(a, b);
Strings.CI.removeStart(name, "user_");
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
  -Drewrite.activeRecipes=com.creed.rewrite.CommonsLang3StringUtilsToStrings
```

先预览不落盘,可用 `dryRun`(diff 输出到 `target/rewrite/rewrite.patch`):

```bash
mvn org.openrewrite.maven:rewrite-maven-plugin:6.12.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.creed:creed-ai-rewrite:1.0.0 \
  -Drewrite.activeRecipes=com.creed.rewrite.CommonsLang3StringUtilsToStrings
```

### 方式二:配置到目标项目 pom 中

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>6.12.0</version>
    <configuration>
        <activeRecipes>
            <recipe>com.creed.rewrite.CommonsLang3StringUtilsToStrings</recipe>
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

## 覆盖的方法(34 个弃用方法,含重载共 39 个签名)

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
