package io.smallrye.config;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Priority;

import org.junit.jupiter.api.Test;

class ExpressionConfigSourceInterceptorTest {
    @Test
    void simpleExpression() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));
        assertEquals("1234", config.getConfigValue("expression").getValue());
        assertEquals("${my.prop}", config.getConfigValue("expression").getRawValue());
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

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value compose in property expression", exception.getMessage());
    }

    @Test
    void multipleExpansions() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "my.prop.two", "${my.prop}", "my.prop.three",
                "${my.prop.two}", "my.prop.four", "${my.prop.three}");

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals("1234", config.getConfigValue("my.prop.two").getValue());
        assertEquals("1234", config.getConfigValue("my.prop.three").getValue());
        assertEquals("1234", config.getConfigValue("my.prop.four").getValue());
    }

    @Test
    void infiniteExpansion() {
        SmallRyeConfig config = buildConfig("my.prop", "${my.prop}");

        assertThrows(IllegalArgumentException.class, () -> config.getConfigValue("my.prop").getValue(),
                "Recursive expression expansion is too deep for my.prop");
    }

    @Test
    void withoutExpansion() {
        SmallRyeConfig config = buildConfig("my.prop", "1234", "expression", "${my.prop}");

        assertEquals("1234", config.getValue("expression", String.class));

        Expressions.withoutExpansion(() -> assertEquals("${my.prop}", config.getValue("expression", String.class)));

        assertEquals("1234", config.getValue("expression", String.class));
    }

    @Test
    void escape() {
        // Test NO_$$
        assertEquals("$1234",
                buildConfig("expression", "$${my.prop}", "my.prop", "1234").getConfigValue("expression").getValue());

        assertEquals("${my.prop}",
                buildConfig("expression", "\\${my.prop}").getConfigValue("expression").getValue());

        assertEquals("file:target/prices/?fileName=${date:now:yyyyMMddssSS}.txt&charset=utf-8",
                buildConfig("camel.expression",
                        "file:target/prices/?fileName=\\${date:now:yyyyMMddssSS}.txt&charset=utf-8")
                        .getConfigValue("camel.expression").getValue());

        assertEquals("\\$\\{%s}",
                buildConfig("key", "\\\\$\\\\{%s}").getConfigValue("key").getValue());

        // Standard escape sequences are interpreted within an expression-bearing value.
        assertEquals("a\tbvalue",
                buildConfig("expression", "a\\tb${my.prop}", "my.prop", "value").getConfigValue("expression").getValue());

        // A backslash before any other character is dropped within an expression-bearing value.
        assertEquals("qwqvalue",
                buildConfig("expression", "q\\wq${my.prop}", "my.prop", "value").getConfigValue("expression").getValue());
    }

    @Test
    void expressionMissing() {
        SmallRyeConfig config = buildConfig("my.prop", "${expression}", "my.prop.partial", "${expression}partial");

        assertThrows(Exception.class, () -> config.getValue("my.prop", String.class));
        assertThrows(Exception.class, () -> config.getValue("my.prop.partial", String.class));
        assertTrue(config.isPropertyPresent("my.prop"));
        assertTrue(config.isPropertyPresent("my.prop.partial"));
    }

    @Test
    void expressionMissingOptional() {
        SmallRyeConfig config = buildConfig("my.prop", "${expression}",
                "my.prop.partial", "${expression}partial",
                "my.prop.anotherPartial", "par${expression}tial",
                "my.prop.dependent", "${my.prop.partial}");

        assertEquals(Optional.empty(), config.getOptionalValue("my.prop", String.class));
        assertEquals(Optional.empty(), config.getOptionalValue("my.prop.partial", String.class));
        assertEquals(Optional.empty(), config.getOptionalValue("my.prop.anotherPartial", String.class));
        assertEquals(Optional.empty(), config.getOptionalValue("my.prop.dependent", String.class));

        ConfigValue noExpression = config.getConfigValue("my.prop");
        assertNotNull(noExpression);
        assertEquals("my.prop", noExpression.getName());
        assertNull(noExpression.getValue());

        ConfigValue noExpressionPartial = config.getConfigValue("my.prop.partial");
        assertNotNull(noExpressionPartial);
        assertEquals("my.prop.partial", noExpressionPartial.getName());
        assertNull(noExpressionPartial.getValue());

        ConfigValue noExpressionAnotherPartial = config.getConfigValue("my.prop.anotherPartial");
        assertNotNull(noExpressionAnotherPartial);
        assertEquals("my.prop.anotherPartial", noExpressionAnotherPartial.getName());
        assertNull(noExpressionAnotherPartial.getValue());

        ConfigValue noExpressionDependent = config.getConfigValue("my.prop.dependent");
        assertNotNull(noExpressionDependent);
        assertEquals("my.prop.dependent", noExpressionDependent.getName());
        assertNull(noExpressionDependent.getValue());

        assertThrows(Exception.class, () -> config.getValue("my.prop", String.class));
        assertThrows(Exception.class, () -> config.getValue("my.prop.partial", String.class));
        assertThrows(Exception.class, () -> config.getValue("my.prop.anotherPartial", String.class));
        assertThrows(Exception.class, () -> config.getValue("my.prop.dependent", String.class));
    }

    @Test
    void arrayEscapes() {
        SmallRyeConfig config = buildConfig("list", "cat,dog,${mouse},sea\\\\,turtle", "mouse", "mouse");
        List<String> list = config.getValues("list", String.class, ArrayList::new);
        assertEquals(4, list.size());
        assertEquals(Stream.of("cat", "dog", "mouse", "sea,turtle").toList(), list);
    }

    @Test
    void escapeDollar() {
        SmallRyeConfig config = buildConfig("my.prop", "\\${value\\${another}end:value}");
        assertEquals("${value${another}end:value}", config.getConfigValue("my.prop").getValue());
    }

    @Test
    void escapeBraces() {
        SmallRyeConfig config = buildConfig("my.prop", "${value:111{111}");
        assertEquals("111{111", config.getConfigValue("my.prop").getValue());
    }

    @Test
    void windowPath() {
        // A value with no expression segment keeps every backslash literal (no escaping applied).
        assertEquals("C:\\Some\\Path",
                buildConfig("window.path", "C:\\Some\\Path").getConfigValue("window.path").getValue());

        // As soon as the value contains an expression, backslash becomes an escape character everywhere in it, so a
        // literal backslash must be doubled
        assertEquals("C:\\some\\path\\value",
                buildConfig("window.path", "C:\\\\some\\\\path\\\\${my.prop}", "my.prop", "value").getConfigValue("window.path")
                        .getValue());
    }

    @Test
    void nullValue() {
        SmallRyeConfig config = buildConfigWithCustomInterceptor("sth", null);
        ConfigValue configValue = config.getConfigValue("sth");

        assertNotNull(configValue);

        // No exception is thrown, only null is returned
        assertNull(configValue.getValue());
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

    private static SmallRyeConfig buildConfigWithCustomInterceptor(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new CustomConfigSourceInterceptor())
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

    @Priority(Priorities.LIBRARY + 201)
    private static class CustomConfigSourceInterceptor implements ConfigSourceInterceptor {

        @Override
        public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
            return ConfigValue.builder().withName(name).withValue(null).build();
        }
    }
}
