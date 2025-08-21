package io.smallrye.config.source.hocon;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithParentName;

class HoconConfigSourceTest {
    @Test
    void provider() {
        Iterator<ConfigSource> iterator = new HoconConfigSourceProvider()
                .getConfigSources(Thread.currentThread().getContextClassLoader()).iterator();
        assertTrue(iterator.hasNext());

        ConfigSource configSource = iterator.next();
        assertEquals(HoconConfigSource.ORDINAL, configSource.getOrdinal());
        assertTrue(configSource.getPropertyNames().contains("hello.world"));
        assertTrue(configSource.getPropertyNames().contains("hello.foo.bar"));
        assertEquals("1", configSource.getValue("hello.world"));
        assertEquals("Hell yeah!", configSource.getValue("hello.foo.bar"));
        assertEquals(2, configSource.getProperties().entrySet().size());
        assertEquals("1", configSource.getProperties().get("hello.world"));
        assertEquals("Hell yeah!", configSource.getProperties().get("hello.foo.bar"));
    }

    @Test
    void hocon() throws Exception {
        HoconConfigSource source = new HoconConfigSource(HoconConfigSource.class.getResource(
                "/META-INF/microprofile-config.conf"));
        assertEquals(2, source.getPropertyNames().size());
        assertTrue(source.getPropertyNames().contains("hello.world"));
        assertTrue(source.getPropertyNames().contains("hello.foo.bar"));
        assertEquals("1", source.getValue("hello.world"));
        assertEquals("Hell yeah!", source.getValue("hello.foo.bar"));
        assertEquals(2, source.getProperties().entrySet().size());
        assertEquals("1", source.getProperties().get("hello.world"));
        assertEquals("Hell yeah!", source.getProperties().get("hello.foo.bar"));
    }

    @Test
    void expressions() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new PropertiesConfigSource(singletonMap("foo", "baz"), "properties", 1000))
                .withSources(new HoconConfigSource(HoconConfigSource.class.getResource("/expressions.conf")))
                .build();

        assertEquals("baz", config.getConfigValue("foo").getValue());
        // Resolved by internally by HOCON
        assertEquals("bar", config.getConfigValue("expression").getValue());
        // Resolved by config interceptor
        assertEquals("baz", config.getConfigValue("interceptor").getValue());
    }

    @Test
    void list() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new HoconConfigSource(HoconConfigSource.class.getResource("/list.conf")))
                .withMapping(Countries.class)
                .build();

        Countries mapping = config.getConfigMapping(Countries.class);
        assertFalse(mapping.countries().isEmpty());
        assertEquals(8, mapping.countries().size());
        assertEquals("FJ", mapping.countries().get(0).code());
        assertEquals("Fiji", mapping.countries().get(0).name());
    }

    @ConfigMapping(prefix = "countries")
    public interface Countries {
        @WithParentName
        List<Country> countries();

        interface Country {
            String name();

            String code();
        }
    }

    @Test
    void systemFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/list.conf");

        assertEquals("FJ", config.getConfigValue("countries[0].code").getValue());
    }

    @Test
    void missingFile() {
        assertThrows(IllegalArgumentException.class, () -> buildConfig("file:/not-found.conf"));
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, String.join(",", locations))
                .build();
    }
}
