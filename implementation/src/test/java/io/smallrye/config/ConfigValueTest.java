package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
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
}
