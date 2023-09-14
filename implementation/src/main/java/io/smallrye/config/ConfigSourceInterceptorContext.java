package io.smallrye.config;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Exposes contextual information about the intercepted invocation of {@link ConfigSourceInterceptor}. This allows
 * implementers to control the behavior of the invocation chain.
 */
public interface ConfigSourceInterceptorContext extends Serializable {
    /**
     * Proceeds to the next interceptor in the chain.
     *
     * @param name the configuration name to lookup. Can be the original key.
     * @return a {@link ConfigValue} with information about the name, value, config source and ordinal, or {@code null}
     *         if the value isn't present.
     */
    ConfigValue proceed(String name);

    /**
     * Proceeds to the next interceptor in the chain.
     *
     * @return an Iterator of Strings with configuration names.
     */
    Iterator<String> iterateNames();

    /**
     * Proceeds to the next interceptor in the chain.
     *
     * @return an Iterator of {@link ConfigValue} with information about the name, value, config source and ordinal.
     */
    @Deprecated(forRemoval = true)
    Iterator<ConfigValue> iterateValues();
}
