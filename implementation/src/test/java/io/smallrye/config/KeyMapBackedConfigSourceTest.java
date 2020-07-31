package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

class KeyMapBackedConfigSourceTest {
    @Test
    void getProperties() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo.bar").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*.baz").putRootValue("anything");

        ConfigSource source = getSource(map);
        Map<String, String> properties = source.getProperties();
        assertTrue(properties.isEmpty());
    }

    @Test
    void getValue() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo.bar").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*.baz").putRootValue("anything");

        ConfigSource source = getSource(map);
        assertEquals("bar", source.getValue("root.foo"));
        assertEquals("baz", source.getValue("root.foo.bar"));
        assertEquals("baz", source.getValue("root.foo.bar.x"));
        assertEquals("baz", source.getValue("root.foo.bar.y"));
        assertEquals("anything", source.getValue("root.foo.bar.x.baz"));
        assertEquals("anything", source.getValue("root.foo.bar.y.baz"));
        assertNull(source.getValue("root.bar"));
        assertNull(source.getValue("root.foo.bar.y.baz.z"));
    }

    private ConfigSource getSource(final KeyMap<String> properties) {
        return new KeyMapBackedConfigSource("test", 0, properties);
    }
}
