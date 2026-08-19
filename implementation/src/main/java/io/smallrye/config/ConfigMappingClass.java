package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.loadClass;
import static java.util.Collections.synchronizedMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;

/**
 * Represents a concrete configuration class (as opposed to a {@code @ConfigMapping} interface).
 * <p>
 * When a user-defined class (a POJO with a no-arg constructor) is registered for configuration mapping,
 * this class introspects its fields, generates a {@link ConfigMappingInterface} compatible interface via bytecode
 * generation.
 *
 * @see ConfigMappingInterface
 * @see ConfigMappingLoader.GeneratedConfigClass
 * @see ConfigMappingGenerator#generate(Class, String, ConfigMappingHandler)
 */
public final class ConfigMappingClass implements GeneratedConfigClass {

    /**
     * Bridge between a generated configuration interface and a configuration class.
     * <p>
     * When a configuration class is registered, a synthetic interface is generated via bytecode that implements
     * this marker. After the interface implementation is populated with configuration values, {@link #map()}
     * is called to create an instance of the class and populate the values from the interface implementation.
     */
    public interface Mapper {
        default Object map() {
            throw new UnsupportedOperationException();
        }
    }

    private static final Map<Class<?>, ConfigMappingClass> CACHE = synchronizedMap(new WeakHashMap<>());

    static ConfigMappingClass get(final Class<?> classType, final ConfigMappingHandler handler) {
        return CACHE.computeIfAbsent(classType, new Function<>() {
            @Override
            public ConfigMappingClass apply(Class<?> type) {
                return of(type, handler);
            }
        });
    }

    private static ConfigMappingClass of(final Class<?> classType, final ConfigMappingHandler handler) {
        if (classType.isInterface() ||
                Modifier.isAbstract(classType.getModifiers()) ||
                classType.isEnum() ||
                classType.isArray() ||
                classType.isPrimitive()) {
            return null;
        }
        if (classType.getName().startsWith("java")) {
            return null;
        }
        if (Collection.class.isAssignableFrom(classType) || Map.class.isAssignableFrom(classType)) {
            return null;
        }
        try {
            classType.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            // There is no good way to distinguish if it is valid, because it may be handled by a runtime Converter
            return null;
        }

        return new ConfigMappingClass(classType, handler);
    }

    private final Class<?> classType;
    private final ConfigMappingHandler handler;
    private final String generatedClassName;
    private final Set<GeneratedConfigClass> nestedClasses;
    private final Class<?> interfaceType;

    ConfigMappingClass(final Class<?> classType, final ConfigMappingHandler handler) {
        this.classType = classType;
        this.handler = handler;
        this.generatedClassName = getGeneratedClassName(classType);
        this.nestedClasses = getNested(classType, handler, new LinkedHashSet<>());
        this.interfaceType = loadClass(this);
    }

    @Override
    public Class<?> getParent() {
        return classType;
    }

    @Override
    public Class<?> getInterfaceType() {
        return interfaceType;
    }

    @Override
    public ConfigMappingHandler getHandler() {
        return handler;
    }

    @Override
    public String getClassName() {
        return generatedClassName;
    }

    @Override
    public byte[] generateClassBytes() {
        return ConfigMappingGenerator.generate(classType, generatedClassName, handler);
    }

    @Override
    public Property[] getProperties() {
        return ConfigMappingInterface.get(interfaceType, Handlers.find(interfaceType)).getProperties();
    }

    static Class<?> getInterfaceType(final Class<?> type) {
        ConfigMappingClass configMappingClass = CACHE.get(type);
        return configMappingClass == null ? null : configMappingClass.getInterfaceType();
    }

    static String getGeneratedClassName(Class<?> type) {
        return type.getPackage().getName() +
                "." +
                type.getSimpleName() +
                type.getName().hashCode() +
                "I";
    }

    Set<GeneratedConfigClass> getNested() {
        return nestedClasses;
    }

    private static Set<GeneratedConfigClass> getNested(
            final Class<?> type,
            final ConfigMappingHandler handler,
            final Set<GeneratedConfigClass> nested) {

        for (Field field : type.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType) ||
                    Map.class.isAssignableFrom(fieldType) ||
                    Optional.class.isAssignableFrom(fieldType) ||
                    Secret.class.isAssignableFrom(fieldType)) {
                java.lang.reflect.Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType parameterizedType) {
                    for (java.lang.reflect.Type typeArg : parameterizedType.getActualTypeArguments()) {
                        if (typeArg instanceof Class<?> argClass) {
                            ConfigMappingClass configMappingClass = get(argClass, handler);
                            if (configMappingClass != null && nested.add(configMappingClass)) {
                                getNested(argClass, handler, nested);
                            }
                        }
                    }
                }
            } else {
                ConfigMappingClass configClass = get(fieldType, handler);
                if (configClass != null && nested.add(configClass)) {
                    getNested(fieldType, handler, nested);
                }
            }
        }
        return nested;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return classType.equals(((ConfigMappingClass) o).classType);
    }

    @Override
    public int hashCode() {
        return classType.hashCode();
    }
}
