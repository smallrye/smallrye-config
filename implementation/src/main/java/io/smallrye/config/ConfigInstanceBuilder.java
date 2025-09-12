package io.smallrye.config;

import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A builder which can produce instances of a configuration interface annotated with {@link ConfigMapping}.
 * <p>
 * Objects which are produced by this API will contain values for every property found on the configuration
 * interface or its supertypes.
 * If no value is given for a property, its default value is used.
 * If a required property has no default value, then an exception will be thrown when {@link #build} is called.
 * The returned object instance is immutable and has a stable {@code equals} and {@code hashCode} method.
 * If the runtime is Java 16 or later, the returned object <em>may</em> be a {@code Record}.
 * <p>
 * To provide a value for a property, use a method reference to indicate which property the value should be associated
 * with. For example,
 *
 * <pre>
    <code>

    &#064;ConfigMapping
    interface MyProgramConfig {
        String message();
        int repeatCount();
    }

    ConfigInstanceBuilder&lt;MyProgramConfig&gt; builder = ConfigInstanceBuilder.forInterface(MyProgramConfig.class);
    builder.with(MyProgramConfig::message, "Hello everyone!");
    builder.with(MyProgramConfig::repeatCount, 42);

    MyProgramConfig config = builder.build();
    for (int i = 0; i < config.repeatCount(); i ++) {
        System.out.println(config.message());
    }
    </code>
 * </pre>
 *
 * Configuration interface member types are automatically converted with a {@link Converter}. Global converters are
 * registered either by being discovered via the {@link java.util.ServiceLoader} mechanism, and can be
 * registered by providing a {@code META-INF/services/org.eclipse.microprofile.config.spi.Converter} file, which
 * contains the fully qualified class name of the custom {@code Converter} implementation, or explicitly by calling
 * {@link ConfigInstanceBuilder#registerConverter(Class, Converter)}.
 * <p>
 * Converters follow the same rules applied to {@link io.smallrye.config.SmallRyeConfig} and
 * {@link io.smallrye.config.ConfigMapping}, including overriding the converter to use with
 * {@link io.smallrye.config.WithConverter}.
 *
 * @param <I> the configuration interface type
 *
 * @see io.smallrye.config.ConfigMapping
 * @see org.eclipse.microprofile.config.spi.Converter
 */
public interface ConfigInstanceBuilder<I> {
    /**
     * {@return the configuration interface (not <code>null</code>)}
     */
    Class<I> configurationInterface();

    /**
     * Set a property on the configuration object to an object value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @param <T> the value type
     * @param <F> the accessor type
     * @throws IllegalArgumentException if the getter is {@code null}
     *         or if the value is {@code null}
     */
    <T, F extends Function<? super I, T> & Serializable> ConfigInstanceBuilder<I> with(F getter, T value);

    /**
     * Set a property on the configuration object to an integer value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    ConfigInstanceBuilder<I> with(ToIntFunctionGetter<I> getter, int value);

    /**
     * Set a property on the configuration object to a long value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    ConfigInstanceBuilder<I> with(ToLongFunctionGetter<I> getter, long value);

    /**
     * Set a property on the configuration object to a floating-point value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    ConfigInstanceBuilder<I> with(ToDoubleFunctionGetter<I> getter, double value);

    /**
     * Set a property on the configuration object to a boolean value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @param <F> the accessor type
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> with(F getter, boolean value);

    /**
     * Set an optional property on the configuration object to an object value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @param <T> the value type
     * @param <F> the accessor type
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     *         or the value is {@code null}
     */
    default <T, F extends Function<? super I, Optional<T>> & Serializable> ConfigInstanceBuilder<I> withOptional(F getter,
            T value) {
        return with(getter, Optional.of(value));
    }

    /**
     * Set an optional property on the configuration object to an integer value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    default ConfigInstanceBuilder<I> withOptional(OptionalIntGetter<I> getter, int value) {
        return with(getter, OptionalInt.of(value));
    }

    /**
     * Set an optional property on the configuration object to an integer value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    default ConfigInstanceBuilder<I> withOptional(OptionalLongGetter<I> getter, long value) {
        return with(getter, OptionalLong.of(value));
    }

    /**
     * Set an optional property on the configuration object to a floating-point value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    default ConfigInstanceBuilder<I> withOptional(OptionalDoubleGetter<I> getter, double value) {
        return with(getter, OptionalDouble.of(value));
    }

    /**
     * Set an optional property on the configuration object to a boolean value.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @param <F> the accessor type
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    default <F extends Function<? super I, Optional<Boolean>> & Serializable> ConfigInstanceBuilder<I> withOptional(F getter,
            boolean value) {
        return with(getter, Optional.of(value));
    }

    /**
     * Build the configuration instance.
     *
     * @return the configuration instance (not {@code null})
     * @throws IllegalArgumentException if a required property does not have a value
     */
    I build();

    /**
     * Get a builder instance for the given configuration interface.
     *
     * @param interfaceClass the interface class object (must not be {@code null})
     * @param <I> the configuration interface type
     * @return a new builder for the configuration interface (not {@code null})
     * @throws IllegalArgumentException if the interface class is {@code null},
     *         or if the class object does not represent an interface,
     *         or if the interface is not a valid configuration interface,
     *         or if the interface has one or more required properties that were not given a value,
     *         or if the interface has one or more converters that could not be instantiated
     * @throws SecurityException if this class does not have permission to introspect the given interface
     *         or one of its superinterfaces
     */
    static <I> ConfigInstanceBuilder<I> forInterface(Class<I> interfaceClass)
            throws IllegalArgumentException, SecurityException {
        return ConfigInstanceBuilderImpl.forInterface(interfaceClass);
    }

    /**
     * Globally registers a {@link org.eclipse.microprofile.config.spi.Converter} to be used by the
     * {@link io.smallrye.config.ConfigInstanceBuilder} to convert configuration interface member types.
     *
     * @param type the class of the type to convert
     * @param converter the converter instance that can convert to the type
     * @param <T> the type to convert
     */
    static <T> void registerConverter(Class<T> type, Converter<T> converter) {
        ConfigInstanceBuilderImpl.CONVERTERS.put(type, converter);
    }

    /**
     * Represents a getter in the configuration interface of primitive type {@code int}.
     *
     * @param <T> the configuration interface type
     */
    interface ToIntFunctionGetter<T> extends ToIntFunction<T>, Serializable {
    }

    /**
     * Represents a getter in the configuration interface of primitive type {@code long}.
     *
     * @param <T> the configuration interface type
     */
    interface ToLongFunctionGetter<T> extends ToLongFunction<T>, Serializable {
    }

    /**
     * Represents a getter in the configuration interface of primitive type {@code double}.
     *
     * @param <T> the configuration interface type
     */
    interface ToDoubleFunctionGetter<T> extends ToDoubleFunction<T>, Serializable {
    }

    /**
     * Represents a getter in the configuration interface of type {@code OptionalInt}.
     *
     * @param <T> the configuration interface type
     */
    interface OptionalIntGetter<T> extends Function<T, OptionalInt>, Serializable {
    }

    /**
     * Represents a getter in the configuration interface of type {@code OptionalLong}.
     *
     * @param <T> the configuration interface type
     */
    interface OptionalLongGetter<T> extends Function<T, OptionalLong>, Serializable {
    }

    /**
     * Represents a getter in the configuration interface of type {@code OptionalDouble}.
     *
     * @param <T> the configuration interface type
     */
    interface OptionalDoubleGetter<T> extends Function<T, OptionalDouble>, Serializable {
    }
}
