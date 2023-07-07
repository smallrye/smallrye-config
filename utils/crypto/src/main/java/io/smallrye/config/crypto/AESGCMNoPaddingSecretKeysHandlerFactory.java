package io.smallrye.config.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import io.smallrye.config.ConfigMessages;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;

public class AESGCMNoPaddingSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    public static final String ENCRYPTION_KEY = "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key";

    @Override
    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
        ConfigValue encryptionKey = context.getValue(ENCRYPTION_KEY);
        if (encryptionKey == null || encryptionKey.getValue() == null) {
            throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(ENCRYPTION_KEY));
        }

        boolean decode = true;
        ConfigValue plain = context.getValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key-decode");
        if (plain != null && plain.getValue() != null) {
            decode = Converters.getImplicitConverter(Boolean.class).convert(plain.getValue());
        }

        byte[] encryptionKeyBytes = decode ? Base64.getUrlDecoder().decode(encryptionKey.getValue())
                : encryptionKey.getValue().getBytes(StandardCharsets.UTF_8);

        return new AESGCMNoPaddingSecretKeysHandler(encryptionKeyBytes);
    }

    @Override
    public String getName() {
        return "aes-gcm-nopadding";
    }
}
