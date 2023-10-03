package io.smallrye.config;

import static io.smallrye.config._private.ConfigMessages.msg;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
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
            String interfaceName = type.getName();
            MethodHandles.Lookup lookup;
            try {
                lookup = MethodHandles.privateLookupIn(type, myLookup);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            String implInternalName = interfaceName.replace('.', '/') + "$$SC_BuilderImpl";
            Class<?> impl;
            try {
                impl = lookup.findClass(implInternalName);
            } catch (ClassNotFoundException e) {
                // generate the impl instead
                throw new UnsupportedOperationException("Todo");
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
                    return (ConfigInstanceBuilderImpl<?>) mh.invokeExact();
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
        protected Function<Object, ?> computeValue(final Class<?> type) {
            assert type.isInterface();
            String interfaceName = type.getName();
            MethodHandles.Lookup lookup;
            try {
                lookup = MethodHandles.privateLookupIn(type, myLookup);
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            String implInternalName = interfaceName.replace('.', '/') + "$$SC_BuilderImpl";
            Class<?> impl;
            try {
                impl = lookup.findClass(implInternalName);
            } catch (ClassNotFoundException e) {
                // generate the impl instead
                throw new UnsupportedOperationException("Todo");
            } catch (IllegalAccessException e) {
                throw msg.accessDenied(getClass(), type);
            }
            MethodHandle mh;
            Class<?> builderClass = null;
            if (true)
                throw new UnsupportedOperationException("Not finished yet...");
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
                    return type.cast(mh.invokeExact(builder));
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
        return new ConfigInstanceBuilderImpl<I>(configurationInterface, builderFactories.get(configurationInterface).get());
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

    public <F extends ToIntFunction<? super I> & Serializable> ConfigInstanceBuilder<I> with(final F getter, final int value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, Integer.valueOf(value));
        return this;
    }

    public <F extends ToLongFunction<? super I> & Serializable> ConfigInstanceBuilder<I> with(final F getter,
            final long value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, Long.valueOf(value));
        return this;
    }

    public <F extends ToLongFunction<? super I> & Serializable> ConfigInstanceBuilder<I> with(final F getter,
            final double value) {
        Assert.checkNotNullParam("getter", getter);
        Class<?> callerClass = sw.getCallerClass();
        BiConsumer<Object, Object> setter = getSetter(getter, callerClass);
        setter.accept(builderObject, Double.valueOf(value));
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
            replaced = writeReplace.invokeExact(lambda);
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
        String implClassName = sl.getImplClass();
        // TODO: check implClassName against the supertype hierarchy of the config interface using shared info mapping
        String setterName = sl.getImplMethodName();
        Class<?> type = parseReturnType(sl.getImplMethodSignature());
        return createSetterByName(setterName, type);
    }

    private BiConsumer<Object, Object> createSetterByName(final String setterName, final Class<?> type) {
        Class<?> builderClass = builderObject.getClass();
        MethodHandle setter;
        try {
            setter = lookup.findVirtual(builderClass, setterName, MethodType.methodType(void.class, builderClass, type));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw msg.accessDenied(getClass(), builderClass);
        }
        // adapt it to be an object consumer
        MethodHandle castSetter = setter.asType(MethodType.methodType(void.class, builderClass, Object.class));
        return (builder, val) -> {
            try {
                castSetter.invokeExact(builderObject, builder, val);
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
        switch (desc.charAt(start)) {
            case 'L': {
                return parseClassName(desc, start + 1, end - 1);
            }
            case '[': {
                return parseType(desc, start + 1, end).arrayType();
            }
            case 'B': {
                return byte.class;
            }
            case 'C': {
                return char.class;
            }
            case 'D': {
                return double.class;
            }
            case 'F': {
                return float.class;
            }
            case 'I': {
                return int.class;
            }
            case 'J': {
                return long.class;
            }
            case 'S': {
                return short.class;
            }
            case 'Z': {
                return boolean.class;
            }
            default: {
                throw msg.invalidGetter();
            }
        }
    }

    private Class<?> parseClassName(final String signature, final int start, final int end) {
        try {
            return lookup.findClass(signature.substring(start, end));
        } catch (ClassNotFoundException e) {
            throw msg.invalidGetter();
        } catch (IllegalAccessException e) {
            throw msg.accessDenied(getClass(), builderObject.getClass());
        }
    }
}
