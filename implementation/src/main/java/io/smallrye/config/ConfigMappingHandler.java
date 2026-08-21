package io.smallrye.config;

import static java.util.Collections.synchronizedMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config._private.ConfigMessages;

/**
 * SPI to support configuration types.
 * <p>
 * Implementations describe how to extract configuration metadata from annotated classes, enabling the core
 * {@link ConfigMappingGenerator} to produce a backing interface and implementation without being coupled to any
 * specific annotation model.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * <p>
 * A configuration type (including nested types) must be processed by a single handler implementation
 * across the application. If multiple handlers attempt to process the same type with different
 * implementations, the behavior is undefined for nested types and an error is raised for root types.
 */
public interface ConfigMappingHandler {
    /**
     * Whether this handler recognizes the given class as a configuration class.
     *
     * @param type the candidate class.
     * @return {@code true} if this handler can extract configuration metadata from the class.
     */
    boolean handles(Class<?> type);

    /**
     * Extract the configuration prefix from the class.
     *
     * @param type the configuration class.
     * @return the prefix, or an empty string if none.
     */
    String getPrefix(Class<?> type);

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
    default NamingStrategy getNamingStrategy(Class<?> type) {
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
        private static final Map<ClassLoader, List<ConfigMappingHandler>> HANDLERS = synchronizedMap(new WeakHashMap<>());
        private static final ClassValue<Holder<ConfigMappingHandler>> CACHE = new ClassValue<>() {
            @Override
            protected Holder<ConfigMappingHandler> computeValue(Class<?> type) {
                return new Holder<>();
            }
        };

        private static class Holder<T> {
            volatile T value;
        }

        static void register(final Class<?> type, final ConfigMappingHandler handler) {
            set(type, handler);
        }

        static ConfigMappingHandler get(final Class<?> type) {
            Holder<ConfigMappingHandler> holder = CACHE.get(type);
            if (holder.value == null) {
                throw ConfigMessages.msg.handlerNotRegistered(type);
            }
            return holder.value;
        }

        static ConfigMappingHandler find(final Class<?> type) {
            return find(type, type.getClassLoader());
        }

        static ConfigMappingHandler find(final Class<?> type, final ClassLoader classLoader) {
            List<ConfigMappingHandler> handlers = HANDLERS.computeIfAbsent(classLoader, Handlers::load);
            Holder<ConfigMappingHandler> holder = CACHE.get(type);
            if (holder.value != null) {
                return holder.value;
            }

            for (ConfigMappingHandler handler : handlers) {
                if (handler.handles(type)) {
                    return handler;
                }
            }
            return FallbackClassHandler.FALLBACK;
        }

        private static void set(final Class<?> type, final ConfigMappingHandler handler) {
            Holder<ConfigMappingHandler> holder = CACHE.get(type);
            if (holder.value == null) {
                synchronized (holder) {
                    if (holder.value == null) {
                        holder.value = handler;
                    }
                }
            } else if (!holder.value.getClass().equals(handler.getClass())) {
                throw ConfigMessages.msg.handlerAlreadyRegistered(type, holder.value, handler);
            }
        }

        private static List<ConfigMappingHandler> load(final ClassLoader classLoader) {
            List<ConfigMappingHandler> handlers = new ArrayList<>();
            for (ConfigMappingHandler handler : ServiceLoader.load(ConfigMappingHandler.class, classLoader)) {
                handlers.add(handler);
            }
            handlers.add(ConfigMappingInterfaceHandler.CONFIG_MAPPING);
            return List.copyOf(handlers);
        }
    }

    final class ConfigMappingInterfaceHandler implements ConfigMappingHandler {
        public static final ConfigMappingInterfaceHandler CONFIG_MAPPING = new ConfigMappingInterfaceHandler();

        private ConfigMappingInterfaceHandler() {
        }

        @Override
        public boolean handles(Class<?> type) {
            if (!type.isInterface() && type.isAnnotationPresent(ConfigMapping.class)) {
                throw ConfigMessages.msg.mappingAnnotationNotSupportedInClass(type);
            }
            return type.isInterface();
        }

        @Override
        public String getPrefix(Class<?> type) {
            ConfigMapping configMapping = type.getAnnotation(ConfigMapping.class);
            return configMapping != null ? configMapping.prefix() : "";
        }

        @Override
        public NamingStrategy getNamingStrategy(Class<?> type) {
            ConfigMapping configMapping = type.getAnnotation(ConfigMapping.class);
            return configMapping != null ? configMapping.namingStrategy() : NamingStrategy.KEBAB_CASE;
        }
    }

    final class FallbackClassHandler implements ConfigMappingHandler {
        public static final ConfigMappingHandler FALLBACK = new FallbackClassHandler();

        private FallbackClassHandler() {
        }

        @Override
        public boolean handles(Class<?> type) {
            return true;
        }

        @Override
        public String getPrefix(Class<?> type) {
            return "";
        }
    }
}
