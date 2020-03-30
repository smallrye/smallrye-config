package io.smallrye.config;

import java.io.Serializable;

/**
 * The ConfigSourceInterceptor allows you to intercept the resolution of a configuration key name before the
 * configuration value is resolved by the Config and before any conversion taking place.
 * <p>
 *
 * This is useful to provide logging, transform the key or substitute the value.
 * <p>
 *
 * Implementations of {@link ConfigSourceInterceptor} are loaded via the {@link java.util.ServiceLoader} mechanism and
 * can be registered by providing a resource named {@code META-INF/services/io.smallrye.config.ConfigSourceInterceptor},
 * which contains the fully qualified {@code ConfigSourceInterceptor} implementation class name as its content.
 * <p>
 *
 * A {@link ConfigSourceInterceptor} implementation class can specify a priority by way of the standard
 * {@code javax.annotation.Priority} annotation. If no priority is explicitly assigned, the default priority value
 * of {@code 100} is assumed. If multiple interceptors are registered with the same priority, then their execution
 * order may be non deterministic.
 */
public interface ConfigSourceInterceptor extends Serializable {
    /**
     * Single method to intercept the resolution of a configuration key. Calling
     * {@link ConfigSourceInterceptorContext#proceed(String)} will proceed executing the interceptor chain. The chain
     * can be short-circuited by returning your own instance of {@link ConfigValue}.
     *
     * @param context the interceptor context. See {@link ConfigSourceInterceptorContext}
     * @param name the configuration key name being intercepted.
     *
     * @return a ConfigValue with information about the key name, value, config source origin and ordinal.
     */
    ConfigValue getValue(ConfigSourceInterceptorContext context, String name);
}
