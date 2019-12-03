package io.smallrye.config.common;

import java.io.Serializable;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A basic converter.
 */
public abstract class AbstractConverter<T> implements Converter<T>, Serializable {
    private static final long serialVersionUID = 6881031847338407885L;
}
