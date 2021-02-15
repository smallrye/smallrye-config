package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

/**
 * 
 * Test sensible error messages are output when edge case config value conversion Exceptions occur
 *
 */
class ConfigValueConversionRulesExceptionsTest {

    @Test
    void missingString() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("none.existing.prop", String.class));
        assertEquals(
                "SRCFG00014: The config property none.existing.prop is required but it could not be found in any config source",
                exception.getMessage());
    }

    @Test
    void missingStringArray() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("none.existing.array.prop", String[].class));
        assertEquals(
                "SRCFG00014: The config property none.existing.array.prop is required but it could not be found in any config source",
                exception.getMessage());
    }

    @Test
    void emptyString() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("empty.string", "")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("empty.string", String.class));
        assertEquals(
                "SRCFG00040: The config property empty.string is defined as the empty String (\"\") which the following Converter considered to be null: io.smallrye.config.Converters$BuiltInConverter",
                exception.getMessage());
    }

    @Test
    void emptyStringArray() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("empty.string.array", "")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("empty.string.array", String[].class));
        assertEquals(
                "SRCFG00040: The config property empty.string.array is defined as the empty String (\"\") which the following Converter considered to be null: io.smallrye.config.Converters$ArrayConverter",
                exception.getMessage());
    }

    @Test
    void commaStringArray() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("comma.string.array", ",")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("comma.string.array", String[].class));
        assertEquals(
                "SRCFG00041: The config property comma.string.array with the config value \",\" was converted to null from the following Converter: io.smallrye.config.Converters$ArrayConverter",
                exception.getMessage());
    }

    @Test
    void doubleCommaStringArray() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("double.comma.string.array", ",,")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("double.comma.string.array", String[].class));
        assertEquals(
                "SRCFG00041: The config property double.comma.string.array with the config value \",,\" was converted to null from the following Converter: io.smallrye.config.Converters$ArrayConverter",
                exception.getMessage());
    }

    @Test
    void missingStringWithBadDefault() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withDefaultValue("bad.default.value", "").build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getValue("bad.default.value", String.class));
        assertEquals(
                "SRCFG00040: The config property bad.default.value is defined as the empty String (\"\") which the following Converter considered to be null: io.smallrye.config.Converters$BuiltInConverter",
                exception.getMessage());
    }

    @Test
    void badConversion() throws Exception {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withDefaultValue("not.an.Integer", "notInt").build();
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> config.getValue("not.an.Integer", Integer.class));
        assertEquals(
                "SRCFG00039: The config property not.an.Integer with the config value \"notInt\" threw an Exception whilst being converted",
                exception.getMessage());
        assertEquals("SRCFG00029: Expected an integer value, got \"notInt\"", exception.getCause().getMessage());
    }
}
