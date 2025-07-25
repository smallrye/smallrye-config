package io.smallrye.config.common;

import java.io.Serial;
import java.io.Serializable;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A basic converter.
 */
public abstract class AbstractConverter<T> implements Converter<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 6881031847338407885L;
}
