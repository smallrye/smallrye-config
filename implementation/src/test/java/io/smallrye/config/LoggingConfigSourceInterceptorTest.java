package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static java.util.logging.Level.ALL;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.LogRecord;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.testing.logging.LogCapture;

class LoggingConfigSourceInterceptorTest {
    @RegisterExtension
    static LogCapture logCapture = LogCapture.with(
            logRecord -> logRecord.getMessage().startsWith("SRCFG01001") || logRecord.getMessage().startsWith("SRCFG01002"),
            ALL);

    @BeforeEach
    void setUp() {
        logCapture.records().clear();
    }

    @Test
    void disabled() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "1234"))
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertTrue(logCapture.records().stream().map(LogRecord::getMessage).findAny().isEmpty());
    }

    @Test
    void interceptor() throws Exception {
        Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOG_VALUES, "true")
                .withSources(new ConfigValuePropertiesConfigSource(
                        LoggingConfigSourceInterceptorTest.class.getResource("/config-values.properties")))
                .withSecretKeys("secret")
                .build();

        assertEquals("abc", config.getValue("my.prop", String.class));
        // No log is done here to not expose any sensitive information
        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class));
        assertThrows(NoSuchElementException.class, () -> config.getValue("not.found", String.class));

        // This should not log the secret value:
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));

        List<String> logs = logCapture.records().stream().map(LogRecord::getMessage).collect(toList());
        // my.prop lookup
        assertTrue(logs.stream()
                .anyMatch(log -> log.contains("The config my.prop was loaded from ConfigValuePropertiesConfigSource")));
        assertTrue(logs.stream().anyMatch(log -> log.contains(":1 with the value abc")));
        // not.found lookup
        assertTrue(logs.contains("SRCFG01002: The config not.found was not found"));
        // secret lookup, shows the key but hides the source and value
        assertTrue(logs.contains("SRCFG01001: The config secret was loaded from secret with the value secret"));
    }

    @Test
    void expansion() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOG_VALUES, "true")
                .withSources(config("my.prop.expand", "${expand}", "expand", "1234"))
                .build();

        assertEquals("1234", config.getRawValue("my.prop.expand"));
        List<String> logs = logCapture.records().stream().map(LogRecord::getMessage).collect(toList());
        assertTrue(logs.contains(
                "SRCFG01001: The config my.prop.expand was loaded from KeyValuesConfigSource with the value ${expand}"));
        assertTrue(logs.contains("SRCFG01001: The config expand was loaded from KeyValuesConfigSource with the value 1234"));
    }

    @Test
    void profiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOG_VALUES, "true")
                .withSources(config("%prod.my.prop", "1234", "my.prop", "5678"))
                .withProfile("prod")
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        List<String> logs = logCapture.records().stream().map(LogRecord::getMessage).collect(toList());
        assertTrue(logs.contains("SRCFG01001: The config my.prop was loaded from KeyValuesConfigSource with the value 1234"));
        assertFalse(logs.stream().anyMatch(log -> log.contains("%prod.my.prop")));
    }
}
