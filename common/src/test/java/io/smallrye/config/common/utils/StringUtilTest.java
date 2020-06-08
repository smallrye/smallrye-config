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

import org.junit.jupiter.api.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class StringUtilTest {
    @Test
    public void testSplit() {
        String text = "large:cheese\\,mushroom,medium:chicken,small:pepperoni";

        String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("large:cheese,mushroom", split[0]);
        assertEquals("medium:chicken", split[1]);
        assertEquals("small:pepperoni", split[2]);
    }

    @Test
    public void testTrailingSegmentsIgnored() {
        String text = "foo,bar,baz,,,,,";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo", split[0]);
        assertEquals("bar", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    public void testLeadingSegmentsIgnored() {
        String text = ",,,,,,,,foo,bar,baz";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo", split[0]);
        assertEquals("bar", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    public void testMidSegmentsIgnored() {
        String text = "foo,,,,bar,,,baz";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo", split[0]);
        assertEquals("bar", split[1]);
        assertEquals("baz", split[2]);
    }

    @Test
    public void testAllEmptySegments() {
        String text = ",,,,,";
        final String[] split = StringUtil.split(text);
        assertEquals(0, split.length);
    }

    @Test
    public void testTwoEmptySegments() {
        String text = ",";
        final String[] split = StringUtil.split(text);
        assertEquals(0, split.length);
    }

    @Test
    public void testEmptyString() {
        assertEquals(0, StringUtil.split("").length);
    }

    @Test
    public void testIffyEscapingSituations() {
        String text = "foo\\\\,bar\\x,,,baz";
        final String[] split = StringUtil.split(text);
        assertEquals(3, split.length);
        assertEquals("foo\\", split[0]);
        assertEquals("barx", split[1]);
        assertEquals("baz", split[2]);
    }
}
