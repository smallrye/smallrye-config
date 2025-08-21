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

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
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
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

class ConvertersTest {
    @Test
    void booleanConverter() {
        SmallRyeConfig config = buildConfig(
                "boolean.TRUE", "TRUE",
                "boolean.1", "1",
                "boolean.YES", "YES",
                "boolean.Y", "Y",
                "boolean.ON", "ON",
                "boolean.JA", "JA",
                "boolean.J", "J",
                "boolean.SI", "SI",
                "boolean.SIM", "SIM",
                "boolean.OUI", "OUI",
                "boolean.true", "true",
                "boolean.yes", "yes",
                "boolean.y", "y",
                "boolean.on", "on",
                "boolean.ja", "ja",
                "boolean.j", "j",
                "boolean.si", "si",
                "boolean.sim", "sim",
                "boolean.oui", "oui",
                "boolean.FALSE", "FALSE",
                "boolean.0", "0",
                "boolean.NO", "NO",
                "boolean.N", "N",
                "boolean.OFF", "OFF",
                "boolean.NEIN", "NEIN",
                "boolean.NÃO", "NÃO",
                "boolean.NON", "NON",
                "boolean.false", "false",
                "boolean.0", "0",
                "boolean.no", "no",
                "boolean.n", "n",
                "boolean.off", "off",
                "boolean.nein", "nein",
                "boolean.não", "não",
                "boolean.non", "non",
                "boolean.unexpected", "unexpected");

        assertTrue(config.getValue("boolean.TRUE", boolean.class));
        assertTrue(config.getValue("boolean.1", boolean.class));
        assertTrue(config.getValue("boolean.YES", boolean.class));
        assertTrue(config.getValue("boolean.Y", boolean.class));
        assertTrue(config.getValue("boolean.ON", boolean.class));
        assertTrue(config.getValue("boolean.JA", boolean.class));
        assertTrue(config.getValue("boolean.J", boolean.class));
        assertTrue(config.getValue("boolean.SI", boolean.class));
        assertTrue(config.getValue("boolean.SIM", boolean.class));
        assertTrue(config.getValue("boolean.OUI", boolean.class));

        assertTrue(config.getValue("boolean.true", boolean.class));
        assertTrue(config.getValue("boolean.yes", boolean.class));
        assertTrue(config.getValue("boolean.y", boolean.class));
        assertTrue(config.getValue("boolean.on", boolean.class));
        assertTrue(config.getValue("boolean.ja", boolean.class));
        assertTrue(config.getValue("boolean.j", boolean.class));
        assertTrue(config.getValue("boolean.si", boolean.class));
        assertTrue(config.getValue("boolean.sim", boolean.class));
        assertTrue(config.getValue("boolean.oui", boolean.class));

        assertFalse(config.getValue("boolean.FALSE", boolean.class));
        assertFalse(config.getValue("boolean.0", boolean.class));
        assertFalse(config.getValue("boolean.NO", boolean.class));
        assertFalse(config.getValue("boolean.N", boolean.class));
        assertFalse(config.getValue("boolean.OFF", boolean.class));
        assertFalse(config.getValue("boolean.NEIN", boolean.class));
        assertFalse(config.getValue("boolean.NÃO", boolean.class));
        assertFalse(config.getValue("boolean.NON", boolean.class));

        assertFalse(config.getValue("boolean.false", boolean.class));
        assertFalse(config.getValue("boolean.0", boolean.class));
        assertFalse(config.getValue("boolean.no", boolean.class));
        assertFalse(config.getValue("boolean.n", boolean.class));
        assertFalse(config.getValue("boolean.off", boolean.class));
        assertFalse(config.getValue("boolean.nein", boolean.class));
        assertFalse(config.getValue("boolean.não", boolean.class));
        assertFalse(config.getValue("boolean.non", boolean.class));

        assertFalse(config.getValue("boolean.unexpected", boolean.class));
    }

