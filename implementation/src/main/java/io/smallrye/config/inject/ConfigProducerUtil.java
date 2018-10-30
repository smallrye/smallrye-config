package io.smallrye.config.inject;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.StringUtil;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Actual implementations for producer method in CDI producer {@link ConfigProducer}.
 *
 * @author <a href="https://github.com/guhilling">Gunnar Hilling</a>
 */
public class ConfigProducerUtil {

    private ConfigProducerUtil() {
    }

    public static <T> Optional<T> optionalConfigValue(InjectionPoint injectionPoint, Config config) {
        Type type = injectionPoint.getAnnotated().getBaseType();
        final Class<T> valueType;
        valueType = resolveValueType(type);
        return Optional.ofNullable(getValue(injectionPoint, valueType, config));
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> resolveValueType(Type type) {
        Class<T> valueType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            valueType = unwrapType(typeArguments[0]);
        } else {
            valueType = (Class<T>) String.class;
        }
        return valueType;
    }

    public static <C extends Collection<T>, T> C collectionConfigProperty(InjectionPoint injectionPoint, Config config, C collection) {
        Type type = injectionPoint.getAnnotated().getBaseType();
        final Class<T> valueType = resolveValueType(type);
        String stringValue = getValue(injectionPoint, String.class, config);
        String[] split = StringUtil.split(stringValue);
        for (String aSplit : split) {
            T item = ((SmallRyeConfig) config).convert(aSplit, valueType);
            collection.add(item);
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> unwrapType(Type type) {
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getRawType();
        }
        return (Class<T>) type;
    }

    public static <T> T getValue
            (InjectionPoint injectionPoint, Class<T> target, Config config) {
        String name = getName(injectionPoint);
        try {
            if (name == null) {
                return null;
            }
            Optional<T> optionalValue = config.getOptionalValue(name, target);
            if (optionalValue.isPresent()) {
                return optionalValue.get();
            } else {
                String defaultValue = getDefaultValue(injectionPoint);
                if (defaultValue != null && !defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                    return ((SmallRyeConfig) config).convert(defaultValue, target);
                } else {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty)qualifier);
                return ConfigExtension.getConfigKey(injectionPoint, configProperty);
            }
        }
        return null;
    }

    private static String getDefaultValue(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                return ((ConfigProperty) qualifier).defaultValue();
            }
        }
        return null;
    }
}
