/*
 * Copyright 2019 Red Hat, Inc.
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

package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ConvertersTestCase {

    @Test
    public void testCollection() {
        SmallRyeConfig config = buildConfig("empty.collection", "", "one.collection", ",foo", "two.collection", "foo,,bar,,");
        final Converter<Collection<String>> conv = Converters
                .newCollectionConverter(Converters.getImplicitConverter(String.class), ArrayList::new);
        try {
            config.getValue("empty.collection", conv);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("missing.collection", conv);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        assertEquals(Collections.singletonList("foo"), config.getValue("one.collection", conv));
        assertEquals(Arrays.asList("foo", "bar"), config.getValue("two.collection", conv));
        final Converter<Collection<String>> conv2 = Converters.newEmptyValueConverter(conv, Collections.emptyList());
        assertEquals(new ArrayList<>(), config.getValue("empty.collection", conv2));
        assertEquals(new ArrayList<>(), config.getValue("missing.collection", conv2));
        assertEquals(Collections.singletonList("foo"), config.getValue("one.collection", conv2));
        assertEquals(Arrays.asList("foo", "bar"), config.getValue("two.collection", conv2));
        final Converter<Optional<Collection<String>>> conv3 = Converters.newOptionalConverter(conv);
        assertFalse(config.getValue("empty.collection", conv3).isPresent());
        assertFalse(config.getValue("missing.collection", conv3).isPresent());
        assertEquals(Collections.singletonList("foo"), config.getValue("one.collection", conv3).orElse(null));
        assertEquals(Arrays.asList("foo", "bar"), config.getValue("two.collection", conv3).orElse(null));
    }

    @Test
    public void testArray() {
        SmallRyeConfig config = buildConfig("empty.collection", "", "one.collection", ",foo", "two.collection", "foo,,bar,,");
        final Converter<String[]> conv = Converters
                .newArrayConverter(Converters.getImplicitConverter(String.class), String[].class);
        try {
            config.getValue("empty.collection", conv);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("missing.collection", conv);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        assertArrayEquals(array("foo"), config.getValue("one.collection", conv));
        assertArrayEquals(array("foo", "bar"), config.getValue("two.collection", conv));
        final Converter<String[]> conv2 = Converters.newEmptyValueConverter(conv, new String[0]);
        assertArrayEquals(array(), config.getValue("empty.collection", conv2));
        assertArrayEquals(array(), config.getValue("missing.collection", conv2));
        assertArrayEquals(array("foo"), config.getValue("one.collection", conv2));
        assertArrayEquals(array("foo", "bar"), config.getValue("two.collection", conv2));
        final Converter<Optional<String[]>> conv3 = Converters.newOptionalConverter(conv);
        assertFalse(config.getValue("empty.collection", conv3).isPresent());
        assertFalse(config.getValue("missing.collection", conv3).isPresent());
        assertArrayEquals(array("foo"), config.getValue("one.collection", conv3).orElse(null));
        assertArrayEquals(array("foo", "bar"), config.getValue("two.collection", conv3).orElse(null));
    }

    @Test
    public void testMinimumValue() {
        SmallRyeConfig config = buildConfig("one.plus.one", "2", "animal", "anteater", "when", "1950-01-01");
        final Converter<Integer> intConv = Converters.getImplicitConverter(Integer.class);
        final Converter<Integer> intMin2Conv = Converters.minimumValueConverter(intConv, Integer.valueOf(2), true);
        final Converter<Integer> intMin2ExConv = Converters.minimumValueConverter(intConv, Integer.valueOf(2), false);
        final Converter<Integer> intMin3Conv = Converters.minimumValueConverter(intConv, Integer.valueOf(3), true);
        assertFalse(config.getOptionalValue("missing", intConv).isPresent());
        try {
            config.getValue("one.plus.one", intMin3Conv);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(2, config.getValue("one.plus.one", intMin2Conv).intValue());
        try {
            config.getValue("one.plus.one", intMin2ExConv);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        final Converter<String> strConv = Converters.getImplicitConverter(String.class);
        final Converter<String> strConv1 = Converters.minimumValueConverter(strConv, "aardvark", true);
        final Converter<String> strConv2 = Converters.minimumValueConverter(strConv, "anteater", true);
        final Converter<String> strConv3 = Converters.minimumValueConverter(strConv, "anteater", false);
        final Converter<String> strConv4 = Converters.minimumValueConverter(strConv, "antelope", true);
        assertEquals("anteater", config.getValue("animal", strConv1));
        assertEquals("anteater", config.getValue("animal", strConv2));
        try {
            config.getValue("animal", strConv3);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        try {
            config.getValue("animal", strConv4);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        final Converter<LocalDate> dateConv = Converters.getImplicitConverter(LocalDate.class);
        final Converter<ChronoLocalDate> dateConv1 = Converters.minimumValueConverter(dateConv, LocalDate.of(1950, 1, 1), true);
        final Converter<ChronoLocalDate> dateConv2 = Converters.minimumValueConverter(dateConv, LocalDate.of(1950, 1, 1),
                false);
        final Converter<ChronoLocalDate> dateConv3 = Converters.minimumValueConverter(dateConv, LocalDate.of(1949, 12, 31),
                true);
        final Converter<ChronoLocalDate> dateConv4 = Converters.minimumValueConverter(dateConv, LocalDate.of(1950, 1, 2), true);
        assertEquals(LocalDate.of(1950, 1, 1), config.getValue("when", dateConv1));
        try {
            config.getValue("when", dateConv2);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(LocalDate.of(1950, 1, 1), config.getValue("when", dateConv3));
        try {
            config.getValue("when", dateConv4);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testMaximumValue() {
        SmallRyeConfig config = buildConfig("one.plus.one", "2", "animal", "anteater", "when", "1950-01-01");
        final Converter<Integer> intConv = Converters.getImplicitConverter(Integer.class);
        final Converter<Integer> intMax2Conv = Converters.maximumValueConverter(intConv, Integer.valueOf(2), true);
        final Converter<Integer> intMax2ExConv = Converters.maximumValueConverter(intConv, Integer.valueOf(2), false);
        final Converter<Integer> intMax3Conv = Converters.maximumValueConverter(intConv, Integer.valueOf(3), true);
        assertFalse(config.getOptionalValue("missing", intConv).isPresent());
        assertEquals(2, config.getValue("one.plus.one", intMax3Conv).intValue());
        assertEquals(2, config.getValue("one.plus.one", intMax2Conv).intValue());
        try {
            config.getValue("one.plus.one", intMax2ExConv);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        final Converter<String> strConv = Converters.getImplicitConverter(String.class);
        final Converter<String> strConv1 = Converters.maximumValueConverter(strConv, "aardvark", true);
        final Converter<String> strConv2 = Converters.maximumValueConverter(strConv, "anteater", true);
        final Converter<String> strConv3 = Converters.maximumValueConverter(strConv, "anteater", false);
        final Converter<String> strConv4 = Converters.maximumValueConverter(strConv, "antelope", true);
        try {
            config.getValue("animal", strConv1);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals("anteater", config.getValue("animal", strConv2));
        try {
            config.getValue("animal", strConv3);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals("anteater", config.getValue("animal", strConv4));
        final Converter<LocalDate> dateConv = Converters.getImplicitConverter(LocalDate.class);
        final Converter<ChronoLocalDate> dateConv1 = Converters.maximumValueConverter(dateConv, LocalDate.of(1950, 1, 1), true);
        final Converter<ChronoLocalDate> dateConv2 = Converters.maximumValueConverter(dateConv, LocalDate.of(1950, 1, 1),
                false);
        final Converter<ChronoLocalDate> dateConv3 = Converters.maximumValueConverter(dateConv, LocalDate.of(1949, 12, 31),
                true);
        final Converter<ChronoLocalDate> dateConv4 = Converters.maximumValueConverter(dateConv, LocalDate.of(1950, 1, 2), true);
        assertEquals(LocalDate.of(1950, 1, 1), config.getValue("when", dateConv1));
        try {
            config.getValue("when", dateConv2);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        try {
            config.getValue("when", dateConv3);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(LocalDate.of(1950, 1, 1), config.getValue("when", dateConv4));
    }

    @Test
    public void testEmpty() {
        SmallRyeConfig config = buildConfig("int.key", "1234", "boolean.key", "true", "empty.key", "");
        assertTrue(config.getOptionalValue("int.key", Integer.class).isPresent());
        assertEquals(1234, config.getOptionalValue("int.key", Integer.class).get().intValue());
        assertTrue(config.getValue("int.key", OptionalInt.class).isPresent());
        assertFalse(config.getValue("int.missing.key", OptionalInt.class).isPresent());
        assertFalse(config.getValue("empty.key", OptionalInt.class).isPresent());
        assertEquals(1234, config.getValue("int.key", OptionalInt.class).getAsInt());
        assertFalse(config.getOptionalValue("int.missing.key", Integer.class).isPresent());
        assertFalse(config.getOptionalValue("empty.key", Integer.class).isPresent());

        try {
            config.getValue("empty.key", Integer.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("int.missing.key", Integer.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }

        assertTrue(config.getOptionalValue("int.key", Long.class).isPresent());
        assertEquals(1234, config.getOptionalValue("int.key", Long.class).get().intValue());
        assertTrue(config.getValue("int.key", OptionalLong.class).isPresent());
        assertEquals(1234, config.getValue("int.key", OptionalLong.class).getAsLong());
        assertFalse(config.getValue("int.missing.key", OptionalLong.class).isPresent());
        assertFalse(config.getValue("empty.key", OptionalLong.class).isPresent());
        assertFalse(config.getOptionalValue("int.missing.key", Long.class).isPresent());
        assertFalse(config.getOptionalValue("empty.key", Long.class).isPresent());

        try {
            config.getValue("empty.key", Long.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("int.missing.key", Long.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }

        assertTrue(config.getOptionalValue("int.key", Double.class).isPresent());
        assertEquals(1234, config.getOptionalValue("int.key", Double.class).get().intValue());
        assertTrue(config.getValue("int.key", OptionalDouble.class).isPresent());
        assertEquals(1234, config.getValue("int.key", OptionalDouble.class).getAsDouble(), 0.0);
        assertFalse(config.getValue("int.missing.key", OptionalDouble.class).isPresent());
        assertFalse(config.getValue("empty.key", OptionalDouble.class).isPresent());
        assertFalse(config.getOptionalValue("int.missing.key", Double.class).isPresent());
        assertFalse(config.getOptionalValue("empty.key", Double.class).isPresent());

        try {
            config.getValue("empty.key", Double.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("int.missing.key", Double.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }

        assertTrue(config.getOptionalValue("int.key", Float.class).isPresent());
        assertEquals(1234, config.getOptionalValue("int.key", Float.class).get().intValue());

        try {
            config.getValue("empty.key", Float.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("int.missing.key", Float.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }

        assertTrue(config.getOptionalValue("boolean.key", Boolean.class).isPresent());
        assertTrue(config.getValue("boolean.key", Boolean.class).booleanValue());
        assertFalse(config.getOptionalValue("boolean.missing.key", Boolean.class).isPresent());
        assertFalse(config.getOptionalValue("empty.key", Boolean.class).isPresent());
        try {
            config.getValue("empty.key", Boolean.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }
        try {
            config.getValue("boolean.missing.key", Boolean.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
        }

        assertFalse(config.getOptionalValue("empty.key", String.class).isPresent());

        assertFalse(config.getOptionalValue("empty.key", Character.class).isPresent());
    }

    @Test
    public void testShortValue() {
        final SmallRyeConfig config = buildConfig("simple.short", "2");
        final short expected = 2;
        assertEquals(expected, (short) config.getValue("simple.short", Short.class), "Unexpected value for short config");
        assertEquals(expected, (short) config.getValue("simple.short", Short.TYPE), "Unexpected value for short config");
    }

    @Test
    public void testByte() {
        final SmallRyeConfig config = buildConfig("simple.byte", "2");
        final byte expected = 2;
        assertEquals(expected, (byte) config.getValue("simple.byte", Byte.class), "Unexpected value for byte config");
        assertEquals(expected, (byte) config.getValue("simple.byte", Byte.TYPE), "Unexpected value for byte config");
    }

    @Test
    void byteArray() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("byte.array", Base64.getEncoder().encodeToString("bytes".getBytes())))
                .withConverter(byte[].class, 1000, (Converter<byte[]>) value -> Base64.getDecoder().decode(value.getBytes()))
                .build();
        assertEquals("Ynl0ZXM=", config.getRawValue("byte.array"));
        assertEquals("bytes", new String(config.getValue("byte.array", byte[].class)));
    }

    @Test
    public void testCurrency() {
        final SmallRyeConfig config = buildConfig("simple.currency", "GBP");
        final Currency expected = Currency.getInstance("GBP");
        assertEquals(expected.getCurrencyCode(),
                ((Currency) config.getValue("simple.currency", Currency.class)).getCurrencyCode(),
                "Unexpected value for byte config");
    }

    @Test
    public void testBitSet() {
        final SmallRyeConfig config = buildConfig("simple.bitset", "AA");
        BitSet expected = new BitSet(8);
        expected.set(1);
        expected.set(3);
        expected.set(5);
        expected.set(7);
        assertEquals(expected.toString(), (config.getValue("simple.bitset", BitSet.class)).toString(),
                "Unexpected value for byte config");
    }

    @SafeVarargs
    private static <T> T[] array(T... items) {
        return items;
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return (SmallRyeConfig) new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
