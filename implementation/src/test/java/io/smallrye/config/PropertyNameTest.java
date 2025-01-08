package io.smallrye.config;

import static io.smallrye.config.PropertyName.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
        assertEquals(name(new String("foo.baz[0].bar[0]")), name(new String("foo.baz[0].bar[0]")));
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
}
