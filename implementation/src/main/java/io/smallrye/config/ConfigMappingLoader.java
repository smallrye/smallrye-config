package io.smallrye.config;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.common.classloader.ClassDefiner;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config._private.ConfigMessages;

public final class ConfigMappingLoader {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ConcurrentHashMap<String, Object> classLoaderLocks = new ConcurrentHashMap<>();

    private static final ClassValue<ConfigMappingImplementation> CACHE = new ClassValue<>() {
        @Override
        protected ConfigMappingImplementation computeValue(final Class<?> type) {
            return new ConfigMappingImplementation(loadImplementation(type));
        }
    };

    public static List<ConfigMappingMetadata> getConfigMappingsMetadata(final Class<?> type) {
        List<ConfigMappingMetadata> mappings = new ArrayList<>();
        ConfigMappingInterface configurationInterface = ConfigMappingInterface.getConfigurationInterface(type);
        if (configurationInterface != null) {
            mappings.add(configurationInterface);
            mappings.addAll(configurationInterface.getNested());
            for (ConfigMappingInterface superType : configurationInterface.getSuperTypes()) {
                mappings.add(superType);
                mappings.addAll(superType.getNested());
            }
        }
        ConfigMappingClass configMappingClass = ConfigMappingClass.getConfigurationClass(type);
        if (configMappingClass != null) {
            mappings.add(configMappingClass);
            mappings.addAll(getConfigMappingsMetadata(getConfigMapping(type).getInterfaceType()));
        }
        return List.copyOf(mappings);
    }

    public static ConfigMappingInterface getConfigMapping(final Class<?> type) {
        return ConfigMappingInterface.getConfigurationInterface(getConfigMappingClass(type));
    }

    static Class<?> getConfigMappingClass(final Class<?> type) {
        ConfigMappingClass configMappingClass = ConfigMappingClass.getConfigurationClass(type);
        if (configMappingClass == null) {
            return type;
        } else {
            return loadClass(type, configMappingClass);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Map<String, String> configMappingProperties(final Class<T> interfaceType) {
        try {
            return (Map<String, String>) CACHE.get(interfaceType).getProperties().invoke();
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
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

    @SuppressWarnings("unchecked")
    static <T> Set<String> configMappingSecrets(final Class<T> interfaceType) {
        try {
            return (Set<String>) CACHE.get(interfaceType).getSecrets().invoke();
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
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

    @SuppressWarnings("unchecked")
    static <T> T configMappingObject(final Class<T> interfaceType, final ConfigMappingContext configMappingContext) {
        try {
            return (T) CACHE.get(interfaceType).constructor().invokeExact(configMappingContext);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
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

    public static ConfigMappingImplementation ensureLoaded(final Class<?> type) {
        return CACHE.get(type);
    }

    static <T> Class<?> loadImplementation(final Class<T> type) {
        try {
            Class<?> implementationClass = type.getClassLoader()
                    .loadClass(ConfigMappingInterface.getImplementationClassName(type));
            if (type.isAssignableFrom(implementationClass)) {
                return implementationClass;
            }

            ConfigMappingMetadata mappingMetadata = ConfigMappingInterface.getConfigurationInterface(type);
            if (mappingMetadata == null) {
                throw ConfigMessages.msg.classIsNotAMapping(type);
            }
            return loadClass(type, mappingMetadata);
        } catch (ClassNotFoundException e) {
            ConfigMappingMetadata mappingMetadata = ConfigMappingInterface.getConfigurationInterface(type);
            if (mappingMetadata == null) {
                throw ConfigMessages.msg.classIsNotAMapping(type);
            }
            return loadClass(type, mappingMetadata);
        }
    }

    static Class<?> loadClass(final Class<?> parent, final ConfigMappingMetadata configMappingMetadata) {
        // acquire a lock on the class name to prevent race conditions in multithreaded use cases
        synchronized (getClassLoaderLock(configMappingMetadata.getClassName())) {
            // Check if the interface implementation was already loaded. If not we will load it.
            try {
                Class<?> klass = parent.getClassLoader().loadClass(configMappingMetadata.getClassName());
                // Check if this is the right classloader class. If not we will load it.
                if (parent.isAssignableFrom(klass)) {
                    return klass;
                }
                // ConfigProperties should not have issues with classloader and interfaces.
                if (configMappingMetadata instanceof ConfigMappingClass) {
                    return klass;
                }
                return defineClass(parent, configMappingMetadata.getClassName(), configMappingMetadata.getClassBytes());
            } catch (ClassNotFoundException e) {
                return defineClass(parent, configMappingMetadata.getClassName(), configMappingMetadata.getClassBytes());
            }
        }
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

    private static Object getClassLoaderLock(final String className) {
        return classLoaderLocks.computeIfAbsent(className, c -> new Object());
    }

    public static final class ConfigMappingImplementation {
        private final Class<?> implementation;
        private final MethodHandle constructor;
        private final MethodHandle getProperties;
        private final MethodHandle getSecrets;

        ConfigMappingImplementation(final Class<?> implementation) {
            try {
                this.implementation = implementation;
                MethodHandle constructor = LOOKUP.findConstructor(implementation,
                        methodType(void.class, ConfigMappingContext.class));
                this.constructor = constructor.asType(constructor.type().changeReturnType(Object.class));
                this.getProperties = LOOKUP.findStatic(implementation, "getProperties", methodType(Map.class));
                this.getSecrets = LOOKUP.findStatic(implementation, "getSecrets", methodType(Set.class));
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            }
        }

        public Class<?> implementation() {
            return implementation;
        }

        public MethodHandle constructor() {
            return constructor;
        }

        public MethodHandle getProperties() {
            return getProperties;
        }

        public MethodHandle getSecrets() {
            return getSecrets;
        }
    }

    /**
     * Implementation of {@link ConfigMappingMetadata} for MicroProfile {@link ConfigProperties}.
     */
    static final class ConfigMappingClass implements ConfigMappingMetadata {
        private static final ClassValue<ConfigMappingClass> cv = new ClassValue<>() {
            @Override
            protected ConfigMappingClass computeValue(final Class<?> classType) {
                return createConfigurationClass(classType);
            }
        };

        static ConfigMappingClass getConfigurationClass(Class<?> classType) {
            Assert.checkNotNullParam("classType", classType);
            return cv.get(classType);
        }

        private static ConfigMappingClass createConfigurationClass(final Class<?> classType) {
            if (classType.isInterface() && classType.getTypeParameters().length == 0 ||
                    Modifier.isAbstract(classType.getModifiers()) ||
                    classType.isEnum()) {
                return null;
            }

            return new ConfigMappingClass(classType);
        }

        private static String generateInterfaceName(final Class<?> classType) {
            if (classType.isInterface() && classType.getTypeParameters().length == 0 ||
                    Modifier.isAbstract(classType.getModifiers()) ||
                    classType.isEnum()) {
                throw new IllegalArgumentException();
            }

            return classType.getPackage().getName() +
                    "." +
                    classType.getSimpleName() +
                    classType.getName().hashCode() +
                    "I";
        }

        private final Class<?> classType;
        private final String interfaceName;

        public ConfigMappingClass(final Class<?> classType) {
            this.classType = classType;
            this.interfaceName = generateInterfaceName(classType);
        }

        @Override
        public Class<?> getInterfaceType() {
            return classType;
        }

        @Override
        public String getClassName() {
            return interfaceName;
        }

        @Override
        public byte[] getClassBytes() {
            return ConfigMappingGenerator.generate(classType, interfaceName);
        }
    }
}
