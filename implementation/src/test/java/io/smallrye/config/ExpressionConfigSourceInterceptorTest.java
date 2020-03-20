package io.smallrye.config;

import static org.junit.Assert.assertEquals;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

public class ExpressionConfigSourceInterceptorTest {
    @Test
    public void simpleExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${my.prop}");

        final String value = config.getValue("expression", String.class);
        assertEquals("1234", value);
    }

    @Test
    public void multipleExpressions() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${my.prop}${my.prop}");

        final String value = config.getValue("expression", String.class);
        assertEquals("12341234", value);
    }

    @Test
    public void composedExpressions() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "expression", "${${compose}}", "compose",
                "my.prop");

        final String value = config.getValue("expression", String.class);
        assertEquals("1234", value);
    }

    @Test
    public void defaultExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop:1234}");

        final String value = config.getValue("expression", String.class);
        assertEquals("1234", value);
    }

    @Test
    public void defaultComposedExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop:${compose}}", "compose", "1234");

        final String value = config.getValue("expression", String.class);
        assertEquals("1234", value);
    }

    @Test(expected = NoSuchElementException.class)
    public void noExpression() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("expression", "${my.prop}");

        config.getValue("expression", String.class);
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
