package io.smallrye.config;

import static io.smallrye.config.ConfigInstanceBuilder.forInterface;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class ConfigInstanceBuilderTest {
    @Test
    void builder() {
        Server server = forInterface(Server.class)
                .with(Server::host, "localhost")
                .with(Server::port, 8080)
                .build();

        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @ConfigMapping
    interface Server {
        String host();

        int port();
    }

    @Test
    void primitives() {
        Primitives primitives = forInterface(Primitives.class)
                .with(Primitives::booleanValue, true)
                .with(Primitives::byteValue, Byte.valueOf((byte) 1))
                .with(Primitives::shortValue, Short.valueOf((short) 1))
                .with(Primitives::intValue, Integer.valueOf(1))
                .with(Primitives::longValue, Long.valueOf(1))
                .with(Primitives::floatValue, Float.valueOf((float) 1.0))
                .with(Primitives::doubleValue, 1.0d)
                .with(Primitives::charValue, Character.valueOf((char) 1))
                .with(Primitives::stringValue, "value")
                .build();

        assertTrue(primitives.booleanValue());
        assertEquals(Byte.valueOf((byte) 1), primitives.byteValue());
        assertEquals(Short.valueOf((short) 1), primitives.shortValue());
        assertEquals(Integer.valueOf(1), primitives.intValue());
        assertEquals(Long.valueOf(1), primitives.longValue());
        assertEquals(Float.valueOf((float) 1.0), primitives.floatValue());
        assertEquals(Double.valueOf(1.0), primitives.doubleValue());
        assertEquals(Character.valueOf((char) 1), primitives.charValue());
        assertEquals("value", primitives.stringValue());
    }

    @ConfigMapping
    interface Primitives {
        boolean booleanValue();

        byte byteValue();

        short shortValue();

        int intValue();

        long longValue();

        float floatValue();

        double doubleValue();

        char charValue();

        String stringValue();
    }

    @Test
    void nested() {
        Nested nested = forInterface(Nested.class)
                .with(Nested::value, "value")
                .with(Nested::group, forInterface(Nested.Group.class).with(Nested.Group::value, "group").build())
                .build();

        assertEquals("value", nested.value());
        assertEquals("group", nested.group().value());
    }

    @ConfigMapping
    interface Nested {
        String value();

        Group group();

        interface Group {
            String value();
        }
    }

    @Test
    void optionals() {
        Optionals optionals = forInterface(Optionals.class)
                .withOptional(Optionals::optional, "value")
                .withOptional(Optionals::optionalInt, 1)
                .withOptional(Optionals::optionalLong, 1)
                .withOptional(Optionals::optionalDouble, 1.1d)
                .withOptional(Optionals::optionalBoolean, true)
                .build();

        assertTrue(optionals.optional().isPresent());
        assertEquals("value", optionals.optional().get());
        assertTrue(optionals.optionalInt().isPresent());
        assertEquals(1, optionals.optionalInt().getAsInt());
        assertTrue(optionals.optionalLong().isPresent());
        assertEquals(1L, optionals.optionalLong().getAsLong());
        assertTrue(optionals.optionalDouble().isPresent());
        assertEquals(1.1d, optionals.optionalDouble().getAsDouble());
        assertTrue(optionals.optionalBoolean().isPresent());
        assertTrue(optionals.optionalBoolean().get());
    }

    @ConfigMapping
    interface Optionals {
        Optional<String> optional();

        OptionalInt optionalInt();

        OptionalLong optionalLong();

        OptionalDouble optionalDouble();

        Optional<Boolean> optionalBoolean();
    }

    @Test
    void defaults() {
        Defaults defaults = forInterface(Defaults.class).build();
        assertEquals("value", defaults.value());
        assertEquals(9, defaults.defaultInt());
        assertEquals("nested", defaults.nested().value());
    }

    @ConfigMapping
    interface Defaults {
        @WithDefault("value")
        String value();

        @WithDefault("9")
        int defaultInt();

        Nested nested();

        interface Nested {
            @WithDefault("nested")
            String value();
        }
    }

    @Test
    void optionalDefaults() {
        OptionalDefaults optionalDefaults = forInterface(OptionalDefaults.class).build();

        assertTrue(optionalDefaults.optional().isPresent());
        assertEquals("value", optionalDefaults.optional().get());
        assertTrue(optionalDefaults.optionalInt().isPresent());
        assertEquals(10, optionalDefaults.optionalInt().getAsInt());
        assertTrue(optionalDefaults.optionalLong().isPresent());
        assertEquals(10L, optionalDefaults.optionalLong().getAsLong());
        assertTrue(optionalDefaults.optionalDouble().isPresent());
        assertEquals(10.10d, optionalDefaults.optionalDouble().getAsDouble());
    }

    interface OptionalDefaults {
        @WithDefault("value")
        Optional<String> optional();

        @WithDefault("10")
        OptionalInt optionalInt();

        @WithDefault("10")
        OptionalLong optionalLong();

        @WithDefault("10.10")
        OptionalDouble optionalDouble();
    }

    @Test
    void collections() {
        Collections collections = forInterface(Collections.class)
                .with(Collections::empty, List.<String> of())
                .with(Collections::list, List.of("one", "two", "three"))
                .build();

        assertIterableEquals(List.of("one", "two", "three"), collections.list());
        assertNotNull(collections.empty());
        assertTrue(collections.empty().isEmpty());
        assertIterableEquals(List.of("one", "two", "three"), collections.defaults());

        assertThrows(NoSuchElementException.class, () -> forInterface(Collections.class).build());
    }

    @ConfigMapping
    interface Collections {
        List<String> list();

        List<String> empty();

        @WithDefault("one,two,three")
        List<String> defaults();
    }

    @Test
    void maps() {
        Maps maps = forInterface(Maps.class)
                .with(Maps::map, Map.of("one", "one", "two", "two"))
                .build();

        assertEquals("one", maps.map().get("one"));
        assertEquals("two", maps.map().get("two"));
        assertEquals("value", maps.defaults().get("one"));
        assertEquals("value", maps.defaults().get("two"));
        assertEquals("value", maps.defaults().get("three"));
        assertEquals("value", maps.group().get("any").value());
    }

    @ConfigMapping
    interface Maps {
        Map<String, String> map();

        Map<String, String> empty();

        @WithDefault("value")
        Map<String, String> defaults();

        @WithDefaults
        Map<String, Group> group();

        interface Group {
            @WithDefault("value")
            String value();
        }
    }
}
