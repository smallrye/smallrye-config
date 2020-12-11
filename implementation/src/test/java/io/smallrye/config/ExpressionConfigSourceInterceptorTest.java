package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ExpressionConfigSourceInterceptorTest {
    @Test
    void simpleExpression() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void multipleExpressions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}${my.prop}");

        assertEquals("12341234", config.getValue("expression", String.class));
    }

    @Test
    void composedExpressions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${${compose}}", "compose",
                "my.prop");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void defaultExpression() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop:1234}");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void defaultExpressionEmpty() {
        SmallRyeConfig config = buildConfig("expression", "12${my.prop:}34");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void defaultExpressionComposed() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop:${compose}}", "compose", "1234");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void defaultExpressionComposedEmpty() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop:${compose:}}", "my.prop", "1234");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void noExpression() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value my.prop in property expression", exception.getMessage());
    }

    @Test
    void noExpressionComposed() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop${compose}}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value compose in property expression", exception.getMessage());
    }

    @Test
    void multipleExpansions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "my.prop.two", "${my.prop}", "my.prop.three",
                "${my.prop.two}", "my.prop.four", "${my.prop.three}");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("1234", config.getRawValue("my.prop.two"));
        assertEquals("1234", config.getRawValue("my.prop.three"));
        assertEquals("1234", config.getRawValue("my.prop.four"));
    }

    @Test
    void infiniteExpansion() {
        SmallRyeConfig config = buildConfig("my.prop", "${my.prop}");

        assertThrows(IllegalArgumentException.class, () -> config.getRawValue("my.prop"),
                "Recursive expression expansion is too deep for my.prop");
    }

    @Test
    void withoutExpansion() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));

        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));
        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));
        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void escape() {
        assertEquals("${my.prop}", buildConfig("expression", "$${my.prop}").getRawValue("expression"));

        assertEquals("file:target/prices/?fileName=${date:now:yyyyMMddssSS}.txt&charset=utf-8",
                buildConfig("camel.expression",
                        "file:target/prices/?fileName=$${date:now:yyyyMMddssSS}.txt&charset=utf-8")
                                .getRawValue("camel.expression"));
    }

    @Test
    void expressionMissing() {
        final SmallRyeConfig config = buildConfig("my.prop", "${expression}", "my.prop.partial", "${expression}partial");

        assertThrows(Exception.class, () -> config.getValue("my.prop", String.class));
        assertThrows(Exception.class, () -> config.getValue("my.prop.partial", String.class));
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new ExpressionConfigSourceInterceptor())
                .build();
    }
}
