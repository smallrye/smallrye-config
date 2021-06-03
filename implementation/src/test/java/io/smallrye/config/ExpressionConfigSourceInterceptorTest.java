package io.smallrye.config;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
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
    void noExpressionButOptional() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop}");

        assertEquals(Optional.empty(), config.getOptionalValue("expression", String.class));
    }

    @Test
    void noExpressionButConfigValue() {
        Config config = buildConfig("expression", "${my.prop}");

        ConfigValue configValue = config.getConfigValue("expression");
        assertNotNull(configValue);
        assertEquals("expression", configValue.getName());
        assertNull(configValue.getValue());
        assertNull(configValue.getSourceName());
        assertEquals(0, configValue.getSourceOrdinal());
    }

    @Test
    void noExpressionComposed() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop${compose}}");

        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("expression", String.class));
        assertEquals("SRCFG00011: Could not expand value compose in property expression", exception.getMessage());
    }

    @Test
    void noExpressionComposedButOptional() {
        SmallRyeConfig config = buildConfig("expression", "${my.prop${compose}}");

        assertEquals(Optional.empty(), config.getOptionalValue("expression", String.class));
    }

    @Test
    void noExpressionComposedButConfigValue() {
        Config config = buildConfig("expression", "${my.prop${compose}}");

        ConfigValue configValue = config.getConfigValue("expression");
        assertNotNull(configValue);
        assertEquals("expression", configValue.getName());
        assertNull(configValue.getValue());
        assertNull(configValue.getSourceName());
        assertEquals(0, configValue.getSourceOrdinal());
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
        assertEquals("${my.prop}", buildConfig("expression", "\\${my.prop}").getRawValue("expression"));

        assertEquals("file:target/prices/?fileName=${date:now:yyyyMMddssSS}.txt&charset=utf-8",
                buildConfig("camel.expression",
                        "file:target/prices/?fileName=$${date:now:yyyyMMddssSS}.txt&charset=utf-8")
                                .getRawValue("camel.expression"));

        assertEquals("file:target/prices/?fileName=${date:now:yyyyMMddssSS}.txt&charset=utf-8",
                buildConfig("camel.expression",
                        "file:target/prices/?fileName=\\${date:now:yyyyMMddssSS}.txt&charset=utf-8")
                                .getRawValue("camel.expression"));
    }

    @Test
    void expressionMissing() {
        final SmallRyeConfig config = buildConfig("my.prop", "${expression}", "my.prop.partial", "${expression}partial");

        assertThrows(Exception.class, () -> config.getValue("my.prop", String.class));
        assertThrows(Exception.class, () -> config.getValue("my.prop.partial", String.class));
    }

    @Test
    void arrayEscapes() {
        final SmallRyeConfig config = buildConfig("list", "cat,dog,${mouse},sea\\,turtle", "mouse", "mouse");
        final List<String> list = config.getValues("list", String.class, ArrayList::new);
        assertEquals(4, list.size());
        assertEquals(list, Stream.of("cat", "dog", "mouse", "sea,turtle").collect(toList()));
    }

    @Test
    void escapeDollar() {
        final SmallRyeConfig config = buildConfig("my.prop", "\\${value\\${another}end:value}");
        assertEquals("${value${another}end:value}", config.getRawValue("my.prop"));
    }

    @Test
    void escapeBraces() {
        final SmallRyeConfig config = buildConfig("my.prop", "${value:111{111}");
        assertEquals("111{111", config.getRawValue("my.prop"));
    }

    @Test
    void windowPath() {
        final SmallRyeConfig config = buildConfig("window.path", "C:\\Some\\Path");
        assertEquals("C:\\Some\\Path", config.getRawValue("window.path"));
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new ExpressionConfigSourceInterceptor())
                .build();
    }
}
