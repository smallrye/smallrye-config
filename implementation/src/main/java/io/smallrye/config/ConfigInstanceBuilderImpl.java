package io.smallrye.config;

import static io.smallrye.config._private.ConfigMessages.msg;
import static java.util.Collections.emptyMap;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    static Map<Type, Converter<?>> CONVERTERS = Converters
            .loadConverters(ConfigInstanceBuilderImpl.class.getClassLoader(), emptyMap());

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
    private final ConfigClassImplementation implementation;

    ConfigInstanceBuilderImpl(final Class<I> interfaceType, final Map<String, Object> values) {
        this.configurationInterface = interfaceType;
        this.values = values;

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
        return configurationInterface.cast(implementation.newInstance(values));
    }

    private static final Map<Object, String> NAME_CACHE = new ConcurrentHashMap<>();

    private static String getPropertyName(final Object getter) {
        return NAME_CACHE.computeIfAbsent(getter, lambda -> {
            MethodHandle writeReplace = ReflectionFactory.getReflectionFactory()
                    .writeReplaceForSerialization(lambda.getClass());
            if (writeReplace == null) {
                throw msg.invalidGetter();
            }
            Object replaced;
            try {
                replaced = writeReplace.invoke(lambda);
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
            return sl.getImplMethodName();
        });
    }
}
