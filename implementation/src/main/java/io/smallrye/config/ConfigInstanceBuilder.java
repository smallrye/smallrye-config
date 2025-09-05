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
 * A builder which can produce instances of a configuration interface.
 * <p>
 * Objects which are produced by this API will contain values for every property found on the configuration
 * interface or its supertypes.
 * If no value is given for a property, its default value is used.
 * If a required property has no default value, then an exception will be thrown when {@link #build} is called.
 * The returned object instance is immutable and has a stable {@code equals} and {@code hashCode} method.
 * If the runtime is Java 16 or later, the returned object <em>may</em> be a {@code Record}.
 * <p>
 * To provide a value for a property, use a method reference to indicate which property the value should be associated
 * with.
 * For example,
 *
 * <pre>
<code>
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
 * @param <I> the configuration interface type
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
        return with(getter, Optional.of(Boolean.valueOf(value)));
    }

    /**
     * Set a property to its default value (if any).
     *
     * @param getter the property to modify (must not be {@code null})
     * @param <T> the value type
     * @param <F> the accessor type
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    <T, F extends Function<? super I, T> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(F getter);

    /**
     * Set a property to its default value (if any).
     *
     * @param getter the property to modify (must not be {@code null})
     * @param <F> the accessor type
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    <F extends ToIntFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(F getter);

    /**
     * Set a property to its default value (if any).
     *
     * @param getter the property to modify (must not be {@code null})
     * @param <F> the accessor type
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    <F extends ToLongFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(F getter);

    /**
     * Set a property to its default value (if any).
     *
     * @param getter the property to modify (must not be {@code null})
     * @param <F> the accessor type
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the getter is {@code null}
     */
    <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(F getter);

    /**
     * Set a property on the configuration object to a string value.
     * The value set on the property will be the result of conversion of the string
     * using the property's converter.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @param <F> the accessor type
     * @throws IllegalArgumentException if the getter is {@code null},
     *         or if the value is {@code null},
     *         or if the value was rejected by the converter
     */
    <F extends Function<? super I, ?> & Serializable> ConfigInstanceBuilder<I> withString(F getter, String value);

    /**
     * Set a property on the configuration object to a string value.
     * The value set on the property will be the result of conversion of the string
     * using the property's converter.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @param <F> the accessor type
     * @throws IllegalArgumentException if the getter is {@code null},
     *         or if the value is {@code null},
     *         or if the value was rejected by the converter
     */
    <F extends ToIntFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withString(F getter, String value);

    /**
     * Set a property on the configuration object to a string value.
     * The value set on the property will be the result of conversion of the string
     * using the property's converter.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @param <F> the accessor type
     * @throws IllegalArgumentException if the getter is {@code null},
     *         or if the value is {@code null},
     *         or if the value was rejected by the converter
     */
    <F extends ToLongFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withString(F getter, String value);

    /**
     * Set a property on the configuration object to a string value.
     * The value set on the property will be the result of conversion of the string
     * using the property's converter.
     *
     * @param getter the property accessor (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @param <F> the accessor type
     * @throws IllegalArgumentException if the getter is {@code null},
     *         or if the value is {@code null},
     *         or if the value was rejected by the converter
     */
    <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> withString(F getter, String value);

    /**
     * Set a property on the configuration object to a string value, using the property's
     * declaring class and name to identify the property to set.
     * The value set on the property will be the result of conversion of the string
     * using the property's converter.
     *
     * @param propertyClass the declaring class of the property to set (must not be {@code null})
     * @param propertyName the name of the property to set (must not be {@code null})
     * @param value the value to set (must not be {@code null})
     * @return this builder (not {@code null})
     * @throws IllegalArgumentException if the property class or name is {@code null},
     *         or if the value is {@code null},
     *         or if the value was rejected by the converter,
     *         or if no property matches the given name and declaring class
     */
    ConfigInstanceBuilder<I> withString(Class<? super I> propertyClass, String propertyName, String value);

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

    static <T> void registerConverter(Class<T> type, Converter<T> converter) {
        ConfigInstanceBuilderImpl.CONVERTERS.put(type, converter);
    }

    interface ToIntFunctionGetter<T> extends ToIntFunction<T>, Serializable {
    }

    interface ToLongFunctionGetter<T> extends ToLongFunction<T>, Serializable {
    }

    interface ToDoubleFunctionGetter<T> extends ToDoubleFunction<T>, Serializable {
    }

    interface OptionalIntGetter<T> extends Function<T, OptionalInt>, Serializable {
    }

    interface OptionalLongGetter<T> extends Function<T, OptionalLong>, Serializable {
    }

    interface OptionalDoubleGetter<T> extends Function<T, OptionalDouble>, Serializable {
    }
}
