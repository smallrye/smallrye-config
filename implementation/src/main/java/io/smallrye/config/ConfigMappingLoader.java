package io.smallrye.config;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.common.classloader.ClassDefiner;
import io.smallrye.config._private.ConfigMessages;

public final class ConfigMappingLoader {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ConcurrentHashMap<String, Object> classLoaderLocks = new ConcurrentHashMap<>();

    private static final ClassValue<ConfigMappingObjectHolder> CACHE = new ClassValue<ConfigMappingObjectHolder>() {
        @Override
        protected ConfigMappingObjectHolder computeValue(final Class<?> type) {
            return new ConfigMappingObjectHolder(getImplementationClass(type));
        }
    };

    public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type) {
        final List<ConfigMappingMetadata> mappings = new ArrayList<>();
        final ConfigMappingInterface configurationInterface = ConfigMappingInterface.getConfigurationInterface(type);
        if (configurationInterface != null) {
            mappings.add(configurationInterface);
            mappings.addAll(configurationInterface.getNested());
            for (ConfigMappingInterface superType : configurationInterface.getSuperTypes()) {
                mappings.add(superType);
                mappings.addAll(superType.getNested());
            }
        }
        final ConfigMappingClass configMappingClass = ConfigMappingClass.getConfigurationClass(type);
        if (configMappingClass != null) {
            mappings.add(configMappingClass);
            mappings.addAll(getConfigMappingsMetadata(getConfigMapping(type).getInterfaceType()));
        }
        return mappings;
    }

    public static ConfigMappingInterface getConfigMapping(final Class<?> type) {
        return ConfigMappingInterface.getConfigurationInterface(getConfigMappingClass(type));
    }

    static Class<?> getConfigMappingClass(final Class<?> type) {
        validateAnnotations(type);

        final ConfigMappingClass configMappingClass = ConfigMappingClass.getConfigurationClass(type);
        if (configMappingClass == null) {
            return type;
        } else {
            return loadClass(type, configMappingClass);
        }
    }

    static <T> T configMappingObject(Class<T> interfaceType, ConfigMappingContext configMappingContext) {
        ConfigMappingObject instance;
        try {
            Constructor<? extends ConfigMappingObject> constructor = CACHE.get(interfaceType).getImplementationClass()
                    .getDeclaredConstructor(ConfigMappingContext.class);
            instance = constructor.newInstance(configMappingContext);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
        return interfaceType.cast(instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type) {
        final ConfigMappingMetadata mappingMetadata = ConfigMappingInterface.getConfigurationInterface(type);
        if (mappingMetadata == null) {
            throw ConfigMessages.msg.classIsNotAMapping(type);
        }
        return (Class<? extends ConfigMappingObject>) loadClass(type, mappingMetadata);
    }

    static Class<?> loadClass(final Class<?> parent, final ConfigMappingMetadata configMappingMetadata) {
        // acquire a lock on the class name to prevent race conditions in multithreaded use cases
        synchronized (getClassLoaderLock(configMappingMetadata.getClassName())) {
            // Check if the interface implementation was already loaded. If not we will load it.
            try {
                final Class<?> klass = parent.getClassLoader().loadClass(configMappingMetadata.getClassName());
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

    static void validateAnnotations(Class<?> type) {
        if (!type.isInterface() && type.isAnnotationPresent(ConfigMapping.class)) {
            throw ConfigMessages.msg.mappingAnnotationNotSupportedInClass(type);
        }

        if (type.isInterface() && type.isAnnotationPresent(ConfigProperties.class)) {
            throw ConfigMessages.msg.propertiesAnnotationNotSupportedInInterface(type);
        }
    }

    /**
     * Do not remove this method or inline it. It is keep separate on purpose, so it is easier to substitute it with
     * the GraalVM API for native image compilation.
     *
     * We cannot keep dynamic references to LOOKUP, so this method may be replaced. This is not a problem, since for
     * native image we can generate the mapping class bytes in the binary so we don't need to dynamically load them.
     */
    private static Class<?> defineClass(final Class<?> parent, final String className, final byte[] classBytes) {
        return ClassDefiner.defineClass(LOOKUP, parent, className, classBytes);
    }

    private static Object getClassLoaderLock(String className) {
        return classLoaderLocks.computeIfAbsent(className, c -> new Object());
    }

    private static final class ConfigMappingObjectHolder {
        private final Class<? extends ConfigMappingObject> implementationClass;

        ConfigMappingObjectHolder(final Class<? extends ConfigMappingObject> implementationClass) {
            this.implementationClass = implementationClass;
        }

        public Class<? extends ConfigMappingObject> getImplementationClass() {
            return implementationClass;
        }
    }
}
