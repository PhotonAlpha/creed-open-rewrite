package com.creed.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CommonsLang3NumberUtilsMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.creed.rewrite.CommonsLang3NumberUtilsMigration")
                .parser(JavaParser.fromJavaVersion().classpath("commons-lang3"));
    }

    @Test
    void migratesIsNumberToIsCreatable() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.math.NumberUtils;

                        class A {
                            boolean test(String s) {
                                return NumberUtils.isNumber(s) && NumberUtils.toInt(s) > 0;
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.math.NumberUtils;

                        class A {
                            boolean test(String s) {
                                return NumberUtils.isCreatable(s) && NumberUtils.toInt(s) > 0;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void migratesCompareToJdkAndRemovesImport() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.math.NumberUtils;

                        class A {
                            void test(byte b1, byte b2, short s1, short s2, int i1, int i2, long l1, long l2) {
                                int a = NumberUtils.compare(b1, b2);
                                int b = NumberUtils.compare(s1, s2);
                                int c = NumberUtils.compare(i1, i2);
                                int d = NumberUtils.compare(l1, l2);
                            }
                        }
                        """,
                        """
                        class A {
                            void test(byte b1, byte b2, short s1, short s2, int i1, int i2, long l1, long l2) {
                                int a = Byte.compare(b1, b2);
                                int b = Short.compare(s1, s2);
                                int c = Integer.compare(i1, i2);
                                int d = Long.compare(l1, l2);
                            }
                        }
                        """
                )
        );
    }
}
