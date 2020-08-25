package io.smallrye.config;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import io.smallrye.common.classloader.ClassDefiner;

public final class ConfigMappingObjectLoader {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassValue<ConfigMappingObjectHolder> CACHE = new ClassValue<ConfigMappingObjectHolder>() {
        @Override
        protected ConfigMappingObjectHolder computeValue(final Class<?> type) {
            return new ConfigMappingObjectHolder(getImplementationClass(type));
        }
    };

    public static ConfigMappingMetadata getConfigMappingMetadata(Class<?> interfaceType) {
        return ConfigMappingInterface.getConfigurationInterface(interfaceType);
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
    static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> interfaceType) {
        final ConfigMappingMetadata mappingMetadata = getConfigMappingMetadata(interfaceType);

        // Check if the interface implementation was already loaded. If not we will load it.
        try {
            return (Class<? extends ConfigMappingObject>) interfaceType.getClassLoader()
                    .loadClass(mappingMetadata.getClassName());
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        return createMappingObjectClass(mappingMetadata.getClassName(),
                mappingMetadata.getClassBytes());
    }

    @SuppressWarnings("unchecked")
    static Class<? extends ConfigMappingObject> createMappingObjectClass(final String className,
            final byte[] classBytes) {
        return (Class<? extends ConfigMappingObject>) ClassDefiner.defineClass(LOOKUP, ConfigMappingObjectLoader.class,
                className, classBytes);
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
