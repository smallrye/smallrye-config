package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ConfigValueConfigSourceWrapperTest {
    @Test
    public void getConfigValue() {
        ConfigValue configValue = config().getConfigValue("my.prop");
        assertNotNull(configValue);
        assertEquals("1234", configValue.getValue());
        assertEquals("1234", configValue.getRawValue());
    }

    @Test
    public void getConfigValueProperties() {
        Map<String, ConfigValue> map = config().getConfigValueProperties();
        assertEquals(1, map.size());
        assertTrue(map.containsKey("my.prop"));
        assertEquals("1234", map.get("my.prop").getValue());
    }

    @Test
    public void getProperties() {
        final Map<String, String> map = config().getProperties();
        assertEquals(1, map.size());
        assertTrue(map.containsKey("my.prop"));
    }

    @Test
    public void getValue() {
        assertEquals("1234", config().getValue("my.prop"));
    }

    @Test
    public void getPropertyNames() {
        final Set<String> names = config().getPropertyNames();
        assertEquals(1, names.size());
        assertEquals("my.prop", names.iterator().next());
    }

    @Test
    public void getName() {
        assertEquals("KeyValuesConfigSource", config().getName());
    }

    @Test
    public void getOrdinal() {
        assertEquals(100, config().getOrdinal());
    }

    private static ConfigValueConfigSource config() {
        return ConfigValueConfigSourceWrapper.wrap(KeyValuesConfigSource.config("my.prop", "1234"));
    }
}
