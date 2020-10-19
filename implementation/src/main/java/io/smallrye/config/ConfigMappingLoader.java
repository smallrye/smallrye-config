package io.smallrye.config;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.classloader.ClassDefiner;

public final class ConfigMappingLoader {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassValue<ConfigMappingObjectHolder> CACHE = new ClassValue<ConfigMappingObjectHolder>() {
        @Override
        protected ConfigMappingObjectHolder computeValue(final Class<?> type) {
            return new ConfigMappingObjectHolder(getImplementationClass(type));
        }
    };

    public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type) {
        final List<ConfigMappingMetadata> mappings = new ArrayList<>();
        final ConfigMappingInterface configurationInterface = ConfigMappingInterface.getConfigurationInterface(type);
        mappings.add(configurationInterface);
        mappings.addAll(configurationInterface.getNested());
        return mappings;
    }

    static ConfigMappingInterface getConfigMappingInterface(final Class<?> type) {
        return ConfigMappingInterface.getConfigurationInterface(type);
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
    static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type) {
        final ConfigMappingMetadata mappingMetadata = ConfigMappingInterface.getConfigurationInterface(type);
        return (Class<? extends ConfigMappingObject>) loadClass(type,
                mappingMetadata.getClassName(),
                mappingMetadata.getClassBytes());
    }

    static Class<?> loadClass(final Class<?> parent, final String className, final byte[] classBytes) {
        // Check if the interface implementation was already loaded. If not we will load it.
        try {
            return parent.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return defineClass(parent, className, classBytes);
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
