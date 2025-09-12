package io.smallrye.config;

import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newOptionalConverter;
import static io.smallrye.config._private.ConfigMessages.msg;

import java.io.Serial;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.Converters.Implicit;
import io.smallrye.config.SmallRyeConfigBuilder.ConverterWithPriority;
import io.smallrye.config._private.ConfigMessages;
import sun.reflect.ReflectionFactory;

/**
 * The implementation for configuration instance builders.
 */
final class ConfigInstanceBuilderImpl<I> implements ConfigInstanceBuilder<I> {

    /**
     * Reflection factory, used for getting the serialized lambda information out of a getter reference.
     */
    private static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
    /**
     * Stack walker for getting caller class, used for setter caching.
     */
    private static final StackWalker sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    /**
     * Our cached lookup object.
     */
    private static final MethodHandles.Lookup myLookup = MethodHandles.lookup();
    /**
     * Class value which holds the cached builder class instance.
     */
    private static final ClassValue<Supplier<?>> builderFactories = new ClassValue<>() {
        protected Supplier<?> computeValue(final Class<?> type) {
            assert type.isInterface();
            // TODO - Should we cache this eagerly in io.smallrye.config.ConfigMappingLoader.ConfigMappingImplementation?
            MethodHandles.Lookup lookup;
            try {
                lookup = MethodHandles.privateLookupIn(type, myLookup);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            Class<?> impl;
            try {
                ConfigMappingLoader.ensureLoaded(type);
                impl = lookup.findClass(ConfigMappingInterface.ConfigMappingBuilder.getBuilderClassName(type));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            MethodHandle mh;
            try {
                mh = lookup.findConstructor(impl, MethodType.methodType(void.class));
            } catch (NoSuchMethodException e) {
                throw msg.noConstructor(impl);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), impl);
            }
            // capture the constructor as a Supplier
            return () -> {
                try {
                    return mh.invoke();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new UndeclaredThrowableException(e);
                }
            };
        }
    };

    /**
     * Class value which holds the cached config class instance constructors.
     */
    private static final ClassValue<Function<Object, ?>> configFactories = new ClassValue<>() {
        // TODO - This is to load the mapping class implementation, which we already have, just missing the right constructor in the ConfigMappingLoader, so we can probably remove this one
        protected Function<Object, ?> computeValue(final Class<?> type) {
            assert type.isInterface();
            // TODO - Should we cache this eagerly in io.smallrye.config.ConfigMappingLoader.ConfigMappingImplementation?
            MethodHandles.Lookup lookup;
            try {
                lookup = MethodHandles.privateLookupIn(type, myLookup);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            Class<?> impl;
            Class<?> builderClass;
            try {
                impl = ConfigMappingLoader.ensureLoaded(type).implementation();
                builderClass = lookup.findClass(ConfigMappingInterface.ConfigMappingBuilder.getBuilderClassName(type));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            MethodHandle mh;

            try {
                mh = lookup.findConstructor(impl, MethodType.methodType(void.class, builderClass));
            } catch (NoSuchMethodException e) {
                throw msg.noConstructor(impl);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), impl);
            }
            // capture the constructor as a Function
            return builder -> {
                try {
                    return mh.invoke(builder);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new UndeclaredThrowableException(e);
                }
            };
        }
    };

    /**
     * Class value that holds the cache of maps of method reference lambdas to their corresponding setter.
     */
    private static final ClassValue<Map<Object, BiConsumer<Object, Object>>> setterMapsByCallingClass = new ClassValue<>() {
        protected Map<Object, BiConsumer<Object, Object>> computeValue(final Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    // =====================================

    static <I> ConfigInstanceBuilderImpl<I> forInterface(Class<I> configurationInterface)
            throws IllegalArgumentException, SecurityException {
        return new ConfigInstanceBuilderImpl<>(configurationInterface, builderFactories.get(configurationInterface).get());
    }

    // =====================================

    private final Class<I> configurationInterface;
    private final MethodHandles.Lookup lookup;
    private final Object builderObject;

    ConfigInstanceBuilderImpl(final Class<I> configurationInterface, final Object builderObject) {
        this.configurationInterface = configurationInterface;
        try {
            lookup = MethodHandles.privateLookupIn(builderObject.getClass(), myLookup);
        } catch (IllegalAccessException e) {
            throw msg.accessDenied(builderObject.getClass(), getClass());
        }
        this.builderObject = builderObject;
    }

    // =====================================

    public Class<I> configurationInterface() {
        return configurationInterface;
    }

    // -------------------------------------

    public <T, F extends Function<? super I, T> & Serializable> ConfigInstanceBuilder<I> with(final F getter, final T value) {
        Assert.checkNotNullParam("getter", getter);
        Assert.checkNotNullParam("value", value);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, value);
        return this;
    }

    public ConfigInstanceBuilder<I> with(final ToIntFunctionGetter<I> getter, final int value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, Integer.valueOf(value));
        return this;
    }

