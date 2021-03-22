package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

// https://github.com/eclipse/microprofile-config/blob/2.0/spec/src/main/asciidoc/configexamples.asciidoc#config-value-conversion-rules
class EmptyValuesTest {
    @Test
    void missingForArray() {
        assertThrows(NoSuchElementException.class, () -> config().getValue("my.prop", String[].class));
    }

    @Test
    void emptyForArray() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", "").getValue("my.prop", String[].class));
    }

    @Test
    void commaForArray() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", ",").getValue("my.prop", String[].class));
        assertArrayEquals(new String[] { "," }, config("my.prop", "\\,").getValue("my.prop", String[].class));
    }

    @Test
    void multipleCommasForArray() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", ",,").getValue("my.prop", String[].class));
    }

    @Test
    void valuesForArray() {
        assertArrayEquals(new String[] { "foo", "bar" }, config("my.prop", "foo,bar").getValue("my.prop", String[].class));
    }

    @Test
    void valuesCommaEndForArray() {
        assertArrayEquals(new String[] { "foo" }, config("my.prop", "foo,").getValue("my.prop", String[].class));
    }

    @Test
    void valuesCommaStartForArray() {
        assertArrayEquals(new String[] { "bar" }, config("my.prop", ",bar").getValue("my.prop", String[].class));
    }

    @Test
    void whitespaceForArray() {
        assertArrayEquals(new String[] { " " }, config("my.prop", " ").getValue("my.prop", String[].class));
    }

    @Test
    void value() {
        assertEquals("foo", config("my.prop", "foo").getValue("my.prop", String.class));
    }

    @Test
    void empty() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", "").getValue("my.prop", String.class));
    }

    @Test
    void comma() {
        assertEquals(",", config("my.prop", ",").getValue("my.prop", String.class));
    }

    @Test
    void missing() {
        assertThrows(NoSuchElementException.class, () -> config().getValue("my.prop", String.class));
    }

    @Test
    void valueForOptional() {
        assertEquals(Optional.of("foo"), config("my.prop", "foo").getOptionalValue("my.prop", String.class));
    }

    @Test
    void emptyForOptional() {
        assertEquals(Optional.empty(), config("my.prop", "").getOptionalValue("my.prop", String.class));
        assertNotEquals(Optional.of(""), config("my.prop", "").getOptionalValue("my.prop", String.class));
    }

    @Test
    void missingForOptional() {
        assertEquals(Optional.empty(), config().getOptionalValue("my.prop", String.class));
    }

    @Test
    void emptyForOptionalArray() {
        assertEquals(Optional.empty(), config("my.prop", "").getOptionalValue("my.prop", String[].class));
        assertNotEquals(Optional.of(new String[] {}), config("my.prop", "").getOptionalValue("my.prop", String[].class));
    }

    @Test
    void commaForOptionalArray() {
        assertEquals(Optional.empty(), config("my.prop", ",").getOptionalValue("my.prop", String[].class));
        assertTrue(config("my.prop", "\\,").getOptionalValue("my.prop", String[].class).isPresent());
        assertArrayEquals(new String[] { "," }, config("my.prop", "\\,").getOptionalValue("my.prop", String[].class).get());
        assertFalse(config("my.prop", ",").getOptionalValue("my.prop", String[].class).isPresent());
    }

    @Test
    void mutipleCommasForOptionalArray() {
        assertEquals(Optional.empty(), config("my.prop", ",,").getOptionalValue("my.prop", String[].class));
    }

    @Test
    void missingForOptionalArray() {
        assertEquals(Optional.empty(), config().getOptionalValue("my.prop", String[].class));
    }

    @Test
    void commaForOptionalList() {
        assertTrue(config("my.prop", "\\,").getOptionalValues("my.prop", String.class, ArrayList::new).isPresent());
    }

    private static SmallRyeConfig config(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
