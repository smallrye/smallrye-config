package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

class ConfigValueTest {
    @Test
    void configValue() {
        final Config config = new SmallRyeConfigBuilder().addDefaultSources()
                .addDefaultInterceptors()
                .withSources(new ConfigValueConfigSource())
                .withSources(new ConfigValueLowerConfigSource())
                .build();

        ConfigValue configValue = (ConfigValue) config.getConfigValue("my.prop");
        assertEquals("my.prop", configValue.getName());
        assertEquals("1234", configValue.getValue());
        assertEquals("1234", configValue.getRawValue());
        assertEquals("ConfigValueConfigSource", configValue.getSourceName());
        assertEquals(1000, configValue.getSourceOrdinal());
    }

    @Test
    void configValueEquals() {
        ConfigValue o1 = ConfigValue.builder().withName("my.prop").build();
        ConfigValue o2 = ConfigValue.builder().withName("my.prop").build();
        assertEquals(o2, o1);

        o1 = ConfigValue.builder().withName("my.prop").withValue("value").build();
        o2 = ConfigValue.builder().withName("my.prop").build();
        assertNotEquals(o2, o1);

        o1 = ConfigValue.builder().withName("my.prop").withLineNumber(1).build();
        o2 = ConfigValue.builder().withName("my.prop").withLineNumber(2).build();
        assertEquals(o2, o1);
    }

    @Test
    void configValueCloning() {
        ConfigValue o1 = ConfigValue.builder().withName("my.prop").build();
        ConfigValue o2 = o1.from().build();
        assertEquals(o2, o1);

        o1 = ConfigValue.builder().withName("my.prop").withValue("value").build();
        o2 = o1.from().withName("my.prop.cloned").build();
        assertNotEquals(o2, o1);
        assertEquals("my.prop.cloned", o2.getName());
        assertEquals(o1.getValue(), o2.getValue());
    }

    @Test
    void configSourcePosition() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("one", "one", "config_ordinal", "1000"))
                .withSources(KeyValuesConfigSource.config("two", "two", "config_ordinal", "500"))
                .withSources(KeyValuesConfigSource.config("three", "three", "config_ordinal", "-500"))
                .withSources(KeyValuesConfigSource.config("four", "four", "config_ordinal", "-1000"))
                .build();

        assertEquals(0, config.getConfigValue("one").getConfigSourcePosition());
        assertEquals(1, config.getConfigValue("two").getConfigSourcePosition());
        assertEquals(2, config.getConfigValue("three").getConfigSourcePosition());
        assertEquals(3, config.getConfigValue("four").getConfigSourcePosition());
    }

    @Test
    void configValueOrDefault() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("one", "one", "two", ""))
                .build();

        assertEquals("one", config.getConfigValue("one").getValueOrDefault(""));
        assertEquals("", config.getConfigValue("two").getValueOrDefault("two"));
        assertEquals("three", config.getConfigValue("three").getValueOrDefault("three"));
    }

    public static class ConfigValueConfigSource implements ConfigSource {
        private final Map<String, String> properties;

        public ConfigValueConfigSource() {
            properties = new HashMap<>();
            properties.put("my.prop", "1234");
        }

        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(final String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return this.getClass().getSimpleName();
        }

        @Override
        public int getOrdinal() {
            return 1000;
        }
    }

    public static class ConfigValueLowerConfigSource implements ConfigSource {
        private final Map<String, String> properties;

        public ConfigValueLowerConfigSource() {
            properties = new HashMap<>();
            properties.put("my.prop", "5678");
        }

        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(final String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return this.getClass().getSimpleName();
        }

        @Override
        public int getOrdinal() {
            return 900;
        }
    }

    @Test
    void withCopies() {
        ConfigValue original = ConfigValue.builder()
                .withName("my.prop")
                .withValue("value")
                .withRawValue("raw")
                .withProfile("prof")
                .withSecret(true)
                .withConfigSourceName("source")
                .withConfigSourceOrdinal(100)
                .withConfigSourcePosition(2)
                .withLineNumber(10)
                .build();

        // every copy keeps all the other fields, the same as a copy through the builder
        assertEquals(original.from().withName("other").build(), original.withName("other"));
        assertEquals(original.from().withValue("other").build(), original.withValue("other"));
        assertEquals(original.from().withProfile("other").build(), original.withProfile("other"));
        assertEquals(original.from().withConfigSourceName("other").build(), original.withConfigSourceName("other"));
        assertEquals(original.from().withConfigSourceOrdinal(1).build(), original.withConfigSourceOrdinal(1));
        assertEquals(original.from().withConfigSourcePosition(1).build(), original.withConfigSourcePosition(1));
        assertEquals(10, original.withName("other").getLineNumber());
        assertEquals(1, original.withLineNumber(1).getLineNumber());
        assertTrue(original.withName("other").isSecret());
        assertFalse(original.withSecret(false).isSecret());
        assertEquals("raw", original.withName("other").getRawValue());
    }

    @Test
    void withCopiesProblems() {
        ConfigValue problems = ConfigValue.builder()
                .withName("my.prop")
                .withValue("value")
                .addProblem(new ConfigValidationException.Problem("problem"))
                .build();
        assertTrue(problems.hasProblems());
        assertNull(problems.getValue());

        // copies keep the problems, and a value cannot be set while there are problems
        assertTrue(problems.withName("other").hasProblems());
        assertEquals(1, problems.withName("other").getProblems().size());
        assertNull(problems.withValue("other").getValue());
        assertTrue(problems.withValue("other").hasProblems());

        ConfigValue noProblems = problems.noProblems();
        assertFalse(noProblems.hasProblems());
        assertEquals(List.of(), noProblems.getProblems());
        assertFalse(noProblems.from().build().hasProblems());
        assertFalse(noProblems.withName("other").hasProblems());
        assertEquals("other", noProblems.withValue("other").getValue());

        // the copy holds the same problems as the original, so the original problems list is left alone
        ConfigValue more = problems.withProblems(List.of(new ConfigValidationException.Problem("another")));
        assertEquals(2, more.getProblems().size());
        assertEquals(1, problems.getProblems().size());
    }
}
