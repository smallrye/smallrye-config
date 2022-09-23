package io.smallrye.config;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class KeyMapTest {
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
    void string() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo.bar").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*").putRootValue("baz");
        map.findOrAdd("root.foo.bar.*.baz").putRootValue("anything");

        assertEquals(
                "KeyMap(no value) {root=>KeyMap(no value) {foo=>KeyMap(value=bar) {bar=>KeyMap(value=baz) {(any)=>KeyMap(value=baz) {baz=>KeyMap(value=anything) {}}}}}}",
                map.toString());
    }

    @Test
    void indexed() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("root.foo[*]").putRootValue("foo.star");
        map.findOrAdd("root.foo[1]").putRootValue("foo.one");
        map.findOrAdd("root.foo[*].name").putRootValue("foo.star.name");

        assertEquals("bar", map.findRootValue("root.foo"));
        assertEquals("foo.star", map.findRootValue("root.foo[*]"));
        assertEquals("foo.one", map.findRootValue("root.foo[1]"));
        assertEquals("foo.star", map.findRootValue("root.foo[2]"));
        assertEquals("foo.star", map.findRootValue("root.foo[1234]"));
        assertEquals("foo.star.name", map.findRootValue("root.foo[3].name"));
        assertNull(map.findRootValue("root.a.name"));
    }

    @Test
    void indexedRoot() {
        ArrayDeque<String> foo = new ArrayDeque<>();
        foo.addLast("root");
        foo.addLast("[");
        foo.addLast("*");
        foo.addLast("]");
        foo.addLast("foo");

        ArrayDeque<String> bar = new ArrayDeque<>();
        bar.addLast("root[*]");
        bar.addLast("bars[*]");

        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd(foo).putRootValue("foo");
        map.findOrAdd(bar).putRootValue("bars");

        assertEquals("foo", map.findRootValue("root.[.0.].foo"));
        assertEquals("bars", map.findRootValue("root[0].bars[0]"));
    }

    @Test
    void putAll() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("root.foo").putRootValue("bar");
        map.findOrAdd("star.foo.*").putRootValue("star.bar");
        map.findOrAdd("star.foo.*.foo").putRootValue("star.bar.foo");

        KeyMap<String> other = new KeyMap<>();
        other.findOrAdd("root.foo.bar").putRootValue("baz");
        other.findOrAdd("star.foo.bar.*").putRootValue("star.baz");

        map.putAll(other);

        assertEquals("bar", map.findRootValue("root.foo"));
        assertEquals("baz", map.findRootValue("root.foo.bar"));
        assertEquals("star.bar", map.findRootValue("star.foo.*"));
        assertEquals("star.bar", map.findRootValue("star.foo.a"));
        assertEquals("star.baz", map.findRootValue("star.foo.bar.*"));
        assertEquals("star.baz", map.findRootValue("star.foo.bar.a"));
        assertEquals("star.bar.foo", map.findRootValue("star.foo.a.foo"));
    }

    @Test
    void putAllAny() {
        KeyMap<String> map = new KeyMap<>();

        KeyMap<String> other = new KeyMap<>();
        other.findOrAdd("root[*].foo").putRootValue("foo");
        other.findOrAdd("root[*].bars[*]").putRootValue("bars");
        other.findOrAdd("root[*].bazs[*]").putRootValue("bazs");

        assertEquals("foo", other.findRootValue("root[0].foo"));
        assertEquals("bars", other.findRootValue("root[0].bars[0]"));
        assertEquals("bazs", other.findRootValue("root[0].bazs[0]"));

        map.putAll(other);

        assertEquals("foo", map.findRootValue("root[0].foo"));
        assertEquals("bars", map.findRootValue("root[0].bars[0]"));
    }

    @Test
    void map() {
        KeyMap<String> map = new KeyMap<>();
        map.findOrAdd("map.roles.*[*]").putRootValue("foo");
        map.findOrAdd("map.threadpool.config[customPool].id").putRootValue("customPool");

        assertEquals("foo", map.findRootValue("map.roles.user[0]"));
        assertEquals("customPool", map.findRootValue("map.threadpool.config[customPool].id"));
    }

    @Test
    void findOrAdd() {
        KeyMap<String> varArgs = new KeyMap<>();
        varArgs.findOrAdd("foo", "bar", "baz").putRootValue("value");

        KeyMap<String> array = new KeyMap<>();
        array.findOrAdd(new String[] { "foo", "bar", "baz" }, 0, 3).putRootValue("value");

        KeyMap<String> string = new KeyMap<>();
        string.findOrAdd("foo.bar.baz").putRootValue("value");

        KeyMap<String> nameIterator = new KeyMap<>();
        nameIterator.findOrAdd(new NameIterator("foo.bar.baz")).putRootValue("value");

        KeyMap<String> iterator = new KeyMap<>();
        iterator.findOrAdd(Stream.of("foo", "bar", "baz").collect(toList()).iterator()).putRootValue("value");

        KeyMap<String> iterable = new KeyMap<>();
        iterable.findOrAdd(Stream.of("foo", "bar", "baz").collect(toList())).putRootValue("value");

        assertEquals("value", varArgs.findRootValue("foo.bar.baz"));
        assertEquals("value", array.findRootValue("foo.bar.baz"));
        assertEquals("value", string.findRootValue("foo.bar.baz"));
        assertEquals("value", nameIterator.findRootValue("foo.bar.baz"));
        assertEquals("value", iterator.findRootValue("foo.bar.baz"));
        assertEquals("value", iterable.findRootValue("foo.bar.baz"));
    }

    @Test
    void findOrAddDotted() {
        //        KeyMap<String> map = new KeyMap<>();
        //        map.findOrAdd("map.\"quoted.key\".value").putRootValue("value");
        //        assertEquals("value", map.findRootValue("map.\"quoted.key\".value"));
        //        assertNull(map.findRootValue("map.quoted.key.value"));

        KeyMap<String> varArgs = new KeyMap<>();
        varArgs.findOrAdd("foo", "bar.bar", "baz").putRootValue("value");

        KeyMap<String> array = new KeyMap<>();
        array.findOrAdd(new String[] { "foo", "bar.bar", "baz" }, 0, 3).putRootValue("value");

        KeyMap<String> string = new KeyMap<>();
        string.findOrAdd("foo.\"bar.bar\".baz").putRootValue("value");

        KeyMap<String> nameIterator = new KeyMap<>();
        nameIterator.findOrAdd(new NameIterator("foo.\"bar.bar\".baz")).putRootValue("value");

        KeyMap<String> iterator = new KeyMap<>();
        iterator.findOrAdd(Stream.of("foo", "bar.bar", "baz").collect(toList()).iterator()).putRootValue("value");

        KeyMap<String> iterable = new KeyMap<>();
        iterable.findOrAdd(Stream.of("foo", "bar.bar", "baz").collect(toList())).putRootValue("value");

        assertEquals("value", varArgs.findRootValue("foo.\"bar.bar\".baz"));
        assertEquals("value", array.findRootValue("foo.\"bar.bar\".baz"));
        assertEquals("value", string.findRootValue("foo.\"bar.bar\".baz"));
        assertEquals("value", nameIterator.findRootValue("foo.\"bar.bar\".baz"));
        assertEquals("value", iterator.findRootValue("foo.\"bar.bar\".baz"));
        assertEquals("value", iterable.findRootValue("foo.\"bar.bar\".baz"));
    }
}
