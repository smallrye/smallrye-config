package io.smallrye.config;

import java.util.Iterator;

import io.smallrye.common.annotation.Experimental;

/**
 * Exposes contextual information on the ConfigSource initialization via {@link ConfigSourceFactory}.
 */
@Experimental("ConfigSource API Enhancements")
public interface ConfigSourceContext {
    ConfigValue getValue(String name);

    Iterator<String> iterateNames();
}
