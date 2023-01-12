package io.smallrye.config;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

class CustomConverterTest {
    @Test
    void customInetAddressConverter() {
        Config config = buildConfig(
                "my.address", "10.0.0.1");
        InetAddress inetaddress = config.getValue("my.address", InetAddress.class);
        assertNotNull(inetaddress);
        assertArrayEquals(new byte[] { 10, 0, 0, 1 }, inetaddress.getAddress());
    }

    @Test
    void characterConverter() {
        Config config = buildConfig(
                "my.char", "a");
        char c = config.getValue("my.char", Character.class);
        assertEquals('a', c);
    }

    @Test
    void explicitConverter() {
        // setup
        SmallRyeConfig config = buildConfig("my.prop", "1234").unwrap(SmallRyeConfig.class);// sanity check
        final Converter<Integer> customConverter = new Converter<Integer>() {
            public Integer convert(final String value) {
                return Integer.valueOf(Integer.parseInt(value) * 2);
            }
        };
        // compare against the implicit converter
        // regular read
        assertEquals(1234, config.getValue("my.prop", Integer.class).intValue());
        assertEquals(2468, config.getValue("my.prop", customConverter).intValue());
        // optional read
        assertEquals(1234,
                config.getOptionalValue("my.prop", Integer.class).orElseThrow(IllegalStateException::new).intValue());
        assertEquals(2468,
                config.getOptionalValue("my.prop", customConverter).orElseThrow(IllegalStateException::new).intValue());
        // collection
        assertEquals(singletonList(Integer.valueOf(1234)), config.getValues("my.prop", Integer.class, ArrayList::new));
        assertEquals(singletonList(Integer.valueOf(2468)), config.getValues("my.prop", customConverter, ArrayList::new));
        // check missing behavior
        // regular read
        try {
            config.getValue("missing.prop", Integer.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("missing.prop", customConverter);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        // optional read
        assertFalse(config.getOptionalValue("missing.prop", Integer.class).isPresent());
        assertFalse(config.getOptionalValue("missing.prop", customConverter).isPresent());
        // collection
        try {
            config.getValues("missing.prop", Integer.class, ArrayList::new);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValues("missing.prop", customConverter, ArrayList::new);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
    }

    @Test
    void UUID() {
        String uuidStringTruth = "e4b3d0cf-55a2-4c01-a5d0-fe016fdc9195";
        String secondUuidStringTruth = "c2d88ee5-e981-4de2-ac54-8b887cc2acbc";
        UUID uuidUUIDTruth = UUID.fromString(uuidStringTruth);
        UUID secondUuidUUIDTruth = UUID.fromString(secondUuidStringTruth);
        final Config config = buildConfig(
                "uuid.key", uuidStringTruth,
                "uuid.whitespace", " ",
                "uuid.shouting", uuidStringTruth.toUpperCase(Locale.ROOT),
                "uuid.multiple", uuidStringTruth + "," + secondUuidStringTruth);

        // Check a UUID is correctly parsed from the config source into the expected UUID
        assertEquals(uuidUUIDTruth, config.getValue("uuid.key", UUID.class), "Unexpected value for UUID config");

        // Check non-existent / just whitespace values are treated as empty
        assertFalse(config.getOptionalValue("uuid.nonexistant", UUID.class).isPresent(), "UUID shouldn't exist");
        assertFalse(config.getOptionalValue("uuid.whitespace", UUID.class).isPresent(), "UUID shouldn't exist");

        // Check a capitalised UUID still works correctly
        assertEquals(uuidUUIDTruth, config.getValue("uuid.shouting", UUID.class), "Uppercase UUID incorrectly converted");

        // Check UUIDs work fine in arrays
        ArrayList<UUID> values = config.unwrap(SmallRyeConfig.class).getValues("uuid.multiple", UUID.class, ArrayList::new);
        assertEquals(uuidUUIDTruth, values.get(0), "Unexpected list item in UUID config");
        assertEquals(secondUuidUUIDTruth, values.get(1), "Unexpected list item in UUID config");

    }

    @Test
    void malformedUUID() {
        final Config config = buildConfig(
                "uuid.invalid", "notauuid");

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> {
            config.getValue("uuid.invalid", UUID.class);
        }, "Malformed UUID should throw exception");
        assertTrue(thrownException.getMessage().startsWith(
                "SRCFG00039: The config property uuid.invalid with the config value \"notauuid\" threw an Exception whilst being converted SRCFG00026:"));
        assertEquals("SRCFG00026: notauuid cannot be converted into a UUID", thrownException.getCause().getMessage());
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
