package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ConfigValueMapViewTest {
    @Test
    public void size() {
        assertEquals(3, sampleMap().size());
    }

    @Test
    public void isEmpty() {
        assertTrue(new ConfigValueMapView(new HashMap<>()).isEmpty());
    }

    @Test
    public void containsKey() {
        final Map<String, String> map = sampleMap();
        assertTrue(map.containsKey("my.prop"));
        assertTrue(map.containsKey("my.null.value"));
        assertTrue(map.containsKey("my.null"));
    }

    @Test
    public void containsValue() {
        final Map<String, String> map = sampleMap();
        assertTrue(map.containsValue("1234"));
    }

    @Test
    public void get() {
        final Map<String, String> map = sampleMap();
        assertEquals("1234", map.get("my.prop"));
        assertNull(map.get("my.null.value"));
        assertNull(map.get("my.null"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void put() {
        sampleMap().put("x", "x");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void remove() {
        sampleMap().remove("my.prop");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void putAll() {
        final HashMap<String, String> newMap = new HashMap<>();
        newMap.put("key", "value");
        sampleMap().putAll(newMap);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clear() {
        sampleMap().clear();
    }

    @Test
    public void keySet() {
        final Set<String> keys = sampleMap().keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("my.prop"));
        assertTrue(keys.contains("my.null"));
        assertTrue(keys.contains("my.null.value"));
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("my.prop"));
    }

    @Test
    public void entrySet() {
        final Set<Map.Entry<String, String>> entries = sampleMap().entrySet();
        assertEquals(3, entries.size());
        assertTrue(entries.contains(new AbstractMap.SimpleImmutableEntry<>("my.prop", "1234")));
        assertTrue(entries.contains(new AbstractMap.SimpleImmutableEntry<>("my.null.value", null)));
        assertTrue(entries.contains(new AbstractMap.SimpleImmutableEntry<>("my.null", null)));
        assertThrows(UnsupportedOperationException.class,
                () -> entries.remove(new AbstractMap.SimpleImmutableEntry<>("my.prop", "1234")));
    }

    @Test
    public void values() {
        final Collection<String> values = sampleMap().values();
        assertEquals(3, values.size());
        assertTrue(values.contains(null));
        assertTrue(values.contains("1234"));
        assertThrows(UnsupportedOperationException.class, () -> values.remove("1234"));
    }

    private ConfigValueMapView sampleMap() {
        final Map<String, ConfigValue> configValueMap = new HashMap<>();
        configValueMap.put("my.prop", ConfigValue.builder().withName("my.prop").withValue("1234").build());
        configValueMap.put("my.prop", ConfigValue.builder().withName("my.prop").withValue("1234").build());
        configValueMap.put("my.null.value", ConfigValue.builder().withName("my.null.value").build());
        configValueMap.put("my.null", null);
        return new ConfigValueMapView(configValueMap);
    }
}
