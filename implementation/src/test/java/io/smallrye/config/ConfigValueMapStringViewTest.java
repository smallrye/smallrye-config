package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ConfigValueMapStringViewTest {
    @Test
    public void size() {
        assertEquals(2, sampleMap().size());
    }

    @Test
    public void isEmpty() {
        assertTrue(new ConfigValueMapStringView(new HashMap<>(), "test", 1).isEmpty());
    }

    @Test
    public void containsKey() {
        final Map<String, ConfigValue> map = sampleMap();
        assertTrue(map.containsKey("my.prop"));
        assertTrue(map.containsKey("my.null"));
    }

    @Test
    public void containsValue() {
        final Map<String, ConfigValue> map = sampleMap();
        assertTrue(map.containsValue(ConfigValue.builder().withValue("1234").build()));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void get() {
        final Map<String, ConfigValue> map = sampleMap();
        assertEquals(toConfigValue("my.prop", "1234"), map.get("my.prop"));
        assertNull(map.get("my.null"));
    }

    @Test
    public void put() {
        assertThrows(UnsupportedOperationException.class, () -> sampleMap().put("x", toConfigValue("x", "y")));
    }

    @Test
    public void remove() {
        assertThrows(UnsupportedOperationException.class, () -> sampleMap().remove("my.prop"));
    }

    @Test
    public void putAll() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final HashMap<String, ConfigValue> newMap = new HashMap<>();
            newMap.put("key", toConfigValue("x", "y"));
            sampleMap().putAll(newMap);
        });
    }

    @Test
    public void clear() {
        assertThrows(UnsupportedOperationException.class, () -> sampleMap().clear());
    }

    @Test
    public void keySet() {
        final Set<String> keys = sampleMap().keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("my.prop"));
        assertTrue(keys.contains("my.null"));
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("my.prop"));
    }

    @Test
    public void entrySet() {
        final Set<Map.Entry<String, ConfigValue>> entries = sampleMap().entrySet();
        assertEquals(2, entries.size());
        assertTrue(entries.contains(new AbstractMap.SimpleImmutableEntry<>("my.prop", toConfigValue("my.prop", "1234"))));
        assertTrue(entries.contains(new AbstractMap.SimpleImmutableEntry<>("my.null", (ConfigValue) null)));
        assertThrows(UnsupportedOperationException.class, () -> entries.remove(
                new AbstractMap.SimpleImmutableEntry<>("my.prop", toConfigValue("my.prop", "1234"))));
    }

    @Test
    public void values() {
        final Collection<ConfigValue> values = sampleMap().values();
        assertEquals(2, values.size());
        assertTrue(values.contains(null));
        assertTrue(values.contains(toConfigValue("my.prop", "1234")));
        assertThrows(UnsupportedOperationException.class, () -> values.remove(toConfigValue("my.prop", "1234")));
    }

    private static ConfigValueMapStringView sampleMap() {
        final Map<String, String> configValueMap = new HashMap<>();
        configValueMap.put("my.prop", "1234");
        configValueMap.put("my.prop", "1234");
        configValueMap.put("my.null", null);
        return new ConfigValueMapStringView(configValueMap, "test", 1);
    }

    private static ConfigValue toConfigValue(String name, String value) {
        return ConfigValue.builder()
                .withName(name)
                .withValue(value)
                .withRawValue(value)
                .withConfigSourceName("test")
                .withConfigSourceOrdinal(1)
                .build();
    }
}
