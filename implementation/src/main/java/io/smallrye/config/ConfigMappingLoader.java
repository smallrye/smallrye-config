package io.smallrye.config;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.classloader.ClassDefiner;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config._private.ConfigMessages;

/**
 * Loads implementation classes for configuration types.
 * <p>
 * Supports two kinds of user-defined configuration types: interfaces annotated with
 * {@link ConfigMapping @ConfigMapping} (represented by {@link ConfigMappingInterface}) and concrete classes
 * with a no-arg constructor (represented by {@link ConfigMappingClass}). For each type, bytecode for an
 * implementation class is generated and defined into the appropriate classloader.
 *
 * @see GeneratedConfigClass
 * @see ConfigMappingInterface
 * @see ConfigMappingClass
 */
public final class ConfigMappingLoader {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ConcurrentHashMap<String, Object> CLASS_LOADER_LOCKS = new ConcurrentHashMap<>();

    /**
     * Collects all {@link GeneratedConfigClass} entries for the given type, including nested types and supertypes,
     * discovering the handler automatically.
     *
     * @param type the configuration type
     * @return an immutable Set of generated config classes
     */
    public static Set<GeneratedConfigClass> getGeneratedConfigClasses(final Class<?> type) {
        return getGeneratedConfigClasses(type, Handlers.find(type));
    }

    /**
     * Collects all {@link GeneratedConfigClass} entries for the given type, including nested types and supertypes.
     *
     * @param type the configuration type
     * @param handler the handler to processing the type
     * @return an immutable Set of generated config classes
     */
    public static Set<GeneratedConfigClass> getGeneratedConfigClasses(final Class<?> type, final ConfigMappingHandler handler) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("handler", handler);

