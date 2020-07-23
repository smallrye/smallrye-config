package io.smallrye.config;

/**
 * An interface implemented internally by configuration object implementations.
 */
interface ConfigMappingObject {
    void fillInOptionals(ConfigMappingContext context);
}
