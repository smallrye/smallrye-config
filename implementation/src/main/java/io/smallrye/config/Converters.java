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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
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
 * General converter utilities and constants.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public final class Converters {
    private Converters() {}

    @SuppressWarnings("unchecked")
    static final Converter<String> STRING_CONVERTER = BuiltInConverter.of(0, (Converter & Serializable) value -> value);

    @SuppressWarnings("unchecked")
    static final Converter<Boolean> BOOLEAN_CONVERTER = BuiltInConverter.of(1, (Converter & Serializable) value -> {
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
    });

    @SuppressWarnings("unchecked")
    static final Converter<Double> DOUBLE_CONVERTER = BuiltInConverter.of(2, (Converter & Serializable) value -> value != null ? Double.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Float> FLOAT_CONVERTER = BuiltInConverter.of(3, (Converter & Serializable) value -> value != null ? Float.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Long> LONG_CONVERTER = BuiltInConverter.of(4, (Converter & Serializable) value -> value != null ? Long.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Integer> INTEGER_CONVERTER = BuiltInConverter.of(5, (Converter & Serializable) value -> value != null ? Integer.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Class<?>> CLASS_CONVERTER = BuiltInConverter.of(6, (Converter & Serializable) value -> {
        try {
            return value != null ? Class.forName(value, true, SecuritySupport.getContextClassLoader()) : null;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    });

    static final Converter<OptionalInt> OPTIONAL_INT_CONVERTER = BuiltInConverter.of(7, (Converter<OptionalInt> & Serializable) value -> value != null && !value.isEmpty() ? OptionalInt.of(Integer.parseInt(value)) : OptionalInt.empty());

    static final Converter<OptionalLong> OPTIONAL_LONG_CONVERTER = BuiltInConverter.of(8, (Converter<OptionalLong> & Serializable) value -> value != null && !value.isEmpty()? OptionalLong.of(Long.parseLong(value)) : OptionalLong.empty());

    static final Converter<OptionalDouble> OPTIONAL_DOUBLE_CONVERTER = BuiltInConverter.of(9, (Converter<OptionalDouble> & Serializable) value -> value != null && !value.isEmpty() ? OptionalDouble.of(Double.parseDouble(value)) : OptionalDouble.empty());

    static final Converter<InetAddress> INET_ADDRESS_CONVERTER = BuiltInConverter.of(10, (Converter<InetAddress> & Serializable) value -> {
        try {
            return value != null && !value.isEmpty() ? InetAddress.getByName(value) : null;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    });

    static final Map<Type, Converter<?>> ALL_CONVERTERS = new HashMap<>();

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

    /**
     * Get the type of the converter specified by {@code clazz}.  If the given class is not a valid
     * converter, then {@code null} is returned.
     *
     * @param clazz the converter class (must not be {@code null})
     * @return the converter target type, or {@code null} if the class does not represent a valid configuration
     * @throws IllegalStateException if the given converter class is not properly parameterized
     */
    public static Type getConverterType(Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return null;
        }

        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType().equals(Converter.class)) {
                    Type[] typeArguments = pt.getActualTypeArguments();
                    if (typeArguments.length != 1) {
                        throw new IllegalStateException("Converter " + clazz + " must be parameterized with a single type");
                    }
                    return typeArguments[0];
                }
            }
        }

        return getConverterType(clazz.getSuperclass());
    }

    static final class BuiltInConverter<T> implements Converter<T>, Serializable {
        private final int id;
        private final Converter<T> function;

        static <T> BuiltInConverter<T> of(int id, Converter<T> function) {
            return new BuiltInConverter<>(id, function);
        }

        private BuiltInConverter(final int id, final Converter<T> function) {
            this.id = id;
            this.function = function;
        }

        public T convert(final String value) {
            return function.convert(value);
        }

        Object writeReplace() {
            return new Ser(id);
        }
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = 5646753664957303950L;

        private final short id;

        Ser(final int id) {
            this.id = (short) id;
        }

        Object readResolve() throws ObjectStreamException {
            switch (id) {
                case 0: return STRING_CONVERTER;
                case 1: return BOOLEAN_CONVERTER;
                case 2: return DOUBLE_CONVERTER;
                case 3: return FLOAT_CONVERTER;
                case 4: return LONG_CONVERTER;
                case 5: return INTEGER_CONVERTER;
                case 6: return CLASS_CONVERTER;
                case 7: return OPTIONAL_INT_CONVERTER;
                case 8: return OPTIONAL_LONG_CONVERTER;
                case 9: return OPTIONAL_DOUBLE_CONVERTER;
                case 10: return INET_ADDRESS_CONVERTER;
                default: throw new InvalidObjectException("Unknown converter ID");
            }
        }
    }
}
