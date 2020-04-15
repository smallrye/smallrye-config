package io.smallrye.config;

import java.io.Serializable;

/**
 * Exposes contextual information about the intercepted invocation of {@link ConfigSourceInterceptor}. This allows you
 * to control the behavior of the invocation chain.
 */
public interface ConfigSourceInterceptorContext extends Serializable {
    /**
     * Proceeds to the next interceptor.
     *
     * @param name the new key name to lookup. Can be the original key.
     * @return a {@link ConfigValue} with information about the key, lookup value and source ConfigSource.
     */
    ConfigValue proceed(String name);
}
