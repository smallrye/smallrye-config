package io.smallrye.config;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This {@code SecretKeysHandlerFactory} allows to initialize a {@link SecretKeysHandler}, with access to the current
 * {@link ConfigSourceContext}.
 * <p>
 *
 * Instances of this interface will be discovered via the {@link java.util.ServiceLoader} mechanism and can be
 * registered by providing a {@code META-INF/services/io.smallrye.config.SecretKeysHandlerFactory} file, which contains
 * the fully qualified class name of the custom {@code SecretKeysHandlerFactory} implementation.
 */
public interface SecretKeysHandlerFactory {
    /**
     * Gets the {@link SecretKeysHandler} from the {@code SecretKeysHandlerFactory}. Implementations of this
     * method must provide an instance of the {@link SecretKeysHandler} to decode secret configuration values by the
     * {@link SecretKeysHandler} name.
     *
     * @param context the current {@link ConfigSourceContext} with access to the current configuration sources.
     * @return the {@link SecretKeysHandler} to decode secret configuration values with the name
     *         {@link SecretKeysHandlerFactory#getName()}.
     */
    SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context);

    /**
     * The name of {@code SecretKeysHandler}.
     *
     * @return the name of the {@code SecretKeysHandler}.
     */
    String getName();

    /**
     * Defers the initialization of a {@code SecretKeysHandlerFactory} to only when a value requires decoding. This
     * allows to initialize a {@link SecretKeysHandler} with configuration coming from sources provided by a
     * {@link ConfigSourceFactory}.
     */
    class LazySecretKeysHandler implements SecretKeysHandler {
        private final SecretKeysHandlerFactory factory;
        private final AtomicReference<SecretKeysHandler> handler = new AtomicReference<>();

        public LazySecretKeysHandler(final SecretKeysHandlerFactory factory) {
            this.factory = factory;
        }

        public SecretKeysHandler get(ConfigSourceContext configSourceContext) {
            if (handler.get() == null) {
                handler.compareAndSet(null, factory.getSecretKeysHandler(configSourceContext));
            }
            return handler.get();
        }

        @Override
        public String decode(final String secret) {
            if (handler.get() == null) {
                throw new IllegalStateException();
            }
            return handler.get().decode(secret);
        }

        @Override
        public String getName() {
            return factory.getName();
        }
    }
}
