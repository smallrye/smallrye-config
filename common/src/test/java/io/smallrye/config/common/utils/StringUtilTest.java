/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class StringUtilTest {
    @Test
    void split() {
        String text = "large:cheese\\,mushroom,medium:chicken,small:pepperoni";

        String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("large:cheese,mushroom", split[0]);
        assertEquals("medium:chicken", split[1]);
        assertEquals("small:pepperoni", split[2]);
    }

    @Test
    void trailingSegmentsIgnored() {
        String text = "foo,bar,baz,,,,,";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo", split[0]);
        assertEquals("bar", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    void leadingSegmentsIgnored() {
        String text = ",,,,,,,,foo,bar,baz";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo", split[0]);
        assertEquals("bar", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    void midSegmentsIgnored() {
        String text = "foo,,,,bar,,,baz";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo", split[0]);
        assertEquals("bar", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    void allEmptySegments() {
        String text = ",,,,,";
        final String[] split = StringUtil.split(text);
        assertEquals(0, split.length);
    }

    @Test
    void twoEmptySegments() {
        String text = ",";
        final String[] split = StringUtil.split(text);
        assertEquals(0, split.length);
    }

    @Test
    void emptyString() {
        assertEquals(0, StringUtil.split("").length);
    }

    @Test
    void escapingSituations() {
        String text = "foo\\\\,bar\\x,,,baz";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo\\", split[0]);
        assertEquals("barx", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    void skewer() {
        assertEquals("sigusr1", StringUtil.skewer("sigusr1"));

        assertThrows(IllegalArgumentException.class, () -> StringUtil.skewer(""));
        assertThrows(IllegalArgumentException.class, () -> StringUtil.skewer("", '.'));

        assertEquals("my-property", StringUtil.skewer("myProperty"));
        assertEquals("my.property", StringUtil.skewer("myProperty", '.'));

        assertEquals("-", StringUtil.skewer("-"));
        assertEquals("_", StringUtil.skewer("_"));
        assertEquals("a", StringUtil.skewer("a"));
        assertEquals("a", StringUtil.skewer("A"));
        assertEquals("a", StringUtil.skewer("a", '.'));
        assertEquals("a-b", StringUtil.skewer("a-b"));
        assertEquals("_a", StringUtil.skewer("_a"));
        assertEquals("_a-b", StringUtil.skewer("_a_b"));

        assertEquals("my-property-abc", StringUtil.skewer("myPropertyABC"));
        assertEquals("my.property.abc", StringUtil.skewer("myPropertyABC", '.'));

        assertEquals("my-property-ab-cabc", StringUtil.skewer("myPropertyABCabc"));
        assertEquals("my.property.ab.cabc", StringUtil.skewer("myPropertyABCabc", '.'));

        assertEquals("is-same-rm-override", StringUtil.skewer("isSameRMOverride"));
        assertEquals("http-client-http-conduit-factory", StringUtil.skewer("HttpClientHTTPConduitFactory"));
        assertEquals("url-connection-http-conduit-factory", StringUtil.skewer("URLConnectionHTTPConduitFactory"));
        assertEquals("abc-default", StringUtil.skewer("ABCDefault"));
        assertEquals("abc", StringUtil.skewer("ABC"));

        assertEquals("discard", StringUtil.skewer("discard"));
        assertEquals("a-b", StringUtil.skewer("A_B"));
        assertEquals("read-uncommitted", StringUtil.skewer("READ_UNCOMMITTED"));
        assertEquals("_read-uncommitted", StringUtil.skewer("_READ_UNCOMMITTED"));
        assertEquals("read-uncommitted", StringUtil.skewer("READ__UNCOMMITTED"));
        assertEquals("_read-uncommitted", StringUtil.skewer("_READ__UNCOMMITTED"));
        assertEquals("sigusr1", StringUtil.skewer("SIGUSR1"));
        assertEquals("sigusr1", StringUtil.skewer("sigusr1"));
        assertEquals("trend-breaker", StringUtil.skewer("TrendBreaker"));
        assertEquals("making-life-difficult", StringUtil.skewer("MAKING_LifeDifficult"));
        assertEquals("making-life-difficult", StringUtil.skewer("makingLifeDifficult"));
    }

    @Test
    void replaceNonAlphanumericByUnderscores() {
        assertEquals("TEST_LANGUAGE__DE_ETR__",
                StringUtil.replaceNonAlphanumericByUnderscores("test.language.\"de.etr\"".toUpperCase()));
    }

    @Test
    void replaceNonAlphanumericByUnderscoresWithStringBuilder() {
        StringBuilder builder = new StringBuilder();
        builder.setLength(0);
        assertEquals("FOO_BAR", StringUtil.replaceNonAlphanumericByUnderscores("foo.bar", builder).toUpperCase());
        builder.setLength(0);
        assertEquals("FOO_BAR_BAZ", StringUtil.replaceNonAlphanumericByUnderscores("foo.bar.baz", builder).toUpperCase());
        builder.setLength(0);
        assertEquals("FOO", StringUtil.replaceNonAlphanumericByUnderscores("foo", builder).toUpperCase());
        builder.setLength(0);
        assertEquals("TEST_LANGUAGE__DE_ETR__",
                StringUtil.replaceNonAlphanumericByUnderscores("test.language.\"de.etr\"".toUpperCase()));
    }

    @Test
    void toLowerCaseAndDotted() {
        assertEquals("", StringUtil.toLowerCaseAndDotted(""));
        assertEquals("f", StringUtil.toLowerCaseAndDotted("F"));
        assertEquals("foo", StringUtil.toLowerCaseAndDotted("FOO"));
        assertEquals("foo.bar", StringUtil.toLowerCaseAndDotted("FOO_BAR"));
        assertEquals("foo.bar.baz", StringUtil.toLowerCaseAndDotted("FOO_BAR_BAZ"));
        assertEquals("foo.bar.baz[20]", StringUtil.toLowerCaseAndDotted("FOO_BAR_BAZ_20_"));
        assertEquals("foo.bar.\"baz\".i[20].e", StringUtil.toLowerCaseAndDotted("FOO_BAR__BAZ__I_20__E"));
        assertEquals("test.language.\"de.etr\"", StringUtil.toLowerCaseAndDotted("TEST_LANGUAGE__DE_ETR__"));
    }

    @Test
    void isNumeric() {
        assertTrue(StringUtil.isNumeric("0"));
        assertFalse(StringUtil.isNumeric("false"));
        assertTrue(StringUtil.isNumeric("foo[0]", 4, 5));
        assertTrue(StringUtil.isNumeric(new StringBuilder("foo[0]"), 4, 5));
    }

    @Test
    void unquoted() {
        assertEquals("", StringUtil.unquoted(""));
        assertEquals("", StringUtil.unquoted("\"\""));
        assertEquals("a", StringUtil.unquoted("a"));
        assertEquals("unquoted", StringUtil.unquoted("\"unquoted\""));
        assertEquals("unquoted", StringUtil.unquoted("my.\"unquoted\"", 3, 13));
        assertEquals("unquoted", StringUtil.unquoted("my.unquoted", 3, 11));
    }

    @Test
    void unIndexed() {
        assertEquals("", StringUtil.unindexed(""));
        assertEquals("[]", StringUtil.unindexed("[]"));
        assertEquals("", StringUtil.unindexed("[0]"));
        assertEquals("", StringUtil.unindexed("[999]"));
        assertEquals("[abc]", StringUtil.unindexed("[abc]"));
        assertEquals("my.prop", StringUtil.unindexed("my.prop[0]"));
        assertEquals("my.prop[]", StringUtil.unindexed("my.prop[]"));
        assertEquals("my.prop[", StringUtil.unindexed("my.prop["));
        assertEquals("my.prop]", StringUtil.unindexed("my.prop]"));
    }
}
