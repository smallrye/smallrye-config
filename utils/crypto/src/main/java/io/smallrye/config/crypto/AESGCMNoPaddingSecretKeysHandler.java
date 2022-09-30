package io.smallrye.config.crypto;

import io.smallrye.config.SecretKeysHandler;

public class AESGCMNoPaddingSecretKeysHandler implements SecretKeysHandler {
    // TODO - Should we provide a way to configure the handler?

    @Override
    public String handleSecret(final String secret) {
        // TODO - handle implementation.
        return "decoded";
    }

    @Override
    public String getName() {
        return "aes-gcm-nopadding";
    }
}
