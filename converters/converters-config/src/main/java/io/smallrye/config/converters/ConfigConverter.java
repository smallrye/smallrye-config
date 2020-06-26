package io.smallrye.config.converters;

import io.smallrye.converters.api.Converter;

public interface ConfigConverter<T> extends Converter<T>, org.eclipse.microprofile.config.spi.Converter<T> {
}
