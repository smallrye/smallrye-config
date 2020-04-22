package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

public class ExpressionConfigSourceInterceptorTest {
    @Test
    public void simpleExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void multipleExpressions() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${my.prop}${my.prop}");

        assertEquals("12341234", config.getValue("expression", String.class));
    }

    @Test
    public void composedExpressions() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${${compose}}", "compose",
                "my.prop");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop:1234}");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpressionEmpty() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "12${my.prop:}34");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpressionComposed() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop:${compose}}", "compose", "1234");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void defaultExpressionComposedEmpty() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop:${compose:}}", "my.prop", "1234");

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    public void noExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value my.prop in property expression", exception.getMessage());
    }

    @Test
    public void noExpressionComposed() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop${compose}}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value compose in property expression", exception.getMessage());
    }

    @Test
    public void withoutExpansion() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));

        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));
        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));
        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));

        assertEquals("1234", config.getValue("expression", String.class));
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new ExpressionConfigSourceInterceptor())
                .build();
    }
}
