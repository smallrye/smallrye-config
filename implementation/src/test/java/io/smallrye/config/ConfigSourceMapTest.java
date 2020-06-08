/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

/**
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ConfigSourceMapTest {
    @SuppressWarnings("serial")
    private static final Map<String, String> MANY_MAP = new HashMap<String, String>() {
        {
            put("key-one", "12345");
            put("null-valued-key", null);
            put("test", "bar");
            put("fruit", "banana");
        }
    };
    private static final ConfigSource MANY_CONF_SRC = new PropertiesConfigSource(MANY_MAP, "test", 100);
    private static final Map<String, String> ONE_MAP = Collections.singletonMap("test", "foo");
    private static final ConfigSource ONE_CONF_SRC = new PropertiesConfigSource(ONE_MAP, "test", 100);

    @Test
    public void testClear() {
        try {
            new ConfigSourceMap(ONE_CONF_SRC).clear();
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testCompute() {
        try {
            new ConfigSourceMap(ONE_CONF_SRC).compute("piano", (k, v) -> "player");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testComputeIfAbsent() {
        try {
            //noinspection ExcessiveLambdaUsage
            new ConfigSourceMap(ONE_CONF_SRC).computeIfAbsent("piano", k -> "player");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testComputeIfPresent() {
        try {
            new ConfigSourceMap(ONE_CONF_SRC).computeIfPresent("test", (k, v) -> "bar");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testContainsKey() {
        final ConfigSourceMap csm = new ConfigSourceMap(MANY_CONF_SRC);
        assertTrue(csm.containsKey("test"));
        assertTrue(csm.containsKey("null-valued-key"));
        assertFalse(csm.containsKey("missing-key"));
    }

    @Test
    public void testContainsValue() {
        final ConfigSourceMap csm = new ConfigSourceMap(MANY_CONF_SRC);
        assertTrue(csm.containsValue("12345"));
        assertFalse(csm.containsValue("apple"));
        assertTrue(csm.containsValue(null));
    }

    @Test
    public void testEquals() {
        assertEquals(MANY_MAP, new ConfigSourceMap(MANY_CONF_SRC));
    }

    @Test
    public void testForEach() {
        final Set<String> need = new HashSet<>(MANY_MAP.keySet());
        final ConfigSourceMap csm = new ConfigSourceMap(MANY_CONF_SRC);
        csm.forEach((k, v) -> {
            assertEquals(MANY_MAP.get(k), v);
            assertTrue(need.remove(k));
        });
        assertTrue(need.isEmpty(), "keys left in set");
    }

    @Test
    public void testGet() {
        final ConfigSourceMap csm = new ConfigSourceMap(MANY_CONF_SRC);
        assertEquals("bar", csm.get("test"));
        assertNull(csm.get("null-valued-key"));
        assertNull(csm.get("nope"));
    }

    @Test
    public void testGetOrDefault() {
        final ConfigSourceMap csm = new ConfigSourceMap(MANY_CONF_SRC);
        assertEquals("bar", csm.getOrDefault("test", "foo"));
        assertNull(csm.getOrDefault("null-valued-key", "oops"));
        assertEquals("yes", csm.getOrDefault("nope", "yes"));
    }

    @Test
    public void testHashCode() {
        assertEquals(MANY_MAP.hashCode(), new ConfigSourceMap(MANY_CONF_SRC).hashCode());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(new ConfigSourceMap(new PropertiesConfigSource(Collections.emptyMap(), "test", 100)).isEmpty());
        assertFalse(new ConfigSourceMap(ONE_CONF_SRC).isEmpty());
        assertFalse(new ConfigSourceMap(MANY_CONF_SRC).isEmpty());
    }

    @Test
    public void testKeySet() {
        assertEquals(ONE_CONF_SRC.getPropertyNames(), new ConfigSourceMap(ONE_CONF_SRC).keySet());
        assertEquals(MANY_CONF_SRC.getPropertyNames(), new ConfigSourceMap(MANY_CONF_SRC).keySet());
    }

    @Test
    public void testMerge() {
        try {
            new ConfigSourceMap(MANY_CONF_SRC).merge("test", "bar", (k, v) -> "oops");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testPut() {
        try {
            new ConfigSourceMap(ONE_CONF_SRC).put("bees", "bzzzz");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testPutAll() {
        try {
            new ConfigSourceMap(ONE_CONF_SRC).putAll(Collections.singletonMap("bees", "bzzzz"));
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testPutIfAbsent() {
        try {
            new ConfigSourceMap(ONE_CONF_SRC).putIfAbsent("bees", "bzzzz");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
        new ConfigSourceMap(ONE_CONF_SRC).putIfAbsent("test", "not absent");
    }

    @Test
    public void testRemove1() {
        try {
            new ConfigSourceMap(MANY_CONF_SRC).remove("test");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testRemove2() {
        try {
            new ConfigSourceMap(MANY_CONF_SRC).remove("test");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testReplace2() {
        try {
            new ConfigSourceMap(MANY_CONF_SRC).replace("test", "oops");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testReplace3() {
        try {
            new ConfigSourceMap(MANY_CONF_SRC).replace("test", "bar", "oops");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            // false or exception are OK
            assertFalse(new ConfigSourceMap(MANY_CONF_SRC).replace("test", "nope", "oops"));
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testReplaceAll() {
        try {
            new ConfigSourceMap(MANY_CONF_SRC).replaceAll((k, v) -> "oops");
            fail("Expected exception");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testSize() {
        assertEquals(MANY_MAP.size(), new ConfigSourceMap(MANY_CONF_SRC).size());
        assertEquals(ONE_MAP.size(), new ConfigSourceMap(ONE_CONF_SRC).size());
    }
}
