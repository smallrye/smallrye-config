package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PropertyNameTest {
    @Test
    void mappingNameEquals() {
        assertEquals(new PropertyName(new String("foo")), new PropertyName(new String("foo")));
        assertEquals(new PropertyName(new String("foo.bar")), new PropertyName(new String("foo.bar")));
        assertEquals(new PropertyName("foo.*"), new PropertyName("foo.bar"));
        assertEquals(new PropertyName(new String("foo.*")), new PropertyName(new String("foo.*")));
        assertEquals(new PropertyName("*"), new PropertyName("foo"));
        assertEquals(new PropertyName("foo"), new PropertyName("*"));
        assertEquals(new PropertyName("foo.*.bar"), new PropertyName("foo.bar.bar"));
        assertEquals(new PropertyName("foo.bar.bar"), new PropertyName("foo.*.bar"));
        assertEquals(new PropertyName("foo.*.bar"), new PropertyName("foo.\"bar\".bar"));
        assertEquals(new PropertyName("foo.\"bar\".bar"), new PropertyName("foo.*.bar"));
        assertEquals(new PropertyName("foo.*.bar"), new PropertyName("foo.\"bar-baz\".bar"));
        assertEquals(new PropertyName("foo.\"bar-baz\".bar"), new PropertyName("foo.*.bar"));
        assertNotEquals(new PropertyName("foo.*.bar"), new PropertyName("foo.bar.baz"));
        assertNotEquals(new PropertyName("foo.bar.baz"), new PropertyName("foo.*.bar"));
        assertEquals(new PropertyName(new String("foo.bar[*]")), new PropertyName(new String("foo.bar[*]")));
        assertEquals(new PropertyName("foo.bar[*]"), new PropertyName("foo.bar[0]"));
        assertEquals(new PropertyName("foo.bar[0]"), new PropertyName("foo.bar[*]"));
        assertEquals(new PropertyName("foo.*[*]"), new PropertyName("foo.bar[0]"));
        assertEquals(new PropertyName("foo.bar[0]"), new PropertyName("foo.*[*]"));
        assertEquals(new PropertyName("foo.*[*]"), new PropertyName("foo.baz[1]"));
        assertEquals(new PropertyName("foo.baz[1]"), new PropertyName("foo.*[*]"));
        assertNotEquals(new PropertyName("foo.*[*]"), new PropertyName("foo.baz[x]"));
        assertNotEquals(new PropertyName("foo.baz[x]"), new PropertyName("foo.*[*]"));
        assertEquals(new PropertyName("foo.*[*].bar[*]"), new PropertyName("foo.baz[0].bar[0]"));
        assertEquals(new PropertyName(new String("foo.*[*].bar[*]")), new PropertyName(new String("foo.*[*].bar[*]")));
        assertEquals(new PropertyName("foo.baz[0].bar[0]"), new PropertyName("foo.*[*].bar[*]"));
        assertEquals(new PropertyName(new String("foo.baz[0].bar[0]")), new PropertyName(new String("foo.baz[0].bar[0]")));
        assertNotEquals(new PropertyName("foo.bar.baz[*]").hashCode(), new PropertyName("foo.bar.*").hashCode());
        assertNotEquals(new PropertyName("foo.bar.baz[*]"), new PropertyName("foo.bar.*"));

        assertEquals(new PropertyName("foo").hashCode(), new PropertyName("foo").hashCode());
        assertEquals(new PropertyName("foo.bar").hashCode(), new PropertyName("foo.bar").hashCode());
        assertEquals(new PropertyName("foo.*").hashCode(), new PropertyName("foo.bar").hashCode());
        assertEquals(new PropertyName("foo.*.bar").hashCode(), new PropertyName("foo.bar.bar").hashCode());
        assertEquals(new PropertyName("foo.*.bar").hashCode(), new PropertyName("foo.\"bar\".bar").hashCode());
        assertEquals(new PropertyName(new String("foo.\"bar\".bar")).hashCode(),
                new PropertyName(new String("foo.\"bar\".bar")).hashCode());
        assertEquals(new PropertyName("foo.*.bar").hashCode(), new PropertyName("foo.\"bar-baz\".bar").hashCode());
        assertEquals(new PropertyName(new String("foo.\"bar-baz\".bar")).hashCode(),
                new PropertyName(new String("foo.\"bar-baz\".bar")).hashCode());
    }
}
