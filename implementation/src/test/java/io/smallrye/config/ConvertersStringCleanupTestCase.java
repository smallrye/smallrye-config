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

import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ConvertersStringCleanupTestCase<T> {

    @Parameterized.Parameters(name = "{0} - {2}")
    public static Object[][] data() {
        return new Object[][] {
                { Boolean.class, true, "true" },
                { Boolean.class, false, "NO" },
                { Double.class, 1.0d, "1.0" },
                { Float.class, 1.0f, "1.0" },
                { Long.class, 42L, "42" },
                { Integer.class, 42, "42" },
                { Class.class, Integer.class, "java.lang.Integer" },
                { OptionalInt.class, OptionalInt.of(42), "42" },
                { OptionalLong.class, OptionalLong.of(42L), "42" },
                { OptionalDouble.class, OptionalDouble.of(1.0d), "1.0" },
                { OptionalDouble.class, OptionalDouble.of(1.0d), "1.0" },
                { Boolean.class, null, "" },
                { Double.class, null, "" },
                { Float.class, null, "" },
                { Long.class, null, "" },
                { Integer.class, null, "" },
                { Class.class, null, "" },
                { OptionalInt.class, OptionalInt.empty(), "" },
                { OptionalLong.class, OptionalLong.empty(), "" },
                { OptionalDouble.class, OptionalDouble.empty(), "" },
                { OptionalDouble.class, OptionalDouble.empty(), "" }
        };
    }

    private final Class<T> type;
    private final T expected;
    private final String string;

    public ConvertersStringCleanupTestCase(Class<T> type, T expected, String string) {
        this.type = type;
        this.expected = expected;
        this.string = string;
    }

    @Test
    public void testSimple() {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(string));
    }

    @Test
    public void testTrailingSpace() {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(string + " "));
    }

    @Test
    public void testLeadingSpace() {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(" " + string));
    }

    @Test
    public void testLeadingAndTrailingWhitespaces() {
        SmallRyeConfig config = buildConfig();
        final Converter<T> converter = config.requireConverter(type);
        assertEquals(expected, converter.convert(" \t " + string + "\t\t "));
    }

    private static SmallRyeConfig buildConfig() {
        return new SmallRyeConfigBuilder().build();
    }
}
