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

package io.smallrye.config;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class Converters {

    @SuppressWarnings("unchecked")
    static final Converter<String> STRING_CONVERTER = (Converter & Serializable) value -> value;

    @SuppressWarnings("unchecked")
    static final Converter<Boolean> BOOLEAN_CONVERTER = (Converter & Serializable) value -> {
        if (value != null) {
            return "TRUE".equalsIgnoreCase(value)
                    || "1".equalsIgnoreCase(value)
                    || "YES".equalsIgnoreCase(value)
                    || "Y".equalsIgnoreCase(value)
                    || "ON".equalsIgnoreCase(value)
                    || "JA".equalsIgnoreCase(value)
                    || "J".equalsIgnoreCase(value)
                    || "SI".equalsIgnoreCase(value)
                    || "SIM".equalsIgnoreCase(value)
                    || "OUI".equalsIgnoreCase(value);
        }
        return null;
    };

    @SuppressWarnings("unchecked")
    static final Converter<Double> DOUBLE_CONVERTER = (Converter & Serializable) value -> value != null ? Double.valueOf(value) : null;

    @SuppressWarnings("unchecked")
    static final Converter<Float> FLOAT_CONVERTER = (Converter & Serializable) value -> value != null ? Float.valueOf(value) : null;

    @SuppressWarnings("unchecked")
    static final Converter<Long> LONG_CONVERTER = (Converter & Serializable) value -> value != null ? Long.valueOf(value) : null;

    @SuppressWarnings("unchecked")
    static final Converter<Integer> INTEGER_CONVERTER = (Converter & Serializable) value -> value != null ? Integer.valueOf(value) : null;

    @SuppressWarnings("unchecked")
    static final Converter<Class<?>> CLASS_CONVERTER = (Converter & Serializable) value -> {
        try {
            return value != null ? Class.forName(value, true, SecuritySupport.getContextClassLoader()) : null;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    };

    static final Converter<OptionalInt> OPTIONAL_INT_CONVERTER = (Converter<OptionalInt> & Serializable) value -> value != null && !value.isEmpty() ? OptionalInt.of(Integer.parseInt(value)) : OptionalInt.empty();

    static final Converter<OptionalLong> OPTIONAL_LONG_CONVERTER = (Converter<OptionalLong> & Serializable) value -> value != null && !value.isEmpty()? OptionalLong.of(Long.parseLong(value)) : OptionalLong.empty();

    static final Converter<OptionalDouble> OPTIONAL_DOUBLE_CONVERTER = (Converter<OptionalDouble> & Serializable) value -> value != null && !value.isEmpty() ? OptionalDouble.of(Double.parseDouble(value)) : OptionalDouble.empty();

    static final Converter<InetAddress> INET_ADDRESS_CONVERTER = (Converter<InetAddress> & Serializable) value -> {
        try {
            return value != null && !value.isEmpty() ? InetAddress.getByName(value) : null;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    };

    public static final Map<Type, Converter<?>> ALL_CONVERTERS = new HashMap<>();

    static {
        ALL_CONVERTERS.put(String.class, STRING_CONVERTER);

        ALL_CONVERTERS.put(Boolean.class, BOOLEAN_CONVERTER);
        ALL_CONVERTERS.put(Boolean.TYPE, BOOLEAN_CONVERTER);

        ALL_CONVERTERS.put(Double.class, DOUBLE_CONVERTER);
        ALL_CONVERTERS.put(Double.TYPE, DOUBLE_CONVERTER);

        ALL_CONVERTERS.put(Float.class, FLOAT_CONVERTER);
        ALL_CONVERTERS.put(Float.TYPE, FLOAT_CONVERTER);

        ALL_CONVERTERS.put(Long.class, LONG_CONVERTER);
        ALL_CONVERTERS.put(Long.TYPE, LONG_CONVERTER);

        ALL_CONVERTERS.put(Integer.class, INTEGER_CONVERTER);
        ALL_CONVERTERS.put(Integer.TYPE, INTEGER_CONVERTER);

        ALL_CONVERTERS.put(Class.class, CLASS_CONVERTER);
        ALL_CONVERTERS.put(InetAddress.class, INET_ADDRESS_CONVERTER);

        ALL_CONVERTERS.put(OptionalInt.class, OPTIONAL_INT_CONVERTER);
        ALL_CONVERTERS.put(OptionalLong.class, OPTIONAL_LONG_CONVERTER);
        ALL_CONVERTERS.put(OptionalDouble.class, OPTIONAL_DOUBLE_CONVERTER);
    }
}
