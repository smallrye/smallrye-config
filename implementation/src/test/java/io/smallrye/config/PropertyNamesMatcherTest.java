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
    void doubleStarGreedy() {
        PropertyNamesMatcher<String> matcher = new PropertyNamesMatcher<>();
        matcher.add("foo.**", "greedy");

        assertTrue(matcher.matches("foo.bar"));
        assertTrue(matcher.matches("foo.bar.baz"));
        assertTrue(matcher.matches("foo.bar.baz.qux"));
        assertFalse(matcher.matches("foo"));
        assertFalse(matcher.matches("bar.baz"));

        assertEquals("greedy", matcher.get("foo.bar"));
        assertEquals("greedy", matcher.get("foo.bar.baz"));
        assertEquals("greedy", matcher.get("foo.bar.baz.qux"));
        assertNull(matcher.get("foo"));
        assertNull(matcher.get("bar.baz"));
    }

    @Test
    void doubleStarWithSpecificMatch() {
        PropertyNamesMatcher<String> matcher = new PropertyNamesMatcher<>();
        matcher.add("foo.**", "greedy");
        matcher.add("foo.bar.baz", "exact");

        assertTrue(matcher.matches("foo.bar.baz"));
        assertTrue(matcher.matches("foo.bar.qux"));
        assertTrue(matcher.matches("foo.one.two.three"));

        assertEquals("exact", matcher.get("foo.bar.baz"));
        assertEquals("greedy", matcher.get("foo.bar.qux"));
        assertEquals("greedy", matcher.get("foo.one.two.three"));
    }

    @Test
    void doubleStarWithSingleStar() {
        PropertyNamesMatcher<String> matcher = new PropertyNamesMatcher<>();
        matcher.add("foo.*", "single");
        matcher.add("foo.**", "greedy");

        assertTrue(matcher.matches("foo.bar"));
        assertTrue(matcher.matches("foo.bar.baz"));

        assertEquals("single", matcher.get("foo.bar"));
        assertEquals("greedy", matcher.get("foo.bar.baz"));
    }

    @Test
    void doubleStarWithWildcardPath() {
        PropertyNamesMatcher<String> matcher = new PropertyNamesMatcher<>();
        matcher.add("foo.*.bar.**", "deep");

        assertTrue(matcher.matches("foo.one.bar.baz"));
        assertTrue(matcher.matches("foo.one.bar.baz.qux"));
        assertFalse(matcher.matches("foo.one.bar"));
        assertFalse(matcher.matches("foo.one.baz"));

        assertEquals("deep", matcher.get("foo.one.bar.baz"));
        assertEquals("deep", matcher.get("foo.one.bar.baz.qux"));
        assertNull(matcher.get("foo.one.bar"));
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

    @Test
    void allPaths() {
        PropertyNamesMatcher<?> matcher = new PropertyNamesMatcher<>();
        matcher.add("name.*");

        assertTrue(matcher.matches("name.one"));
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