    @Test
    void collectionConverters() {
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
    void arrayConverters() {
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
    void minimumValue() {
        SmallRyeConfig config = buildConfig("one.plus.one", "2", "animal", "anteater", "when", "1950-01-01");
        final Converter<Integer> intConv = Converters.getImplicitConverter(Integer.class);
        final Converter<Integer> intMin2Conv = Converters.minimumValueConverter(intConv, 2, true);
        final Converter<Integer> intMin2ExConv = Converters.minimumValueConverter(intConv, 2, false);
        final Converter<Integer> intMin3Conv = Converters.minimumValueConverter(intConv, 3, true);
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
    void maximumValue() {
        SmallRyeConfig config = buildConfig("one.plus.one", "2", "animal", "anteater", "when", "1950-01-01");
        final Converter<Integer> intConv = Converters.getImplicitConverter(Integer.class);
        final Converter<Integer> intMax2Conv = Converters.maximumValueConverter(intConv, 2, true);
        final Converter<Integer> intMax2ExConv = Converters.maximumValueConverter(intConv, 2, false);
        final Converter<Integer> intMax3Conv = Converters.maximumValueConverter(intConv, 3, true);
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
    void empty() {
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
        assertTrue(config.getValue("boolean.key", Boolean.class));
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
    void shortValue() {
        final SmallRyeConfig config = buildConfig("simple.short", "2");
        final short expected = 2;
        assertEquals(expected, (short) config.getValue("simple.short", Short.class), "Unexpected value for short config");
        assertEquals(expected, (short) config.getValue("simple.short", Short.TYPE), "Unexpected value for short config");
    }

    @Test
    void byteValue() {
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
        assertEquals("Ynl0ZXM=", config.getConfigValue("byte.array").getValue());
        assertEquals("bytes", new String(config.getValue("byte.array", byte[].class)));
    }

    @Test
    void currency() {
        final SmallRyeConfig config = buildConfig("simple.currency", "GBP");
        final Currency expected = Currency.getInstance("GBP");
        assertEquals(expected.getCurrencyCode(),
                config.getValue("simple.currency", Currency.class).getCurrencyCode(),
                "Unexpected value for byte config");
    }

    @Test
    void bitSet() {
        final SmallRyeConfig config = buildConfig("simple.bitset", "AA");
        BitSet expected = new BitSet(8);
        expected.set(1);
        expected.set(3);
        expected.set(5);
        expected.set(7);
        assertEquals(expected.toString(), (config.getValue("simple.bitset", BitSet.class)).toString(),
                "Unexpected value for byte config");
    }

    @Test
    void pattern() {
        final SmallRyeConfig config = buildConfig("simple.pattern", "[0-9]");
        final Pattern expected = Pattern.compile("[0-9]");
        assertEquals(expected.pattern(),
                config.getValue("simple.pattern", Pattern.class).pattern(),
                "Unexpected value for pattern");
    }

    @Test
    void path() {
        final SmallRyeConfig config = buildConfig("simple.path", "/test", "path.leading.space", " test");
        assertEquals(Path.of("/test"),
                config.getValue("simple.path", Path.class),
                "Unexpected value for path");
        assertEquals(Path.of(" test"),
                config.getValue("path.leading.space", Path.class),
                "Unexpected value for path");
    }

    @Test
    void nulls() {
        final Config config = ConfigProvider.getConfig();
        assertThrows(NullPointerException.class, () -> convertNull(config, Boolean.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Byte.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Short.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Integer.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Long.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Float.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Double.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, Character.class));

        assertThrows(NullPointerException.class, () -> convertNull(config, OptionalInt.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, OptionalLong.class));
        assertThrows(NullPointerException.class, () -> convertNull(config, OptionalDouble.class));
    }

    @Test
    void file() {
        SmallRyeConfig config = buildConfig("file", "/test", "file.leading.space", " test");
        assertEquals(new File("/test"), config.getValue("file", File.class));
        assertEquals(new File(" test"), config.getValue("file.leading.space", File.class));
        Converter<File> fileConverter = config.getConverterOrNull(File.class);
        assertNotNull(fileConverter);
        assertTrue(fileConverter.getClass().getName().contains("BuiltInConverter"));
    }

    @Test
    void uri() {
        SmallRyeConfig config = buildConfig("uri", "http://localhost", "uri.leading.space", " http://localhost");
        assertEquals(URI.create("http://localhost"), config.getValue("uri", URI.class));
        assertEquals(URI.create("http://localhost"), config.getValue("uri.leading.space", URI.class));
        Converter<URI> uriConverter = config.getConverterOrNull(URI.class);
        assertNotNull(uriConverter);
        assertTrue(uriConverter.getClass().getName().contains("BuiltInConverter"));
    }

    @Test
    void dateTimeFormatter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("dtf", "yyyy-MM-dd"))
                .build();
        DateTimeFormatter dtf = config.getValue("dtf", DateTimeFormatter.class);
        assertEquals("2020-10-10", dtf.format(LocalDate.of(2020, 10, 10)));
    }

    @Test
    void charSequence() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("cs", "CharSequence"))
                .build();

        assertEquals("CharSequence", config.getValue("cs", CharSequence.class));
    }

    @SafeVarargs
    private static <T> T[] array(T... items) {
        return items;
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

    private static <T> void convertNull(Config config, Class<T> converterType) {
        config.getConverter(converterType)
                .map(converter -> converter.convert(null))
                .orElseThrow(NoSuchElementException::new);
    }
}
