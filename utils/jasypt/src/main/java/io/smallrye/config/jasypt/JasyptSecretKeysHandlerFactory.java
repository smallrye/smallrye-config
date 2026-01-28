package io.smallrye.config.jasypt;

import java.util.NoSuchElementException;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config._private.ConfigMessages;

public class JasyptSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    @Override
    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
        ConfigValue enabled = context.getValue("smallrye.config.secret-handler.jasypt.enabled");
        if (enabled != null && enabled.getValue() != null) {
            if (Converters.getImplicitConverter(Boolean.class).convert(enabled.getValue())) {
                String password = requireValue(context, "smallrye.config.secret-handler.jasypt.password");
                String algorithm = requireValue(context, "smallrye.config.secret-handler.jasypt.algorithm");
                return new JasyptSecretKeysHandler(password, algorithm);
            }
        }
        throw new UnsupportedOperationException("""
                Jasypt is no longer maintained. The integration with SmallRye Config is deprecated and will be \
                removed in a future version. You can re-enable Jasypt \
                with `smallrye.config.secret-handler.jasypt.enabled=true`.""");
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
