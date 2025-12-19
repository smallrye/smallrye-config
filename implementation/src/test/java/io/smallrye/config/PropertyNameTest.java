package io.smallrye.config;

import static io.smallrye.config.PropertyName.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("StringOperationCanBeSimplified")
class PropertyNameTest {
    @Test
    void propertyNameEquals() {
        assertEquals(name(new String("foo")), name(new String("foo")));
        assertEquals(name(new String("foo.bar")), name(new String("foo.bar")));
        assertEquals(name("foo.*"), name("foo.bar"));
        assertEquals(name(new String("foo.*")), name(new String("foo.*")));
        assertEquals(name("*"), name("foo"));
        assertEquals(name("foo"), name("*"));
        assertEquals(name("foo.*.bar"), name("foo.bar.bar"));
        assertEquals(name("foo.bar.bar"), name("foo.*.bar"));
        assertEquals(name("foo.*.bar"), name("foo.\"bar\".bar"));
        assertEquals(name("foo.\"bar\".bar"), name("foo.*.bar"));
        assertEquals(name("foo.*.bar"), name("foo.\"bar-baz\".bar"));
        assertEquals(name("foo.\"bar-baz\".bar"), name("foo.*.bar"));
        assertNotEquals(name("foo.*.bar"), name("foo.bar.baz"));
        assertNotEquals(name("foo.bar.baz"), name("foo.*.bar"));
        assertEquals(name(new String("foo.bar[*]")), name(new String("foo.bar[*]")));
        assertEquals(name("foo.bar[*]"), name("foo.bar[0]"));
        assertEquals(name("foo.bar[0]"), name("foo.bar[*]"));
        assertEquals(name("foo.*[*]"), name("foo.bar[0]"));
        assertEquals(name("foo.bar[0]"), name("foo.*[*]"));
        assertEquals(name("foo.*[*]"), name("foo.baz[1]"));
        assertEquals(name("foo.baz[1]"), name("foo.*[*]"));
        assertNotEquals(name("foo.*[*]"), name("foo.baz[x]"));
        assertNotEquals(name("foo.baz[x]"), name("foo.*[*]"));
        assertEquals(name("foo.*[*].bar[*]"), name("foo.baz[0].bar[0]"));
        assertEquals(name(new String("foo.*[*].bar[*]")), name(new String("foo.*[*].bar[*]")));
        assertEquals(name("foo.baz[0].bar[0]"), name("foo.*[*].bar[*]"));
        assertEquals(name("foo.baz[99].bar[0]"), name("foo.*[99].bar[*]"));
        assertNotEquals(name("foo.baz[99].bar[0]"), name("foo.*[9].bar[*]"));
        assertNotEquals(name("foo.baz[99].bar[0]"), name("foo.*[xy].bar[*]"));
        assertEquals(name(new String("foo.baz[0].bar[0]")), name(new String("foo.baz[0].bar[0]")));
        assertEquals(name(new String("foo.baz[99].bar[123]")), name(new String("foo.baz[99].bar[123]")));
        assertNotEquals(name(new String("foo.baz[99].bar[123]")), name(new String("foo.baz[99].bar[xyz]")));
        assertNotEquals(name("foo.bar.baz[*]").hashCode(), name("foo.bar.*").hashCode());
        assertNotEquals(name("foo.bar.baz[*]"), name("foo.bar.*"));

        assertEquals(name("foo").hashCode(), name("foo").hashCode());
        assertEquals(name("foo.bar").hashCode(), name("foo.bar").hashCode());
        assertEquals(name("foo.*").hashCode(), name("foo.bar").hashCode());
        assertEquals(name("foo.*.bar").hashCode(), name("foo.bar.bar").hashCode());
        assertEquals(name("foo.*.bar").hashCode(), name("foo.\"bar\".bar").hashCode());
        assertEquals(name(new String("foo.\"bar\".bar")).hashCode(), name(new String("foo.\"bar\".bar")).hashCode());
        assertEquals(name("foo.*.bar").hashCode(), name("foo.\"bar-baz\".bar").hashCode());
        assertEquals(name(new String("foo.\"bar-baz\".bar")).hashCode(), name(new String("foo.\"bar-baz\".bar")).hashCode());

        assertEquals(name("*"), name("\"foo\""));
        assertEquals(name("\"foo\""), name("*"));

        assertEquals(name("*.bar"), name("foo.bar"));
        assertEquals(name("*.bar"), name("\"foo\".bar"));

        assertNotEquals(name("*"), name(""));
        assertNotEquals(name(""), name("*"));
    }

