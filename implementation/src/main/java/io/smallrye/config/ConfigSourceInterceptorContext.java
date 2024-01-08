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
     * @param name the configuration name to look up (can be the original key)
     * @return a {@link ConfigValue} with information about the name, value, config source and ordinal, or {@code null}
     *         if the value isn't present.
     */
    ConfigValue proceed(String name);

    /**
     * Re-calls the first interceptor in the chain.
     * If the original name is given, then it is possible to cause a recursive loop, so care must be taken.
     * This method is intended to be used by relocating and other compatibility-related interceptors.
     *
     * @param name the configuration name to look up (can be the original key)
     * @return a {@link ConfigValue} with information about the name, value, config source and ordinal, or {@code null}
     *         if the value isn't present.
     */
    ConfigValue restart(String name);

    /**
     * {@return an iterator over the configuration names known to this interceptor.
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
