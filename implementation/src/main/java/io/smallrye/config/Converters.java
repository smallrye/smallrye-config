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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.common.AbstractConverter;
import io.smallrye.config.common.AbstractDelegatingConverter;
import io.smallrye.config.common.AbstractSimpleDelegatingConverter;
import io.smallrye.config.common.utils.StringUtil;

/**
 * General converter utilities and constants.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public final class Converters {
    private Converters() {
    }

    static final Converter<String> STRING_CONVERTER = BuiltInConverter.of(0, newEmptyValueConverter(value -> value));

    static final Converter<Boolean> BOOLEAN_CONVERTER = BuiltInConverter.of(1, newTrimmingConverter(newEmptyValueConverter(
            value -> Boolean.valueOf(
                    "TRUE".equalsIgnoreCase(value)
                            || "1".equalsIgnoreCase(value)
                            || "YES".equalsIgnoreCase(value)
                            || "Y".equalsIgnoreCase(value)
                            || "ON".equalsIgnoreCase(value)
                            || "JA".equalsIgnoreCase(value)
                            || "J".equalsIgnoreCase(value)
                            || "SI".equalsIgnoreCase(value)
                            || "SIM".equalsIgnoreCase(value)
                            || "OUI".equalsIgnoreCase(value)))));

    static final Converter<Double> DOUBLE_CONVERTER = BuiltInConverter.of(2,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Double.valueOf(value);
                } catch (NumberFormatException nfe) {
                    throw ConfigMessages.msg.doubleExpected(value);
                }
            })));

    static final Converter<Float> FLOAT_CONVERTER = BuiltInConverter.of(3,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Float.valueOf(value);
                } catch (NumberFormatException nfe) {
                    throw ConfigMessages.msg.floatExpected(value);
                }
            })));

    static final Converter<Long> LONG_CONVERTER = BuiltInConverter.of(4,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException nfe) {
                    throw ConfigMessages.msg.longExpected(value);
                }
            })));

    static final Converter<Integer> INTEGER_CONVERTER = BuiltInConverter.of(5,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException nfe) {
                    throw ConfigMessages.msg.integerExpected(value);
                }
            })));

    static final Converter<Class<?>> CLASS_CONVERTER = BuiltInConverter.of(6,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Class.forName(value, true, SecuritySupport.getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw ConfigMessages.msg.classConverterNotFound(e, value);
                }
            })));

    static final Converter<OptionalInt> OPTIONAL_INT_CONVERTER = BuiltInConverter.of(7,
            newOptionalIntConverter(INTEGER_CONVERTER));

    static final Converter<OptionalLong> OPTIONAL_LONG_CONVERTER = BuiltInConverter.of(8,
            newOptionalLongConverter(LONG_CONVERTER));

    static final Converter<OptionalDouble> OPTIONAL_DOUBLE_CONVERTER = BuiltInConverter.of(9,
            newOptionalDoubleConverter(DOUBLE_CONVERTER));

    static final Converter<InetAddress> INET_ADDRESS_CONVERTER = BuiltInConverter.of(10,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return InetAddress.getByName(value);
                } catch (UnknownHostException e) {
                    throw ConfigMessages.msg.unknownHost(e, value);
                }
            })));

    static final Converter<Character> CHARACTER_CONVERTER = BuiltInConverter.of(11, newEmptyValueConverter(value -> {
        if (value.length() == 1) {
            return value.charAt(0);
        }
        throw ConfigMessages.msg.failedCharacterConversion(value);
    }));

    static final Converter<Short> SHORT_CONVERTER = BuiltInConverter.of(12,
            newTrimmingConverter(newEmptyValueConverter(Short::valueOf)));

    static final Converter<Byte> BYTE_CONVERTER = BuiltInConverter.of(13,
            newTrimmingConverter(newEmptyValueConverter(Byte::valueOf)));

    static final Converter<UUID> UUID_CONVERTER = BuiltInConverter.of(14,
            newTrimmingConverter(newEmptyValueConverter((s) -> {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException e) {
                    throw ConfigMessages.msg.malformedUUID(e, s);
                }
            })));

    static final Converter<Currency> CURRENCY_CONVERTER = BuiltInConverter.of(15,
            newTrimmingConverter(newEmptyValueConverter((s) -> Currency.getInstance(s))));

    static final Converter<BitSet> BITSET_CONVERTER = BuiltInConverter.of(16,
            newTrimmingConverter(newTrimmingConverter((s) -> {
                int len = s.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
                }
                return BitSet.valueOf(data);
            })));

    static final Converter<Pattern> PATTERN_CONVERTER = BuiltInConverter.of(17,
            newTrimmingConverter(newEmptyValueConverter(Pattern::compile)));

    static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES;

    static final Map<Type, Converter<?>> ALL_CONVERTERS = new HashMap<>();

    static {
        ALL_CONVERTERS.put(String.class, STRING_CONVERTER);

        ALL_CONVERTERS.put(Boolean.class, BOOLEAN_CONVERTER);

        ALL_CONVERTERS.put(Double.class, DOUBLE_CONVERTER);

        ALL_CONVERTERS.put(Float.class, FLOAT_CONVERTER);

        ALL_CONVERTERS.put(Long.class, LONG_CONVERTER);

        ALL_CONVERTERS.put(Integer.class, INTEGER_CONVERTER);

        ALL_CONVERTERS.put(Short.class, SHORT_CONVERTER);

        ALL_CONVERTERS.put(Class.class, CLASS_CONVERTER);
        ALL_CONVERTERS.put(InetAddress.class, INET_ADDRESS_CONVERTER);

        ALL_CONVERTERS.put(OptionalInt.class, OPTIONAL_INT_CONVERTER);
        ALL_CONVERTERS.put(OptionalLong.class, OPTIONAL_LONG_CONVERTER);
        ALL_CONVERTERS.put(OptionalDouble.class, OPTIONAL_DOUBLE_CONVERTER);

        ALL_CONVERTERS.put(Character.class, CHARACTER_CONVERTER);

        ALL_CONVERTERS.put(Byte.class, BYTE_CONVERTER);

        ALL_CONVERTERS.put(UUID.class, UUID_CONVERTER);

        ALL_CONVERTERS.put(Currency.class, CURRENCY_CONVERTER);

        ALL_CONVERTERS.put(BitSet.class, BITSET_CONVERTER);

        ALL_CONVERTERS.put(Pattern.class, PATTERN_CONVERTER);

        Map<Class<?>, Class<?>> primitiveTypes = new HashMap<>(9);
        primitiveTypes.put(byte.class, Byte.class);
        primitiveTypes.put(short.class, Short.class);
        primitiveTypes.put(int.class, Integer.class);
        primitiveTypes.put(long.class, Long.class);

        primitiveTypes.put(float.class, Float.class);
        primitiveTypes.put(double.class, Double.class);

        primitiveTypes.put(char.class, Character.class);

        primitiveTypes.put(boolean.class, Boolean.class);

        primitiveTypes.put(void.class, Void.class);
        PRIMITIVE_TYPES = primitiveTypes;
    }

    static Class<?> wrapPrimitiveType(Class<?> primitiveType) {
        assert primitiveType.isPrimitive();
        return PRIMITIVE_TYPES.get(primitiveType);
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
                        throw ConfigMessages.msg.singleTypeConverter(clazz.getName());
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
            throw ConfigMessages.msg.notArrayType(arrayType.toString());
        }
        return new ArrayConverter<>(itemConverter, arrayType);
    }

    /**
     * Get a converter that converts content of type {@code <key1>=<value1>;<key2>=<value2>...} into
     * a {@code Map<K, V>} using the given key and value converters.
     *
     * @param keyConverter the converter used to convert the keys
     * @param valueConverter the converter used to convert the values
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @return the new converter (not {@code null})
     */
    public static <K, V> Converter<Map<K, V>> newMapConverter(Converter<? extends K> keyConverter,
            Converter<? extends V> valueConverter) {
        return new MapConverter<>(keyConverter, valueConverter);
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
     * Get a converter which wraps another converter's result into an {@code OptionalInt}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @return the new converter (not {@code null})
     */
    public static Converter<OptionalInt> newOptionalIntConverter(Converter<Integer> delegateConverter) {
        return new OptionalIntConverter(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code OptionalLong}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @return the new converter (not {@code null})
     */
    public static Converter<OptionalLong> newOptionalLongConverter(Converter<Long> delegateConverter) {
        return new OptionalLongConverter(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code OptionalDouble}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @return the new converter (not {@code null})
     */
    public static Converter<OptionalDouble> newOptionalDoubleConverter(Converter<Double> delegateConverter) {
        return new OptionalDoubleConverter(delegateConverter);
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
     * Get a converter which wraps another converter and handles empty values correctly. This allows the
     * delegate converter to assume that the value being converted will not be {@code null} or empty.
     *
     * @param delegateConverter the converter to delegate to (must not be {@code null})
     * @param <T> the value type
     * @return the converter
     */
    public static <T> Converter<T> newEmptyValueConverter(Converter<T> delegateConverter) {
        return new EmptyValueConverter<>(delegateConverter, null);
    }

    /**
     * Get a converter which trims the string input before passing it on to the delegate converter.
     *
     * @param delegateConverter the converter to delegate to (must not be {@code null})
     * @param <T> the value type
     * @return the converter
     */
    public static <T> Converter<T> newTrimmingConverter(Converter<T> delegateConverter) {
        return new TrimmingConverter<>(delegateConverter);
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
        return new RangeCheckConverter<>(Comparator.naturalOrder(), delegate, minimumValue, inclusive, null, false);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is greater than, or optionally equal to,
     * the given minimum value.
     *
     * @param comparator the comparator to use (must not be {@code null})
     * @param delegate the delegate converter (must not be {@code null})
     * @param minimumValue the minimum value (must not be {@code null})
     * @param inclusive {@code true} if the minimum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     */
    public static <T> Converter<T> minimumValueConverter(Comparator<? super T> comparator, Converter<? extends T> delegate,
            T minimumValue, boolean inclusive) {
        return new RangeCheckConverter<>(comparator, delegate, minimumValue, inclusive, null, false);
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
     * Get a wrapping converter which verifies that the configuration value is greater than, or optionally equal to,
     * the given minimum value (in string form).
     *
     * @param comparator the comparator to use (must not be {@code null})
     * @param delegate the delegate converter (must not be {@code null})
     * @param minimumValue the minimum value (must not be {@code null})
     * @param inclusive {@code true} if the minimum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     * @throws IllegalArgumentException if the given minimum value fails conversion
     */
    public static <T> Converter<T> minimumValueStringConverter(Comparator<? super T> comparator,
            Converter<? extends T> delegate, String minimumValue, boolean inclusive) {
        return minimumValueConverter(comparator, delegate, delegate.convert(minimumValue), inclusive);
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
        return new RangeCheckConverter<>(Comparator.naturalOrder(), delegate, null, false, maximumValue, inclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is less than, or optionally equal to,
     * the given maximum value.
     *
     * @param comparator the comparator to use (must not be {@code null})
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param inclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     */
    public static <T> Converter<T> maximumValueConverter(Comparator<? super T> comparator, Converter<? extends T> delegate,
            T maximumValue, boolean inclusive) {
        return new RangeCheckConverter<>(comparator, delegate, null, false, maximumValue, inclusive);
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
     * Get a wrapping converter which verifies that the configuration value is less than, or optionally equal to,
     * the given maximum value (in string form).
     *
     * @param comparator the comparator to use (must not be {@code null})
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param inclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     * @throws IllegalArgumentException if the given maximum value fails conversion
     */
    public static <T> Converter<T> maximumValueStringConverter(Comparator<? super T> comparator,
            Converter<? extends T> delegate, String maximumValue, boolean inclusive) {
        return maximumValueConverter(comparator, delegate, delegate.convert(maximumValue), inclusive);
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
        return new RangeCheckConverter<>(Comparator.naturalOrder(), delegate, minimumValue, minInclusive, maximumValue,
                maxInclusive);
    }

    /**
     * Get a wrapping converter which verifies that the configuration value is within the given range.
     *
     * @param comparator the comparator to use (must not be {@code null})
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param maxInclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     */
    public static <T> Converter<T> rangeValueConverter(Comparator<? super T> comparator, Converter<? extends T> delegate,
            T minimumValue, boolean minInclusive, T maximumValue, boolean maxInclusive) {
        return new RangeCheckConverter<>(comparator, delegate, minimumValue, minInclusive, maximumValue, maxInclusive);
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
     * Get a wrapping converter which verifies that the configuration value is within the given range (in string form).
     *
     * @param comparator the comparator to use (must not be {@code null})
     * @param delegate the delegate converter (must not be {@code null})
     * @param maximumValue the maximum value (must not be {@code null})
     * @param maxInclusive {@code true} if the maximum value is inclusive, {@code false} otherwise
     * @param <T> the converter target type
     * @return a range-validating converter
     * @throws IllegalArgumentException if the given minimum or maximum value fails conversion
     */
    public static <T> Converter<T> rangeValueStringConverter(Comparator<? super T> comparator, Converter<? extends T> delegate,
            String minimumValue, boolean minInclusive, String maximumValue, boolean maxInclusive) {
        return rangeValueConverter(comparator, delegate, delegate.convert(minimumValue), minInclusive,
                delegate.convert(maximumValue), maxInclusive);
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

    static <T> boolean isOptionalConverter(Converter<T> converter) {
        return converter instanceof Converters.OptionalConverter<?> ||
                converter.equals(Converters.OPTIONAL_INT_CONVERTER) ||
                converter.equals(Converters.OPTIONAL_LONG_CONVERTER) ||
                converter.equals(Converters.OPTIONAL_DOUBLE_CONVERTER);
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
            throw ConfigMessages.msg.valueNotMatchPattern(pattern, value);
        }
    }

    static final class RangeCheckConverter<T> implements Converter<T>, Serializable {

        private static final long serialVersionUID = 2764654140347010865L;

        private final Comparator<? super T> comparator;
        private final Converter<? extends T> delegate;
        private final T min;
        private final boolean minInclusive;
        private final T max;
        private final boolean maxInclusive;

        RangeCheckConverter(final Comparator<? super T> cmp, final Converter<? extends T> delegate, final T min,
                final boolean minInclusive, final T max, final boolean maxInclusive) {
            this.comparator = cmp;
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
                final int cmp = comparator.compare(result, min);
                if (minInclusive) {
                    if (cmp < 0) {
                        throw ConfigMessages.msg.lessThanMinimumValue(min, value);
                    }
                } else {
                    if (cmp <= 0) {
                        throw ConfigMessages.msg.lessThanEqualToMinimumValue(min, value);
                    }
                }
            }
            if (max != null) {
                final int cmp = comparator.compare(result, max);
                if (maxInclusive) {
                    if (cmp > 0) {
                        throw ConfigMessages.msg.greaterThanMaximumValue(max, value);
                    }
                } else {
                    if (cmp >= 0) {
                        throw ConfigMessages.msg.greaterThanEqualToMaximumValue(max, value);
                    }
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        Object readResolve() {
            return comparator != null ? this
                    : new RangeCheckConverter(Comparator.naturalOrder(), delegate, min, minInclusive, max, maxInclusive);
        }
    }

    static final class CollectionConverter<T, C extends Collection<T>> extends AbstractDelegatingConverter<T, C> {
        private static final long serialVersionUID = -8452214026800305628L;

        private final IntFunction<C> collectionFactory;

        CollectionConverter(final Converter<? extends T> delegate, final IntFunction<C> collectionFactory) {
            super(delegate);
            this.collectionFactory = collectionFactory;
        }

        Converter<C> create(final Converter<? extends T> newDelegate) {
            return new CollectionConverter<>(newDelegate, collectionFactory);
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
                    final T item = getDelegate().convert(itemString);
                    if (item != null) {
                        collection.add(item);
                    }
                }
            }
            return collection.isEmpty() ? null : collection;
        }
    }

    static final class ArrayConverter<T, A> extends AbstractDelegatingConverter<T, A> {
        private static final long serialVersionUID = 2630282286159527380L;

        private final Class<A> arrayType;

        ArrayConverter(final Converter<? extends T> delegate, final Class<A> arrayType) {
            super(delegate);
            this.arrayType = arrayType;
        }

        ArrayConverter<T, A> create(final Converter<? extends T> newDelegate) {
            return new ArrayConverter<>(newDelegate, arrayType);
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
                    final T item = getDelegate().convert(itemString);
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
                throw ConfigMessages.msg.unknownArrayType();
            }
        }
    }

    static final class OptionalConverter<T> extends AbstractDelegatingConverter<T, Optional<T>> {
        private static final long serialVersionUID = -4051551570591834428L;

        OptionalConverter(final Converter<? extends T> delegate) {
            super(delegate);
        }

        OptionalConverter<T> create(final Converter<? extends T> newDelegate) {
            return new OptionalConverter<>(newDelegate);
        }

        public Optional<T> convert(final String value) {
            if (value.isEmpty()) {
                try {
                    return Optional.ofNullable(getDelegate().convert(value));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            } else {
                return Optional.ofNullable(getDelegate().convert(value));
            }
        }
    }

    static final class OptionalIntConverter extends AbstractDelegatingConverter<Integer, OptionalInt> {
        private static final long serialVersionUID = 4331039532024222756L;

        OptionalIntConverter(final Converter<? extends Integer> delegate) {
            super(delegate);
        }

        OptionalIntConverter create(final Converter<? extends Integer> newDelegate) {
            return new OptionalIntConverter(newDelegate);
        }

        public OptionalInt convert(final String value) {
            if (value.isEmpty()) {
                return OptionalInt.empty();
            } else {
                final Integer converted = getDelegate().convert(value);
                return converted == null ? OptionalInt.empty() : OptionalInt.of(converted);
            }
        }
    }

    static final class OptionalLongConverter extends AbstractDelegatingConverter<Long, OptionalLong> {
        private static final long serialVersionUID = 140937551800590852L;

        OptionalLongConverter(final Converter<? extends Long> delegate) {
            super(delegate);
        }

        OptionalLongConverter create(final Converter<? extends Long> newDelegate) {
            return new OptionalLongConverter(newDelegate);
        }

        public OptionalLong convert(final String value) {
            if (value.isEmpty()) {
                return OptionalLong.empty();
            } else {
                final Long converted = getDelegate().convert(value);
                return converted == null ? OptionalLong.empty() : OptionalLong.of(converted);
            }
        }
    }

    static final class OptionalDoubleConverter extends AbstractDelegatingConverter<Double, OptionalDouble> {
        private static final long serialVersionUID = -2882741842811044902L;

        OptionalDoubleConverter(final Converter<? extends Double> delegate) {
            super(delegate);
        }

        OptionalDoubleConverter create(final Converter<? extends Double> newDelegate) {
            return new OptionalDoubleConverter(newDelegate);
        }

        public OptionalDouble convert(final String value) {
            if (value.isEmpty()) {
                return OptionalDouble.empty();
            } else {
                final Double converted = getDelegate().convert(value);
                return converted == null ? OptionalDouble.empty() : OptionalDouble.of(converted);
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
                case 12:
                    return SHORT_CONVERTER;
                case 13:
                    return BYTE_CONVERTER;
                case 14:
                    return UUID_CONVERTER;
                case 15:
                    return CURRENCY_CONVERTER;
                case 16:
                    return BITSET_CONVERTER;
                case 17:
                    return PATTERN_CONVERTER;
                default:
                    throw ConfigMessages.msg.unknownConverterId(id);
            }
        }
    }

    static class EmptyValueConverter<T> extends AbstractSimpleDelegatingConverter<T> {
        private static final long serialVersionUID = 5607979836385662739L;

        private final T emptyValue;

        EmptyValueConverter(final Converter<? extends T> delegate, final T emptyValue) {
            super(delegate);
            this.emptyValue = emptyValue;
        }

        protected EmptyValueConverter<T> create(final Converter<? extends T> newDelegate) {
            return new EmptyValueConverter<>(newDelegate, emptyValue);
        }

        public T convert(final String value) {
            if (value.isEmpty()) {
                return emptyValue;
            }
            final T result = getDelegate().convert(value);
            if (result == null) {
                return emptyValue;
            } else {
                return result;
            }
        }
    }

    static class TrimmingConverter<T> extends AbstractSimpleDelegatingConverter<T> {
        private static final long serialVersionUID = 3241445721544473135L;

        TrimmingConverter(final Converter<? extends T> delegate) {
            super(delegate);
        }

        protected TrimmingConverter<T> create(final Converter<? extends T> newDelegate) {
            return new TrimmingConverter<>(newDelegate);
        }

        public T convert(final String value) {
            if (value == null) {
                throw ConfigMessages.msg.converterNullValue();
            }
            return getDelegate().convert(value.trim());
        }
    }

    /**
     * A converter for a Map knowing that the expected format is {@code <key1>=<value1>;<key2>=<value2>...}.
     * <p>
     * The special characters {@code =} and {@code ;} can be used respectively in the key and in the value
     * if they are escaped with a backslash.
     * <p>
     * It will ignore properties whose key contains sub namespaces, in other words if the name of a property
     * contains the special character {@code .} it will be ignored.
     *
     * @param <K> The type of the key
     * @param <V> The type of the value
     */
    static class MapConverter<K, V> extends AbstractConverter<Map<K, V>> {
        private static final long serialVersionUID = 4343545736186221103L;

        /**
         * The converter to use the for keys.
         */
        private final Converter<? extends K> keyConverter;
        /**
         * The converter to use the for values.
         */
        private final Converter<? extends V> valueConverter;

        /**
         * Construct a {@code MapConverter} with the given converters.
         * 
         * @param keyConverter the converter to use the for keys
         * @param valueConverter the converter to use the for values
         */
        MapConverter(Converter<? extends K> keyConverter, Converter<? extends V> valueConverter) {
            this.keyConverter = keyConverter;
            this.valueConverter = valueConverter;
        }

        @Override
        public Map<K, V> convert(String value) throws IllegalArgumentException, NullPointerException {
            if (value == null) {
                return null;
            }
            final Map<K, V> map = new HashMap<>();
            final StringBuilder currentLine = new StringBuilder(value.length());
            int fromIndex = 0;
            for (int idx; (idx = value.indexOf(';', fromIndex)) >= 0; fromIndex = idx + 1) {
                if (value.charAt(idx - 1) == '\\') {
                    // The line separator has been escaped
                    currentLine.append(value, fromIndex, idx + 1);
                    continue;
                }
                processLine(map, value, currentLine.append(value, fromIndex, idx).toString());
                currentLine.delete(0, currentLine.length());
            }
            currentLine.append(value, fromIndex, value.length());
            if (currentLine.length() > 0) {
                processLine(map, value, currentLine.toString());
            }
            return map.isEmpty() ? null : map;
        }

        /**
         * Converts the line into an entry and add it to the given map.
         * 
         * @param map the map to which the extracted entries are added.
         * @param value the original value to convert.
         * @param rawLine the extracted line to convert into an entry.
         * @throws NoSuchElementException if the line could not be converted into an entry or doesn't have the expected format.
         */
        private void processLine(Map<K, V> map, String value, String rawLine) {
            final String line = rawLine.replace("\\;", ";");
            for (int idx, fromIndex = 0; (idx = line.indexOf('=', fromIndex)) >= 0; fromIndex = idx + 1) {
                if (line.charAt(idx - 1) == '\\') {
                    // The key separator has been escaped
                    continue;
                }
                processEntry(map, line.substring(0, idx).replace("\\=", "="), line.substring(idx + 1).replace("\\=", "="));
                return;
            }
            throw ConfigMessages.msg.valueNotMatchMapFormat(value);
        }

        /**
         * Converts the key/value pair into the expected format and add it to the given map. It will ignore
         * keys with sub namespace.
         *
         * @param map the map to which the key/value pair is added.
         * @param key the key of the key/value pair to add to the map
         * @param value the value of the key/value pair to add to the map
         */
        private void processEntry(Map<K, V> map, String key, String value) {
            if (key.indexOf('.') >= 0) {
                // Ignore sub namespaces
                return;
            }
            map.put(keyConverter.convert(key), valueConverter.convert(value));
        }
    }
}
