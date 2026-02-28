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
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigInstanceBuilderTest.ConverterNotFound.NotFound;
import io.smallrye.config.ConfigInstanceBuilderTest.Converters.Numbers;

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
                .withOptional(Optionals::group, ConfigInstanceBuilder.forInterface(Optionals.Group.class)
                        .with(Optionals.Group::value, "value")
                        .build())
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
        assertTrue(optionals.group().isPresent());
        assertEquals("value", optionals.group().get().value());

        Optionals empty = forInterface(Optionals.class).build();
        assertTrue(empty.optional().isEmpty());
        assertTrue(empty.optionalInt().isEmpty());
        assertTrue(empty.optionalLong().isEmpty());
        assertTrue(empty.optionalDouble().isEmpty());
        assertTrue(empty.optionalBoolean().isEmpty());
        assertTrue(empty.group().isEmpty());
    }

    @ConfigMapping
    interface Optionals {
        Optional<String> optional();

        OptionalInt optionalInt();

        OptionalLong optionalLong();

        OptionalDouble optionalDouble();

        Optional<Boolean> optionalBoolean();

        Optional<Group> group();

        interface Group {
            String value();
        }
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
        assertTrue(optionalDefaults.optionalList().isPresent());
        assertIterableEquals(List.of("one", "two", "three"), optionalDefaults.optionalList().get());
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

        @WithDefault("one,two,three")
        Optional<List<String>> optionalList();
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
        assertTrue(collections.setDefaults().contains("one"));

        assertThrows(NoSuchElementException.class, () -> forInterface(Collections.class).build());
    }

    @ConfigMapping
    interface Collections {
        List<String> list();

        List<String> empty();

        @WithDefault("one,two,three")
        List<String> defaults();

        @WithDefault("one")
        Set<String> setDefaults();
    }

    @Test
    void maps() {
        Maps maps = forInterface(Maps.class)
                .with(Maps::map, Map.of("one", "one", "two", "two"))
                .with(Maps::nested, Map.of("one", Map.<String, String> of()))
                .build();

        assertEquals("one", maps.map().get("one"));
        assertEquals("two", maps.map().get("two"));
        assertEquals("value", maps.defaults().get("one"));
        assertEquals("value", maps.defaults().get("two"));
        assertEquals("value", maps.defaults().get("three"));
        assertEquals(10, maps.mapIntegers().get("default"));
        assertEquals("value", maps.group().get("any").value());
        assertIterableEquals(List.of("one", "two", "three"), maps.mapLists().get("any"));
        assertIterableEquals(List.of(1, 2, 3), maps.mapListsIntegers().get("any"));

        assertThrows(NoSuchElementException.class, () -> forInterface(Maps.class)
                .with(Maps::map, Map.of("one", "one", "two", "two"))
                .build());
    }

    @ConfigMapping
    interface Maps {
        Map<String, String> map();

        Map<String, String> empty();

        @WithDefault("value")
        Map<String, String> defaults();

        @WithDefault("10")
        Map<String, Integer> mapIntegers();

        @WithDefaults
        Map<String, Group> group();

        // TODO - Add defaults for middle maps?
        @WithDefault("any")
        Map<String, Map<String, String>> nested();

        @WithDefault("one,two,three")
        Map<String, List<String>> mapLists();

        @WithDefault("1,2,3")
        Map<String, List<Integer>> mapListsIntegers();

        interface Group {
            @WithDefault("value")
            String value();
        }
    }

    @Test
    void converters() {
        Converters converters = forInterface(Converters.class).build();

        assertEquals("converted", converters.value());
        assertEquals(999, converters.intValue());
        assertEquals(Numbers.ONE, converters.numbers());
        assertEquals(Numbers.THREE, converters.numbersOverride());
        assertTrue(converters.optional().isPresent());
        assertEquals("converted", converters.optional().get());
        assertTrue(converters.optionalInt().isPresent());
        assertEquals(999, converters.optionalInt().get());
        assertTrue(converters.optionalList().isPresent());
        assertIterableEquals(List.of("converted", "converted", "converted"), converters.optionalList().get());
        assertIterableEquals(List.of("converted", "converted", "converted"), converters.list());
        assertIterableEquals(List.of(999, 999, 999), converters.listInt());
        assertEquals("converted", converters.map().get("default"));
        assertEquals("converted", converters.map().get("any"));
        assertEquals(999, converters.mapInt().get("default"));
        assertEquals(999, converters.mapInt().get("any"));
        assertIterableEquals(List.of("converted", "converted", "converted"), converters.mapList().get("default"));
        assertIterableEquals(List.of("converted", "converted", "converted"), converters.mapList().get("any"));
        assertIterableEquals(List.of(999, 999, 999), converters.mapListInt().get("default"));
        assertIterableEquals(List.of(999, 999, 999), converters.mapListInt().get("any"));
    }

    @ConfigMapping
    interface Converters {
        @WithDefault("to-convert")
        @WithConverter(StringValueConverter.class)
        String value();

        @WithDefault("to-convert")
        @WithConverter(IntegerValueConverter.class)
        int intValue();

        @WithDefault("one")
        Numbers numbers();

        @WithDefault("to-convert")
        @WithConverter(NumbersConverter.class)
        Numbers numbersOverride();

        @WithDefault("to-convert")
        Optional<@WithConverter(StringValueConverter.class) String> optional();

        @WithDefault("to-convert")
        Optional<@WithConverter(IntegerValueConverter.class) Integer> optionalInt();

        @WithDefault("one,two,three")
        Optional<@WithConverter(StringValueConverter.class) List<String>> optionalList();

        @WithDefault("one,two,three")
        List<@WithConverter(StringValueConverter.class) String> list();

        @WithDefault("1,2,3")
        List<@WithConverter(IntegerValueConverter.class) Integer> listInt();

        @WithDefault("to-convert")
        Map<String, @WithConverter(StringValueConverter.class) String> map();

        @WithDefault("to-convert")
        Map<String, @WithConverter(IntegerValueConverter.class) Integer> mapInt();

        @WithDefault("one,two,three")
        Map<String, @WithConverter(StringValueConverter.class) List<String>> mapList();

        @WithDefault("1,2,3")
        Map<String, @WithConverter(IntegerValueConverter.class) List<String>> mapListInt();

        class StringValueConverter implements Converter<String> {
            @Override
            public String convert(String value) throws IllegalArgumentException, NullPointerException {
                return "converted";
            }
        }

        class IntegerValueConverter implements Converter<Integer> {
            @Override
            public Integer convert(String value) throws IllegalArgumentException, NullPointerException {
                return 999;
            }
        }

        enum Numbers {
            ONE,
            TWO,
            THREE
        }

        class NumbersConverter implements Converter<Numbers> {
            @Override
            public Numbers convert(String value) throws IllegalArgumentException, NullPointerException {
                return Numbers.THREE;
            }
        }
    }

    @Test
    void converterNotFound() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> forInterface(ConverterNotFound.class).build());
        assertTrue(illegalArgumentException.getMessage().contains("SRCFG00013"));
    }

    interface ConverterNotFound {
        @WithDefault("value")
        NotFound value();

        class NotFound {

        }
    }

    @Test
    void converterNull() {
        assertThrows(NoSuchElementException.class, () -> forInterface(ConverterNull.class).build());
    }

    interface ConverterNull {
        @WithDefault("")
        String value();
    }
}
