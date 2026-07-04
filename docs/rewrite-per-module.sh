#!/usr/bin/env bash
# 分模块执行 rewrite-maven-plugin,避免大型多模块项目一次性载入全部 LST 导致 OOM。
#
# 用法:在目标项目根目录执行(先 mvn install -DskipTests 保证兄弟模块依赖可解析):
#   bash rewrite-per-module.sh
#
# 可用环境变量覆盖默认值:
#   GOAL     run 或 dryRun,默认 run
#   HEAP     每次执行的最大堆,默认 8g
#   COORDS   recipe 依赖坐标
#   RECIPES  要执行的 recipe(逗号分隔)
# 示例(dryRun 预览 / 换用其他 recipe):
#   GOAL=dryRun bash rewrite-per-module.sh
#   COORDS=org.openrewrite.recipe:rewrite-testing-frameworks:3.11.0 \
#     RECIPES=org.openrewrite.java.testing.junit5.JUnit4to5Migration bash rewrite-per-module.sh
set -u

GOAL="${GOAL:-run}"
HEAP="${HEAP:-8g}"
COORDS="${COORDS:-com.creed:creed-ai-rewrite:1.0.0}"
RECIPES="${RECIPES:-com.creed.rewrite.CommonsLang3Deprecations}"
PLUGIN="org.openrewrite.maven:rewrite-maven-plugin:6.12.0"
FAILED_LOG="$(pwd)/rewrite-failed.txt"

# 让 Maven 输出 reactor 中每个模块的目录(顺序即构建顺序,包含根聚合 pom,
# 自动排除 target/ 下及未被 <modules> 引用的 pom)
modules=$(mvn -q exec:exec -Dexec.executable=pwd 2>/dev/null)

if [ -z "$modules" ]; then
  echo "未能获取模块列表,请确认在项目根目录执行且 mvn 可用" >&2
  exit 1
fi

: > "$FAILED_LOG"
total=$(printf '%s\n' "$modules" | wc -l | tr -d ' ')
i=0

while IFS= read -r dir; do
  i=$((i + 1))
  echo "=== [$i/$total] $dir ==="
  # -N(非递归)保证每个模块只处理一次:聚合模块不会把子模块再整棵跑一遍
  (cd "$dir" && MAVEN_OPTS="-Xmx${HEAP}" mvn -N \
    "${PLUGIN}:${GOAL}" \
    -Drewrite.recipeArtifactCoordinates="$COORDS" \
    -Drewrite.activeRecipes="$RECIPES") \
    || echo "$dir" >> "$FAILED_LOG"
done <<< "$modules"

echo ""
if [ -s "$FAILED_LOG" ]; then
  echo "以下模块执行失败(已记录到 $FAILED_LOG),修复后可单独进入目录重跑:"
  cat "$FAILED_LOG"
  exit 1
fi
rm -f "$FAILED_LOG"
echo "全部 $total 个模块执行完成"
