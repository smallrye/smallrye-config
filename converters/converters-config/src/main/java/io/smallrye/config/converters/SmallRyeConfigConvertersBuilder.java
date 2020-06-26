package io.smallrye.config.converters;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.converters.SmallRyeConvertersBuilder;

public class SmallRyeConfigConvertersBuilder {
    private SmallRyeConvertersBuilder convertersBuilder = new SmallRyeConvertersBuilder();

    public SmallRyeConfigConvertersBuilder() {
    }

    public SmallRyeConvertersBuilder getConvertersBuilder() {
        return convertersBuilder;
    }

    public SmallRyeConfigConvertersBuilder withConverters(List<Converter<?>> converters) {
        withConverters(converters.toArray(new Converter<?>[0]));
        return this;
    }

    // For compatibility reasons with SmallRye Config
    public SmallRyeConfigConvertersBuilder withConverters(Map<Type, Converter<?>> converters) {
        withConverters(converters.values().toArray(new Converter<?>[0]));
        return this;
    }

    public SmallRyeConfigConvertersBuilder withConverters(Converter<?>[] converters) {
        for (Converter<?> converter : converters) {
            withConverter(converter);
        }
        return this;
    }

    public <T> SmallRyeConfigConvertersBuilder withConverter(Converter<T> converter) {
        Type type = getConverterType(converter.getClass());
        if (type == null) {
            throw new IllegalStateException(
                    "Can not add converter " + converter + " that is not parameterized with a type");
        }

        withConverter(type, getPriority(converter), converter);
        return this;
    }

    public <T> SmallRyeConfigConvertersBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        this.convertersBuilder.withConverter(type, priority, converter::convert);
        return this;
    }

    public <T> SmallRyeConfigConvertersBuilder withConverter(Type type, int priority, Converter<T> converter) {
        this.convertersBuilder.withConverter(type, priority, converter::convert);
        return this;
    }

    public ConfigConverters build() {
        return new SmallRyeConfigConverters(this);
    }

    private static Type getConverterType(Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return null;
        }

        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType().equals(Converter.class)) {
                    Type[] typeArguments = pt.getActualTypeArguments();
                    if (typeArguments.length != 1) {
                        throw new IllegalStateException("Converter " + clazz + " must be parameterized with a single type");
                    }
                    return typeArguments[0];
                }
            }
        }

        return getConverterType(clazz.getSuperclass());
    }

    private static int getPriority(Converter<?> converter) {
        int priority = 100;
        Priority priorityAnnotation = converter.getClass().getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }
        return priority;
    }
}
