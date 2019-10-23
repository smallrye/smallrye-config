package io.smallrye.configsource;

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Test;

public class TypeSafeConfigConfigSourceTest {

    @Test
    public void testBasic() {
        final ConfigSource configSource = new TypeSafeConfigConfigSource();
        assertEquals(TypeSafeConfigConfigSource.DEFAULT_ORDINAL, configSource.getOrdinal());
        assertEquals("1", configSource.getValue("hello.world"));
        assertEquals("Hello there!", configSource.getValue("hello.foo.bar"));
        assertEquals("1", configSource.getProperties().get("hello.world"));
        assertEquals("Hello there!", configSource.getProperties().get("hello.foo.bar"));
    }
}
