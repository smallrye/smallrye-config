package io.smallrye.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config._private.ConfigMessages;

/**
 * SPI to support configuration classes.
 * <p>
 * Implementations describe how to extract configuration metadata from annotated classes, enabling the core
 * {@link ConfigMappingGenerator} to produce a backing interface and implementation without being coupled to any
 * specific annotation model.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface ConfigMappingHandler {
    /**
     * Whether this handler recognizes the given class as a configuration class.
     *
     * @param classType the candidate class.
     * @return {@code true} if this handler can extract configuration metadata from the class.
     */
    boolean handles(Class<?> classType);

    /**
     * Extract the configuration prefix from the class.
     *
     * @param classType the configuration class.
     * @return the prefix, or an empty string if none.
     */
    String getPrefix(Class<?> classType);

    /**
     * Extract configuration metadata from a field of a configuration class.
     * <p>
     * The returned {@link FieldMember} provides name, default value, and converter metadata derived from
     * handler-specific annotations. SmallRye Config's own annotations ({@link WithName}, {@link WithDefault},
     * {@link WithConverter}) take precedence over the values returned here.
     *
     * @param field the field to process.
     * @return a {@link FieldMember} with the extracted metadata, or {@link FieldMember#EMPTY} if this handler has
     *         nothing to contribute for the given field.
     */
    default FieldMember processField(Field field) {
        return FieldMember.EMPTY;
    }

    /**
     * The naming strategy to use when generating the backing interface for the configuration class.
     * <p>
     * Defaults to {@link NamingStrategy#VERBATIM}, which uses field names as-is.
     *
     * @return the naming strategy.
     */
    default NamingStrategy getNamingStrategy() {
        return NamingStrategy.VERBATIM;
    }

    /**
     * Whether unmapped properties under the configuration prefix should be ignored during validation.
     * <p>
     * When {@code true}, properties present in config sources that do not map to any field in the configuration
     * class are silently ignored. When {@code false}, they are reported as validation errors.
     * <p>
     * Defaults to {@code false}.
     *
     * @return {@code true} to ignore unmapped properties, {@code false} to validate them.
     */
    default boolean ignoreUnmappedProperties() {
        return false;
    }

    record FieldMember(String name, String defaultValue, Class<? extends Converter<?>> converter) {
        public static final FieldMember EMPTY = new FieldMember(null, null, null);
    }

    final class Handlers {
        private static final ConfigMappingHandler FALLBACK = new FallbackClassHandler();
        private static final Map<ClassLoader, List<ConfigMappingHandler>> cache = Collections
                .synchronizedMap(new WeakHashMap<>());

        static ConfigMappingHandler find(final Class<?> classType) {
            List<ConfigMappingHandler> handlers = cache.computeIfAbsent(classType.getClassLoader(), Handlers::load);
            for (ConfigMappingHandler handler : handlers) {
                if (handler.handles(classType)) {
                    return handler;
                }
            }
            Class<?> declaring = classType.getDeclaringClass();
            if (declaring != null) {
                return find(declaring);
            }
            return FALLBACK;
        }

        private static List<ConfigMappingHandler> load(final ClassLoader classLoader) {
            List<ConfigMappingHandler> handlers = new ArrayList<>();
            for (ConfigMappingHandler handler : ServiceLoader.load(ConfigMappingHandler.class, classLoader)) {
                handlers.add(handler);
            }
            handlers.add(new ConfigMappingInterfaceHandler());
            return List.copyOf(handlers);
        }

        private final static class ConfigMappingInterfaceHandler implements ConfigMappingHandler {
            @Override
            public boolean handles(Class<?> classType) {
                if (!classType.isInterface() && classType.isAnnotationPresent(ConfigMapping.class)) {
                    throw ConfigMessages.msg.mappingAnnotationNotSupportedInClass(classType);
                }
                return classType.isInterface();
            }

            @Override
            public String getPrefix(Class<?> classType) {
                ConfigMapping configMapping = classType.getAnnotation(ConfigMapping.class);
                return configMapping != null ? configMapping.prefix() : "";
            }
        }

        private final static class FallbackClassHandler implements ConfigMappingHandler {
            @Override
            public boolean handles(Class<?> classType) {
                return true;
            }

            @Override
            public String getPrefix(Class<?> classType) {
                return "";
            }
        }
    }
}
