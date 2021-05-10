package io.smallrye.config.inject;

import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newOptionalConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.common.AbstractDelegatingConverter;

/**
 * Actual implementations for producer method in CDI producer {@link ConfigProducer}.
 *
 * @author <a href="https://github.com/guhilling">Gunnar Hilling</a>
 */
public final class ConfigProducerUtil {

    private ConfigProducerUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T> T getValue(InjectionPoint injectionPoint, Config config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }

        String defaultValue = getDefaultValue(injectionPoint);
        String rawValue = getRawValue(name, config);
        if (hasCollection(injectionPoint.getType())) {
            return convertValues(name, injectionPoint.getType(), rawValue, defaultValue, config);
        }

        return SmallRyeConfig.convertValue(name, resolveDefault(rawValue, defaultValue),
                resolveConverter(injectionPoint, config));
    }

    public static <T> T getValue(String name, Type type, String defaultValue, Config config) {
        if (name == null) {
            return null;
        }
        String resolvedValue = resolveValue(name, defaultValue, config);
        return SmallRyeConfig.convertValue(name, resolvedValue, resolveConverter(type, config));
    }

    public static <T> T convertValues(String name, Type type, String rawValue, String defaultValue, Config config) {
        List<String> indexedProperties = ((SmallRyeConfig) config).getIndexedProperties(name);
        // If converting a config property which exists (i.e. myProp[1] = aValue) or no indexed properties exist for the config property
        if (rawValue != null || indexedProperties.isEmpty()) {
            return SmallRyeConfig.convertValue(name, resolveDefault(rawValue, defaultValue), resolveConverter(type, config));
        }

        BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter = (itemConverter,
                collectionFactory) -> {
            Collection<T> collection = collectionFactory.apply(indexedProperties.size());
            for (String indexedProperty : indexedProperties) {
                // Never null by the rules of converValue
                collection.add(
                        SmallRyeConfig.convertValue(indexedProperty, resolveValue(indexedProperty, null, config),
                                itemConverter));
            }
            return collection;
        };

        return resolveConverterForIndexed(type, config, indexedConverter).convert(" ");
    }

    public static ConfigValue getConfigValue(InjectionPoint injectionPoint, Config config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }

        ConfigValue configValue = config.getConfigValue(name);
        if (configValue.getRawValue() == null) {
            if (configValue instanceof io.smallrye.config.ConfigValue) {
                configValue = ((io.smallrye.config.ConfigValue) configValue).withValue(getDefaultValue(injectionPoint));
            }
        }

        return configValue;
    }

    public static String getRawValue(String name, Config config) {
        return SecretKeys.doUnlocked(() -> config.getConfigValue(name).getValue());
    }

    private static String resolveValue(String name, String defaultValue, Config config) {
        String rawValue = getRawValue(name, config);
        return resolveDefault(rawValue, defaultValue);
    }

    private static String resolveDefault(String rawValue, String defaultValue) {
        return rawValue != null ? rawValue : defaultValue;
    }

    public static <T> Converter<T> resolveConverter(final InjectionPoint injectionPoint, final Config config) {
        return resolveConverter(injectionPoint.getType(), config);
    }

    @SuppressWarnings("unchecked")
    private static <T> Converter<T> resolveConverter(final Type type, final Config config) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) newCollectionConverter(resolveConverter(typeArgs[0], config), ArrayList::new);
            } else if (rawType == Set.class) {
                return (Converter<T>) newCollectionConverter(resolveConverter(typeArgs[0], config), HashSet::new);
            } else if (rawType == Optional.class) {
                return (Converter<T>) newOptionalConverter(resolveConverter(typeArgs[0], config));
            } else if (rawType == Supplier.class) {
                return resolveConverter(typeArgs[0], config);
            }
        }
        // just try the raw type
        return config.getConverter(rawType).orElseThrow(() -> InjectionMessages.msg.noRegisteredConverter(rawType));
    }

    /**
     * We need to handle indexed properties in a special way, since a Collection may be wrapped in other converters.
     * The issue is that in the original code the value was retrieve by calling the first converter that will delegate
     * to all the wrapped types until it finally gets the result. For indexed properties, because it requires
     * additional key lookups, a special converter is added to perform the work. This is mostly a workaround, since
     * converters do not have the proper API, and probably should not have to handle this type of logic.
     *
     * @see IndexedCollectionConverter
     */
    @SuppressWarnings("unchecked")
    private static <T> Converter<T> resolveConverterForIndexed(
            final Type type,
            final Config config,
            final BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter) {

        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) new IndexedCollectionConverter<>(resolveConverter(typeArgs[0], config), ArrayList::new,
                        indexedConverter);
            } else if (rawType == Set.class) {
                return (Converter<T>) new IndexedCollectionConverter<>(resolveConverter(typeArgs[0], config), HashSet::new,
                        indexedConverter);
            } else if (rawType == Optional.class) {
                return (Converter<T>) newOptionalConverter(resolveConverterForIndexed(typeArgs[0], config, indexedConverter));
            } else if (rawType == Supplier.class) {
                return resolveConverterForIndexed(typeArgs[0], config, indexedConverter);
            }
        }

        throw new IllegalStateException();
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> rawTypeOf(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return (Class<T>) Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else {
            throw InjectionMessages.msg.noRawType(type);
        }
    }

    private static <T> boolean hasCollection(final Type type) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return true;
            } else if (rawType == Set.class) {
                return true;
            } else {
                return hasCollection(typeArgs[0]);
            }
        }
        return false;
    }

    public static String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty) qualifier);
                return getConfigKey(injectionPoint, configProperty);
            }
        }
        return null;
    }

    public static String getDefaultValue(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                String str = ((ConfigProperty) qualifier).defaultValue();
                if (!ConfigProperty.UNCONFIGURED_VALUE.equals(str)) {
                    return str;
                }
                Class<?> rawType = rawTypeOf(injectionPoint.getType());
                if (rawType.isPrimitive()) {
                    if (rawType == char.class) {
                        return null;
                    } else if (rawType == boolean.class) {
                        return "false";
                    } else {
                        return "0";
                    }
                }
                return null;
            }
        }
        return null;
    }

    public static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (!key.trim().isEmpty()) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember<?> member = (AnnotatedMember<?>) ip.getAnnotated();
            AnnotatedType<?> declaringType = member.getDeclaringType();
            if (declaringType != null) {
                String[] parts = declaringType.getJavaClass().getCanonicalName().split("\\.");
                StringBuilder sb = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    sb.append(".").append(parts[i]);
                }
                sb.append(".").append(member.getJavaMember().getName());
                return sb.toString();
            }
        }
        throw InjectionMessages.msg.noConfigPropertyDefaultName(ip);
    }

    final static class IndexedCollectionConverter<T, C extends Collection<T>> extends AbstractDelegatingConverter<T, C> {
        private static final long serialVersionUID = 5186940408317652618L;

        private final IntFunction<Collection<T>> collectionFactory;
        private final BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter;

        public IndexedCollectionConverter(
                final Converter<T> resolveConverter,
                final IntFunction<Collection<T>> collectionFactory,
                final BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter) {
            super(resolveConverter);

            this.collectionFactory = collectionFactory;
            this.indexedConverter = indexedConverter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public C convert(final String value) throws IllegalArgumentException, NullPointerException {
            return (C) indexedConverter.apply((Converter<T>) getDelegate(), collectionFactory);
        }
    }
}
