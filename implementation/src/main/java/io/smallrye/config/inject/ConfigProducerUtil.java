package io.smallrye.config.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.Converters;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;

/**
 * Actual implementations for producer method in CDI producer {@link ConfigProducer}.
 *
 * @author <a href="https://github.com/guhilling">Gunnar Hilling</a>
 */
public class ConfigProducerUtil {

    private ConfigProducerUtil() {
    }

    public static <T> T getValue(InjectionPoint injectionPoint, Config config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }
        final SmallRyeConfig src = (SmallRyeConfig) config;
        Converter<T> converter = resolveConverter(injectionPoint, src);
        String rawValue = getRawValue(src, name);
        if (rawValue == null) {
            rawValue = getDefaultValue(injectionPoint);
        }
        T converted;
        if (rawValue == null) {
            // convert an empty value
            try {
                converted = converter.convert("");
            } catch (IllegalArgumentException ignored) {
                throw InjectionMessages.msg.propertyNotFound(name);
            }
        } else {
            converted = converter.convert(rawValue);
        }
        if (converted == null) {
            throw InjectionMessages.msg.propertyNotFound(name);
        }
        return converted;
    }

    private static String getRawValue(SmallRyeConfig config, String name) {
        return SecretKeys.doUnlocked(() -> config.getRawValue(name));
    }

    private static <T> Converter<T> resolveConverter(final InjectionPoint injectionPoint, final SmallRyeConfig src) {
        return resolveConverter(injectionPoint.getType(), src);
    }

    @SuppressWarnings("unchecked")
    private static <T> Converter<T> resolveConverter(final Type type, final SmallRyeConfig src) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) Converters.newCollectionConverter(resolveConverter(typeArgs[0], src), ArrayList::new);
            } else if (rawType == Set.class) {
                return (Converter<T>) Converters.newCollectionConverter(resolveConverter(typeArgs[0], src), HashSet::new);
            } else if (rawType == Optional.class) {
                return (Converter<T>) Converters.newOptionalConverter(resolveConverter(typeArgs[0], src));
            } else if (rawType == Supplier.class) {
                return resolveConverter(typeArgs[0], src);
            }
        }
        // just try the raw type
        return src.getConverter(rawType);
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

    public static String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty) qualifier);
                return ConfigExtension.getConfigKey(injectionPoint, configProperty);
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
}
