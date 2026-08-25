package io.smallrye.config;

import static io.smallrye.config._private.ConfigMessages.msg;

import java.io.Serial;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingLoader.ConfigClassImplementation;
import io.smallrye.config.Converters.ConverterWithPriority;
import io.smallrye.config.Converters.Implicit;
import io.smallrye.config._private.ConfigMessages;
import sun.reflect.ReflectionFactory;

/**
 * The implementation for configuration instance builders.
 */
final class ConfigInstanceBuilderImpl<I> implements ConfigInstanceBuilder<I> {
    private static final Map<Object, String> NAME_CACHE = new ConcurrentHashMap<>();

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

    static final Map<Type, Converter<?>> CONVERTERS = new ConcurrentHashMap<>();

    static {
        registerConverters();
    }

    private static void registerConverters() {
        Map<Type, ConverterWithPriority> convertersToBuild = new HashMap<>();
        for (Converter<?> converter : ServiceLoader.load(Converter.class, ConfigInstanceBuilderImpl.class.getClassLoader())) {
            Type type = Converters.getConverterType(converter.getClass());
            if (type == null) {
                throw ConfigMessages.msg.unableToAddConverter(converter);
            }
            Converters.addConverter(type, converter, convertersToBuild);
        }

        CONVERTERS.putAll(Converters.ALL_CONVERTERS);
        CONVERTERS.put(ConfigValue.class, Converters.CONFIG_VALUE_CONVERTER);
        for (Entry<Type, ConverterWithPriority> entry : convertersToBuild.entrySet()) {
            CONVERTERS.put(entry.getKey(), entry.getValue().converter());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Converter<T> getConverter(Class<T> type) {
        Converter<?> exactConverter = CONVERTERS.get(type);
        if (exactConverter != null) {
            return (Converter<T>) exactConverter;
        }
        if (type.isPrimitive()) {
            return (Converter<T>) getConverter(Converters.wrapPrimitiveType(type));
        }
        if (type.isArray()) {
            Converter<?> conv = getConverter(type.getComponentType());
            if (conv != null) {
                return Converters.newArrayConverter(conv, type);
            }
            throw ConfigMessages.msg.noRegisteredConverter(type);
        }

        Converter<T> converter = Implicit.getConverter(type);
        if (converter == null) {
            throw ConfigMessages.msg.noRegisteredConverter(type);
        }
        return converter;
    }

    @SuppressWarnings("unused")
    public static <T> T requireValue(final String name, final T value) {
        if (value == null) {
            throw msg.propertyNotSet(name);
        }
        return value;
    }

    public static class MapWithDefault<K, V> extends HashMap<K, V> {
        @Serial
        private static final long serialVersionUID = 1390928078837140814L;
        private final V defaultValue;

        MapWithDefault(final V defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public V get(final Object key) {
            return getOrDefault(key, defaultValue);
        }
    }
}
