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
        assertThrows(IllegalArgumentException.class, () -> StringUtil.skewer(""));
        assertThrows(IllegalArgumentException.class, () -> StringUtil.skewer("", '.'));

        assertEquals("my-property", StringUtil.skewer("myProperty"));
        assertEquals("my.property", StringUtil.skewer("myProperty", '.'));

        assertEquals("a", StringUtil.skewer("a"));
        assertEquals("a", StringUtil.skewer("a", '.'));

        assertEquals("my-property-abc", StringUtil.skewer("myPropertyABC"));
        assertEquals("my.property.abc", StringUtil.skewer("myPropertyABC", '.'));

        assertEquals("my-property-abc-abc", StringUtil.skewer("myPropertyABCabc"));
        assertEquals("my.property.abc.abc", StringUtil.skewer("myPropertyABCabc", '.'));
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
}
