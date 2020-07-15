package io.smallrye.config;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class KeyMapTest {
    @Test
    void find() {
        KeyMap<String> root = new KeyMap<>();
        root.findOrAdd("root").findOrAdd("foo").putRootValue("foo");
        root.findOrAdd("root").findOrAdd("bar").putRootValue("bar");

        assertEquals("foo", root.findRootValue("root.foo"));
        assertEquals("bar", root.findRootValue("root.bar"));
    }

    @Test
    void findOrAddPath() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo.bar").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*.baz").putRootValue("anything");

        assertEquals("bar", map.findRootValue("root.foo"));
        assertEquals("baz", map.findRootValue("root.foo.bar"));
        assertEquals("baz", map.findRootValue("root.foo.bar.x"));
        assertEquals("baz", map.findRootValue("root.foo.bar.y"));
        assertEquals("anything", map.findRootValue("root.foo.bar.x.baz"));
        assertEquals("anything", map.findRootValue("root.foo.bar.y.baz"));
        assertNull(map.findRootValue("root.bar"));
        assertNull(map.findRootValue("root.foo.bar.y.baz.z"));
    }

    @Test
    void findOrAddVarArgs() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root", "foo").putRootValue("bar");
        map.findOrAdd("root", "foo", "bar").putRootValue("baz");
        map.findOrAdd("root", "foo", "bar", "*").putRootValue("baz");
        map.findOrAdd("root", "foo", "bar", "*", "baz").putRootValue("anything");

        assertEquals("bar", map.findRootValue("root.foo"));
        assertEquals("baz", map.findRootValue("root.foo.bar"));
        assertEquals("baz", map.findRootValue("root.foo.bar.x"));
        assertEquals("baz", map.findRootValue("root.foo.bar.y"));
        assertEquals("anything", map.findRootValue("root.foo.bar.x.baz"));
        assertEquals("anything", map.findRootValue("root.foo.bar.y.baz"));
        assertNull(map.findRootValue("root.bar"));
        assertNull(map.findRootValue("root.foo.bar.y.baz.z"));
    }

    @Test
    void findOrAddIterator() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd(Stream.of("root", "foo").collect(toList())).putRootValue("bar");
        map.findOrAdd(Stream.of("root", "foo", "bar").collect(toList())).putRootValue("baz");
        map.findOrAdd(Stream.of("root", "foo", "bar", "*").collect(toList())).putRootValue("baz");
        map.findOrAdd(Stream.of("root", "foo", "bar", "*", "baz").collect(toList())).putRootValue("anything");

        assertEquals("bar", map.findRootValue("root.foo"));
        assertEquals("baz", map.findRootValue("root.foo.bar"));
        assertEquals("baz", map.findRootValue("root.foo.bar.x"));
        assertEquals("baz", map.findRootValue("root.foo.bar.y"));
        assertEquals("anything", map.findRootValue("root.foo.bar.x.baz"));
        assertEquals("anything", map.findRootValue("root.foo.bar.y.baz"));
        assertNull(map.findRootValue("root.bar"));
        assertNull(map.findRootValue("root.foo.bar.y.baz.z"));
    }

    @Test
    void merge() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo.bar").putRootValue("baz");
        Map<String, String> flatMap = new HashMap<>();
        flatMap.put("root.foo", "foo");
        flatMap.put("root.foo.bar.*", "baz");
        flatMap.put("root.foo.bar.*.baz", "anything");

        flatMap.forEach((key, value) -> map.findOrAdd(key).putRootValue(value));

        assertEquals("foo", map.findRootValue("root.foo"));
        assertEquals("baz", map.findRootValue("root.foo.bar"));
        assertEquals("baz", map.findRootValue("root.foo.bar.x"));
        assertEquals("baz", map.findRootValue("root.foo.bar.y"));
        assertEquals("anything", map.findRootValue("root.foo.bar.x.baz"));
        assertEquals("anything", map.findRootValue("root.foo.bar.y.baz"));
        assertNull(map.findRootValue("root.bar"));
        assertNull(map.findRootValue("root.foo.bar.y.baz.z"));
    }

    @Test
    void empty() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("", "foo").putRootValue("bar");

        assertEquals("bar", map.findRootValue(".foo"));
    }

    @Test
    void unwrap() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo.bar").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*.baz").putRootValue("anything");

        Map<String, String> properties = map.toMap();
        assertTrue(properties.containsKey("root.foo"));
        assertTrue(properties.containsKey("root.foo.bar"));
        assertTrue(properties.containsKey("root.foo.bar.*"));
        assertTrue(properties.containsKey("root.foo.bar.*.baz"));

        assertEquals("bar", properties.get("root.foo"));
        assertEquals("baz", properties.get("root.foo.bar"));
        assertEquals("baz", properties.get("root.foo.bar.*"));
        assertEquals("anything", properties.get("root.foo.bar.*.baz"));
    }
}
