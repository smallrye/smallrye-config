package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ConfigSourceOrdinalTest {
    @Test
    void propertiesOrdinal() {
        Map<String, String> properties = new HashMap<>();
        properties.put("config_ordinal", "999");
        properties.put("my.prop", "1234");

        Map<String, String> propertiesWithoutPriority = new HashMap<>();
        propertiesWithoutPriority.put("my.prop", "5678");
        propertiesWithoutPriority.put("another.prop", "5678");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "test", 0))
                .withSources(new PropertiesConfigSource(propertiesWithoutPriority, "test", 998))
                .build();

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals(999, config.getConfigValue("my.prop").getConfigSourceOrdinal());
        assertEquals("5678", config.getConfigValue("another.prop").getValue());
        assertEquals(998, config.getConfigValue("another.prop").getConfigSourceOrdinal());
    }

    @Test
    void valuesOrdinal() {
        Map<String, String> properties = new HashMap<>();
        properties.put("config_ordinal", "999");
        properties.put("my.prop", "1234");

        Map<String, String> propertiesWithoutPriority = new HashMap<>();
        propertiesWithoutPriority.put("my.prop", "5678");
        propertiesWithoutPriority.put("another.prop", "5678");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test", 0))
                .withSources(new ConfigValuePropertiesConfigSource(propertiesWithoutPriority, "test", 998))
                .build();

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals(999, config.getConfigValue("my.prop").getConfigSourceOrdinal());
        assertEquals("5678", config.getConfigValue("another.prop").getValue());
        assertEquals(998, config.getConfigValue("another.prop").getConfigSourceOrdinal());
    }
}
