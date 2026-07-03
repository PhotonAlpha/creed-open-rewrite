package com.creed.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CommonsLang3StringUtilsToStringsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CommonsLang3StringUtilsToStrings())
                .parser(JavaParser.fromJavaVersion().classpath("commons-lang3"));
    }

    @Test
    void migratesCaseSensitiveAndIgnoreCase() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.StringUtils;

                        class A {
                            boolean test(String a, String b) {
                                return StringUtils.equals(a, b)
                                        || StringUtils.equalsIgnoreCase(a, b)
                                        || StringUtils.contains(a, b)
                                        || StringUtils.startsWithIgnoreCase(a, b);
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.Strings;

                        class A {
                            boolean test(String a, String b) {
                                return Strings.CS.equals(a, b)
                                        || Strings.CI.equals(a, b)
                                        || Strings.CS.contains(a, b)
                                        || Strings.CI.startsWith(a, b);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void keepsStringUtilsImportWhenStillUsed() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.StringUtils;

                        class A {
                            String test(String a) {
                                if (StringUtils.isBlank(a)) {
                                    return "";
                                }
                                return StringUtils.removeStartIgnoreCase(a, "pre");
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.StringUtils;
                        import org.apache.commons.lang3.Strings;

                        class A {
                            String test(String a) {
                                if (StringUtils.isBlank(a)) {
                                    return "";
                                }
                                return Strings.CI.removeStart(a, "pre");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void migratesVarargsAndExtraArgOverloads() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.StringUtils;

                        class A {
                            void test(String a, String b) {
                                boolean x = StringUtils.equalsAnyIgnoreCase(a, "x", "y");
                                int i = StringUtils.indexOf(a, b, 1);
                                String r = StringUtils.replace(a, "x", "y", 2);
                                String p = StringUtils.appendIfMissing(a, ".txt");
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.Strings;

                        class A {
                            void test(String a, String b) {
                                boolean x = Strings.CI.equalsAny(a, "x", "y");
                                int i = Strings.CS.indexOf(a, b, 1);
                                String r = Strings.CS.replace(a, "x", "y", 2);
                                String p = Strings.CS.appendIfMissing(a, ".txt");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotTouchNonDeprecatedOverloads() {
        rewriteRun(
                java(
                        """
                        import org.apache.commons.lang3.StringUtils;

                        class A {
                            void test(String a, String b) {
                                boolean c = StringUtils.contains(a, 'x');
                                int i = StringUtils.indexOf(a, 'x');
                                String r = StringUtils.remove(a, 'x');
                                int cmp = StringUtils.compare(a, b, true);
                                boolean blank = StringUtils.isBlank(a);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void migratesStaticImport() {
        rewriteRun(
                java(
                        """
                        import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

                        class A {
                            boolean test(String a) {
                                return containsIgnoreCase(a, "x");
                            }
                        }
                        """,
                        """
                        import org.apache.commons.lang3.Strings;

                        class A {
                            boolean test(String a) {
                                return Strings.CI.contains(a, "x");
                            }
                        }
                        """
                )
        );
    }
}
