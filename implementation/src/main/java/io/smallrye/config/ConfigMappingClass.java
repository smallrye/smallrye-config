package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.loadClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.config.ConfigMappingHandler.ConfigMappingInterfaceHandler;
import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;
import io.smallrye.config._private.ConfigMessages;

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
        /**
         * Creates a new instance of the configuration class, with its no-args constructor, and populates every
         * non-final field from the interface implementation.
         *
         * @return the populated instance of the configuration class
         */
        default Object map() {
            throw new UnsupportedOperationException();
        }

        /**
         * Populates every non-final field of an existing instance of the configuration class from the interface
         * implementation. A final field keeps the value of the instance.
         *
         * @param target the instance of the configuration class to populate
         * @return the populated instance, which is always the {@code target}
         */
        default Object map(Object target) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A {@link ClassValue} stores its values in the key {@link Class} own {@code classValueMap}, so the metadata and
     * the {@link Class} it describes form a cycle that is collected together with the {@link ClassLoader}. A
     * {@code WeakHashMap} keyed by {@link Class} cannot do the same, because the metadata strongly references its own
     * key, which keeps the entry alive forever. The box allows the metadata to be computed by the caller, which
     * requires a {@link ConfigMappingHandler} that {@code ClassValue#computeValue(Class)} does not receive.
     * <p>
     * The box is an {@link AtomicReference} rather than a type of ours, because the key {@link Class} holds the entry
     * strongly, and so holds the class of whatever the entry contains. A box of ours parked on a type loaded
     * elsewhere keeps the SmallRye Config {@link ClassLoader} alive for as long as that type, even while the box is
     * empty, and {@link #getInterfaceType(Class)} leaves it empty for every type that turns out not to be mapped
     * here. An {@link AtomicReference} is defined by the bootstrap loader, so an empty entry costs the type nothing.
     * <p>
     * A filled entry still pins, through the metadata itself. Only a type accepted by
     * {@link #isConfigurationClass(Class, boolean)} is ever filled, so it is that guard that has to keep the metadata of one
     * class loader off the types of another.
     */
    private static final ClassValue<AtomicReference<ConfigMappingClass>> CACHE = new ClassValue<>() {
        @Override
        protected AtomicReference<ConfigMappingClass> computeValue(final Class<?> type) {
            return new AtomicReference<>();
        }
    };

    static ConfigMappingClass get(final Class<?> classType, final ConfigMappingHandler handler) {
        // a root class is registered explicitly, so it does not require a no-args constructor, because it can be
        // mapped from a template instance
        return get(classType, handler, false);
    }

    static ConfigMappingClass get(final Class<?> classType, final ConfigMappingHandler handler,
            final boolean requireNoArgsConstructor) {
        if (!isConfigurationClass(classType, requireNoArgsConstructor)) {
            return null;
        }

        AtomicReference<ConfigMappingClass> holder = CACHE.get(classType);
        ConfigMappingClass configMappingClass = holder.get();
        if (configMappingClass == null) {
            // use synchronized block instead of compareAndSet,
            // because computation is expensive so we run once per type instead of once per racing thread
            synchronized (holder) {
                configMappingClass = holder.get();
                if (configMappingClass == null) {
                    configMappingClass = new ConfigMappingClass(classType, handler);
                    holder.set(configMappingClass);
                }
            }
        }
        return configMappingClass;
    }

    /**
     * Whether the type may be mapped as a configuration class. A type rejected here is never looked up in the
     * {@link #CACHE}, so the cache only holds entries for types that belong to the application.
     */
    private static boolean isConfigurationClass(final Class<?> classType, final boolean requireNoArgsConstructor) {
        if (classType.isInterface() ||
                Modifier.isAbstract(classType.getModifiers()) ||
                classType.isEnum() ||
                classType.isArray() ||
                classType.isPrimitive()) {
            return false;
        }
        if (classType.getName().startsWith("java")) {
            return false;
        }
        if (Collection.class.isAssignableFrom(classType) || Map.class.isAssignableFrom(classType)) {
            return false;
        }
        if (requireNoArgsConstructor && !hasNoArgsConstructor(classType)) {
            // There is no good way to distinguish if it is valid, because it may be handled by a runtime Converter
            return false;
        }

        return true;
    }

    static boolean hasNoArgsConstructor(final Class<?> classType) {
        try {
            classType.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * The default values of a mapping populated from a template instance, which are the values of the template
     * fields, in the same way the field initializers of a configuration class are the default values of a regular
     * mapping. A template value must be converted back by the converter of the field, with {@link Object#toString()}.
     * A nested configuration class instance is walked recursively, a collection becomes a comma separated value, a
     * map becomes an entry for each key, and an {@link Optional} or a {@link Secret} is unwrapped.
     *
     * @param classType the configuration class
     * @param template the template instance
     * @param prefix the prefix of the mapping
     * @param handler the handler of the configuration class
     * @return the default values, by property name
     */
    static Map<String, String> defaults(
            final Class<?> classType,
            final Object template,
            final String prefix,
            final ConfigMappingHandler handler) {
        Map<String, String> defaults = new HashMap<>();
        for (Field field : classType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value;
            try {
                field.setAccessible(true);
                value = field.get(template);
            } catch (IllegalAccessException | RuntimeException e) {
                continue;
            }
            if (value == null) {
                continue;
            }

            ConfigMappingHandler.FieldMember fieldMember = ConfigMappingHandler.FieldMember.of(field, handler);
            String name = fieldMember.name() != null
                    ? fieldMember.name()
                    : handler.getNamingStrategy(classType).apply(field.getName());
            String propertyName = prefix.isEmpty() ? name : name.isEmpty() ? prefix : prefix + "." + name;
            defaults(value, propertyName, fieldMember.converter() != null, handler, defaults);
        }
        return defaults;
    }

    private static void defaults(
            final Object value,
            final String name,
            final boolean converter,
            final ConfigMappingHandler handler,
            final Map<String, String> defaults) {
        if (value instanceof Optional<?> optional) {
            optional.ifPresent(v -> defaults(v, name, converter, handler, defaults));
        } else if (value instanceof Secret<?> secret) {
            defaults(secret.get(), name, converter, handler, defaults);
        } else if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String key = String.valueOf(entry.getKey());
                    key = key.indexOf('.') != -1 ? "\"" + key + "\"" : key;
                    defaults(entry.getValue(), name + "." + key, converter, handler, defaults);
                }
            }
        } else if (value instanceof Collection<?> collection) {
            StringBuilder joined = new StringBuilder();
            int index = 0;
            for (Object element : collection) {
                if (element == null) {
                    continue;
                }
                if (!converter && isConfigurationClass(element.getClass(), true)) {
                    defaults(element, name + "[" + index + "]", false, handler, defaults);
                } else {
                    if (!joined.isEmpty()) {
                        joined.append(',');
                    }
                    joined.append(String.valueOf(element).replace("\\", "\\\\").replace(",", "\\,"));
                }
                index++;
            }
            if (!joined.isEmpty()) {
                defaults.put(name, joined.toString());
            }
        } else if (!converter && isConfigurationClass(value.getClass(), true)) {
            defaults.putAll(defaults(value.getClass(), value, name, handler));
        } else if (!(value instanceof Character character && character == 0)) {
            defaults.put(name, String.valueOf(value));
        }
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
        return getMappingBridge().getProperties();
    }

    /**
     * The metadata of the generated interface bridge.
     * <p>
     * The bridge is a plain {@link ConfigMapping} interface: {@link ConfigMappingGenerator} already translated
     * everything the configuration class handler contributes, the member names and the naming strategy, into
     * annotations on it. It is resolved with {@link ConfigMappingInterfaceHandler} and not with the handler of the
     * class it bridges, which is also what {@link Handlers#find(Class)} yields for it, because a handler for
     * configuration classes does not handle interfaces.
     * <p>
     * Every caller that needs the bridge metadata goes through here.
     * {@link ConfigMappingInterface#get(Class, ConfigMappingHandler)} caches by type and the first writer wins, so a
     * caller resolving the bridge with a different handler would decide, for all the others, which handler the
     * bridge metadata reports.
     */
    ConfigMappingInterface getMappingBridge() {
        ConfigMappingInterface configMappingInterface = ConfigMappingInterface.get(interfaceType,
                ConfigMappingInterfaceHandler.CONFIG_MAPPING);
        if (configMappingInterface == null) {
            throw ConfigMessages.msg.classIsNotAMapping(interfaceType);
        }
        return configMappingInterface;
    }

    static Class<?> getInterfaceType(final Class<?> type) {
        if (!isConfigurationClass(type, true)) {
            return null;
        }
        ConfigMappingClass configMappingClass = CACHE.get(type).get();
        return configMappingClass == null ? null : configMappingClass.getInterfaceType();
    }

    static String getGeneratedClassName(Class<?> type) {
        // do not use getSimpleName(), it resolves the enclosing class, which may not be visible to the same
        // class loader as the nested class, and it drops the package for classes in the default package
        return type.getName() + "$$CMClass";
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
                            ConfigMappingClass configMappingClass = get(argClass, handler, true);
                            if (configMappingClass != null && nested.add(configMappingClass)) {
                                getNested(argClass, handler, nested);
                            }
                        }
                    }
                }
            } else {
                ConfigMappingClass configClass = get(fieldType, handler, true);
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
