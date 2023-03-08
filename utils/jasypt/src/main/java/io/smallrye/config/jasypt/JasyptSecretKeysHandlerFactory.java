package io.smallrye.config.jasypt;

import java.util.NoSuchElementException;

import io.smallrye.config.ConfigMessages;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;

public class JasyptSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    @Override
    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
        String password = requireValue(context, "smallrye.config.secret-handler.jasypt.password");
        String algorithm = requireValue(context, "smallrye.config.secret-handler.jasypt.algorithm");
        return new JasyptSecretKeysHandler(password, algorithm);
    }

    @Override
    public String getName() {
        return "jasypt";
    }

    private static String requireValue(final ConfigSourceContext context, final String name) {
        ConfigValue value = context.getValue(name);
        if (value != null) {
            return value.getValue();
        }
        throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name));
    }
}
