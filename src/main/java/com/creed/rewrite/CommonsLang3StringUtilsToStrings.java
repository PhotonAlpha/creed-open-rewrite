package com.creed.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * 将 commons-lang3 3.18+ 中已被 @Deprecated 标记的 StringUtils 静态方法
 * 迁移为 Strings.CS(区分大小写)/ Strings.CI(忽略大小写)上的等价调用,
 * 并自动维护 org.apache.commons.lang3.Strings 的 import。
 */
public class CommonsLang3StringUtilsToStrings extends Recipe {

    private static final String STRING_UTILS = "org.apache.commons.lang3.StringUtils";
    private static final String STRINGS = "org.apache.commons.lang3.Strings";
    private static final JavaType.ShallowClass STRINGS_TYPE = JavaType.ShallowClass.build(STRINGS);

    private static final class Mapping {
        final MethodMatcher matcher;
        /** Strings 上的单例字段:CS 或 CI */
        final String field;
        /** Strings 上的目标方法名(IgnoreCase 后缀被去掉) */
        final String newName;

        Mapping(String signature, String field, String newName) {
            this.matcher = new MethodMatcher(STRING_UTILS + " " + signature);
            this.field = field;
            this.newName = newName;
        }
    }

    private static final List<Mapping> MAPPINGS = buildMappings();

    private static List<Mapping> buildMappings() {
        String CS = "java.lang.CharSequence";
        String CSA = "java.lang.CharSequence[]";
        String S = "java.lang.String";
        List<Mapping> m = new ArrayList<>();

        // 区分大小写 -> Strings.CS(签名精确到参数类型,避免误改未弃用的重载,
        // 例如 contains(CharSequence, int)、remove(String, char)、compare(String, String, boolean))
        m.add(new Mapping("appendIfMissing(" + S + ", " + CS + ", " + CSA + ")", "CS", "appendIfMissing"));
        m.add(new Mapping("compare(" + S + ", " + S + ")", "CS", "compare"));
        m.add(new Mapping("contains(" + CS + ", " + CS + ")", "CS", "contains"));
        m.add(new Mapping("containsAny(" + CS + ", " + CSA + ")", "CS", "containsAny"));
        m.add(new Mapping("endsWith(" + CS + ", " + CS + ")", "CS", "endsWith"));
        m.add(new Mapping("endsWithAny(" + CS + ", " + CSA + ")", "CS", "endsWithAny"));
        m.add(new Mapping("equals(" + CS + ", " + CS + ")", "CS", "equals"));
        m.add(new Mapping("equalsAny(" + CS + ", " + CSA + ")", "CS", "equalsAny"));
        m.add(new Mapping("indexOf(" + CS + ", " + CS + ")", "CS", "indexOf"));
        m.add(new Mapping("indexOf(" + CS + ", " + CS + ", int)", "CS", "indexOf"));
        m.add(new Mapping("lastIndexOf(" + CS + ", " + CS + ")", "CS", "lastIndexOf"));
        m.add(new Mapping("lastIndexOf(" + CS + ", " + CS + ", int)", "CS", "lastIndexOf"));
        m.add(new Mapping("prependIfMissing(" + S + ", " + CS + ", " + CSA + ")", "CS", "prependIfMissing"));
        m.add(new Mapping("remove(" + S + ", " + S + ")", "CS", "remove"));
        m.add(new Mapping("removeEnd(" + S + ", " + S + ")", "CS", "removeEnd"));
        m.add(new Mapping("removeStart(" + S + ", " + S + ")", "CS", "removeStart"));
        m.add(new Mapping("replace(" + S + ", " + S + ", " + S + ")", "CS", "replace"));
        m.add(new Mapping("replace(" + S + ", " + S + ", " + S + ", int)", "CS", "replace"));
        m.add(new Mapping("replaceOnce(" + S + ", " + S + ", " + S + ")", "CS", "replaceOnce"));
        m.add(new Mapping("startsWith(" + CS + ", " + CS + ")", "CS", "startsWith"));
        m.add(new Mapping("startsWithAny(" + CS + ", " + CSA + ")", "CS", "startsWithAny"));

        // 忽略大小写 -> Strings.CI,方法名去掉 IgnoreCase 后缀
        m.add(new Mapping("appendIfMissingIgnoreCase(" + S + ", " + CS + ", " + CSA + ")", "CI", "appendIfMissing"));
        m.add(new Mapping("compareIgnoreCase(" + S + ", " + S + ")", "CI", "compare"));
        m.add(new Mapping("containsIgnoreCase(" + CS + ", " + CS + ")", "CI", "contains"));
        m.add(new Mapping("containsAnyIgnoreCase(" + CS + ", " + CSA + ")", "CI", "containsAny"));
        m.add(new Mapping("endsWithIgnoreCase(" + CS + ", " + CS + ")", "CI", "endsWith"));
        m.add(new Mapping("equalsIgnoreCase(" + CS + ", " + CS + ")", "CI", "equals"));
        m.add(new Mapping("equalsAnyIgnoreCase(" + CS + ", " + CSA + ")", "CI", "equalsAny"));
        m.add(new Mapping("indexOfIgnoreCase(" + CS + ", " + CS + ")", "CI", "indexOf"));
        m.add(new Mapping("indexOfIgnoreCase(" + CS + ", " + CS + ", int)", "CI", "indexOf"));
        m.add(new Mapping("lastIndexOfIgnoreCase(" + CS + ", " + CS + ")", "CI", "lastIndexOf"));
        m.add(new Mapping("lastIndexOfIgnoreCase(" + CS + ", " + CS + ", int)", "CI", "lastIndexOf"));
        m.add(new Mapping("prependIfMissingIgnoreCase(" + S + ", " + CS + ", " + CSA + ")", "CI", "prependIfMissing"));
        m.add(new Mapping("removeIgnoreCase(" + S + ", " + S + ")", "CI", "remove"));
        m.add(new Mapping("removeEndIgnoreCase(" + S + ", " + S + ")", "CI", "removeEnd"));
        m.add(new Mapping("removeStartIgnoreCase(" + S + ", " + S + ")", "CI", "removeStart"));
        m.add(new Mapping("replaceIgnoreCase(" + S + ", " + S + ", " + S + ")", "CI", "replace"));
        m.add(new Mapping("replaceIgnoreCase(" + S + ", " + S + ", " + S + ", int)", "CI", "replace"));
        m.add(new Mapping("replaceOnceIgnoreCase(" + S + ", " + S + ", " + S + ")", "CI", "replaceOnce"));
        m.add(new Mapping("startsWithIgnoreCase(" + CS + ", " + CS + ")", "CI", "startsWith"));
        return m;
    }

