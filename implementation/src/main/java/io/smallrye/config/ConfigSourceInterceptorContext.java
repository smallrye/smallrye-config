package io.smallrye.config;

import java.io.Serializable;

public interface ConfigSourceInterceptorContext extends Serializable {
    /**
     * Proceeds to the next interceptor.
     *
     * @param name the new key name to lookup. Can be the original key.
     * @return a ConfigValue with information about the key, lookup value and source ConfigSource.
     */
    ConfigValue proceed(String name);
}
