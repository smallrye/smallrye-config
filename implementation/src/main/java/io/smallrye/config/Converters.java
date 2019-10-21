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
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.utils.StringUtil;

/**
 * General converter utilities and constants.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public final class Converters {
    private Converters() {
    }

    @SuppressWarnings("unchecked")
    static final Converter<String> STRING_CONVERTER = BuiltInConverter.of(0,
            (Converter & Serializable) value -> value != null && !value.isEmpty() ? value : null);

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
    static final Converter<Double> DOUBLE_CONVERTER = BuiltInConverter.of(2,
            (Converter & Serializable) value -> value != null ? Double.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Float> FLOAT_CONVERTER = BuiltInConverter.of(3,
            (Converter & Serializable) value -> value != null ? Float.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Long> LONG_CONVERTER = BuiltInConverter.of(4,
            (Converter & Serializable) value -> value != null ? Long.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Integer> INTEGER_CONVERTER = BuiltInConverter.of(5,
            (Converter & Serializable) value -> value != null ? Integer.valueOf(value) : null);

    @SuppressWarnings("unchecked")
    static final Converter<Class<?>> CLASS_CONVERTER = BuiltInConverter.of(6, (Converter & Serializable) value -> {
        try {
            return value != null ? Class.forName(value, true, SecuritySupport.getContextClassLoader()) : null;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    });

    static final Converter<OptionalInt> OPTIONAL_INT_CONVERTER = BuiltInConverter.of(7,
            (Converter<OptionalInt> & Serializable) value -> value != null && !value.isEmpty()
                    ? OptionalInt.of(Integer.parseInt(value))
                    : OptionalInt.empty());

    static final Converter<OptionalLong> OPTIONAL_LONG_CONVERTER = BuiltInConverter.of(8,
            (Converter<OptionalLong> & Serializable) value -> value != null && !value.isEmpty()
                    ? OptionalLong.of(Long.parseLong(value))
                    : OptionalLong.empty());

    static final Converter<OptionalDouble> OPTIONAL_DOUBLE_CONVERTER = BuiltInConverter.of(9,
            (Converter<OptionalDouble> & Serializable) value -> value != null && !value.isEmpty()
                    ? OptionalDouble.of(Double.parseDouble(value))
                    : OptionalDouble.empty());

    static final Converter<InetAddress> INET_ADDRESS_CONVERTER = BuiltInConverter.of(10,
            (Converter<InetAddress> & Serializable) value -> {
                try {
                    return value != null && !value.isEmpty() ? InetAddress.getByName(value) : null;
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException(e);
                }
            });

    @SuppressWarnings("unchecked")
    static final Converter<Character> CHARACTER_CONVERTER = BuiltInConverter.of(11, (Converter & Serializable) value -> {
        if (value != null) {
            if (value.length() == 1) {
                return Character.valueOf(value.charAt(0));
            }
            throw new IllegalArgumentException(value + " can not be converted to a Character");
        }
        return null;
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

        ALL_CONVERTERS.put(Character.class, CHARACTER_CONVERTER);
        ALL_CONVERTERS.put(Character.TYPE, CHARACTER_CONVERTER);

    }

    /**
     * Get the type of the converter specified by {@code clazz}. If the given class is not a valid
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

    /**
     * Get the implicit converter for the given type class, if any.
     *
     * @param type the type class
     * @param <T> the type
     * @return the implicit converter for the given type class, or {@code null} if none exists
     */
    public static <T> Converter<T> getImplicitConverter(Class<? extends T> type) {
        return ImplicitConverters.getConverter(type);
    }

    /**
     * Get a converter that converts a comma-separated string into a list of converted items.
     *
     * @param itemConverter the item converter (must not be {@code null})
     * @param collectionFactory the collection factory (must not be {@code null})
     * @param <T> the item type
     * @param <C> the collection type
     * @return the new converter (not {@code null})
     */
    public static <T, C extends Collection<T>> Converter<C> newCollectionConverter(Converter<? extends T> itemConverter,
            IntFunction<C> collectionFactory) {
        return new CollectionConverter<>(itemConverter, collectionFactory);
    }

    /**
     * Get a converter that converts a comma-separated string into an array of converted items.
     *
     * @param itemConverter the item converter (must not be {@code null})
     * @param arrayType the array type class (must not be {@code null})
     * @param <T> the item type
     * @param <A> the array type
     * @return the new converter (not {@code null})
     */
    public static <A, T> Converter<A> newArrayConverter(Converter<? extends T> itemConverter, Class<A> arrayType) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException(arrayType.toString() + " is not an array type");
        }
        return new ArrayConverter<>(itemConverter, arrayType);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code Optional}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @param <T> the item type
     * @return the new converter (not {@code null})
     */
    public static <T> Converter<Optional<T>> newOptionalConverter(Converter<? extends T> delegateConverter) {
        return new OptionalConverter<>(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter and returns a special value to represent empty.
     *
     * @param delegateConverter the converter to delegate to (must not be {@code null})
     * @param emptyValue the empty value to return
     * @param <T> the value type
     * @return the converter
     */
    public static <T> Converter<T> newEmptyValueConverter(Converter<T> delegateConverter, T emptyValue) {
        if (emptyValue == null) {
            return delegateConverter;
        }
        return new EmptyValueConverter<>(delegateConverter, emptyValue);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is greater than, or optionally equal to,
     * the given minimum value.
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param minimumValue the minimum value (must not be {@code null})
     * @param inclusive {@code true} if the minimum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     */
    public static <T extends Comparable<T>> Converter<T> minimumValueConverter(Converter<? extends T> delegate, T minimumValue,
            boolean inclusive) {
        return new RangeCheckConverter<>(delegate, minimumValue, inclusive, null, false);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is greater than, or optionally equal to,
     * the given minimum value (in string form).
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param minimumValue the minimum value (must not be {@code null})
     * @param inclusive {@code true} if the minimum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     * @throws IllegalArgumentException if the given minimum value fails conversion
     */
    public static <T extends Comparable<T>> Converter<T> minimumValueStringConverter(Converter<? extends T> delegate,
            String minimumValue, boolean inclusive) {
        return minimumValueConverter(delegate, delegate.convert(minimumValue), inclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is less than, or optionally equal to,
     * the given maximum value.
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param inclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     */
    public static <T extends Comparable<T>> Converter<T> maximumValueConverter(Converter<? extends T> delegate, T maximumValue,
            boolean inclusive) {
        return new RangeCheckConverter<>(delegate, null, false, maximumValue, inclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is less than, or optionally equal to,
     * the given maximum value (in string form).
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param inclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     * @throws IllegalArgumentException if the given maximum value fails conversion
     */
    public static <T extends Comparable<T>> Converter<T> maximumValueStringConverter(Converter<? extends T> delegate,
            String maximumValue, boolean inclusive) {
        return maximumValueConverter(delegate, delegate.convert(maximumValue), inclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is within the given range.
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param maxInclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     */
    public static <T extends Comparable<T>> Converter<T> rangeValueConverter(Converter<? extends T> delegate, T minimumValue,
            boolean minInclusive, T maximumValue, boolean maxInclusive) {
        return new RangeCheckConverter<>(delegate, minimumValue, minInclusive, maximumValue, maxInclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is within the given range (in string form).
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param maxInclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     * @throws IllegalArgumentException if the given minimum or maximum value fails conversion
     */
    public static <T extends Comparable<T>> Converter<T> rangeValueStringConverter(Converter<? extends T> delegate,
            String minimumValue, boolean minInclusive, String maximumValue, boolean maxInclusive) {
        return rangeValueConverter(delegate, delegate.convert(minimumValue), minInclusive, delegate.convert(maximumValue),
                maxInclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value matches the given pattern.
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param pattern the pattern to match (must not be {@code null})
     * @param <T> the converter target type
     * @return a pattern-validating converter
     */
    public static <T> Converter<T> patternValidatingConverter(Converter<? extends T> delegate, Pattern pattern) {
        return new PatternCheckConverter<>(delegate, pattern);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value matches the given pattern.
     *
     * @param delegate the delegate converter (must not be {@code null})
     * @param pattern the pattern string to match (must not be {@code null})
     * @param <T> the converter target type
     * @return a pattern-validating converter
     * @throws PatternSyntaxException if the given pattern has invalid syntax
     */
    public static <T> Converter<T> patternValidatingConverter(Converter<? extends T> delegate, String pattern) {
        return patternValidatingConverter(delegate, Pattern.compile(pattern));
    }

    static final class PatternCheckConverter<T> implements Converter<T>, Serializable {
        private static final long serialVersionUID = 358813973126582008L;

        private final Converter<? extends T> delegate;
        private final Pattern pattern;

        PatternCheckConverter(final Converter<? extends T> delegate, final Pattern pattern) {
            this.delegate = delegate;
            this.pattern = pattern;
        }

        public T convert(final String value) {
            if (value == null) {
                return null;
            }
            if (pattern.matcher(value).matches()) {
                return delegate.convert(value);
            }
            throw new IllegalArgumentException("Value does not match pattern " + pattern + " (value was \"" + value + "\")");
        }
    }

    static final class RangeCheckConverter<T extends Comparable<T>> implements Converter<T>, Serializable {

        private static final long serialVersionUID = 2764654140347010865L;

        private final Converter<? extends T> delegate;
        private final T min;
        private final boolean minInclusive;
        private final T max;
        private final boolean maxInclusive;

        RangeCheckConverter(final Converter<? extends T> delegate, final T min, final boolean minInclusive, final T max,
                final boolean maxInclusive) {
            this.delegate = delegate;
            this.min = min;
            this.minInclusive = minInclusive;
            this.max = max;
            this.maxInclusive = maxInclusive;
        }

        public T convert(final String value) {
            final T result = delegate.convert(value);
            if (result == null) {
                return null;
            }
            if (min != null) {
                final int cmp = result.compareTo(min);
                if (minInclusive) {
                    if (cmp < 0) {
                        throw new IllegalArgumentException(
                                "Value must not be less than " + min + " (value was \"" + value + "\")");
                    }
                } else {
                    if (cmp <= 0) {
                        throw new IllegalArgumentException(
                                "Value must not be less than or equal to " + min + " (value was \"" + value + "\")");
                    }
                }
            }
            if (max != null) {
                final int cmp = result.compareTo(max);
                if (maxInclusive) {
                    if (cmp > 0) {
                        throw new IllegalArgumentException(
                                "Value must not be greater than " + max + " (value was \"" + value + "\")");
                    }
                } else {
                    if (cmp >= 0) {
                        throw new IllegalArgumentException(
                                "Value must not be greater than or equal to " + max + " (value was \"" + value + "\")");
                    }
                }
            }
            return result;
        }
    }

    static final class CollectionConverter<T, C extends Collection<T>> implements Converter<C>, Serializable {
        private static final long serialVersionUID = -8452214026800305628L;

        private final Converter<? extends T> itemConverter;
        private final IntFunction<C> collectionFactory;

        CollectionConverter(final Converter<? extends T> itemConverter, final IntFunction<C> collectionFactory) {
            this.itemConverter = itemConverter;
            this.collectionFactory = collectionFactory;
        }

        public C convert(final String str) {
            if (str.isEmpty()) {
                // empty collection
                return null;
            }
            final String[] itemStrings = StringUtil.split(str);
            final C collection = collectionFactory.apply(itemStrings.length);
            for (String itemString : itemStrings) {
                if (!itemString.isEmpty()) {
                    final T item = itemConverter.convert(itemString);
                    if (item != null) {
                        collection.add(item);
                    }
                }
            }
            return collection.isEmpty() ? null : collection;
        }
    }

    static final class ArrayConverter<A, T> implements Converter<A>, Serializable {
        private static final long serialVersionUID = 2630282286159527380L;

        private final Converter<T> itemConverter;
        private final Class<A> arrayType;

        ArrayConverter(final Converter<T> itemConverter, final Class<A> arrayType) {
            this.itemConverter = itemConverter;
            this.arrayType = arrayType;
        }

        public A convert(final String str) {
            if (str.isEmpty()) {
                // empty array
                return null;
            }
            final String[] itemStrings = StringUtil.split(str);
            final A array = arrayType.cast(Array.newInstance(arrayType.getComponentType(), itemStrings.length));
            int size = 0;
            for (String itemString : itemStrings) {
                if (!itemString.isEmpty()) {
                    final T item = itemConverter.convert(itemString);
                    if (item != null) {
                        Array.set(array, size++, item);
                    }
                }
            }
            return size == 0 ? null : size < itemStrings.length ? copyArray(array, arrayType, size) : array;
        }

        private static <A> A copyArray(A array, Class<A> arrayType, int newSize) {
            if (array instanceof Object[]) {
                return arrayType.cast(Arrays.copyOf((Object[]) array, newSize));
            } else if (array instanceof boolean[]) {
                return arrayType.cast(Arrays.copyOf((boolean[]) array, newSize));
            } else if (array instanceof char[]) {
                return arrayType.cast(Arrays.copyOf((char[]) array, newSize));
            } else if (array instanceof byte[]) {
                return arrayType.cast(Arrays.copyOf((byte[]) array, newSize));
            } else if (array instanceof short[]) {
                return arrayType.cast(Arrays.copyOf((short[]) array, newSize));
            } else if (array instanceof int[]) {
                return arrayType.cast(Arrays.copyOf((int[]) array, newSize));
            } else if (array instanceof long[]) {
                return arrayType.cast(Arrays.copyOf((long[]) array, newSize));
            } else if (array instanceof float[]) {
                return arrayType.cast(Arrays.copyOf((float[]) array, newSize));
            } else if (array instanceof double[]) {
                return arrayType.cast(Arrays.copyOf((double[]) array, newSize));
            } else {
                throw new IllegalStateException();
            }
        }
    }

    static final class OptionalConverter<T> implements Converter<Optional<T>>, Serializable {
        private static final long serialVersionUID = -4051551570591834428L;
        private final Converter<? extends T> delegate;

        OptionalConverter(final Converter<? extends T> delegate) {
            this.delegate = delegate;
        }

        public Optional<T> convert(final String value) {
            if (value.isEmpty()) {
                try {
                    return Optional.ofNullable(delegate.convert(value));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            } else {
                return Optional.ofNullable(delegate.convert(value));
            }
        }
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
                case 0:
                    return STRING_CONVERTER;
                case 1:
                    return BOOLEAN_CONVERTER;
                case 2:
                    return DOUBLE_CONVERTER;
                case 3:
                    return FLOAT_CONVERTER;
                case 4:
                    return LONG_CONVERTER;
                case 5:
                    return INTEGER_CONVERTER;
                case 6:
                    return CLASS_CONVERTER;
                case 7:
                    return OPTIONAL_INT_CONVERTER;
                case 8:
                    return OPTIONAL_LONG_CONVERTER;
                case 9:
                    return OPTIONAL_DOUBLE_CONVERTER;
                case 10:
                    return INET_ADDRESS_CONVERTER;
                case 11:
                    return CHARACTER_CONVERTER;
                default:
                    throw new InvalidObjectException("Unknown converter ID");
            }
        }
    }

    static class EmptyValueConverter<T> implements Converter<T> {
        private final Converter<T> delegateConverter;
        private final T emptyValue;

        EmptyValueConverter(final Converter<T> delegateConverter, final T emptyValue) {
            this.delegateConverter = delegateConverter;
            this.emptyValue = emptyValue;
        }

        public T convert(final String value) {
            if (value.isEmpty()) {
                return emptyValue;
            }
            final T result = delegateConverter.convert(value);
            if (result == null) {
                return emptyValue;
            } else {
                return result;
            }
        }
    }
}
