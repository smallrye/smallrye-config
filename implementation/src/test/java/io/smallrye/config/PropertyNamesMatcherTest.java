package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PropertyNamesMatcherTest {
    @Test
    void conflicts() {
        PropertyNamesMatcher<String> matcher = new PropertyNamesMatcher<>();
        matcher.add(configClass(Another.class).getProperties());
        matcher.add(configClass(Conflicts.class).getProperties());

        assertTrue(matcher.matches("map.1.one"));
        assertTrue(matcher.matches("map.2.one.two"));
        assertEquals("one", matcher.get("map.1.one"));
        assertEquals("two", matcher.get("map.2.one.two"));

        assertTrue(matcher.matches("plain.one"));
        assertTrue(matcher.matches("plain.one.two"));
        assertTrue(matcher.matches("plain.one.value"));
        assertNull(matcher.get("plain.one"));
        assertNull(matcher.get("plain.one.two"));
        assertEquals("value", matcher.get("plain.one.value"));

        assertTrue(matcher.matches("map.one[1].one"));
        assertTrue(matcher.matches("map.one[1].two"));
        assertTrue(matcher.matches("map.one[1].one.two"));
    }

    @Test
    void greedyAndCollections() {
        PropertyNamesMatcher<String> matcher = new PropertyNamesMatcher<>();
        matcher.add("map.*", "*");
        matcher.add("map.*[*]", "*[*]");
        assertTrue(matcher.matches("map.one"));
        assertTrue(matcher.matches("map.one[1]"));
        assertEquals("*", matcher.get("map.one"));
        assertEquals("*[*]", matcher.get("map.one[1]"));
    }

    @ConfigMapping
    interface Conflicts {
        Map<String, Nested> map();

        Map<String, String> plain();

        interface Nested {
            @WithDefault("one")
            String one();

            @WithName("one.two")
            @WithDefault("two")
            String two();
        }
    }

    @ConfigMapping
    interface Another {
        Map<String, List<String>> map();

        Map<String, Nested> plain();

        interface Nested {
            @WithDefault("value")
            String value();
        }
    }
}
