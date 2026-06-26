package io.smallrye.config.source.toml.test;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.logging.Level.ALL;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.toml.TomlConfigSource;
import io.smallrye.testing.logging.LogCapture;

class TomlLocationConfigSourceFactoryTest {
    @RegisterExtension
    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord.getMessage().startsWith("SRCFG01005"), ALL);

    @BeforeEach
    void setUp() {
        logCapture.records().clear();
    }

    @Test
    void systemFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/additional.toml");

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertNull(config.getConfigValue("more.prop").getValue());
        assertEquals(1, countSources(config));
    }

    @Test
    void systemFolder() {
        SmallRyeConfig config = buildConfig("./src/test/resources");

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals("5678", config.getConfigValue("more.prop").getValue());
        assertEquals(2, countSources(config));
    }

    @Test
    void classpath() {
        SmallRyeConfig config = buildConfig("additional.toml");

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals(1, countSources(config));
    }

    @Test
    void notFound() {
        SmallRyeConfig config = buildConfig("not.found");

        assertNull(config.getConfigValue("my.prop").getValue());
        assertEquals(0, countSources(config));
    }

    @Test
    void noPropertiesFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/random.properties");

        assertEquals(0, countSources(config));
    }

    @Test
    void warningConfigLocationsNotFound() {
        buildConfig("not.found");

        assertEquals("SRCFG01005: Could not find sources with smallrye.config.locations in not.found",
                logCapture.records().get(0).getMessage());
    }

    @Test
    void warningNoMessageIfAnySourceFound() {
        buildConfig("additional.toml");

        assertTrue(logCapture.records().isEmpty());
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
                TomlConfigSource.class::isInstance).count();
    }
}
