package com.creed.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CommonsLang3DeprecationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.creed.rewrite.CommonsLang3Deprecations")
                .parser(JavaParser.fromJavaVersion().classpath("commons-lang3"));
    }

    @Test
    void migratesStringUtilsAndNumberUtilsTogether() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.StringUtils;
                        import org.apache.commons.lang3.math.NumberUtils;

                        class A {
                            boolean test(String a, String b) {
                                return StringUtils.equalsIgnoreCase(a, b)
                                        || NumberUtils.isNumber(a)
                                        || NumberUtils.compare(a.length(), b.length()) > 0;
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.Strings;
                        import org.apache.commons.lang3.math.NumberUtils;

                        class A {
                            boolean test(String a, String b) {
                                return Strings.CI.equals(a, b)
                                        || NumberUtils.isCreatable(a)
                                        || Integer.compare(a.length(), b.length()) > 0;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void removesUnusedImports() {
        rewriteRun(
                java(
                        """
                        import java.util.List;
                        import java.util.Map;

                        import org.apache.commons.lang3.StringUtils;

                        class A {
                            boolean test(String a, String b) {
                                return StringUtils.equals(a, b);
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.Strings;

                        class A {
                            boolean test(String a, String b) {
                                return Strings.CS.equals(a, b);
                            }
                        }
                        """
                )
        );
    }
}
