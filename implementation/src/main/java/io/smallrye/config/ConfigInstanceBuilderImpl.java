package io.smallrye.config;

import static io.smallrye.config._private.ConfigMessages.msg;
import static java.util.Collections.emptyMap;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingLoader.ConfigClassImplementation;
import sun.reflect.ReflectionFactory;

/**
 * The implementation for configuration instance builders.
 */
final class ConfigInstanceBuilderImpl<I> implements ConfigInstanceBuilder<I> {
    /**
     * The converters to use for a given configuration interface, resolved from the interface's {@link ClassLoader} so
     * that application-provided converters are visible. Keyed on the interface {@link Class}, the computed value lives in
     * the {@code Class}'s {@code classValueMap} and is collected together with the interface and its loader.
     */
    private static final ClassValue<Map<Type, Converter<?>>> CONVERTERS = new ClassValue<>() {
        @Override
        protected Map<Type, Converter<?>> computeValue(final Class<?> interfaceType) {
            return convertersForLoader(interfaceType.getClassLoader());
        }
    };

    /**
     * The converters discovered for each {@link ClassLoader}, so that all configuration interfaces loaded by the same
     * loader share a single set of converters (and a single {@link java.util.ServiceLoader} discovery pass).
     * <p>
     * The map is keyed weakly by the loader and holds the converters through a {@link WeakReference}, so it never keeps a
     * loader (nor its application-provided converters, which strongly reference it) from being collected. The strong
     * reference that keeps a live loader's converters around is held by {@link #CONVERTERS}, whose values live in the
     * {@code classValueMap} of the interface {@link Class} and are therefore collected together with the loader.
     */
    private static final Map<ClassLoader, WeakReference<Map<Type, Converter<?>>>> CONVERTERS_BY_LOADER = new WeakHashMap<>();

    private static synchronized Map<Type, Converter<?>> convertersForLoader(final ClassLoader classLoader) {
        WeakReference<Map<Type, Converter<?>>> reference = CONVERTERS_BY_LOADER.get(classLoader);
        if (reference != null) {
            Map<Type, Converter<?>> converters = reference.get();
            if (converters != null) {
                return converters;
            }
        }
        Map<Type, Converter<?>> converters = Converters.loadConverters(classLoader, emptyMap());
        CONVERTERS_BY_LOADER.put(classLoader, new WeakReference<>(converters));
        return converters;
    }

    static <I> ConfigInstanceBuilderImpl<I> forInterface(Class<I> interfaceType) throws IllegalArgumentException {
        Assert.checkNotNullParam("interfaceType", interfaceType);
        if (!interfaceType.isInterface() || interfaceType.getTypeParameters().length != 0
                || interfaceType.getName().startsWith("java")
                || Secret.class.isAssignableFrom(interfaceType)
                || interfaceType.equals(ConfigMappingClass.Mapper.class)) {
            throw msg.invalidConfigurationInterface(interfaceType.getName());
        }

        return new ConfigInstanceBuilderImpl<>(interfaceType, new HashMap<>());
    }

    private final Class<I> configurationInterface;
    private final Map<String, Object> values;
    private final Map<Type, Converter<?>> converters;
    private final ConfigClassImplementation implementation;

    ConfigInstanceBuilderImpl(final Class<I> interfaceType, final Map<String, Object> values) {
        this.configurationInterface = interfaceType;
        this.values = values;
        this.converters = CONVERTERS.get(interfaceType);

        Handlers.register(interfaceType, Handlers.find(interfaceType));
        this.implementation = ConfigClassImplementation.get(interfaceType);
    }

    public Class<I> configurationInterface() {
        return configurationInterface;
    }

    public <T, F extends Function<? super I, T> & Serializable> ConfigInstanceBuilder<I> with(final F getter, final T value) {
        Assert.checkNotNullParam("getter", getter);
        Assert.checkNotNullParam("value", value);
        values.put(getPropertyName(getter), value);
        return this;
    }

    public ConfigInstanceBuilder<I> with(final ToIntFunctionGetter<I> getter, final int value) {
        Assert.checkNotNullParam("getter", getter);
        values.put(getPropertyName(getter), value);
        return this;
    }

    public ConfigInstanceBuilder<I> with(final ToLongFunctionGetter<I> getter, final long value) {
        Assert.checkNotNullParam("getter", getter);
        values.put(getPropertyName(getter), value);
        return this;
    }

    public ConfigInstanceBuilder<I> with(final ToDoubleFunctionGetter<I> getter, final double value) {
        Assert.checkNotNullParam("getter", getter);
        values.put(getPropertyName(getter), value);
        return this;
    }

    public <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> with(final F getter, final boolean value) {
        Assert.checkNotNullParam("getter", getter);
        values.put(getPropertyName(getter), value);
        return this;
    }

    public I build() {
        return configurationInterface.cast(implementation.newInstance(values, converters));
    }

    /**
     * Extract the configuration property name from a serializable getter method reference.
     * <p>
     * The getter is deserialized into a {@link SerializedLambda} to recover the implemented method name, which is the
     * property name. The reference must be a zero-argument method reference to an instance method on the configuration
     * interface (kind {@link MethodHandleInfo#REF_invokeInterface} or {@link MethodHandleInfo#REF_invokeVirtual}); any
     * other shape (a lambda body, a bound reference, or a non-serializable target) is rejected with
     * {@link io.smallrye.config._private.ConfigMessages#invalidGetter()}.
     * <p>
     * The {@link SerializedLambda} is obtained via {@link sun.reflect.ReflectionFactory#writeReplaceForSerialization}
     * rather than by reflecting the lambda's {@code private writeReplace} method: that method belongs to the caller's
     * module, so {@link java.lang.reflect.Method#setAccessible} or {@link java.lang.invoke.MethodHandles#privateLookupIn}
     * fail for callers in a named module that does not {@code open} its package. {@code ReflectionFactory} uses the
     * same privileged access as the serialization runtime and works flag-free for both classpath and modular callers.
     * <p>
     * A future alternative that avoids {@code sun.reflect} is a proxy-recorder: apply the getter to a
     * {@link java.lang.reflect.Proxy} of the interface that records the invoked method. It uses only public API but
     * <em>executes</em> the getter, so a getter invoking no (or several) methods would be silently misidentified rather
     * than rejected.
     * <p>
     * The result is intentionally not cached: a builder is typically configured once, so each getter is extracted only
     * once and a cache would have nothing to hit. One may be added later if repeated extraction proves costly.
     *
     * @param getter the serializable getter method reference (must not be {@code null})
     * @return the configuration property name
     * @throws IllegalArgumentException if the getter is not a valid property accessor
     */
    private static String getPropertyName(final Object getter) {
        MethodHandle writeReplace = ReflectionFactory.getReflectionFactory()
                .writeReplaceForSerialization(getter.getClass());
        if (writeReplace == null) {
            throw msg.invalidGetter();
        }
        Object replaced;
        try {
            replaced = writeReplace.invoke(getter);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
        if (!(replaced instanceof SerializedLambda sl)) {
            throw msg.invalidGetter();
        }
        if (sl.getCapturedArgCount() != 0) {
            throw msg.invalidGetter();
        }
        // A property accessor must be a method reference to an instance method on the configuration interface
        int kind = sl.getImplMethodKind();
        if (kind != MethodHandleInfo.REF_invokeInterface && kind != MethodHandleInfo.REF_invokeVirtual) {
            throw msg.invalidGetter();
        }
        return sl.getImplMethodName();
    }
}
