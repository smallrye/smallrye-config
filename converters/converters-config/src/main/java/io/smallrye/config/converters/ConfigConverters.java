package io.smallrye.config.converters;

import java.util.Optional;

import io.smallrye.converters.api.Converters;

public interface ConfigConverters extends Converters {
    @Override
    <T> ConfigConverter<T> getConverter(Class<T> asType);

    @Override
    <T> ConfigConverter<Optional<T>> getOptionalConverter(Class<T> asType);

    <T> T convertValue(final String value, final ConfigConverter<T> converter);
}
