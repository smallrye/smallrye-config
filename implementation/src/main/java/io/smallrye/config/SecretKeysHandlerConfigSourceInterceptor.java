package io.smallrye.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretKeysHandlerConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -5228028387733656005L;

    private final Map<String, SecretKeysHandler> handlers = new HashMap<>();

    public SecretKeysHandlerConfigSourceInterceptor(final List<SecretKeysHandler> handlers) {
        for (SecretKeysHandler handler : handlers) {
            this.handlers.put(handler.getName(), handler);
        }
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        if (configValue != null && configValue.getValue() != null) {
            String handler = configValue.getExtendedExpressionHandler();
            if (handler != null) {
                return configValue.withValue(getSecretValue(handler, configValue.getValue()));
            }
        }
        return configValue;
    }

    private String getSecretValue(final String handlerName, final String secretName) {
        SecretKeysHandler handler = handlers.get(handlerName);
        if (handler != null) {
            return handler.decode(secretName);
        }
        throw ConfigMessages.msg.secretKeyHandlerNotFound(handlerName);
    }
}
