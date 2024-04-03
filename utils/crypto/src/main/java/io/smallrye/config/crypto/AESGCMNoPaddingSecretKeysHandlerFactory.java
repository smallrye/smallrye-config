package io.smallrye.config.crypto;

import static io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandler.DECODE_KEY;
import static io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandler.ENCRYPTION_KEY;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config._private.ConfigMessages;

public class AESGCMNoPaddingSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    @Override
    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
        return new LazySecretKeysHandler(new SecretKeysHandlerFactory() {
            @Override
            public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
                ConfigValue encryptionKey = context.getValue(ENCRYPTION_KEY);
                if (encryptionKey == null || encryptionKey.getValue() == null) {
                    throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(ENCRYPTION_KEY));
                }

                boolean decode = false;
                ConfigValue plain = context.getValue(DECODE_KEY);
                if (plain != null && plain.getValue() != null) {
                    decode = Converters.getImplicitConverter(Boolean.class).convert(plain.getValue());
                }

                byte[] encryptionKeyBytes = decode ? Base64.getUrlDecoder().decode(encryptionKey.getValue())
                        : encryptionKey.getValue().getBytes(StandardCharsets.UTF_8);

                return new AESGCMNoPaddingSecretKeysHandler(encryptionKeyBytes);
            }

            @Override
            public String getName() {
                return AESGCMNoPaddingSecretKeysHandlerFactory.this.getName();
            }
        });
    }

    @Override
    public String getName() {
        return "aes-gcm-nopadding";
    }
}