        Set<GeneratedConfigClass> generatedClasses = new HashSet<>();
        ConfigMappingInterface configMappingInterface = ConfigMappingInterface.get(type, handler);
        if (configMappingInterface != null) {
            generatedClasses.add(configMappingInterface);
            generatedClasses.addAll(configMappingInterface.getNested());
            for (ConfigMappingInterface superType : configMappingInterface.getSuperTypes()) {
                generatedClasses.add(superType);
                generatedClasses.addAll(superType.getNested());
            }
        }
        ConfigMappingClass configMappingClass = ConfigMappingClass.get(type, handler);
        if (configMappingClass != null) {
            generatedClasses.add(configMappingClass);
            generatedClasses.addAll(configMappingClass.getNested());
            generatedClasses.addAll(getGeneratedConfigClasses(configMappingClass.getInterfaceType(),
                    configMappingClass.getHandler()));
        }
        return Set.copyOf(generatedClasses);
    }

    // Visible for testing
    static Class<?> loadClass(final GeneratedConfigClass generatedClass) {
        Class<?> parent = generatedClass.getParent();
        ConfigMappingLoader.class.getModule().addReads(parent.getModule());
        // acquire a lock on the class name to prevent race conditions in multithreaded use cases
        synchronized (CLASS_LOADER_LOCKS.computeIfAbsent(generatedClass.getClassName(), c -> new Object())) {
            // Check if the interface implementation was already loaded. If not we will load it.
            try {
                Class<?> loadedClass = parent.getClassLoader().loadClass(generatedClass.getClassName());
                // Check if this is the right classloader class. If not we will load it.
                if (parent.isAssignableFrom(loadedClass)) {
                    return loadedClass;
                }
                // A ConfigMappingClass generates an interface, not a subclass of the parent,
                // so isAssignableFrom is always false — but the loaded class is still correct.
                if (generatedClass instanceof ConfigMappingClass) {
                    return loadedClass;
                }
                return loadClass(parent, generatedClass);
            } catch (ClassNotFoundException e) {
                return loadClass(parent, generatedClass);
            } finally {
                CLASS_LOADER_LOCKS.remove(generatedClass.getClassName());
            }
        }
    }

    private static Class<?> loadClass(final Class<?> parent, final GeneratedConfigClass generatedClass) {
        for (GeneratedConfigClass auxiliaryClass : generatedClass.getAuxiliaryClasses()) {
            defineClass(parent, auxiliaryClass.getClassName(), auxiliaryClass.generateClassBytes());
        }
        return defineClass(parent, generatedClass.getClassName(), generatedClass.generateClassBytes());
    }

    /**
     * Do not remove this method or inline it. It is keep separate on purpose, so it is easier to substitute it with
     * the GraalVM API for native image compilation.
     * <p>
     * We cannot keep dynamic references to LOOKUP, so this method may be replaced. This is not a problem, since for
     * native image we can generate the mapping class bytes in the binary so we don't need to dynamically load them.
     */
    private static Class<?> defineClass(final Class<?> parent, final String className, final byte[] classBytes) {
        return ClassDefiner.defineClass(LOOKUP, parent, className, classBytes);
    }

    /**
     * Represents a configuration type that requires a generated implementation class.
     * <p>
     * Two kinds of user-defined configuration types are supported: interfaces annotated with
     * {@link ConfigMapping @ConfigMapping} (handled by {@link ConfigMappingInterface}) and concrete classes
     * with a no-arg constructor (handled by {@link ConfigMappingClass}).
     *
     * @see ConfigMappingInterface
     * @see ConfigMappingClass
     */
    public interface GeneratedConfigClass {
        /**
         * Returns the original user-defined type (interface or class) from which the implementation is generated.
         *
         * @return the source configuration type
         */
        Class<?> getParent();

        /**
         * Returns the interface type used internally for configuration mapping. For interface-based mappings this
         * is the same as the parent; for class-based mappings it is the generated compatible interface.
         *
         * @return the interface type used for mapping
         */
        Class<?> getInterfaceType();

        /**
         * Returns the {@link ConfigMappingHandler} that processed this configuration type.
         *
         * @return the handler
         */
        ConfigMappingHandler getHandler();

        /**
         * Returns the fully-qualified name of the generated class.
         *
         * @return the generated class name
         */
        String getClassName();

        /**
         * Generates the bytecode for the generated class.
         *
         * @return the class bytes for the generated class
         */
        byte[] generateClassBytes();

        /**
         * Returns the property descriptors for the generated class.
         *
         * @return the properties
         */
        Property[] getProperties();

        /**
         * Returns additional generated classes that must be defined alongside this class.
         *
         * @return the auxiliary classes, empty by default
         */
        default Set<GeneratedConfigClass> getAuxiliaryClasses() {
            return Collections.emptySet();
        }
    }

    static final class ConfigClassImplementation {
        private static final ClassValue<ConfigClassImplementation> CACHE = new ClassValue<>() {
            @Override
            protected ConfigClassImplementation computeValue(Class<?> type) {
                // Try to load the implementation class of a config interface from the CL
                try {
                    Class<?> implementationClass = type.getClassLoader()
                            .loadClass(ConfigMappingInterface.getGeneratedClassName(type));
                    if (type.isAssignableFrom(implementationClass)) {
                        return new ConfigClassImplementation(type, implementationClass);
                    }
                } catch (ClassNotFoundException e) {
                    // Fall through to dynamic generation
                }

                // Try to load the implementation class of a class compatible config interface from the CL
                try {
                    Class<?> interfaceType = type.getClassLoader()
                            .loadClass(ConfigMappingClass.getGeneratedClassName(type));
                    Class<?> implementationClass = type.getClassLoader()
                            .loadClass(ConfigMappingInterface.getGeneratedClassName(interfaceType));
                    if (interfaceType.isAssignableFrom(implementationClass)) {
                        return new ConfigClassImplementation(interfaceType, implementationClass);
                    }
                } catch (ClassNotFoundException e) {
                    // Fall through to dynamic generation
                }

                // Dynamically generate and load the implementation class
                ConfigMappingHandler handler = Handlers.get(type);
                ConfigMappingInterface configMappingInterface = ConfigMappingInterface.get(type, handler);
                if (configMappingInterface != null) {
                    return new ConfigClassImplementation(type, loadImplementation(type, configMappingInterface.getHandler()));
                }

                ConfigMappingClass configMappingClass = ConfigMappingClass.get(type, handler);
                Class<?> interfaceType = configMappingClass != null ? configMappingClass.getInterfaceType() : type;
                return new ConfigClassImplementation(interfaceType, loadImplementation(interfaceType, handler));
            }
        };

        private static <T> Class<?> loadImplementation(final Class<T> type, final ConfigMappingHandler handler) {
            // Load the entire config class hierarchy, plus nested elements
            ConfigMappingInterface generatedClass = ConfigMappingInterface.get(type, handler);
            if (generatedClass == null) {
                throw ConfigMessages.msg.classIsNotAMapping(type);
            }

            Class<?> implementationClass = loadClass(generatedClass);
            for (GeneratedConfigClass nestedGeneratedClass : generatedClass.getNested()) {
                loadClass(nestedGeneratedClass);
            }

            return implementationClass;
        }

        static ConfigClassImplementation get(final Class<?> type) {
            return CACHE.get(type);
        }

        private final Class<?> interfaceType;
        private final Class<?> implementation;

        private volatile MethodHandle constructor;
        private volatile MethodHandle getProperties;
        private volatile MethodHandle getSecrets;

        ConfigClassImplementation(Class<?> interfaceType, final Class<?> implementation) {
            // ensure modular access
            ConfigClassImplementation.class.getModule().addReads(implementation.getModule());
            this.implementation = implementation;
            this.interfaceType = interfaceType;
        }

        Class<?> getInterfaceType() {
            return interfaceType;
        }

        Class<?> getImplementation() {
            return implementation;
        }

        @SuppressWarnings("unchecked")
        <T> T newInstance(final ConfigMappingContext configMappingContext) {
            MethodHandle ctor = this.constructor;
            if (ctor == null) {
                try {
                    this.constructor = ctor = LOOKUP.findConstructor(implementation,
                            methodType(void.class, ConfigMappingContext.class))
                            .asType(methodType(Object.class, ConfigMappingContext.class));
                } catch (NoSuchMethodException e) {
                    throw new NoSuchMethodError(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                }
            }
            return (T) invoke(ctor, configMappingContext);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> getProperties() {
            MethodHandle props = this.getProperties;
            if (props == null) {
                try {
                    this.getProperties = props = LOOKUP.findStatic(implementation, "getProperties",
                            methodType(Map.class));
                } catch (NoSuchMethodException e) {
                    throw new NoSuchMethodError(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                }
            }
            return (Map<String, String>) invoke(props);
        }

        @SuppressWarnings("unchecked")
        Set<String> getSecrets() {
            MethodHandle secrets = this.getSecrets;
            if (secrets == null) {
                try {
                    this.getSecrets = secrets = LOOKUP.findStatic(implementation, "getSecrets",
                            methodType(Set.class));
                } catch (NoSuchMethodException e) {
                    throw new NoSuchMethodError(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                }
            }
            return (Set<String>) invoke(secrets);
        }

        private static Object invoke(final MethodHandle handle, final Object... args) {
            try {
                return handle.invokeWithArguments(args);
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException | Error r) {
                    throw r;
                } catch (Throwable t) {
                    throw new UndeclaredThrowableException(t);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
    }
}
