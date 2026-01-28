package io.smallrye.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config._private.ConfigMessages;

/**
 * {@link Config} provides a way to retrieve configuration values from a configuration name.
 * <p>
 * A {@link Config} instance is obtained via the {@link SmallRyeConfigBuilder#build()}, or by calling the static
 * methods {@link io.smallrye.config.SmallRyeConfig#getOrCreate()}, {@link SmallRyeConfig#getOrCreate(ClassLoader)},
 * {@link io.smallrye.config.SmallRyeConfig#get()} or {@link io.smallrye.config.SmallRyeConfig#get(ClassLoader)}, which
 * details how {@link Config} will behave. Generally, a {@link Config} instance is composed of:
 * <ul>
 * <li>{@linkplain ConfigSource Configuration Sources} to lookup the configuration values</li>
 * <li>{@linkplain Converter Converters} to convert values to specific types</li>
 * <li>{@linkplain ConfigSourceInterceptor Interceptors} to enhance the configuration lookup process</li>
 * <li>{@linkplain ConfigMapping} Config Mappings classes to group multiple configuration values in a common prefix</li>
 * </ul>
 */
public interface Config extends org.eclipse.microprofile.config.Config {
    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for indexed
     * properties. An indexed property uses the original property name with square brackets and an index in between, as
     * {@code my.property[0]}. All indexed properties are queried for their value, which represents a single
     * element in the returning {@code List} converted to their specified property type. The following
     * configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in a {@code List} with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in a {@code List} with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param propertyType The type into which the resolved property values are converted
     * @return the resolved property values as a {@code List} of instances of the property type
     * @param <T> the item type
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, Class, IntFunction)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    @Override
    <T> List<T> getValues(String name, Class<T> propertyType);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for indexed
     * properties. An indexed property uses the original property name with square brackets and an index in between, as
     * {@code my.property[0]}. All indexed properties are queried for their value, which represents a single
     * element in the returning {@code Collection} converted to their specified property type. The following
     * configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in a {@code Collection} with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in a {@code Collection} with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param itemClass The type into which the resolved property values are converted
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Collection} of instances of the property type
     * @param <T> the item type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, Class)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for indexed
     * properties. An indexed property uses the original property name with square brackets and an index in between, as
     * {@code my.property[0]}. All indexed properties are queried for their value, which represents a single
     * element in the returning {@code Collection} converted by the specified
     * {@link org.eclipse.microprofile.config.spi.Converter}. The following configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in a {@code Collection} with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in a {@code Collection} with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the {@link org.eclipse.microprofile.config.spi.Converter} to convert the property type
     * as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param converter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Collection} of instances of the property type
     * @param <T> the item type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, Class)
     * @see Config#getValues(String, Class IntFunction)
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed
     * properties. A keyed property uses the original property name plus an additional dotted segment to represent
     * a {@code Map} key, as {@code my.property.key}, where {@code my.property} is the property name and {@code key}
     * is the {@code Map} key. All keyed properties are queried for their values, which represent a single entry in the
     * returning {@code Map} converting both the key and value to their specified types.
     * The following configuration:
     * <ul>
     * <li>server.reasons.200=OK</li>
     * <li>server.reasons.201=CREATED</li>
     * <li>server.reasons.404=NOT_FOUND</li>
     * </ul>
     * <p>
     * Results in a {@code Map} with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND},
     * considering the configuration name as {@code server.reasons}, the key type as {@code Integer} and the
     * property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} with the backslash ({@code \}) as
     * the escape character.
     * A configuration of {@code server.reasons=200=OK;201=CREATED;404=NOT_FOUND} results in a {@code Map}
     * with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND}, considering the configuration
     * name as {@code server.reasons}, the key type as {@code Integer} and the property type as a {@code String}.
     * <p>
     * The keyed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyClass The type into which the resolved property keys are converted
     * @param valueClass The type into which the resolved property values are converted
     * @return the resolved property values as a {@code Map} of keys of the property name and values of the property
     *         type
     * @param <K> the key type
     * @param <V> the value type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <K, V> Map<K, V> getValues(String name, Class<K> keyClass, Class<V> valueClass);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed
     * properties. A keyed property uses the original property name plus an additional dotted segment to represent
     * a {@code Map} key, as {@code my.property.key}, where {@code my.property} is the property name and {@code key}
     * is the {@code Map} key. All keyed properties are queried for their values, which represent a single entry in the
     * returning {@code Map} converting both the key and value using the specified {@linkplain Converters Converters}.
     * The following configuration:
     * <ul>
     * <li>server.reasons.200=OK</li>
     * <li>server.reasons.201=CREATED</li>
     * <li>server.reasons.404=NOT_FOUND</li>
     * </ul>
     * <p>
     * Results in a {@code Map} with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND},
     * considering the configuration name as {@code server.reasons} and
     * {@linkplain Converters Converters} to convert the key type as an {@code Integer} and the property type as a
     * {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} with the backslash ({@code \}) as
     * the escape character.
     * A configuration of {@code server.reasons=200=OK;201=CREATED;404=NOT_FOUND} results in a {@code Map}
     * with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND}, considering the configuration
     * name as {@code server.reasons} and {@linkplain Converters Converters} to convert the key type as an
     * {@code Integer} and the property type as a {@code String}.
     * <p>
     * The keyed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property keys
     * @param valueConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @return the resolved property values as a {@code Map} of keys of the property name and values of the property
     *         type
     * @param <K> the key type
     * @param <V> the value type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, Class, Class)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed
     * properties. A keyed property uses the original property name plus an additional dotted segment to represent
     * a {@code Map} key, as {@code my.property.key}, where {@code my.property} is the property name and {@code key}
     * is the {@code Map} key. All keyed properties are queried for their values, which represent a single entry in the
     * returning {@code Map} converting both the key and value using the specified {@linkplain Converters Converters}.
     * The following configuration:
     * <ul>
     * <li>server.reasons.200=OK</li>
     * <li>server.reasons.201=CREATED</li>
     * <li>server.reasons.404=NOT_FOUND</li>
     * </ul>
     * <p>
     * Results in a {@code Map} with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND},
     * considering the configuration name as {@code server.reasons} and
     * {@linkplain Converters Converters} to convert the key type as an {@code Integer} and the property type as a
     * {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} with the backslash ({@code \}) as
     * the escape character.
     * A configuration of {@code server.reasons=200=OK;201=CREATED;404=NOT_FOUND} results in a {@code Map}
     * with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND}, considering the configuration
     * name as {@code server.reasons} and {@linkplain Converters Converters} to convert the key type as an
     * {@code Integer} and the property type as a {@code String}.
     * <p>
     * The keyed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property keys
     * @param valueConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @param mapFactory the resulting instance of a {@code Map} to return the property keys and values
     * @return the resolved property values as a {@code Map} of keys of the property name and values of the property
     *         type
     * @param <K> the key type
     * @param <V> the value type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, Class, Class)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <K, V> Map<K, V> getValues(
            String name,
            Converter<K> keyConverter,
            Converter<V> valueConverter,
            IntFunction<Map<K, V>> mapFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed indexed
     * properties. A keyed indexed property uses the original property name plus an additional dotted segment to
     * represent a {@code Map} key followed by square brackets and an index in between, as {@code my.property.key[0]},
     * where {@code my.property} is the property name, {@code key} is the {@code Map} key and {code [0]} the index of
     * the {@code Collection} element. All keyed indexed properties are queried for their value, which represent
     * a single entry in the returning {@code Map}, and single element in the {@code Collection} value, converting both
     * the key and value to their specified types. The following configuration:
     * <ul>
     * <li>server.env.prod[0]=alpha</li>
     * <li>server.env.prod[1]=beta</li>
     * <li>server.env.dev[0]=local</li>
     * </ul>
     * <p>
     * Results in a {@code Map} with the entry key {@code prod} and entry value {@code Collection} with the values
     * {@code alpha} and {@code beta}, and the entry key {@code dev} and entry value {@code Collection} with the value
     * {@code local}, considering the configuration name as {@code server.env}, the key type as a {@code String}, and
     * the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} and value as a comma-separated
     * string ({@code ,}) can represent, and split into multiple elements with the backslash ({@code \}) as the
     * escape character. A configuration of {@code server.env=prod=alpha,beta;dev=local} results in a {@code Map} with
     * the entry key {@code prod} and entry value {@code Collection} with the values {@code alpha} and {@code beta},
     * and the entry key {@code dev} and entry value {@code Collection} with the value {@code local}, considering the
     * configuration name as {@code server.env}, the key type as a {@code String}, and the property type as a
     * {@code String}.
     * <p>
     * The keyed indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyClass The type into which the resolved property keys are converted
     * @param valueClass The type into which the resolved property values are converted
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Map} of keys of the property name and values as a
     *         {@code Collections} of instances of the property type
     * @param <K> the key type
     * @param <V> the value type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     * @see Config#getOptionalValues(String, Class, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     */
    <K, V, C extends Collection<V>> Map<K, C> getValues(
            String name,
            Class<K> keyClass,
            Class<V> valueClass,
            IntFunction<C> collectionFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed indexed
     * properties. A keyed indexed property uses the original property name plus an additional dotted segment to
     * represent a {@code Map} key followed by square brackets and an index in between, as {@code my.property.key[0]},
     * where {@code my.property} is the property name, {@code key} is the {@code Map} key and {code [0]} the index of
     * the {@code Collection} element. All keyed indexed properties are queried for their value, which represent
     * a single entry in the returning {@code Map}, and single element in the {@code Collection} value, converting both
     * the key and value using the specified {@linkplain Converters Converters}. The following configuration:
     * <ul>
     * <li>server.env.prod[0]=alpha</li>
     * <li>server.env.prod[1]=beta</li>
     * <li>server.env.dev[0]=local</li>
     * </ul>
     * <p>
     * Results in a {@code Map} with the entry key {@code prod} and entry value {@code Collection} with the values
     * {@code alpha} and {@code beta}, and the entry key {@code dev} and entry value {@code Collection} with the value
     * {@code local}, considering the configuration name as {@code server.env}
     * and {@linkplain Converters Converters} to convert the key type as a {@code String} and the property type as a
     * {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} and value as a comma-separated
     * string ({@code ,}) can represent, and split into multiple elements with the backslash ({@code \}) as the
     * escape character. A configuration of {@code server.env=prod=alpha,beta;dev=local} results in a {@code Map} with
     * the entry key {@code prod} and entry value {@code Collection} with the values {@code alpha} and {@code beta},
     * and the entry key {@code dev} and entry value {@code Collection} with the value {@code local}, considering the
     * configuration name as {@code server.env}, and {@linkplain Converters Converters} to convert the key type as a
     * {@code String} and the property type as a {@code String}.
     * <p>
     * The keyed indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property keys
     * @param valueConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @param mapFactory the resulting instance of a {@code Map} to return the property keys and values
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Map} of keys of the property name and values as a
     *         {@code Collections} of instances of the property type
     * @param <K> the key type
     * @param <V> the value type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     * @see Config#getOptionalValues(String, Class, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     */
    <K, V, C extends Collection<V>> Map<K, C> getValues(
            String name,
            Converter<K> keyConverter,
            Converter<V> valueConverter,
            IntFunction<Map<K, C>> mapFactory,
            IntFunction<C> collectionFactory);

    /**
     * Returns the value for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * If the requested type is a type array, the lookup to the configuration will first query
     * {@link Config#getPropertyNames()} for indexed properties. An indexed property uses the original property
     * name with square brackets and an index in between, as {@code my.property[0]}. All indexed properties are
     * queried for their value, which represents a single element in the returning type array converted to their
     * specified property type. The following configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in an array with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in an array with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param propertyType The type into which the resolved property value is converted
     * @return the resolved property value as an instance of the property type
     * @param <T> the property type
     *
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValue(String, org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getOptionalValue(String, Class)
     * @see Config#getOptionalValue(String, org.eclipse.microprofile.config.spi.Converter)
     */
    @Override
    <T> T getValue(String name, Class<T> propertyType);

    /**
     * Returns the value for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * If the requested type is a type array, the lookup to the configuration will first query
     * {@link Config#getPropertyNames()} for indexed properties. An indexed property uses the original property
     * name with square brackets and an index in between, as {@code my.property[0]}. All indexed properties are
     * queried for their value, which represents a single element in the returning type array converted by the
     * specified {@link org.eclipse.microprofile.config.spi.Converter}. The following configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in an array with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and
     * the {@link org.eclipse.microprofile.config.spi.Converter} to convert the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in an array with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the {@link org.eclipse.microprofile.config.spi.Converter} to convert the property type
     * as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param converter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property value
     * @return the resolved property value as an instance of the converter type
     * @param <T> the property type
     *
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getValue(String, Class)
     * @see Config#getOptionalValue(String, Class)
     * @see Config#getOptionalValue(String, org.eclipse.microprofile.config.spi.Converter)
     */
    <T> T getValue(String name, Converter<T> converter);

    /**
     * Returns the value for the specified {@link ConfigValue}, converting the value using the specified
     * {@link org.eclipse.microprofile.config.spi.Converter}.
     *
     * @param configValue a {@link ConfigValue} with the value to convert
     * @param converter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property value
     * @return the resolved property value as an instance of the converter type
     * @param <T> the property type
     */
    <T> T convertValue(ConfigValue configValue, Converter<T> converter);

    /**
     * Returns the {@link ConfigValue} for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup of the configuration is performed immediately, meaning that calls to {@link ConfigValue} will always
     * yield the same results.
     * <p>
     * A {@link ConfigValue} is always returned even if a property name cannot be found. In this case, every method in
     * {@link ConfigValue} returns {@code null}, or the default value for primitive types, except for
     * {@link ConfigValue#getName()}, which includes the original property name being looked up.
     *
     * @param name The configuration property name to look for in the configuration
     * @return the resolved property value as a {@link ConfigValue}
     */
    @Override
    ConfigValue getConfigValue(String name);

    /**
     * Returns the value for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * If the requested type is a type array, the lookup to the configuration will first query
     * {@link Config#getPropertyNames()} for indexed properties. An indexed property uses the original property
     * name with square brackets and an index in between, as {@code my.property[0]}. All indexed properties are
     * queried for their value, which represents a single element in the returning type array converted to their
     * specified property type. The following configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in an array with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in an array with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param propertyType The type into which the resolved property value is converted
     * @return the resolved property value as a {@code Optional} instance of the property type
     * @param <T> the property type
     *
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     *
     * @see Config#getOptionalValue(String, org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getValue(String, Class)
     * @see Config#getValue(String, org.eclipse.microprofile.config.spi.Converter)
     */
    @Override
    <T> Optional<T> getOptionalValue(String name, Class<T> propertyType);

    /**
     * Returns the value for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * If the requested type is a type array, the lookup to the configuration will first query
     * {@link Config#getPropertyNames()} for indexed properties. An indexed property uses the original property
     * name with square brackets and an index in between, as {@code my.property[0]}. All indexed properties are
     * queried for their value, which represents a single element in the returning type array converted by the
     * specified {@link org.eclipse.microprofile.config.spi.Converter}. The following configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in an array with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and
     * the {@link org.eclipse.microprofile.config.spi.Converter} to convert the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in an array with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the {@link org.eclipse.microprofile.config.spi.Converter} to convert the property type
     * as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param converter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property value
     * @return the resolved property value as a {@code Optional} instance of the property type
     * @param <T> the property type
     *
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     *
     * @see Config#getValue(String, Class)
     * @see Config#getOptionalValue(String, Class)
     * @see Config#getOptionalValue(String, org.eclipse.microprofile.config.spi.Converter)
     */
    <T> Optional<T> getOptionalValue(String name, Converter<T> converter);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for indexed
     * properties. An indexed property uses the original property name with square brackets and an index in between, as
     * {@code my.property[0]}. All indexed properties are queried for their value, which represents a single
     * element in the returning {@code Optional<List>} converted to their specified property type. The following
     * configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in an {@code Optional<List>} with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in a {@code Optional<List>} with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param propertyType The type into which the resolved property values are converted
     * @return the resolved property values as a {@code Optional<List>} of instances of the property type
     * @param <T> the item type
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     *
     * @see Config#getOptionalValues(String, Class, IntFunction)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getValues(String, Class)
     * @see Config#getValues(String, Class, IntFunction)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    @Override
    <T> Optional<List<T>> getOptionalValues(String name, Class<T> propertyType);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for indexed
     * properties. An indexed property uses the original property name with square brackets and an index in between, as
     * {@code my.property[0]}. All indexed properties are queried for their value, which represents a single
     * element in the returning {@code Optional<Collection>} converted to their specified property type. The following
     * configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Collection>} with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in a {@code Optional<Collection>} with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param itemClass The type into which the resolved property values are converted
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Optional<Collection>} of instances of the property type
     * @param <T> the item type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getValues(String, Class)
     * @see Config#getValues(String, Class, IntFunction)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <T, C extends Collection<T>> Optional<C> getOptionalValues(
            String name,
            Class<T> itemClass,
            IntFunction<C> collectionFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for indexed
     * properties. An indexed property uses the original property name with square brackets and an index in between, as
     * {@code my.property[0]}. All indexed properties are queried for their value, which represents a single
     * element in the returning {@code Optional<Collection>} converted by the
     * specified {@link org.eclipse.microprofile.config.spi.Converter}. The following configuration:
     * <ul>
     * <li>my.property[0]=dog</li>
     * <li>my.property[1]=cat</li>
     * <li>my.property[2]=turtle</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Collection>} with the elements {@code dog}, {@code cat}, and {@code turtle},
     * considering the configuration name as {@code my.property} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element that a comma-separated string ({@code ,}) can represent,
     * and split into multiple elements with the backslash ({@code \}) as the escape character.
     * A configuration of {@code my.property=dog,cat,turtle} results in a {@code Optional<Collection>} with the elements
     * {@code dog}, {@code cat}, and {@code turtle}, considering the configuration name as
     * {@code my.property} and the property type as a {@code String}.
     * <p>
     * The indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param converter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Optional<Collection>} of instances of the property type
     * @param <T> the item type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property values cannot be converted to the specified type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getOptionalValues(String, Class)
     * @see Config#getOptionalValues(String, Class IntFunction)
     * @see Config#getValues(String, Class)
     * @see Config#getValues(String, Class, IntFunction)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <T, C extends Collection<T>> Optional<C> getOptionalValues(
            String name,
            Converter<T> converter,
            IntFunction<C> collectionFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed
     * properties. A keyed property uses the original property name plus an additional dotted segment to represent
     * a {@code Map} key, as {@code my.property.key}, where {@code my.property} is the property name and {@code key}
     * is the {@code Map} key. All keyed properties are queried for their values, which represent a single entry in the
     * returning {@code Optional<Map>} converting both the key and value to their specified types.
     * The following configuration:
     * <ul>
     * <li>server.reasons.200=OK</li>
     * <li>server.reasons.201=CREATED</li>
     * <li>server.reasons.404=NOT_FOUND</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Map>} with the entries {@code 200=OK}, {@code 201=CREATED}, and
     * {@code 404=NOT_FOUND}, considering the configuration name as {@code server.reasons}, the key type as
     * {@code Integer} and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} with the backslash ({@code \}) as
     * the escape character.
     * A configuration of {@code server.reasons=200=OK;201=CREATED;404=NOT_FOUND} results in a {@code Optional<Map>}
     * with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND}, considering the configuration
     * name as {@code server.reasons}, the key type as {@code Integer} and the property type as a {@code String}.
     * <p>
     * The keyed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyClass The type into which the resolved property keys are converted
     * @param valueClass The type into which the resolved property values are converted
     * @return the resolved property values as a {@code Optional<Map>} of keys of the property name and values of the
     *         property type
     * @param <K> the key type
     * @param <V> the value type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getValues(String, Class, Class)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <K, V> Optional<Map<K, V>> getOptionalValues(
            String name,
            Class<K> keyClass,
            Class<V> valueClass);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed
     * properties. A keyed property uses the original property name plus an additional dotted segment to represent
     * a {@code Map} key, as {@code my.property.key}, where {@code my.property} is the property name and {@code key}
     * is the {@code Map} key. All keyed properties are queried for their values, which represent a single entry in the
     * returning {@code Optional<Map>} converting both the key and value using the specified
     * {@linkplain Converters Converters}. The following configuration:
     * <ul>
     * <li>server.reasons.200=OK</li>
     * <li>server.reasons.201=CREATED</li>
     * <li>server.reasons.404=NOT_FOUND</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Map>} with the entries {@code 200=OK}, {@code 201=CREATED}, and
     * {@code 404=NOT_FOUND}, considering the configuration name as {@code server.reasons} and
     * {@linkplain Converters Converters} to convert the key type as an {@code Integer} and the property type as a
     * {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} with the backslash ({@code \}) as
     * the escape character.
     * A configuration of {@code server.reasons=200=OK;201=CREATED;404=NOT_FOUND} results in a {@code Optional<Map>}
     * with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND}, considering the configuration
     * name as {@code server.reasons} and {@linkplain Converters Converters} to convert the key type as an
     * {@code Integer} and the property type as a {@code String}.
     * <p>
     * The keyed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property keys
     * @param valueConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @return the resolved property values as a {@code Optional<Map>} of keys of the property name and values of the
     *         property type
     * @param <K> the key type
     * @param <V> the value type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getOptionalValues(String, Class, Class)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     * @see Config#getValues(String, Class, Class)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <K, V> Optional<Map<K, V>> getOptionalValues(
            String name,
            Converter<K> keyConverter,
            Converter<V> valueConverter);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed
     * properties. A keyed property uses the original property name plus an additional dotted segment to represent
     * a {@code Map} key, as {@code my.property.key}, where {@code my.property} is the property name and {@code key}
     * is the {@code Map} key. All keyed properties are queried for their values, which represent a single entry in the
     * returning {@code Optional<Map>} converting both the key and value using the specified
     * {@linkplain Converters Converters}. The following configuration:
     * <ul>
     * <li>server.reasons.200=OK</li>
     * <li>server.reasons.201=CREATED</li>
     * <li>server.reasons.404=NOT_FOUND</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Map>} with the entries {@code 200=OK}, {@code 201=CREATED}, and
     * {@code 404=NOT_FOUND}, considering the configuration name as {@code server.reasons} and
     * {@linkplain Converters Converters} to convert the key type as an {@code Integer} and the property type as a
     * {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} with the backslash ({@code \}) as
     * the escape character.
     * A configuration of {@code server.reasons=200=OK;201=CREATED;404=NOT_FOUND} results in a {@code Optional<Map>}
     * with the entries {@code 200=OK}, {@code 201=CREATED}, and {@code 404=NOT_FOUND}, considering the configuration
     * name as {@code server.reasons} and {@linkplain Converters Converters} to convert the key type as an
     * {@code Integer} and the property type as a {@code String}.
     * <p>
     * The keyed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property keys
     * @param valueConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @param mapFactory the resulting instance of a {@code Map} to return the property keys and values
     * @return the resolved property values as a {@code Optional<Map>} of keys of the property name and values of the
     *         property type
     * @param <K> the key type
     * @param <V> the value type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration or is defined as
     *         an empty string, or the converter returns {@code null}
     *
     * @see Config#getOptionalValues(String, Class, Class)
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getValues(String, Class, Class)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction)
     */
    <K, V> Optional<Map<K, V>> getOptionalValues(
            String name,
            Converter<K> keyConverter,
            Converter<V> valueConverter,
            IntFunction<Map<K, V>> mapFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed indexed
     * properties. A keyed indexed property uses the original property name plus an additional dotted segment to
     * represent a {@code Map} key followed by square brackets and an index in between, as {@code my.property.key[0]},
     * where {@code my.property} is the property name, {@code key} is the {@code Map} key and {code [0]} the index of
     * the {@code Collection} element. All keyed indexed properties are queried for their value, which represent
     * a single entry in the returning {@code Optional<Map>}, and single element in the {@code Collection} value,
     * converting both the key and value to their specified types. The following configuration:
     * <ul>
     * <li>server.env.prod[0]=alpha</li>
     * <li>server.env.prod[1]=beta</li>
     * <li>server.env.dev[0]=local</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Map>} with the entry key {@code prod} and entry value {@code Collection} with the
     * values {@code alpha} and {@code beta}, and the entry key {@code dev} and entry value {@code Collection} with the
     * value {@code local}, considering the configuration name as {@code server.env}, the key type as a {@code String},
     * and the property type as a {@code String}.
     * <p>
     * Otherwise, the configuration value is a single element represented by key value pairs as
     * {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} and value as a comma-separated
     * string ({@code ,}) can represent, and split into multiple elements with the backslash ({@code \}) as the
     * escape character. A configuration of {@code server.env=prod=alpha,beta;dev=local} results in a
     * {@code Optional<Map>} with the entry key {@code prod} and entry value {@code Collection} with the values
     * {@code alpha} and {@code beta}, and the entry key {@code dev} and entry value {@code Collection} with the value
     * {@code local}, considering the configuration name as {@code server.env}, the key type as a {@code String}, and
     * the property type as a {@code String}.
     * <p>
     * The keyed indexed property format has priority when both styles are found in the same configuration source. When
     * available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyClass The type into which the resolved property keys are converted
     * @param valueClass The type into which the resolved property values are converted
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Optional<Map>} of keys of the property name and values as a
     *         {@code Collections} of instances of the property type
     * @param <K> the key type
     * @param <V> the value type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified type
     *
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     * @see Config#getValues(String, Class, Class, IntFunction)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     */
    <K, V, C extends Collection<V>> Optional<Map<K, C>> getOptionalValues(
            String name,
            Class<K> keyClass,
            Class<V> valueClass,
            IntFunction<C> collectionFactory);

    /**
     * Returns the values for the specified configuration name from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * The lookup to the configuration will first query {@link Config#getPropertyNames()} for keyed indexed
     * properties. A keyed indexed property uses the original property name plus an additional dotted segment to
     * represent a {@code Map} key followed by square brackets and an index in between, as {@code my.property.key[0]},
     * where {@code my.property} is the property name, {@code key} is the {@code Map} key and {code [0]} the index of
     * the {@code Collection} element. All keyed indexed properties are queried for their value, which represent
     * a single entry in the returning {@code Optional<Map>}, and single element in the {@code Collection} value,
     * converting both the key and value using the specified {@linkplain Converters Converters}. The following
     * configuration:
     * <ul>
     * <li>server.env.prod[0]=alpha</li>
     * <li>server.env.prod[1]=beta</li>
     * <li>server.env.dev[0]=local</li>
     * </ul>
     * <p>
     * Results in a {@code Optional<Map>} with the entry key {@code prod} and entry value {@code Collection} with the
     * values {@code alpha} and {@code beta}, and the entry key {@code dev} and entry value {@code Collection} with the
     * value {@code local}, considering the configuration name as {@code server.env} and
     * {@linkplain Converters Converters} to convert the key type as a {@code String} and the property type as a {
     *
     * @code String}.
     *       <p>
     *       Otherwise, the configuration value is a single element represented by key value pairs as
     *       {@code <key1>=<value1>;<key2>=<value2>...} separated by a semicolon {@code ;} and value as a comma-separated
     *       string ({@code ,}) can represent, and split into multiple elements with the backslash ({@code \}) as the
     *       escape character. A configuration of {@code server.env=prod=alpha,beta;dev=local} results in a
     *       {@code Optional<Map>} with the entry key {@code prod} and entry value {@code Collection} with the values
     *       {@code alpha} and {@code beta}, and the entry key {@code dev} and entry value {@code Collection} with the value
     *       {@code local}, considering the configuration name as {@code server.env},
     *       nd {@linkplain Converters Converters} to convert the key type as a {@code String} and the property type as a
     *       {@code String}.
     *       <p>
     *       The keyed indexed property format has priority when both styles are found in the same configuration source. When
     *       available in multiple sources, the higher ordinal source wins, like any other configuration lookup.
     *
     * @param name The configuration property name to look for in the configuration
     * @param keyConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property keys
     * @param valueConverter The {@link org.eclipse.microprofile.config.spi.Converter} to use to convert the resolved
     *        property values
     * @param mapFactory the resulting instance of a {@code Map} to return the property keys and values
     * @param collectionFactory the resulting instance of a {@code Collection} to return the property values
     * @return the resolved property values as a {@code Optional<Map>} of keys of the property name and values as a
     *         {@code Collections} of instances of the property type
     * @param <K> the key type
     * @param <V> the value type
     * @param <C> the collection type
     * @throws IllegalArgumentException if the property keys or values cannot be converted to the specified
     *         type
     *
     * @see Config#getOptionalValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     * @see Config#getValues(String, Class, Class, IntFunction)
     * @see Config#getValues(String, org.eclipse.microprofile.config.spi.Converter,
     *      org.eclipse.microprofile.config.spi.Converter, IntFunction, IntFunction)
     */
    <K, V, C extends Collection<V>> Optional<Map<K, C>> getOptionalValues(
            String name,
            Converter<K> keyConverter,
            Converter<V> valueConverter,
            IntFunction<Map<K, C>> mapFactory,
            IntFunction<C> collectionFactory);

    /**
     * Returns an instance of a {@link ConfigMapping} annotated type, mapping all the configuration names matching the
     * {@link ConfigMapping#prefix()} and the {@link ConfigMapping} members to values from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * {@linkplain ConfigMapping ConfigMapping} instances are cached. They are populated when the
     * {@link SmallRyeConfig} instance is initialized and their values are not updated on
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources} changes.
     *
     * @param type an interface annotated with {@link ConfigMapping}
     * @return an instance of a {@link ConfigMapping} annotated type
     * @param <T> the type of the {@link ConfigMapping}
     * @throws ConfigValidationException if the mapping names or values cannot be converter to the specified types, if
     *         the properties values are not present, defined as an empty string, or the conversion returns {@code null}
     *
     * @see SmallRyeConfig#getConfigMapping(Class, String)
     */
    <T> T getConfigMapping(Class<T> type);

    /**
     * Returns an instance of a {@link ConfigMapping} annotated type, mapping all the configuration names matching the
     * prefix and the {@link ConfigMapping} members to values from the underlying
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}.
     * <p>
     * {@linkplain ConfigMapping ConfigMapping} instances are cached. They are populated when the
     * {@link SmallRyeConfig} instance is initialized and their values are not updated on
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources} changes.
     *
     * @param type an interface annotated with {@link ConfigMapping}
     * @param prefix the prefix to override {@link ConfigMapping#prefix()}
     * @return an instance of a {@link ConfigMapping} annotated type
     * @param <T> the type of the {@link ConfigMapping}
     * @throws ConfigValidationException if the mapping names or values cannot be converter to the specified types, if
     *         the properties values are not present, defined as an empty string, or the conversion returns {@code null}
     *
     * @see SmallRyeConfig#getConfigMapping(Class)
     */
    <T> T getConfigMapping(Class<T> type, String prefix);

    /**
     * {@inheritDoc}
     *
     * This implementation caches the list of property names collected when {@link SmallRyeConfig} is built via
     * {@link SmallRyeConfigBuilder#build()}. The cache may be disabled with
     * {@link SmallRyeConfigBuilder#isCachePropertyNames()}.
     *
     * @return the cached names of all configured keys of the underlying configuration
     * @see SmallRyeConfig#getLatestPropertyNames()
     * @see SmallRyeConfigBuilder#isCachePropertyNames()
     */
    @Override
    Iterable<String> getPropertyNames();

    /**
     * Provides a way to retrieve an updated list of all property names. The updated list replaces the cached list
     * returned by {@link SmallRyeConfig#getPropertyNames()}.
     *
     * @return the names of all configured keys of the underlying configuration
     */
    Iterable<String> getLatestPropertyNames();

    /**
     * Checks if a property is present in the {@link Config} instance.
     * <br>
     * Because {@link org.eclipse.microprofile.config.spi.ConfigSource#getPropertyNames()} may not include all
     * available properties, it is not possible to reliably determine if the property is present in the properties
     * list. The property needs to be retrieved to make sure it exists. The lookup is done without expression
     * expansion, because the expansion value may not be available, and it is not relevant for the final check.
     *
     * @param name the property name.
     * @return true if the property is present or false otherwise.
     */
    boolean isPropertyPresent(String name);

    /**
     * {@inheritDoc}
     */
    @Override
    Iterable<ConfigSource> getConfigSources();

    /**
     * Return the currently registered {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}
     * in {@link SmallRyeConfig} that match the specified type.
     * <p>
     * The returned sources will be sorted by descending ordinal value and name, which can be iterated in a thread-safe
     * manner. The {@link Iterable} contains a fixed number of
     * {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}, determined at
     * configuration initialization, and the config sources themselves may be static or dynamic.
     *
     * @param type The type of the {@link org.eclipse.microprofile.config.spi.ConfigSource} to look for in
     *        the configuration
     * @return an {@link Iterable} of {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}
     */
    Iterable<ConfigSource> getConfigSources(Class<?> type);

    /**
     * Return the first registered {@linkplain org.eclipse.microprofile.config.spi.ConfigSource configuration sources}
     * in {@link SmallRyeConfig} that match the specified name, sorted by descending ordinal value and name.
     * <p>
     *
     * @param name the {{@linkplain org.eclipse.microprofile.config.spi.ConfigSource} name to look for in
     *        the configuration
     * @return an {@link Optional} of a {@linkplain org.eclipse.microprofile.config.spi.ConfigSource}, or an
     *         empty {@link Optional} if no {@linkplain org.eclipse.microprofile.config.spi.ConfigSource} matches the
     *         specified name.
     */
    Optional<ConfigSource> getConfigSource(String name);

    /**
     * Return the {@link org.eclipse.microprofile.config.spi.Converter} used by this instance to produce instances of
     * the specified type from {@code String} values.
     *
     * @param asType the type to be produced by the converter
     * @return an instance of the {@link org.eclipse.microprofile.config.spi.Converter} the specified type
     * @param <T> the conversion type
     * @throws IllegalArgumentException if no {@link org.eclipse.microprofile.config.spi.Converter} is registered for
     *         the specified type
     */
    <T> Converter<T> requireConverter(Class<T> asType);

    /**
     * Returns a {@code List} of the active profiles in {@link SmallRyeConfig}.
     * <p>
     * Profiles are sorted in reverse order according to how they were set in
     * {@link SmallRyeConfig#SMALLRYE_CONFIG_PROFILE}, as the last profile overrides the previous one until there are
     * no profiles left in the list.
     *
     * @return a {@code List} of the active profiles
     */
    List<String> getProfiles();

    /**
     * Get a {@link SmallRyeConfig} instance for the current {@link Thread#getContextClassLoader()}.
     *
     * @return the {@link SmallRyeConfig} instance for the thread context class loader
     * @throws IllegalArgumentException if the config is not registered for the thread context class loader
     * @throws IllegalArgumentException if the registered config is not a {@link SmallRyeConfig}
     */
    static Config get() {
        if (ConfigProviderResolver.instance() instanceof SmallRyeConfigProviderResolver resolver) {
            return resolver.get();
        }
        throw ConfigMessages.msg.incompatibleConfigProvider(
                SmallRyeConfigProviderResolver.class.getName(), ConfigProviderResolver.instance().getClass().getName());
    }

    /**
     * Get or Create a {@link SmallRyeConfig} instance for the current {@link Thread#getContextClassLoader()}.
     * <p>
     * The {@link SmallRyeConfig} instance will be created and registered to the context classloader if no such
     * configuration is already created and registered.
     *
     * @return the {@link SmallRyeConfig} instance for the thread context class loader
     * @throws IllegalArgumentException if the registered config is not a {@link SmallRyeConfig}
     */
    static Config getOrCreate() {
        return ConfigProviderResolver.instance().getConfig().unwrap(SmallRyeConfig.class);
    }

    /**
     * Get a {@link SmallRyeConfig} instance for the specified {@link ClassLoader}.
     *
     * @return the {@link SmallRyeConfig} instance for the specified class loader
     * @throws IllegalArgumentException if the config is not registered for the thread context class loader
     * @throws IllegalArgumentException if the registered config is not a {@link SmallRyeConfig}
     */
    static Config get(ClassLoader classLoader) {
        if (ConfigProviderResolver.instance() instanceof SmallRyeConfigProviderResolver resolver) {
            return resolver.get(classLoader);
        }
        throw ConfigMessages.msg.incompatibleConfigProvider(
                SmallRyeConfigProviderResolver.class.getName(), ConfigProviderResolver.instance().getClass().getName());
    }

    /**
     * Get or Create a {@link SmallRyeConfig} instance for the specified {@link ClassLoader}.
     * <p>
     * The {@link SmallRyeConfig} instance will be created and registered to the specified class loader if no such
     * configuration is already created and registered.
     *
     * @return the {@link SmallRyeConfig} instance for the specified class loader
     * @throws IllegalArgumentException if the registered config is not a {@link SmallRyeConfig}
     */
    static Config getOrCreate(ClassLoader classLoader) {
        return ConfigProviderResolver.instance().getConfig(classLoader).unwrap(SmallRyeConfig.class);
    }
}
