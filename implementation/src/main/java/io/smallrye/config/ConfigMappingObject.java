package io.smallrye.config;

/**
 * An interface implemented internally by configuration object implementations.
 */
public interface ConfigMappingObject {
    void fillInOptionals(ConfigMappingContext context);
    void fillInSuppliers(ConfigMappingContext context);
}
