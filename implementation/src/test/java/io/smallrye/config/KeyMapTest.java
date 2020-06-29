package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

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
    void unwrap() {
        KeyMap<String> root = new KeyMap<>();
        root.findOrAdd("root").findOrAdd("foo").putRootValue("foo");
        root.findOrAdd("root").findOrAdd("bar").putRootValue("bar");

        Map<String, String> properties = root.toMap();
        assertEquals("foo", properties.get("root.foo"));
        assertEquals("bar", properties.get("root.bar"));
    }
}
