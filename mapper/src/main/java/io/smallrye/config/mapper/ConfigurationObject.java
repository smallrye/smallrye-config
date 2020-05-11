package io.smallrye.config.mapper;

/**
 * An interface implemented internally by configuration object implementations.
 */
public interface ConfigurationObject {
    void fillInOptionals(MappingContext context);
}
