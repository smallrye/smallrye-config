package io.smallrye.config;

import java.util.OptionalInt;

import io.smallrye.common.annotation.Experimental;

/**
 * This ConfigSourceInterceptorFactory allows to initialize a {@link ConfigSourceInterceptor}, with access to the
 * current {@link ConfigSourceInterceptorContext}.
 * <p>
 *
 * Interceptors in the chain are initialized in priority order and the current
 * {@link ConfigSourceInterceptorContext} contains the current interceptor, plus all other interceptors already
 * initialized.
 * <p>
 *
 * Instances of this interface will be discovered by {@code SmallRyeConfigBuilder#addDiscoveredInterceptors()} via the
 * {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/io.smallrye.config.ConfigSourceInterceptorFactory}
 * {@linkplain ClassLoader#getResource(String) resource} which contains the fully qualified class name of the
 * custom {@code ConfigSourceProvider} implementation.
 */
@Experimental("Interceptor API to intercept resolution of a configuration name")
public interface ConfigSourceInterceptorFactory {
    /**
     * The default priority value, {@link Priorities#APPLICATION}.
     */
    int DEFAULT_PRIORITY = Priorities.APPLICATION;

    /**
     * Gets the {@link ConfigSourceInterceptor} from the ConfigSourceInterceptorFactory. Implementations of this
     * method must provide the instance of the {@link ConfigSourceInterceptor} to add into the Config Interceptor Chain.
     *
     * @param context the current {@link ConfigSourceInterceptorContext} with the interceptors already initialized.
     * @return the {@link ConfigSourceInterceptor} to add to Config Interceptor Chain and initialize.
     */
    ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context);

    /**
     * Returns the interceptor priority. This is required, because the interceptor priority needs to be sorted
     * before doing initialization.
     *
     * @return the priority value.
     */
    default OptionalInt getPriority() {
        return OptionalInt.empty();
    }
}
