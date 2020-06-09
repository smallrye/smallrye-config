package io.smallrye.config.source.hocon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

public class HoconConfigSourceTest {

    @Test
    public void testHocon() {
        final ConfigSource configSource = HoconConfigSourceProvider.getConfigSource(
                Thread.currentThread().getContextClassLoader(), "hocon/microprofile-config.conf", 100);
        assertEquals(HoconConfigSource.DEFAULT_ORDINAL, configSource.getOrdinal());
        assertArrayEquals(new String[] { "hello.world", "hello.foo.bar" }, configSource.getPropertyNames().toArray());
        assertArrayEquals(new String[] { "hello.world", "hello.foo.bar" }, configSource.getPropertyNames().toArray());
        assertEquals("1", configSource.getValue("hello.world"));
        assertEquals("Hell yeah!", configSource.getValue("hello.foo.bar"));
        assertEquals(2, configSource.getProperties().entrySet().size());
        assertEquals("1", configSource.getProperties().get("hello.world"));
        assertEquals("Hell yeah!", configSource.getProperties().get("hello.foo.bar"));
    }
}
