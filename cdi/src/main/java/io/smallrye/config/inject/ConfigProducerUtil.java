package io.smallrye.config.inject;

import static io.smallrye.config.Converters.createCollectionFactory;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newMapConverter;
import static io.smallrye.config.Converters.newOptionalConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;

/**
 * Actual implementations for producer method in CDI producer {@link ConfigProducer}.
 *
 * @author <a href="https://github.com/guhilling">Gunnar Hilling</a>
 */
public final class ConfigProducerUtil {

    private ConfigProducerUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a converted configuration value from {@link Config}.
     *
     * @param injectionPoint the {@link InjectionPoint} where the configuration value will be injected
     * @param config the current {@link Config} instance.
     *
     * @return the converted configuration value.
     */
    public static <T> T getValue(InjectionPoint injectionPoint, Config config) {
        return getValue(getName(injectionPoint), getType(injectionPoint), getDefaultValue(injectionPoint), config);
    }

    private static Type getType(InjectionPoint injectionPoint) {
        Type type = injectionPoint.getType();
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType().equals(Provider.class)
                    || parameterizedType.getRawType().equals(Instance.class)) {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return type;
    }

    /**
     * Retrieves a converted configuration value from {@link Config}.
     *
     * @param name the name of the configuration property.
     * @param type the {@link Type} of the configuration value to convert.
     * @param defaultValue the default value to use if no configuration value is found.
     * @param config the current {@link Config} instance.
     *
     * @return the converted configuration value.
     */
    public static <T> T getValue(String name, Type type, String defaultValue, Config config) {
        if (name == null) {
            return null;
        }

        SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);
        // Injected values are allowed to read secret keys, so the whole resolution runs unlocked.
        return SecretKeys.doUnlocked(() -> resolveValue(name, type, defaultValue, smallRyeConfig));
    }

    private static <T> T resolveValue(final String name, final Type type, final String defaultValue,
            final SmallRyeConfig config) {
        if (isType(type, Supplier.class)) {
            return resolveValue(name, ((ParameterizedType) type).getActualTypeArguments()[0], defaultValue, config);
        }

        if (hasCollection(type)) {
            return convertCollection(name, type, defaultValue, config);
        } else if (hasMap(type)) {
            return convertMap(name, type, defaultValue, config);
        }

        ConfigValue configValue = config.getConfigValue(name);
        configValue = configValue.withValue(resolveDefault(configValue.getValue(), defaultValue));
        return config.convertValue(configValue, resolveConverter(type, config));
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertCollection(final String name, final Type type, final String defaultValue,
            final SmallRyeConfig config) {
        boolean optional = isType(type, Optional.class);
        ParameterizedType collectionType = (ParameterizedType) (optional
                ? ((ParameterizedType) type).getActualTypeArguments()[0]
                : type);

        ConfigValue configValue = config.getConfigValue(name);
        if (defaultValue != null && configValue.getValue() == null && config.getIndexedProperties(name).isEmpty()) {
            return config.convertValue(configValue.withValue(defaultValue), resolveConverter(type, config));
        }

        Converter<Object> itemConverter = resolveConverter(collectionType.getActualTypeArguments()[0], config);
        IntFunction<? extends Collection<Object>> collectionFactory = createCollectionFactory(rawTypeOf(collectionType));
        return optional
                ? (T) config.getOptionalValues(name, itemConverter, collectionFactory)
                : (T) config.getValues(name, itemConverter, collectionFactory);
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertMap(final String name, final Type type, final String defaultValue,
            final SmallRyeConfig config) {
        boolean optional = isType(type, Optional.class);
        ParameterizedType mapType = (ParameterizedType) (optional ? ((ParameterizedType) type).getActualTypeArguments()[0]
                : type);
        Type valueType = mapType.getActualTypeArguments()[1];

        if (isType(valueType, List.class) || isType(valueType, Set.class)) {
            ConfigValue configValue = config.getConfigValue(name);
            if (defaultValue != null && configValue.getValue() == null && config.getMapIndexedKeys(name).isEmpty()) {
                return config.convertValue(configValue.withValue(defaultValue), resolveConverter(type, config));
            }

            Converter<Object> keyConverter = resolveConverter(mapType.getActualTypeArguments()[0], config);
            ParameterizedType collectionType = (ParameterizedType) valueType;
            Converter<Object> valueConverter = resolveConverter(collectionType.getActualTypeArguments()[0], config);
            IntFunction<? extends Collection<Object>> collectionFactory = createCollectionFactory(rawTypeOf(collectionType));
            return optional
                    ? (T) config.getOptionalValues(name, keyConverter, valueConverter, HashMap::new, collectionFactory)
                    : (T) config.getValues(name, keyConverter, valueConverter, HashMap::new, collectionFactory);
        }

        ConfigValue configValue = config.getConfigValue(name);
        if (defaultValue != null && configValue.getValue() == null && config.getMapKeys(name).isEmpty()) {
            return config.convertValue(configValue.withValue(defaultValue), resolveConverter(type, config));
        }

        Converter<Object> keyConverter = resolveConverter(mapType.getActualTypeArguments()[0], config);
        Converter<Object> valueConverter = resolveConverter(valueType, config);
        return optional
                ? (T) config.getOptionalValues(name, keyConverter, valueConverter, HashMap::new)
                : (T) config.getValues(name, keyConverter, valueConverter, HashMap::new);
    }

    private static boolean isType(final Type type, final Class<?> rawType) {
        return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(rawType);
    }

    static ConfigValue getConfigValue(InjectionPoint injectionPoint, SmallRyeConfig config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }

        io.smallrye.config.ConfigValue configValue = config.getConfigValue(name);
        if (configValue.getRawValue() == null) {
            configValue = configValue.withValue(getDefaultValue(injectionPoint));
        }

        return configValue;
    }

    static ConfigValue getConfigValue(String name, SmallRyeConfig config) {
        return SecretKeys.doUnlocked(() -> config.getConfigValue(name));
    }

    private static String resolveDefault(String rawValue, String defaultValue) {
        return rawValue != null ? rawValue : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static <T> Converter<T> resolveConverter(final Type type, final SmallRyeConfig config) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) newCollectionConverter(resolveConverter(typeArgs[0], config), ArrayList::new);
            } else if (rawType == Set.class) {
                return (Converter<T>) newCollectionConverter(resolveConverter(typeArgs[0], config), HashSet::new);
            } else if (rawType == Map.class) {
                return (Converter<T>) newMapConverter(resolveConverter(typeArgs[0], config),
                        resolveConverter(typeArgs[1], config), HashMap::new);
            } else if (rawType == Optional.class) {
                return (Converter<T>) newOptionalConverter(resolveConverter(typeArgs[0], config));
            } else if (rawType == Supplier.class) {
                return resolveConverter(typeArgs[0], config);
            }
        }
        // just try the raw type
        return config.getConverter(rawType).orElseThrow(() -> InjectionMessages.msg.noRegisteredConverter(rawType));
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

    private static boolean hasMap(final Type type) {
        Class<?> rawType = rawTypeOf(type);
        if (rawType == Map.class) {
            return true;
        } else if (type instanceof ParameterizedType) {
            return hasMap(((ParameterizedType) type).getActualTypeArguments()[0]);
        }
        return false;
    }

    private static <T> boolean hasCollection(final Type type) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType paramType) {
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

    private static String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty) qualifier);
                return getConfigKey(injectionPoint, configProperty);
            }
        }
        return null;
    }

    private static String getDefaultValue(InjectionPoint injectionPoint) {
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

    static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (!key.trim().isEmpty()) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember<?> member) {
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
}
