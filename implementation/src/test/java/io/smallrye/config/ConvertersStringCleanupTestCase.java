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

import java.util.*;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConvertersStringCleanupTestCase<T> {
    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(Boolean.class, true, "true"),
                Arguments.of(Boolean.class, false, "NO"),
                Arguments.of(Double.class, 1.0d, "1.0"),
                Arguments.of(Float.class, 1.0f, "1.0"),
                Arguments.of(Long.class, 42L, "42"),
                Arguments.of(Integer.class, 42, "42"),
                Arguments.of(Class.class, Integer.class, "java.lang.Integer"),
                Arguments.of(OptionalInt.class, OptionalInt.of(42), "42"),
                Arguments.of(OptionalLong.class, OptionalLong.of(42L), "42"),
                Arguments.of(OptionalDouble.class, OptionalDouble.of(1.0d), "1.0"),
                Arguments.of(OptionalDouble.class, OptionalDouble.of(1.0d), "1.0"),
                Arguments.of(Boolean.class, null, ""),
                Arguments.of(Double.class, null, ""),
                Arguments.of(Float.class, null, ""),
                Arguments.of(Long.class, null, ""),
                Arguments.of(Integer.class, null, ""),
                Arguments.of(Class.class, null, ""),
                Arguments.of(OptionalInt.class, OptionalInt.empty(), ""),
                Arguments.of(OptionalLong.class, OptionalLong.empty(), ""),
                Arguments.of(OptionalDouble.class, OptionalDouble.empty(), ""),
                Arguments.of(OptionalDouble.class, OptionalDouble.empty(), ""));
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("data")
    public void testSimple(Class<T> type, T expected, String string) {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(string));
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("data")
    public void testTrailingSpace(Class<T> type, T expected, String string) {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(string + " "));
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("data")
    public void testLeadingSpace(Class<T> type, T expected, String string) {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(" " + string));
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("data")
    public void testLeadingAndTrailingWhitespaces(Class<T> type, T expected, String string) {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(" \t " + string + "\t\t "));
    }

    private static SmallRyeConfig buildConfig() {
        return new SmallRyeConfigBuilder().build();
    }
}
