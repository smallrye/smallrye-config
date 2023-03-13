package io.smallrye.config.source.yaml;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.testing.logging.LogCapture;

class YamlLocationConfigSourceFactoryTest {
    @RegisterExtension
    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord.getMessage().startsWith("SRCFG"), Level.ALL);

    @BeforeEach
    void setUp() {
        logCapture.records().clear();
    }

    @Test
    void systemFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/additional.yml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("more.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void systemFolder() {
        SmallRyeConfig config = buildConfig("./src/test/resources");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("5678", config.getRawValue("more.prop"));
        assertEquals(11, countSources(config));
    }

    @Test
    void webResource() {
        SmallRyeConfig config = buildConfig(
                "https://raw.githubusercontent.com/smallrye/smallrye-config/main/sources/yaml/src/test/resources/example-profiles.yml");

        assertEquals("default", config.getRawValue("foo.bar"));
        assertEquals(1, countSources(config));
    }

    @Test
    void classpath() {
        SmallRyeConfig config = buildConfig("additional.yml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void all() {
        SmallRyeConfig config = buildConfig("./src/test/resources",
                "https://raw.githubusercontent.com/smallrye/smallrye-config/main/sources/yaml/src/test/resources/example-profiles.yml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("5678", config.getRawValue("more.prop"));
        assertEquals(12, countSources(config));
    }

    @Test
    void notFound() {
        SmallRyeConfig config = buildConfig("not.found");

        assertNull(config.getRawValue("my.prop"));
        assertEquals(0, countSources(config));
    }

    @Test
    void noPropertiesFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/random.properties");

        assertEquals(0, countSources(config));
    }

    @Test
    void invalidWebResource() {
        assertThrows(IllegalArgumentException.class,
                () -> buildConfig("https://raw.githubusercontent.com/smallrye/smallrye-config/notfound.yml"));
        buildConfig("https://github.com/smallrye/smallrye-config/blob/3cc4809734d7fbd03852a20b5870ca743a2427bc/pom.xml");
    }

    @Test
    void warningConfigLocationsNotFound() {
        buildConfig("not.found");

        assertEquals("SRCFG01005: Could not find sources with smallrye.config.locations in not.found",
                logCapture.records().get(0).getMessage());
    }

    @Test
    void warningNoMessageIfAnySourceFound() {
        buildConfig("additional.yml");

        assertTrue(logCapture.records().isEmpty());
    }

    @Test
    void missingFile() {
        assertThrows(IllegalArgumentException.class, () -> buildConfig("file:/not-found.yaml"));
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, String.join(",", locations))
                .build();
    }

    private static int countSources(SmallRyeConfig config) {
        return (int) stream(config.getConfigSources().spliterator(), false).filter(
                YamlConfigSource.class::isInstance).count();
    }
}
