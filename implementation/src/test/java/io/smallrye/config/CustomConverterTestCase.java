package io.smallrye.config;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

public class CustomConverterTestCase {

    @Test
    public void testCustomInetAddressConverter() {
        Config config = buildConfig(
                "my.address", "10.0.0.1");
        InetAddress inetaddress = config.getValue("my.address", InetAddress.class);
        assertNotNull(inetaddress);
        assertArrayEquals(new byte[]{10, 0, 0, 1}, inetaddress.getAddress());
    }

    @Test
    public void testCharacterConverter() {
        Config config = buildConfig(
                "my.char", "a");
        char c = config.getValue("my.char", Character.class);
        assertEquals('a', c);
    }

    private static Config buildConfig(String... keyValues) {
        return SmallRyeConfigProviderResolver.INSTANCE.getBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

    @Test
    public void testExplicitConverter() {
        // setup
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234");// sanity check
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
        assertEquals(1234, config.getOptionalValue("my.prop", Integer.class).orElseThrow(IllegalStateException::new).intValue());
        assertEquals(2468, config.getOptionalValue("my.prop", customConverter).orElseThrow(IllegalStateException::new).intValue());
        // collection
        assertEquals(singletonList(Integer.valueOf(1234)), config.getValues("my.prop", Integer.class, ArrayList::new));
        assertEquals(singletonList(Integer.valueOf(2468)), config.getValues("my.prop", customConverter, ArrayList::new));
        // check missing behavior
        // regular read
        try {
            config.getValue("missing.prop", Integer.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {}
        try {
            config.getValue("missing.prop", customConverter);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {}
        // optional read
        assertFalse(config.getOptionalValue("missing.prop", Integer.class).isPresent());
        assertFalse(config.getOptionalValue("missing.prop", customConverter).isPresent());
        // collection
        assertTrue(config.getValues("missing.prop", Integer.class, ArrayList::new).isEmpty());
        assertTrue(config.getValues("missing.prop", customConverter, ArrayList::new).isEmpty());
    }
}