    @Test
    void propertyNameEqualsRegions() {
        assertTrue(PropertyName.equals("foo.bar", 4, 3, "bar", 0, 3));
        assertTrue(PropertyName.equals("foo.\"bar\".baz", 4, 5, "*", 0, 1));
    }

    @Test
    void simpleNames() {
        assertFalse(PropertyName.equals("sync", "async"));
        assertFalse(PropertyName.equals("async", "sync"));
        assertFalse(PropertyName.equals("async-client", "sync-client"));
        assertFalse(PropertyName.equals("sync-client", "async-client"));
    }

    @Test
    void indexed() {
        assertEquals(name("indexed[]"), name(new String("indexed[]")));
        assertEquals(name("indexed[0]"), name(new String("indexed[0]")));

        assertEquals(name("indexed[*]"), name(new String("indexed[0]")));
        assertEquals(name("indexed[*]"), name(new String("indexed[10]")));
        assertEquals(name("indexed[*]"), name(new String("indexed[99999]")));

        assertEquals(name("indexed[0]"), name(new String("indexed[*]")));
        assertEquals(name("indexed[10]"), name(new String("indexed[*]")));
        assertEquals(name("indexed[123456789]"), name(new String("indexed[*]")));

        assertNotEquals(name("indexed[1]"), name(new String("indexed[0]")));
        assertNotEquals(name("indexed[123]"), name(new String("indexed[456]")));
        assertNotEquals(name("indexed[0]"), name(new String("indexed[x]")));
        assertNotEquals(name("indexed[123]"), name(new String("indexed[x]")));
        assertNotEquals(name("indexed[0]"), name(new String("indexed[xyz]")));
    }

    @Test
    void greedyMap() {
        assertEquals(name("*"), name("greedy"));
        assertEquals(name("greedy"), name("*"));
        assertEquals(name("*"), name("greedy.one.two"));
        assertEquals(name("greedy.one.two"), name("*"));

        assertEquals(name("greedy.*.map.*"), name(new String("greedy.key.map.key")));
        assertEquals(name("greedy.key.map.key"), name(new String("greedy.*.map.*")));
        assertEquals(name("greedy.*.map.*"), name(new String("greedy.key.map.one.two")));
        assertEquals(name("greedy.key.map.one.two"), name(new String("greedy.*.map.*")));
        assertEquals(name("greedy.*.map.*"), name(new String("greedy.key.map.one.two.three")));
        assertEquals(name("greedy.key.map.one.two.three"), name(new String("greedy.*.map.*")));

        assertNotEquals(name("greedy.*.map.*"), name(new String("greedy.one.two.map.key")));
        assertNotEquals(name("greedy.one.two.map.key"), name(new String("greedy.*.map.*")));
        assertNotEquals(name("greedy.*.map.*"), name(new String("greedy.one.two.map.one.two")));
        assertNotEquals(name("greedy.one.two.map.one.two"), name(new String("greedy.*.map.*")));

        assertEquals(name("greedy.*.map-list.*[*]"), name(new String("greedy.key.map-list.key[0]")));
        assertEquals(name("greedy.key.map-list.key[0]"), name(new String("greedy.*.map-list.*[*]")));
        assertEquals(name("greedy.*.map-list.*[*]"), name(new String("greedy.key.map-list.one.two[0]")));
        assertEquals(name("greedy.key.map-list.one.two[0]"), name(new String("greedy.*.map-list.*[*]")));

        assertNotEquals(name("greedy.*.map-list.*[*]"), name(new String("greedy.one.two.map-list.key[0]")));
        assertNotEquals(name("greedy.one.two.map-list.key[0]"), name(new String("greedy.*.map-list.*[*]")));
        assertNotEquals(name("greedy.*.map-list.*[*]"), name(new String("greedy.one.two.map-list.one.two[0]")));
        assertNotEquals(name("greedy.one.two.map-list.one.two[0]"), name(new String("greedy.*.map-list.*[*]")));

        assertEquals(name("greedy.*.map.*").hashCode(), name(("greedy.key.map.key")).hashCode());
        assertEquals(name("greedy.*.map.*").hashCode(), name(("greedy.key.map.one.two")).hashCode());
        assertEquals(name("greedy.*.map-list.*[*]").hashCode(), name(("greedy.one.two.map-list.key[0]")).hashCode());
        assertEquals(name("greedy.*.map-list.*[*]").hashCode(), name(("greedy.one.two.map-list.one.two[0]")).hashCode());
    }
}
