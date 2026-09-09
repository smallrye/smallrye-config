package io.smallrye.config;

import static io.smallrye.config.PropertyName.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "StringOperationCanBeSimplified", "EqualsWithItself" })
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
        assertTrue(PropertyName.equals("foo.\"bar\".baz", 4, 5, new String("\"bar\""), 0, 5));
        assertTrue(PropertyName.equals("foo.\"bar.baz\".qux", 4, 9, new String("\"bar.baz\""), 0, 9));
        assertTrue(PropertyName.equals("foo.\"bar.baz\".\"qux\"", 14, 5, new String("qux"), 0, 3));
        assertFalse(PropertyName.equals("foo.\"bar.baz\".qux", 4, 9, new String("bar.baz"), 0, 7));
    }

    @Test
    void propertyNameEqualsRegionsQuoteOutsideRegion() {
        // a quote that falls outside the region is not part of the comparison, so the quote that remains is one
        // the segment requires and it opens a run that the region never closes. The region below is "a.b".c cut
        // after the opening quote, which is the two segments a and b".c

        assertTrue(PropertyName.equals("a.*", 0, 3, "foo.\"a.b\".c", 5, 6));
        assertTrue(PropertyName.equals("foo.\"a.b\".c", 5, 6, "a.*", 0, 3));
        assertTrue(PropertyName.equals("*.*", 0, 3, "foo.\"a.b\".c", 5, 4));
        assertTrue(PropertyName.equals("foo.\"a.b\".c", 5, 4, "*.*", 0, 3));

        assertFalse(PropertyName.equals("*.c", 0, 3, "foo.\"a.b\".c", 5, 6));
        assertFalse(PropertyName.equals("foo.\"a.b\".c", 5, 6, "*.c", 0, 3));

        // the unclosed quote holds the dot, so b".c is a single segment that the star stands for
        assertTrue(PropertyName.equals("*", 0, 1, "foo.\"a.b\".c", 7, 4));
        assertTrue(PropertyName.equals("foo.\"a.b\".c", 7, 4, "*", 0, 1));

        assertFalse(PropertyName.equals("*.map.*", 0, 7, "greedy.\"a.b\".map.k", 8, 10));
        assertFalse(PropertyName.equals("greedy.\"a.b\".map.k", 8, 10, "*.map.*", 0, 7));

        // the same regions, compared as standalone names
        assertTrue(PropertyName.equals(new String("a.*"), new String("a.b\".c")));
        assertTrue(PropertyName.equals(new String("*.*"), new String("a.b\"")));
        assertFalse(PropertyName.equals(new String("*.c"), new String("a.b\".c")));
        assertTrue(PropertyName.equals(new String("*"), new String("b\".c")));
        assertFalse(PropertyName.equals(new String("*.map.*"), new String("a.b\".map.k")));
    }

    @Test
    void propertyNameEqualsRegionsBracketOutsideRegion() {
        // an opening bracket that falls outside the region is not part of the comparison, so the region does not
        // hold an indexed segment and the remaining index digits are plain characters
        assertFalse(PropertyName.equals("[0]", 0, 3, "foo[0].bar", 4, 2));
        assertFalse(PropertyName.equals("foo[0].bar", 4, 2, "[0]", 0, 3));
        assertFalse(PropertyName.equals("[*]", 0, 3, "foo[0].bar", 4, 2));
        assertFalse(PropertyName.equals("foo[0].bar", 4, 2, "[*]", 0, 3));
        assertFalse(PropertyName.equals("[0]", 0, 3, "foo[0].bar", 5, 1));
        assertFalse(PropertyName.equals("foo[0].bar", 5, 1, "[0]", 0, 3));
        assertFalse(PropertyName.equals("[*]", 0, 3, "x[12].y[3]", 3, 2));
        assertFalse(PropertyName.equals("x[12].y[3]", 3, 2, "[*]", 0, 3));
        assertFalse(PropertyName.equals("[*]", 0, 3, "list[*].nested[*]", 16, 1));
        assertFalse(PropertyName.equals("list[*].nested[*]", 16, 1, "[*]", 0, 3));

        // the bracket may also fall outside the region of the name being matched, which leaves a star that a
        // plain character follows. A star only stands for a whole segment, so it does not match a part of one
        assertFalse(PropertyName.equals("list[*].nested[*]", 15, 2, "nested[1]", 0, 9));
        assertFalse(PropertyName.equals("nested[1]", 0, 9, "list[*].nested[*]", 15, 2));

        // the same regions, compared as standalone names
        assertFalse(PropertyName.equals(new String("[0]"), new String("0]")));
        assertFalse(PropertyName.equals(new String("[*]"), new String("0]")));
        assertFalse(PropertyName.equals(new String("[0]"), new String("]")));
        assertFalse(PropertyName.equals(new String("[*]"), new String("2]")));
        assertFalse(PropertyName.equals(new String("[*]"), new String("]")));
        assertFalse(PropertyName.equals(new String("*]"), new String("nested[1]")));
    }

    @Test
    void propertyNameEqualsSameInstanceRegions() {
        // the regions are compared, even when both sides are the same instance
        assertTrue(PropertyName.equals("foo.bar.foo", 0, 3, "foo.bar.foo", 8, 3));
        assertTrue(PropertyName.equals("foo.bar.foo", 0, 11, "foo.bar.foo", 0, 11));

        assertFalse(PropertyName.equals("foo.bar.foo", 0, 3, "foo.bar.foo", 4, 3));
        assertFalse(PropertyName.equals("foo.bar.foo", 0, 3, "foo.bar.foo", 0, 7));
        assertFalse(PropertyName.equals("foo.bar.foo", 0, 0, "foo.bar.foo", 0, 1));

        assertTrue(PropertyName.equals("*.foo", 0, 1, "*.foo", 2, 3));
        assertFalse(PropertyName.equals("*.foo", 0, 1, "*.foo", 1, 4));
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

        // a greedy star matches ending segments, but it does not match a star, wherever the star is: neither in the
        // segments that remain nor in the ending segments that the greedy star would match
        assertNotEquals(name("greedy.*"), name(("greedy.*.name")));
        assertNotEquals(name("greedy.*.name"), name(("greedy.*")));
        assertNotEquals(name("greedy.*"), name(("greedy.one.*")));
        assertNotEquals(name("greedy.one.*"), name(("greedy.*")));
        assertNotEquals(name("greedy.*"), name(("greedy.one.two.*")));
        assertNotEquals(name("greedy.*"), name(("greedy.\"a.b\".*")));
        assertNotEquals(name("greedy.\"a.b\".*"), name(("greedy.*")));
        assertNotEquals(name("foo.*"), name(("foo.bar.*")));
        assertNotEquals(name("foo.bar.*"), name(("foo.*")));
        assertNotEquals(name("*"), name(("foo.*")));
        assertNotEquals(name("foo.*"), name(("*")));
        assertNotEquals(name("*"), name(("foo.bar.*")));
        assertNotEquals(name("greedy.*.map.*"), name(("greedy.key.map.one.*")));
        assertNotEquals(name("greedy.key.map.one.*"), name(("greedy.*.map.*")));

        // a star that the other name matches by position is not swallowed, so it still matches
        assertEquals(name("greedy.*"), name(new String("greedy.*")));
        assertEquals(name("greedy.*.map.*"), name(new String("greedy.key.map.*")));
        assertEquals(name("greedy.key.map.*"), name(new String("greedy.*.map.*")));
        assertEquals(name("greedy.*.map.*"), name(new String("greedy.*.map.*")));
    }

    @Test
    void greedyMapRegions() {
        // a greedy star matches the ending segments, no matter where the regions start
        assertTrue(PropertyName.equals("x.greedy.*", 2, 8, "greedy.one.two", 0, 14));
        assertTrue(PropertyName.equals("greedy.*", 0, 8, "x.greedy.one.two", 2, 14));
        assertTrue(PropertyName.equals("x.greedy.*", 2, 8, "x.greedy.one.two", 2, 14));
        assertTrue(PropertyName.equals("p.greedy.*.map.*", 2, 14, "greedy.key.map.one.two", 0, 22));
        assertTrue(PropertyName.equals("p.g.*.m.*[*]", 2, 10, "g.k.m.one.two[0]", 0, 16));

        assertFalse(PropertyName.equals("p.greedy.*.map.*", 2, 14, "greedy.one.two.map.key", 0, 22));
        assertFalse(PropertyName.equals("p.greedy.*", 2, 8, "greedy.*.name", 0, 13));
        assertFalse(PropertyName.equals("p.*", 2, 1, "", 0, 0));

        // the ending segments may require the quotes
        assertTrue(PropertyName.equals("x.greedy.*", 2, 8, "greedy.one.\"c.d\"", 0, 16));
        assertTrue(PropertyName.equals("greedy.*", 0, 8, "x.greedy.one.\"c.d\"", 2, 16));
        assertTrue(PropertyName.equals("x.foo.*", 2, 5, "z.foo.\"a.b\".two", 2, 13));
        assertTrue(PropertyName.equals("x.foo.*", 2, 5, "z.foo.\"a.b\".\"c.d\"", 2, 15));
        assertTrue(PropertyName.equals("p.greedy.*.map.*", 2, 14, "greedy.key.map.one.\"c.d\"", 0, 24));

        assertFalse(PropertyName.equals("p.greedy.*.map.*", 2, 14, "greedy.one.two.map.\"c.d\"", 0, 24));
    }

    @Test
    void quotedSegment() {
        // single-segment quote equals the unquoted form
        assertEquals(name("foo.\"bar\""), name("foo.bar"));
        assertEquals(name("foo.bar"), name("foo.\"bar\""));
        assertEquals(name("\"foo\""), name("foo"));
        assertEquals(name("foo"), name("\"foo\""));
        assertEquals(name("foo.\"bar\".baz"), name("foo.bar.baz"));
        assertEquals(name("foo.bar.baz"), name("foo.\"bar\".baz"));

        // multi-segment quote does NOT equal the dot-split form
        assertNotEquals(name("foo.\"bar.baz\""), name("foo.bar.baz"));
        assertNotEquals(name("foo.bar.baz"), name("foo.\"bar.baz\""));
        assertNotEquals(name("foo.\"bar.baz\".qux"), name("foo.bar.baz.qux"));
        assertNotEquals(name("foo.bar.baz.qux"), name("foo.\"bar.baz\".qux"));

        // wildcard still matches quoted segments regardless of dots inside
        assertEquals(name("foo.*"), name("foo.\"bar\""));
        assertEquals(name("foo.*"), name("foo.\"bar.baz\""));
        assertEquals(name("foo.\"bar\""), name("foo.*"));
        assertEquals(name("foo.\"bar.baz\""), name("foo.*"));
    }

    @Test
    void quotedSegmentEqualsItself() {
        assertEquals(name(new String("\"foo\"")), name(new String("\"foo\"")));
        assertEquals(name(new String("foo.\"bar\"")), name(new String("foo.\"bar\"")));
        assertEquals(name(new String("foo.\"bar\".baz")), name(new String("foo.\"bar\".baz")));
        assertEquals(name(new String("foo.\"bar\"[0]")), name(new String("foo.\"bar\"[0]")));
        assertEquals(name(new String("foo.\"bar\".\"baz\"")), name(new String("foo.\"bar\".\"baz\"")));

        assertEquals(name(new String("\"foo.bar\"")), name(new String("\"foo.bar\"")));
        assertEquals(name(new String("foo.\"bar.baz\"")), name(new String("foo.\"bar.baz\"")));
        assertEquals(name(new String("foo.\"bar.baz\".qux")), name(new String("foo.\"bar.baz\".qux")));
        assertEquals(name(new String("foo.\"bar.baz\"[0]")), name(new String("foo.\"bar.baz\"[0]")));
        assertEquals(name(new String("foo.\"bar.baz\".\"qux.quz\"")), name(new String("foo.\"bar.baz\".\"qux.quz\"")));

        assertNotEquals(name(new String("foo.\"bar\"")), name(new String("foo.\"baz\"")));
        assertNotEquals(name(new String("foo.\"bar.baz\"")), name(new String("foo.\"bar.qux\"")));
    }

    @Test
    void quotedSegmentMixed() {
        // a quoted single segment is transparent, even when preceded by an opaque quoted segment
        assertEquals(name("foo.\"bar.baz\".\"qux\""), name("foo.\"bar.baz\".qux"));
        assertEquals(name("foo.\"bar.baz\".qux"), name("foo.\"bar.baz\".\"qux\""));
        assertEquals(name("\"foo\".\"bar.baz\".\"qux\""), name("foo.\"bar.baz\".qux"));
        assertEquals(name("foo.\"bar.baz\".qux"), name("\"foo\".\"bar.baz\".\"qux\""));
        assertEquals(name("foo.\"bar\"[0]"), name("foo.bar[0]"));
        assertEquals(name("foo.bar[0]"), name("foo.\"bar\"[0]"));

        assertNotEquals(name("foo.\"bar.baz\".\"qux\""), name("foo.\"bar.qux\".qux"));
        assertNotEquals(name("foo.\"bar.baz\".qux"), name("foo.bar.baz.\"qux\""));

        // wildcards still match either form
        assertEquals(name("foo.*.qux"), name("foo.\"bar.baz\".\"qux\""));
        assertEquals(name("foo.\"bar.baz\".\"qux\""), name("foo.*.qux"));
        assertEquals(name("foo.\"bar.baz\".*"), name("foo.\"bar.baz\".qux"));
        assertEquals(name("foo.\"bar.baz\".qux"), name("foo.\"bar.baz\".*"));

        // a greedy star matches the ending segments, quoted or not
        assertEquals(name("foo.*"), name("foo.one.\"two\""));
        assertEquals(name("foo.one.\"two\""), name("foo.*"));
        assertEquals(name("foo.*"), name("foo.\"one\".\"two\""));
        assertEquals(name("greedy.*.map.*"), name("greedy.key.map.one.\"two\""));
        assertEquals(name("greedy.key.map.one.\"two\""), name("greedy.*.map.*"));

        // a greedy star also matches ending segments that require the quotes
        assertEquals(name("foo.*"), name("foo.one.\"c.d\""));
        assertEquals(name("foo.one.\"c.d\""), name("foo.*"));
        assertEquals(name("foo.*"), name("foo.\"a.b\".two"));
        assertEquals(name("foo.\"a.b\".two"), name("foo.*"));
        assertEquals(name("foo.*"), name("foo.\"a.b\".\"c.d\""));
        assertEquals(name("foo.\"a.b\".\"c.d\""), name("foo.*"));
        assertEquals(name("foo.*"), name("foo.one.two.\"c.d\""));
        assertEquals(name("foo.one.two.\"c.d\""), name("foo.*"));
        assertEquals(name("*"), name("one.\"c.d\""));
        assertEquals(name("one.\"c.d\""), name("*"));
        assertEquals(name("greedy.*.map.*"), name("greedy.key.map.one.\"c.d\""));
        assertEquals(name("greedy.key.map.one.\"c.d\""), name("greedy.*.map.*"));
        assertEquals(name("greedy.*.map.*"), name("greedy.\"a.b\".map.one.\"c.d\""));
        assertEquals(name("greedy.\"a.b\".map.one.\"c.d\""), name("greedy.*.map.*"));
        assertEquals(name("foo.\"a.b\".*"), name("foo.\"a.b\".one.\"c.d\""));
        assertEquals(name("foo.\"a.b\".one.\"c.d\""), name("foo.\"a.b\".*"));

        // the segments before the greedy star must still match
        assertNotEquals(name("greedy.*.map.*"), name("greedy.one.two.map.\"c.d\""));
        assertNotEquals(name("greedy.one.two.map.\"c.d\""), name("greedy.*.map.*"));
        assertNotEquals(name("greedy.*.map.*"), name("greedy.one.\"a.b\".map.key"));
        assertNotEquals(name("greedy.one.\"a.b\".map.key"), name("greedy.*.map.*"));
        assertNotEquals(name("greedy.*"), name("greedy.*.\"a.b\""));

        // equal names hash the same
        assertEquals(name("foo.*").hashCode(), name("foo.one.\"c.d\"").hashCode());
        assertEquals(name("foo.*").hashCode(), name("foo.\"a.b\".\"c.d\"").hashCode());
        assertEquals(name("greedy.*.map.*").hashCode(), name("greedy.key.map.one.\"c.d\"").hashCode());

        // an empty segment is empty, quoted or not, and a star does not match it
        assertNotEquals(name("*"), name("\"\""));
        assertNotEquals(name("\"\""), name("*"));
        assertNotEquals(name("*.foo"), name("\"\".foo"));
        assertNotEquals(name("\"\".foo"), name("*.foo"));
        assertEquals(name("\"\".foo"), name(new String(".foo")));
    }

    @Test
    void quotedSegmentIndexed() {
        // a quote around a bracket is required, so it is not transparent
        assertNotEquals(name("foo.\"bar[0]\""), name("foo.bar[0]"));
        assertNotEquals(name("foo.bar[0]"), name("foo.\"bar[0]\""));
        assertNotEquals(name("foo.\"bar]\""), name("foo.bar]"));
        assertNotEquals(name("foo.\"bar[\""), name("foo.bar["));

        // a quote outside the brackets is still transparent
        assertEquals(name("foo.\"bar\"[0]"), name("foo.bar[0]"));
        assertEquals(name("foo.\"bar\"[*]"), name("foo.bar[0]"));

        // a wildcard matches a quoted segment, brackets or not
        assertEquals(name("foo.*"), name("foo.\"bar[0]\""));
        assertEquals(name("foo.\"bar[0]\""), name("foo.*"));
        assertEquals(name("foo.*"), name("foo.\"a.b[0]\""));

        // equal names hash the same
        assertNotEquals(name("foo.\"bar[0]\"").hashCode(), name("foo.bar[0]").hashCode());
        assertEquals(name("foo.\"bar\"[0]").hashCode(), name("foo.bar[0]").hashCode());
    }

    @Test
    void quotedSegmentIndexedWildcard() {
        // the brackets are inside the quotes, so they do not delimit an index and the star is not an index
        // wildcard, which makes the quoted segment a plain name that only matches the very same name
        assertNotEquals(name("foo.\"bar[*]\""), name("foo.\"bar[0]\""));
        assertNotEquals(name("foo.\"bar[0]\""), name("foo.\"bar[*]\""));
        assertNotEquals(name("\"bar[*]\""), name("\"bar[0]\""));
        assertNotEquals(name("\"bar[0]\""), name("\"bar[*]\""));
        assertNotEquals(name("\"[*]\""), name("\"[0]\""));
        assertNotEquals(name("\"[0]\""), name("\"[*]\""));
        assertNotEquals(name("foo.\"bar[*]\".baz"), name("foo.\"bar[0]\".baz"));
        assertNotEquals(name("foo.\"bar[0]\".baz"), name("foo.\"bar[*]\".baz"));
        assertNotEquals(name("foo.\"a.b[*]\""), name("foo.\"a.b[0]\""));
        assertNotEquals(name("foo.\"a.b[0]\""), name("foo.\"a.b[*]\""));
        assertNotEquals(name("foo.\"bar[*]\""), name("foo.\"bar[99]\""));
        assertNotEquals(name("foo.\"bar[*]\""), name("foo.\"bar[x]\""));

        // a quoted index does not match an unquoted index either, in either direction
        assertNotEquals(name("foo.\"bar[*]\""), name("foo.bar[0]"));
        assertNotEquals(name("foo.bar[0]"), name("foo.\"bar[*]\""));
        assertNotEquals(name("foo.\"bar[*]\""), name("foo.bar[*]"));
        assertNotEquals(name("foo.bar[*]"), name("foo.\"bar[*]\""));
        assertNotEquals(name("foo.\"bar[0]\""), name("foo.bar[*]"));
        assertNotEquals(name("foo.bar[*]"), name("foo.\"bar[0]\""));
        assertNotEquals(name("foo.\"bar[*]\""), name("foo.*[*]"));
        assertNotEquals(name("foo.*[*]"), name("foo.\"bar[*]\""));

        // a quoted index still equals the very same quoted index
        assertEquals(name(new String("foo.\"bar[*]\"")), name(new String("foo.\"bar[*]\"")));
        assertEquals(name(new String("foo.\"[*]\"")), name(new String("foo.\"[*]\"")));
        assertEquals(name(new String("foo.\"a.b[*]\"")), name(new String("foo.\"a.b[*]\"")));
        assertEquals(name(new String("foo.\"bar[*]\".baz")), name(new String("foo.\"bar[*]\".baz")));
        assertEquals(name(new String("\"bar[*]\"")), name(new String("\"bar[*]\"")));

        // a quote the segment does not require is still transparent, so the index is still an index
        assertEquals(name("foo.\"bar\"[*]"), name("foo.bar[0]"));
        assertEquals(name("foo.bar[0]"), name("foo.\"bar\"[*]"));
        assertEquals(name("foo.\"bar\"[*]"), name("foo.\"bar\"[0]"));
        assertEquals(name("foo.\"bar\"[0]"), name("foo.\"bar\"[*]"));

        // a wildcard segment still matches the whole quoted segment, index wildcard or not
        assertEquals(name("foo.*"), name("foo.\"bar[*]\""));
        assertEquals(name("foo.\"bar[*]\""), name("foo.*"));
        assertEquals(name("*"), name("foo.\"bar[*]\""));

        // a literal bracket inside the quotes equals itself, even after an unquoted index
        assertEquals(name(new String("bar[0].\"bar]\"")), name(new String("bar[0].\"bar]\"")));
        assertEquals(name("bar[*].\"bar]\""), name("bar[0].\"bar]\""));
        assertEquals(name("bar[0].\"bar]\""), name("bar[*].\"bar]\""));

        // equal names hash the same
        assertEquals(name("foo.*").hashCode(), name("foo.\"bar[*]\"").hashCode());
        assertEquals(name("bar[*].\"bar]\"").hashCode(), name("bar[0].\"bar]\"").hashCode());
        assertEquals(name("foo.\"bar\"[*]").hashCode(), name("foo.bar[0]").hashCode());
    }

    @Test
    void quotedSegmentStar() {
        // a star inside a quote the segment requires is a plain character, not a wildcard
        assertNotEquals(name("foo.\"a.*\""), name("foo.\"a.b\""));
        assertNotEquals(name("foo.\"a.b\""), name("foo.\"a.*\""));
        assertNotEquals(name("foo.\"a.*\""), name("foo.\"a.b*c\""));
        assertNotEquals(name("foo.\"a.b*c\""), name("foo.\"a.*\""));
        assertNotEquals(name("\"*.b\""), name("\"a.b\""));
        assertNotEquals(name("\"a.b\""), name("\"*.b\""));
        assertNotEquals(name("\"[*]\""), name("\"[9]\""));

        // which makes it equal to the very same name
        assertEquals(name(new String("foo.\"a.*\"")), name(new String("foo.\"a.*\"")));
        assertEquals(name(new String("foo.\"a.b*c\"")), name(new String("foo.\"a.b*c\"")));
        assertEquals(name(new String("\"a.b*\"")), name(new String("\"a.b*\"")));
        assertEquals(name(new String("\"*.b\"")), name(new String("\"*.b\"")));

        // a quote the segment does not require carries no meaning, so the star is still a wildcard
        assertEquals(name("foo.\"*\""), name("foo.bar"));
        assertEquals(name("foo.bar"), name("foo.\"*\""));
        assertEquals(name("\"*\""), name("foo"));
        assertEquals(name("foo.\"*\""), name("foo.*"));
        assertEquals(name("foo.\"*\".baz"), name("foo.bar.baz"));

        // a wildcard segment still matches a quoted segment that holds a star
        assertEquals(name("foo.*"), name("foo.\"a.*\""));
        assertEquals(name("foo.\"a.*\""), name("foo.*"));
        assertEquals(name("foo.*"), name("foo.\"a.b*c\""));

        // a greedy star swallows ending segments that hold a star, because that star is a plain character
        assertEquals(name("greedy.*"), name("greedy.one.\"a.*\""));
        assertEquals(name("greedy.one.\"a.*\""), name("greedy.*"));
        assertEquals(name("greedy.*"), name("greedy.\"a.*\".one"));
        assertEquals(name("greedy.*.map.*"), name("greedy.k.map.one.\"a.*\""));

        // but it still refuses to swallow a wildcard, including one that a redundant quote holds
        assertNotEquals(name("greedy.*"), name("greedy.one.*"));
        assertNotEquals(name("greedy.*"), name("greedy.\"a.b\".*"));
        assertNotEquals(name("greedy.*"), name("greedy.one.\"*\""));

        // equal names hash the same
        assertEquals(name("foo.\"*\"").hashCode(), name("foo.bar").hashCode());
        assertEquals(name("foo.*").hashCode(), name("foo.\"a.*\"").hashCode());
        assertEquals(name("greedy.*").hashCode(), name("greedy.one.\"a.*\"").hashCode());
    }

    @Test
    void unbalancedQuote() {
        // a quote that no other quote closes opens a run that swallows every dot that follows it, so the tail is a
        // single opaque segment and the quote itself is a plain character that only another quote matches
        assertNotEquals(name("foo"), name("\"foo"));
        assertNotEquals(name("foo"), name("foo\""));
        assertNotEquals(name("foo.bar"), name("\"foo.bar"));
        assertNotEquals(name("foo.bar"), name("foo\".bar"));
        assertNotEquals(name("foo.*"), name("foo\".*"));
        assertNotEquals(name("b\".*"), name("b\".."));

        // and a wildcard stands for that one segment, whatever it holds
        assertEquals(name("*"), name("b\".c"));
        assertEquals(name("foo.*"), name("foo.b\".c"));

        // segmentEnd and the quoting rules have to agree on where a segment ends, or matching one name against the
        // other splits them into a different number of segments and equals stops being symmetric
        String[] segments = { "foo", "*", "\"foo\"", "bar[0]", "", "\"foo", "foo\"", "b\"" };
        List<String> names = new ArrayList<>();
        for (String one : segments) {
            names.add(one);
            for (String two : segments) {
                names.add(one + "." + two);
                for (String three : segments) {
                    names.add(one + "." + two + "." + three);
                }
            }
        }
        for (String left : names) {
            for (String right : names) {
                assertEquals(PropertyName.equals(left, right), PropertyName.equals(right, left),
                        "asymmetric: " + left + " / " + right);
            }
        }
    }
}
