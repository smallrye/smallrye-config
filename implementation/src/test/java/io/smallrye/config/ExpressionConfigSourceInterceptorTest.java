package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class ExpressionConfigSourceInterceptorTest {
    @Test
    public void simpleExpression() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void multipleExpressions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}${my.prop}");

        assertEquals("12341234", config.getValue("expression", String.class));
    }

    @Test
    public void composedExpressions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${${compose}}", "compose",
                "my.prop");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpression() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop:1234}");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpressionEmpty() {
        SmallRyeConfig config = buildConfig("expression", "12${my.prop:}34");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpressionComposed() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop:${compose}}", "compose", "1234");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpressionComposedEmpty() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop:${compose:}}", "my.prop", "1234");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void noExpression() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value my.prop in property expression", exception.getMessage());
    }

    @Test
    public void noExpressionComposed() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop${compose}}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value compose in property expression", exception.getMessage());
    }

    @Test
    public void multipleExpansions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "my.prop.two", "${my.prop}", "my.prop.three",
                "${my.prop.two}", "my.prop.four", "${my.prop.three}");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("1234", config.getRawValue("my.prop.two"));
        assertEquals("1234", config.getRawValue("my.prop.three"));
        assertEquals("1234", config.getRawValue("my.prop.four"));
    }

    @Test
    public void infiniteExpansion() {
        SmallRyeConfig config = buildConfig("my.prop", "${my.prop}");

        assertThrows(IllegalArgumentException.class, () -> config.getRawValue("my.prop"),
                "Recursive expression expansion is too deep for my.prop");
    }

    @Test
    public void withoutExpansion() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));

        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));
        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));
        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void escape() {
        assertEquals("${my.prop}", buildConfig("expression", "$${my.prop}").getRawValue("expression"));

        assertEquals("file:target/prices/?fileName=${date:now:yyyyMMddssSS}.txt&charset=utf-8",
                buildConfig("camel.expression",
                        "file:target/prices/?fileName=$${date:now:yyyyMMddssSS}.txt&charset=utf-8")
                                .getRawValue("camel.expression"));
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new ExpressionConfigSourceInterceptor())
                .build();
    }
}
