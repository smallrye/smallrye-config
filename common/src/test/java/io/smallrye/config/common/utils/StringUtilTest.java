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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        assertEquals("", StringUtil.skewer(""));
        assertEquals(".", StringUtil.skewer("."));

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

        assertEquals("foo.bar", StringUtil.skewer("foo.bar"));
        assertEquals("foo.bar-baz", StringUtil.skewer("foo.barBaz"));
        assertEquals("foo.bar-baz[0]", StringUtil.skewer("foo.barBaz[0]"));
        assertEquals("foo.bar-baz[*]", StringUtil.skewer("foo.barBaz[*]"));

        assertEquals("across-b2b", StringUtil.skewer("acrossB2b"));
        assertEquals("across-b22b", StringUtil.skewer("acrossB22b"));
        assertEquals("across-b22b", StringUtil.skewer("acrossB22B"));
        assertEquals("across-b22bb", StringUtil.skewer("acrossB22BB"));
        assertEquals("across-b22b-bb", StringUtil.skewer("acrossB22BBb"));
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
        assertEquals("foo.bar.baz2", StringUtil.toLowerCaseAndDotted("FOO_BAR_BAZ2"));
        assertEquals("foo.bar.2baz", StringUtil.toLowerCaseAndDotted("FOO_BAR_2BAZ"));
        assertEquals("foo.bar.baz20", StringUtil.toLowerCaseAndDotted("FOO_BAR_BAZ20"));
        assertEquals("foo.bar.20baz", StringUtil.toLowerCaseAndDotted("FOO_BAR_20BAZ"));
        assertEquals("foo.bar.baz[20]", StringUtil.toLowerCaseAndDotted("FOO_BAR_BAZ_20_"));
        assertEquals("foo.bar.\"baz\".i[20].e", StringUtil.toLowerCaseAndDotted("FOO_BAR__BAZ__I_20__E"));
        assertEquals("test.language.\"de.etr\"", StringUtil.toLowerCaseAndDotted("TEST_LANGUAGE__DE_ETR__"));
        assertEquals("%foo.bar", StringUtil.toLowerCaseAndDotted("_FOO_BAR"));
        assertEquals(".\"foo.bar", StringUtil.toLowerCaseAndDotted("__FOO_BAR"));
        assertNotNull(StringUtil.toLowerCaseAndDotted("00_PYTHON_2_7"));

        assertEquals("foo.bar.3", StringUtil.toLowerCaseAndDotted("FOO_BAR_3"));
        assertEquals("foo.bar[3]", StringUtil.toLowerCaseAndDotted("FOO_BAR_3_"));
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
        assertEquals("my.\"unquoted\"", StringUtil.unquoted("my.\"unquoted\""));
        assertEquals("unquoted", StringUtil.unquoted("my.\"unquoted\"", 3, 13));
        assertEquals("unquoted", StringUtil.unquoted("my.unquoted", 3, 11));
    }

    @Test
    void index() {
        assertEquals(0, StringUtil.index("foo[0]"));
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
