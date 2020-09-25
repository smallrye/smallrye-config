package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.testing.logging.LogCapture;

public class LoggingConfigSourceInterceptorTest {
    @RegisterExtension
    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord.getMessage().startsWith("SRCFG"), Level.ALL);

    @BeforeEach
    void setUp() {
        logCapture.records().clear();
    }

    @Test
    public void interceptor() throws Exception {
        Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(new ConfigValuePropertiesConfigSource(
                        LoggingConfigSourceInterceptorTest.class.getResource("/config-values.properties")))
                .withInterceptors(new LoggingConfigSourceInterceptor())
                .withSecretKeys("secret")
                .build();

        assertEquals("abc", config.getValue("my.prop", String.class));
        // No log is done here to not expose any sensitive information
        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class));
        assertThrows(NoSuchElementException.class, () -> config.getValue("not.found", String.class));

        // This should not log the secret value:
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));

        // First 2 elements are the profile lookups
        List<String> logs = logCapture.records().stream().map(LogRecord::getMessage).collect(toList());
        // my.prop lookup
        assertTrue(logs.get(10).startsWith("SRCFG01001"));
        assertTrue(logs.get(10).contains("The config my.prop was loaded from ConfigValuePropertiesConfigSource"));
        assertTrue(logs.get(10).contains(":1 with the value abc"));
        // not.found lookup
        assertEquals("SRCFG01002: The config not.found was not found", logs.get(11));
        // secret lookup, shows the key but hides the source and value
        assertEquals("SRCFG01001: The config secret was loaded from secret with the value secret", logs.get(12));
    }

    @Test
    void expansion() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new LoggingConfigSourceInterceptor())
                .withSources(config("my.prop.expand", "${expand}", "expand", "1234"))
                .build();

        assertEquals("1234", config.getRawValue("my.prop.expand"));
        // First 2 elements are the profile lookups
        List<String> logs = logCapture.records().stream().map(LogRecord::getMessage).collect(toList());
        assertEquals("SRCFG01001: The config my.prop.expand was loaded from KeyValuesConfigSource with the value ${expand}",
                logs.get(6));
        assertEquals("SRCFG01001: The config expand was loaded from KeyValuesConfigSource with the value 1234", logs.get(7));
    }
}
