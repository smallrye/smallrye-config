package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.logging.Level.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.testing.logging.LogCapture;

class ConfigUnknownReporterTest {
    @RegisterExtension
    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord.getMessage().startsWith("SRCFG01010"), WARNING);

    @BeforeEach
    void setUp() {
        logCapture.records().clear();
    }

    @Test
    void modeDefaultsToError() {
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withSources(config("root.value", "value", "root.unknown", "value"))
                .build());

        assertEquals(1, exception.getProblemCount());
        assertEquals("SRCFG00050: root.unknown in KeyValuesConfigSource does not map to any root",
                exception.getProblem(0).getMessage());
        assertTrue(logCapture.records().isEmpty());
    }

    @Test
    void modeWarn() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withUnknownReporter(ConfigUnknownReporter.WARN)
                .withSources(config("root.value", "value", "root.unknown", "value"))
                .build();

        assertEquals("value", config.getConfigMapping(Root.class).value());
        List<String> messages = logCapture.records().stream().map(LogRecord::getMessage).toList();
        assertEquals(List.of("SRCFG01010: root.unknown in KeyValuesConfigSource does not map to any root"), messages);
    }

    @Test
    void modeFromReporter() {
        Map<String, String> reported = new HashMap<>();
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withUnknownReporter(new ConfigUnknownReporter() {
                    @Override
                    public ConfigUnknownReporter.Mode mode() {
                        return ConfigUnknownReporter.Mode.WARN;
                    }

                    @Override
                    public boolean accepts(String name) {
                        return name.startsWith("other.");
                    }

                    @Override
                    public void report(String name, ConfigValue configValue) {
                        reported.put(name, configValue.getValue());
                    }
                })
                .withSources(config("root.value", "value", "root.unknown", "one", "other.unknown", "two"))
                .build();

        assertEquals(Map.of("other.unknown", "two"), reported);
        List<String> messages = logCapture.records().stream().map(LogRecord::getMessage).toList();
        assertEquals(List.of("SRCFG01010: root.unknown in KeyValuesConfigSource does not map to any root"), messages);
    }

    @Test
    void modeDoesNotApplyWhenValidationIsDisabled() {
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withValidateUnknown(false)
                .withUnknownReporter(ConfigUnknownReporter.WARN)
                .withSources(config("root.value", "value", "root.unknown", "value"))
                .build();

        assertTrue(logCapture.records().isEmpty());
    }

    @Test
    void modeKeepsIgnores() {
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withMappingIgnore("root.**")
                .withUnknownReporter(ConfigUnknownReporter.WARN)
                .withSources(config("root.value", "value", "root.unknown", "value"))
                .build();

        assertTrue(logCapture.records().isEmpty());
    }

    @Test
    void overlappingPrefixesReportOnce() {
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withMapping(Nested.class)
                .withSources(config("root.value", "value", "root.nested.value", "value", "root.nested.unknown", "value"))
                .build());

        assertEquals(1, exception.getProblemCount());
        assertEquals("SRCFG00050: root.nested.unknown in KeyValuesConfigSource does not map to any root",
                exception.getProblem(0).getMessage());
    }

    @Test
    void reporterReceivesAcceptedNamesIncludingIgnoredAndOutOfRoot() {
        Map<String, String> reported = new HashMap<>();
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withMappingIgnore("root.**")
                .withUnknownReporter(reporter(reported, "root.", "other."))
                .withSources(config("root.value", "value", "root.unknown", "one", "other.unknown", "two", "skipped", "three"))
                .build();

        assertEquals(Map.of("root.unknown", "one", "other.unknown", "two"), reported);
    }

    @Test
    void reporterAcceptedNamesAreNotValidated() {
        Map<String, String> reported = new HashMap<>();
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withUnknownReporter(reporter(reported, "root."))
                .withSources(config("root.value", "value", "root.unknown", "one"))
                .build();

        assertEquals(Map.of("root.unknown", "one"), reported);
    }

    @Test
    void reporterRejectedNamesAreValidated() {
        Map<String, String> reported = new HashMap<>();
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withUnknownReporter(reporter(reported, "other."))
                .withSources(config("root.value", "value", "root.unknown", "one", "other.unknown", "two"))
                .build());

        assertEquals(1, exception.getProblemCount());
        assertEquals("SRCFG00050: root.unknown in KeyValuesConfigSource does not map to any root",
                exception.getProblem(0).getMessage());
        assertEquals(Map.of("other.unknown", "two"), reported);
    }

    @Test
    void reporterRunsWhenValidationIsDisabled() {
        Map<String, String> reported = new HashMap<>();
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withValidateUnknown(false)
                .withUnknownReporter(reporter(reported, "root."))
                .withSources(config("root.value", "value", "root.unknown", "one"))
                .build();

        assertEquals(Map.of("root.unknown", "one"), reported);
    }

    @Test
    void reporterDoesNotReceiveUsedNames() {
        Map<String, String> reported = new HashMap<>();
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withUnknownReporter(reporter(reported, "root."))
                .withSources(config("root.value", "value"))
                .build();

        assertTrue(reported.isEmpty());
    }

    @Test
    void reporterDoesNotReceiveEnvNames() {
        Map<String, String> reported = new HashMap<>();
        new SmallRyeConfigBuilder()
                .withMapping(Root.class)
                .withUnknownReporter(reporter(reported, "root."))
                .withSources(config("root.value", "value"))
                .withSources(new EnvConfigSource(Map.of("ROOT_UNKNOWN", "one"), 300))
                .build();

        assertTrue(reported.isEmpty());
    }

    private static ConfigUnknownReporter reporter(Map<String, String> reported, String... prefixes) {
        return new ConfigUnknownReporter() {
            @Override
            public boolean accepts(String name) {
                for (String prefix : prefixes) {
                    if (name.startsWith(prefix)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void report(String name, ConfigValue configValue) {
                reported.put(name, configValue.getValue());
            }
        };
    }

    @ConfigMapping(prefix = "root")
    interface Root {
        String value();
    }

    @ConfigMapping(prefix = "root.nested")
    interface Nested {
        String value();
    }
}
