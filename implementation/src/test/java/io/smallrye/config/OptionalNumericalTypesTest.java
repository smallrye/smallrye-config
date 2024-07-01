package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OptionalNumericalTypesTest {

    Config config;

    @BeforeEach
    void setUp() {
        Map<String, String> properties = new HashMap<>();
        properties.put("my.int", "123");
        properties.put("my.long", "456");
        properties.put("my.double", "789");
        properties.put("my.empty.prop", "");

        config = SmallRyeConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(properties, "my properties"))
                .build();
    }

    @Test
    void optionalIntWithExistingProperty() {
        assertEquals(123, config.getValue("my.int", Integer.class).intValue());

        OptionalInt optionalInt = config.getValue("my.int", OptionalInt.class);
        assertTrue(optionalInt.isPresent());
        assertEquals(123, optionalInt.getAsInt());

        Optional<OptionalInt> optionalOptionalInt = config.getOptionalValue("my.int", OptionalInt.class);
        assertTrue(optionalOptionalInt.isPresent());
        assertTrue(optionalOptionalInt.get().isPresent());
        assertEquals(123, optionalOptionalInt.get().getAsInt());
    }

    @Test
    void optionalIntWithAbsentProperty() {
        assertThrows(NoSuchElementException.class, () -> config.getValue("my.int.not.found", Integer.class));
        assertFalse(config.getOptionalValue("my.int.not.found", Integer.class).isPresent());
        assertFalse(config.getValue("my.int.not.found", OptionalInt.class).isPresent());
        assertTrue(config.getOptionalValue("my.int.not.found", OptionalInt.class).isPresent());
    }

    @Test
    void optionalLongWithExistingProperty() {
        assertEquals(456, config.getValue("my.long", Long.class).longValue());

        OptionalLong optionalLong = config.getValue("my.long", OptionalLong.class);
        assertTrue(optionalLong.isPresent());
        assertEquals(456, optionalLong.getAsLong());

        Optional<OptionalLong> optionalOptionalLong = config.getOptionalValue("my.long", OptionalLong.class);
        assertTrue(optionalOptionalLong.isPresent());
        assertTrue(optionalOptionalLong.get().isPresent());
        assertEquals(456, optionalOptionalLong.get().getAsLong());
    }

    @Test
    void optionalLongWithAbsentProperty() {
        assertThrows(NoSuchElementException.class, () -> config.getValue("my.long.not.found", Integer.class));
        assertFalse(config.getOptionalValue("my.long.not.found", Long.class).isPresent());
        assertFalse(config.getValue("my.long.not.found", OptionalLong.class).isPresent());
        assertTrue(config.getOptionalValue("my.long.not.found", OptionalLong.class).isPresent());
    }

    @Test
    void optionalDoubleWithExistingProperty() {
        assertEquals(789.0, config.getValue("my.double", Double.class), 0.0);

        OptionalDouble optionalDouble = config.getValue("my.double", OptionalDouble.class);
        assertTrue(optionalDouble.isPresent());
        assertEquals(789.0, optionalDouble.getAsDouble(), 0.0);

        Optional<OptionalDouble> optionalOptionalDouble = config.getOptionalValue("my.double", OptionalDouble.class);
        assertTrue(optionalOptionalDouble.isPresent());
        assertTrue(optionalOptionalDouble.get().isPresent());
        assertEquals(789.0, optionalOptionalDouble.get().getAsDouble(), 0.0);
    }

    @Test
    void optionalDoubleWithAbsentProperty() {
        assertThrows(NoSuchElementException.class, () -> config.getValue("my.double.not.found", Integer.class));
        assertFalse(config.getOptionalValue("my.double.not.found", Double.class).isPresent());
        assertFalse(config.getValue("my.double.not.found", OptionalDouble.class).isPresent());
        assertTrue(config.getOptionalValue("my.double.not.found", OptionalDouble.class).isPresent());
    }

    @Test
    void emptyPropertyIsConsideredOptionalEmpty() {
        OptionalInt optionalInt = config.getValue("my.empty.prop", OptionalInt.class);
        assertFalse(optionalInt.isPresent());

        Optional<OptionalInt> optionalOptionalInt = config.getOptionalValue("my.empty.prop", OptionalInt.class);
        assertTrue(optionalOptionalInt.isPresent());
        assertFalse(optionalOptionalInt.get().isPresent());

        OptionalLong optionalLong = config.getValue("my.empty.prop", OptionalLong.class);
        assertFalse(optionalLong.isPresent());

        Optional<OptionalLong> optionalOptionalLong = config.getOptionalValue("my.empty.prop", OptionalLong.class);
        assertTrue(optionalOptionalLong.isPresent());
        assertFalse(optionalOptionalLong.get().isPresent());

        OptionalDouble optionalDouble = config.getValue("my.empty.prop", OptionalDouble.class);
        assertFalse(optionalDouble.isPresent());

        Optional<OptionalDouble> optionalOptionalDouble = config.getOptionalValue("my.empty.prop", OptionalDouble.class);
        assertTrue(optionalOptionalDouble.isPresent());
        assertFalse(optionalOptionalDouble.get().isPresent());
    }
}
