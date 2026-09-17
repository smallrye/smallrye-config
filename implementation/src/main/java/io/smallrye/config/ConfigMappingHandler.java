package io.smallrye.config;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

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
        /**
         * The handlers discovered for each {@link ClassLoader}, so that all configuration types loaded by the same
         * loader share a single {@link ServiceLoader} discovery pass.
         * <p>
         * The map is keyed weakly by the loader and holds the handlers through a {@link WeakReference}, so it never
         * keeps a loader (nor its application-provided handlers, which strongly reference it) from being collected.
         * The strong reference that keeps a live loader's handlers around is the {@link #DISCOVERED} entry, which
         * lives in the {@code classValueMap} of the configuration type {@link Class} and is therefore collected
         * together with the loader. Only {@link #find(Class)} publishes the handlers there, so a lookup that goes
         * through {@link #find(Class, ClassLoader)} alone may have to discover them again.
         */
        private static final Map<ClassLoader, WeakReference<List<ConfigMappingHandler>>> HANDLERS = new WeakHashMap<>();
        /**
         * The handler registered for each configuration type. A {@link ClassValue} stores its values in the key
         * {@link Class} own {@code classValueMap}, so the registration is collected together with the type and its
         * {@link ClassLoader}. A {@code WeakHashMap} keyed by {@link Class} cannot do the same, because the handler
         * may be loaded by the key's own {@link ClassLoader}, which keeps the entry alive forever.
         * <p>
         * The box is an {@link AtomicReference} rather than a type of ours, because the key {@link Class} holds the
         * entry strongly, and so holds the class of whatever the entry contains. Nothing filters the types this is
         * asked about, so most entries stay empty, and an {@link AtomicReference} is defined by the bootstrap loader,
         * which means an empty entry costs the type nothing.
         */
        private static final ClassValue<AtomicReference<ConfigMappingHandler>> REGISTERED = new ClassValue<>() {
            @Override
            protected AtomicReference<ConfigMappingHandler> computeValue(Class<?> type) {
                return new AtomicReference<>();
            }
        };
        /**
         * The handlers discovered for the {@link ClassLoader} of each configuration type, memoizing
         * {@link #load(ClassLoader)}. Kept apart from {@link #REGISTERED} because the two do not have the same
         * concurrency requirements: a registration has to report a conflict with the one already in place, while a
         * discovery only has to publish a list that {@link #load(ClassLoader)} is already returning per loader, so
         * threads that race here publish the same instance and do not repeat the discovery.
         */
        private static final ClassValue<AtomicReference<List<ConfigMappingHandler>>> DISCOVERED = new ClassValue<>() {
            @Override
            protected AtomicReference<List<ConfigMappingHandler>> computeValue(Class<?> type) {
                return new AtomicReference<>();
            }
        };

        static void register(final Class<?> type, final ConfigMappingHandler handler) {
            set(type, handler);
        }

        static ConfigMappingHandler get(final Class<?> type) {
            ConfigMappingHandler registered = REGISTERED.get(type).get();
            if (registered == null) {
                throw ConfigMessages.msg.handlerNotRegistered(type);
            }
            return registered;
        }

        static ConfigMappingHandler find(final Class<?> type) {
            AtomicReference<List<ConfigMappingHandler>> holder = DISCOVERED.get(type);
            List<ConfigMappingHandler> discovered = holder.get();
            if (discovered == null) {
                // unguarded on purpose: threads that race here use the same list, they do not discover it twice
                discovered = load(type.getClassLoader());
                holder.set(discovered);
            }
            return find(type, discovered);
        }

        static ConfigMappingHandler find(final Class<?> type, final ClassLoader classLoader) {
            return find(type, load(classLoader));
        }

        private static ConfigMappingHandler find(final Class<?> type, final List<ConfigMappingHandler> handlers) {
            ConfigMappingHandler registered = REGISTERED.get(type).get();
            if (registered != null) {
                return registered;
            }

            for (ConfigMappingHandler handler : handlers) {
                if (handler.handles(type)) {
                    return handler;
                }
            }
            return FallbackClassHandler.FALLBACK;
        }

        private static void set(final Class<?> type, final ConfigMappingHandler handler) {
            // the first registration wins, and the witness tells the loser whether it disagreed with it
            ConfigMappingHandler registered = REGISTERED.get(type).compareAndExchange(null, handler);
            if (registered != null && !registered.getClass().equals(handler.getClass())) {
                throw ConfigMessages.msg.handlerAlreadyRegistered(type, registered, handler);
            }
        }

        private static synchronized List<ConfigMappingHandler> load(final ClassLoader classLoader) {
            WeakReference<List<ConfigMappingHandler>> reference = HANDLERS.get(classLoader);
            if (reference != null) {
                List<ConfigMappingHandler> handlers = reference.get();
                if (handlers != null) {
                    return handlers;
                }
            }

            List<ConfigMappingHandler> handlers = new ArrayList<>();
            for (ConfigMappingHandler handler : ServiceLoader.load(ConfigMappingHandler.class, classLoader)) {
                handlers.add(handler);
            }
            handlers.add(ConfigMappingInterfaceHandler.CONFIG_MAPPING);

            HANDLERS.put(classLoader, new WeakReference<>(handlers));
            return handlers;
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
