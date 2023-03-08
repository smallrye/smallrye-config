package io.smallrye.config;

import io.smallrye.common.annotation.Experimental;

@Experimental("")
public interface SecretKeysHandlerFactory {
    SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context);

    String getName();
}
