package io.smallrye.config.crypto;

import java.util.NoSuchElementException;

import io.smallrye.config.ConfigMessages;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;

public class AESGCMNoPaddingSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    @Override
    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
        String encryptionKey = requireValue(context, "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key");
        return new AESGCMNoPaddingSecretKeysHandler(encryptionKey);
    }

    @Override
    public String getName() {
        return "aes-gcm-nopadding";
    }

    private static String requireValue(final ConfigSourceContext context, final String name) {
        ConfigValue value = context.getValue(name);
        if (value != null) {
            return value.getValue();
        }
        throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name));
    }
}
