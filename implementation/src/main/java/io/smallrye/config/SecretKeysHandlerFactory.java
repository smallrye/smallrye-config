package io.smallrye.config;

import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.common.annotation.Experimental;

@Experimental("")
public interface SecretKeysHandlerFactory {
    SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context);

    String getName();

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
