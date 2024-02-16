package io.smallrye.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.smallrye.config.SecretKeysHandlerFactory.LazySecretKeysHandler;
import io.smallrye.config._private.ConfigMessages;

public class SecretKeysHandlerConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -5228028387733656005L;

    private final boolean enabled;
    private final Map<String, SecretKeysHandler> handlers = new HashMap<>();

    public SecretKeysHandlerConfigSourceInterceptor(final boolean enabled, final List<SecretKeysHandler> handlers) {
        this.enabled = enabled;
        for (SecretKeysHandler handler : handlers) {
            this.handlers.put(handler.getName(), handler);
        }
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        if (enabled && configValue != null && configValue.getValue() != null) {
            String handler = configValue.getExtendedExpressionHandler();
            if (handler != null) {
                return configValue.withValue(getHandler(handler, context).decode(configValue.getValue()));
            }
        }
        return configValue;
    }

    private SecretKeysHandler getHandler(final String handlerName, final ConfigSourceInterceptorContext context) {
        SecretKeysHandler handler = handlers.get(handlerName);
        if (handler == null) {
            throw ConfigMessages.msg.secretKeyHandlerNotFound(handlerName);
        }

        if (handler instanceof LazySecretKeysHandler) {
            handler = ((LazySecretKeysHandler) handler).get(new ConfigSourceContext() {
                @Override
                public ConfigValue getValue(final String name) {
                    return context.proceed(name);
                }

                @Override
                public Iterator<String> iterateNames() {
                    return context.iterateNames();
                }
            });
        }
        return handler;
    }
}