    public ConfigInstanceBuilder<I> with(final ToLongFunctionGetter<I> getter, final long value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, Long.valueOf(value));
        return this;
    }

    public ConfigInstanceBuilder<I> with(final ToDoubleFunctionGetter<I> getter, final double value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, value);
        return this;
    }

    public <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> with(final F getter, final boolean value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, Boolean.valueOf(value));
        return this;
    }

    // -------------------------------------

    public <T, F extends Function<? super I, T> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(final F getter) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        Consumer<Object> resetter = getResetter(getter, callerClass);
        resetter.accept(builderObject);
        return this;
    }

    public <F extends ToIntFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(final F getter) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        Consumer<Object> resetter = getResetter(getter, callerClass);
        resetter.accept(builderObject);
        return this;
    }

    public <F extends ToLongFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(final F getter) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        Consumer<Object> resetter = getResetter(getter, callerClass);
        resetter.accept(builderObject);
        return this;
    }

    public <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> withDefaultFor(final F getter) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        Consumer<Object> resetter = getResetter(getter, callerClass);
        resetter.accept(builderObject);
        return this;
    }

    // -------------------------------------

    public <F extends Function<? super I, ?> & Serializable> ConfigInstanceBuilder<I> withString(final F getter,
            final String value) {
        return withString(getter, value, sw.getCallerClass());
    }

    public <F extends ToIntFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withString(final F getter,
            final String value) {
        return withString(getter, value, sw.getCallerClass());
    }

    public <F extends ToLongFunction<? super I> & Serializable> ConfigInstanceBuilder<I> withString(final F getter,
            final String value) {
        return withString(getter, value, sw.getCallerClass());
    }

    public <F extends Predicate<? super I> & Serializable> ConfigInstanceBuilder<I> withString(final F getter,
            final String value) {
        return withString(getter, value, sw.getCallerClass());
    }

    private ConfigInstanceBuilderImpl<I> withString(final Object getter, final String value, final Class<?> callerClass) {
        Assert.checkNotNullParam("getter", getter);
        Assert.checkNotNullParam("value", value);
        Converter<?> converter = getConverter(getter, callerClass);
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, converter.convert(value));
        return this;
    }

    // -------------------------------------

    public ConfigInstanceBuilder<I> withString(final Class<? super I> propertyClass, final String propertyName,
            final String value) {
        throw new UnsupportedOperationException("Need class info registry");
    }

    // -------------------------------------

    public I build() {
        return configurationInterface.cast(configFactories.get(configurationInterface).apply(builderObject));
    }

    // =====================================

    static final Map<Type, Converter<?>> CONVERTERS = new ConcurrentHashMap<>();

    static {
        registerConverters();
    }

    private static void registerConverters() {
        Map<Type, SmallRyeConfigBuilder.ConverterWithPriority> convertersToBuild = new HashMap<>();

        // TODO - We need to register this for Native in Quarkus - Also, we are doubling the work because SR Config also does the registration
        for (Converter<?> converter : ServiceLoader.load(Converter.class, SecuritySupport.getContextClassLoader())) {
            Type type = Converters.getConverterType(converter.getClass());
            if (type == null) {
                throw ConfigMessages.msg.unableToAddConverter(converter);
            }
            SmallRyeConfigBuilder.addConverter(type, converter, convertersToBuild);
        }

        CONVERTERS.putAll(Converters.ALL_CONVERTERS);
        CONVERTERS.put(ConfigValue.class, Converters.CONFIG_VALUE_CONVERTER);
        for (Entry<Type, ConverterWithPriority> entry : convertersToBuild.entrySet()) {
            CONVERTERS.put(entry.getKey(), entry.getValue().getConverter());
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

    public static <T> T convertValue(final String value, final Converter<T> converter) {
        T convert = converter.convert(value);
        if (convert == null) {
            // TODO - new messsage instead of reuse?
            throw ConfigMessages.msg.converterReturnedNull("", value, converter.getClass().getTypeName());
        }
        return convert;
    }

    public static <T> Optional<T> convertOptionalValue(final String value, final Converter<T> converter) {
        return convertValue(value, Converters.newOptionalConverter(converter));
    }

    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> C convertValues(
            final String value,
            final Converter<T> converter,
            final Class<C> collectionType) {
        return (C) convertValue(value, newCollectionConverter(converter, createCollectionFactory(collectionType)));
    }

    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> Optional<C> convertOptionalValues(
            final String value,
            final Converter<T> converter,
            final Class<C> collectionType) {
        Converter<Collection<T>> collectionConverter = newCollectionConverter(converter,
                createCollectionFactory(collectionType));
        return (Optional<C>) newOptionalConverter(collectionConverter).convert(value);
    }

    public static <T> T requireValue(final T value, final String name) {
        if (value == null) {
            throw msg.propertyNotSet(name);
        }
        return value;
    }

    // TODO - Duplicated from ConfigMappingContext
    static <T, C extends Collection<T>> IntFunction<? extends Collection<T>> createCollectionFactory(
            final Class<C> type) {
        if (type.equals(List.class)) {
            return ArrayList::new;
        }

        if (type.equals(Set.class)) {
            return HashSet::new;
        }

        throw new IllegalArgumentException();
    }

    // TODO - Duplicated from ConfigMappingContext
    static class MapWithDefault<K, V> extends HashMap<K, V> {
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

    private Converter<?> getConverter(final Object getter, final Class<?> callerClass) {
        throw new UnsupportedOperationException("Need class info registry");
    }

    private BiConsumer<Object, Object> getSetter(final Object getter, final Class<?> callerClass) {
        Map<Object, BiConsumer<Object, Object>> setterMap = setterMapsByCallingClass.get(callerClass);
        BiConsumer<Object, Object> setter = setterMap.get(getter);
        if (setter == null) {
            setter = setterMap.computeIfAbsent(getter, this::createSetter);
        }
        return setter;
    }

    private BiConsumer<Object, Object> createSetter(Object lambda) {
        MethodHandle writeReplace = rf.writeReplaceForSerialization(lambda.getClass());
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
        if (!(replaced instanceof SerializedLambda)) {
            throw msg.invalidGetter();
        }
        SerializedLambda sl = (SerializedLambda) replaced;
        if (sl.getCapturedArgCount() != 0) {
            throw msg.invalidGetter();
        }
        // TODO: check implClassName against the supertype hierarchy of the config interface using shared info mapping
        String setterName = sl.getImplMethodName();
        Class<?> type = parseReturnType(sl.getImplMethodSignature());
        return createSetterByName(setterName, type);
    }

    private BiConsumer<Object, Object> createSetterByName(final String setterName, final Class<?> type) {
        Class<?> builderClass = builderObject.getClass();
        MethodHandle setter;
        try {
            setter = lookup.findVirtual(builderClass, setterName, MethodType.methodType(void.class, type));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw msg.accessDenied(getClass(), builderClass);
        }
        // adapt it to be an object consumer
        MethodHandle castSetter = setter.asType(MethodType.methodType(void.class, builderClass, Object.class));
        return (builder, val) -> {
            try {
                castSetter.invoke(builderObject, val);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        };
    }

    private Consumer<Object> getResetter(final Object getter, final Class<?> callerClass) {
        throw new UnsupportedOperationException("Unsupported for now");
    }

    private Class<?> parseReturnType(final String signature) {
        int idx = signature.lastIndexOf(')');
        if (idx == -1) {
            throw new IllegalStateException("Unexpected invalid signature");
        }
        return parseType(signature, idx + 1, signature.length());
    }

    private Class<?> parseType(String desc, int start, int end) {
        return switch (desc.charAt(start)) {
            case 'L' -> parseClassName(desc, start + 1, end - 1);
            case '[' -> parseType(desc, start + 1, end).arrayType();
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'D' -> double.class;
            case 'F' -> float.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'S' -> short.class;
            case 'Z' -> boolean.class;
            default -> throw msg.invalidGetter();
        };
    }

    private Class<?> parseClassName(final String signature, final int start, final int end) {
        try {
            return lookup.findClass(signature.substring(start, end).replaceAll("/", "."));
        } catch (ClassNotFoundException e) {
            throw msg.invalidGetter();
        } catch (IllegalAccessException e) {
            throw msg.accessDenied(getClass(), builderObject.getClass());
        }
    }
}