    @Override
    public String getDisplayName() {
        return "Migrate deprecated `StringUtils` methods to `Strings.CS` / `Strings.CI`";
    }

    @Override
    public String getDescription() {
        return "Replaces `org.apache.commons.lang3.StringUtils` methods deprecated since commons-lang3 3.18.0 " +
                "with their `Strings.CS` (case-sensitive) or `Strings.CI` (case-insensitive) equivalents, " +
                "and adds an import for `org.apache.commons.lang3.Strings`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(STRING_UTILS, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                for (Mapping mapping : MAPPINGS) {
                    if (mapping.matcher.matches(m)) {
                        String originalName = m.getSimpleName();
                        maybeRemoveImport(STRING_UTILS);
                        // 处理 import static org.apache.commons.lang3.StringUtils.xxx 的情况
                        maybeRemoveImport(STRING_UTILS + "." + originalName);
                        maybeAddImport(STRINGS, null, false);

                        Space selectPrefix = m.getSelect() != null ? m.getSelect().getPrefix() : Space.EMPTY;
                        J.Identifier strings = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                                emptyList(), "Strings", STRINGS_TYPE, null);
                        // fieldType 必须设置:AddImport 的 FQN 缩短逻辑会把
                        // “name.fieldType == null 且 name.type == 被导入类” 的 FieldAccess
                        // 误判为全限定类名引用并截断为单独的 CS/CI
                        JavaType.Variable singletonType = new JavaType.Variable(null,
                                Flag.flagsToBitMap(java.util.EnumSet.of(Flag.Public, Flag.Static, Flag.Final)),
                                mapping.field, STRINGS_TYPE, STRINGS_TYPE, null);
                        J.Identifier singleton = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                                emptyList(), mapping.field, STRINGS_TYPE, singletonType);
                        J.FieldAccess newSelect = new J.FieldAccess(Tree.randomId(), selectPrefix, Markers.EMPTY,
                                strings, JLeftPadded.build(singleton), STRINGS_TYPE);

                        J.Identifier name = m.getName().withSimpleName(mapping.newName);
                        JavaType.Method mt = m.getMethodType();
                        if (mt != null) {
                            mt = mt.withName(mapping.newName).withDeclaringType(STRINGS_TYPE);
                            name = name.withType(mt);
                            m = m.withMethodType(mt);
                        }
                        return m.withSelect(newSelect).withName(name);
                    }
                }
                return m;
            }
        });
    }
}
