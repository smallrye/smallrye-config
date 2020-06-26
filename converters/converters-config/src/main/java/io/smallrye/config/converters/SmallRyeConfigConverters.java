package io.smallrye.config.converters;

import java.util.Collection;
import java.util.Optional;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.converters.Converters;
import io.smallrye.converters.SmallRyeConverters;

public class SmallRyeConfigConverters implements ConfigConverters {
    private SmallRyeConverters converters;

    SmallRyeConfigConverters(final SmallRyeConfigConvertersBuilder builder) {
        this.converters = builder.getConvertersBuilder().build();
    }

    @Override
    public <T> ConfigConverter<T> getConverter(final Class<T> asType) {
        return (ConfigConverter<T>) value -> converters.getConverter(asType).convert(value);
    }

    @Override
    public <T> ConfigConverter<Optional<T>> getOptionalConverter(final Class<T> asType) {
        return (ConfigConverter<Optional<T>>) value -> converters.getOptionalConverter(asType).convert(value);
    }

    @Override
    public <T> T convertValue(final String value, final Class<T> asType) {
        return getConverter(asType).convert(value);
    }

    @Override
    public <T> T convertValue(final String value, final io.smallrye.converters.api.Converter<T> converter) {
        return converter.convert(value);
    }

    public <T> T convertValue(final String value, final ConfigConverter<T> converter) {
        return converter.convert(value);
    }

    public static <T> Converter<Optional<T>> newOptionalConverter(Converter<? extends T> delegateConverter) {
        return Converters.<T> newOptionalConverter(delegateConverter::convert)::convert;
    }

    public static <T, C extends Collection<T>> Converter<C> newCollectionConverter(
            Converter<? extends T> itemConverter,
            IntFunction<C> collectionFactory) {
        return Converters.newCollectionConverter(itemConverter::convert, collectionFactory)::convert;
    }
}
