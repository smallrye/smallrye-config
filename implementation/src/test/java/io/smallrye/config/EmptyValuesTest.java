package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

// https://github.com/eclipse/microprofile-config/issues/446
class EmptyValuesTest {
    @Test // Rule 1
    void missingForArray() {
        assertThrows(NoSuchElementException.class, () -> config().getValue("my.prop", String[].class));
    }

    @Test // Rule 2
    void emptyForArray() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", "").getValue("my.prop", String[].class));
    }

    @Test // Rule 3
    void commaForArray() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", ",").getValue("my.prop", String[].class));
        assertThrows(NoSuchElementException.class, () -> config("my.prop", "\\,").getValue("my.prop", String[].class));
    }

    @Test // Rule 4
    void multipleCommasForArray() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", ",,").getValue("my.prop", String[].class));
    }

    @Test // Rule 5
    void valuesForArray() {
        assertArrayEquals(new String[] { "foo", "bar" }, config("my.prop", "foo,bar").getValue("my.prop", String[].class));
    }

    @Test // Rule 6
    void valuesCommaEndForArray() {
        assertArrayEquals(new String[] { "foo" }, config("my.prop", "foo,").getValue("my.prop", String[].class));
    }

    @Test // Rule 7
    void valuesCommaStartForArray() {
        assertArrayEquals(new String[] { "bar" }, config("my.prop", ",bar").getValue("my.prop", String[].class));
    }

    @Test // Rule 8
    void whitespaceForArray() {
        assertArrayEquals(new String[] { " " }, config("my.prop", " ").getValue("my.prop", String[].class));
    }

    @Test // Rule 9
    void value() {
        assertEquals("foo", config("my.prop", "foo").getValue("my.prop", String.class));
    }

    @Test // Rule 10
    void empty() {
        assertThrows(NoSuchElementException.class, () -> config("my.prop", "").getValue("my.prop", String.class));
    }

    @Test // Rule 11
    void comma() {
        assertEquals(",", config("my.prop", ",").getValue("my.prop", String.class));
    }

    @Test // Rule 12
    void missing() {
        assertThrows(NoSuchElementException.class, () -> config().getValue("my.prop", String.class));
    }

    @Test // Rule 13
    void valueForOptional() {
        assertEquals(Optional.of("foo"), config("my.prop", "foo").getOptionalValue("my.prop", String.class));
    }

    @Test // Rule 14
    void emptyForOptional() {
        assertEquals(Optional.empty(), config("my.prop", "").getOptionalValue("my.prop", String.class));
        assertNotEquals(Optional.of(""), config("my.prop", "").getOptionalValue("my.prop", String.class));
    }

    @Test // Rule 15
    void missingForOptional() {
        assertEquals(Optional.empty(), config().getOptionalValue("my.prop", String.class));
    }

    @Test // Rule 16
    void emptyForOptionalArray() {
        assertEquals(Optional.empty(), config("my.prop", "").getOptionalValue("my.prop", String[].class));
        assertNotEquals(Optional.of(new String[] {}), config("my.prop", "").getOptionalValue("my.prop", String[].class));
    }

    @Test // Rule 17
    void commaForOptionalArray() {
        assertEquals(Optional.empty(), config("my.prop", ",").getOptionalValue("my.prop", String[].class));
        assertNotEquals(Optional.of(new String[] { "", "" }),
                config("my.prop", ",").getOptionalValue("my.prop", String[].class));
    }

    @Test // Rule 18
    void mutipleCommasForOptionalArray() {
        assertEquals(Optional.empty(), config("my.prop", ",,").getOptionalValue("my.prop", String[].class));
        assertNotEquals(Optional.of(new String[] { "", "", "" }),
                config("my.prop", ",").getOptionalValue("my.prop", String[].class));
    }

    @Test // Rule 19
    void missingForOptionalArray() {
        assertEquals(Optional.empty(), config().getOptionalValue("my.prop", String[].class));
    }

    private static SmallRyeConfig config(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
