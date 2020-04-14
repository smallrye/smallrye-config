package io.smallrye.config;

import java.util.OptionalInt;

/**
 * This ConfigSourceInterceptorFactory allows to initialize a {@link ConfigSourceInterceptor}, with access to the
 * current {@link ConfigSourceInterceptorContext}.
 *
 * Interceptors in the chain are initialized in priority order and the current
 * {@link ConfigSourceInterceptorContext} contains the current interceptor, plus all other interceptors already
 * initialized.
 */
public interface ConfigSourceInterceptorFactory {
    /**
     * The default priority value, {@code 100}.
     */
    int DEFAULT_PRIORITY = 100;

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
