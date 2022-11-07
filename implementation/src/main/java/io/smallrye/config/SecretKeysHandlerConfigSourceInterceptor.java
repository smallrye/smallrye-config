package io.smallrye.config;

public class SecretKeysHandlerConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -5228028387733656005L;

    private final SecretKeys secretKeys;

    public SecretKeysHandlerConfigSourceInterceptor(final SecretKeys secretKeys) {
        this.secretKeys = secretKeys;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        if (configValue != null && configValue.getValue() != null) {
            String handler = configValue.getExtendedExpressionHandler();
            if (handler != null) {
                return configValue.withValue(secretKeys.getSecretValue(handler, configValue.getValue()));
            }
        }
        return configValue;
    }
}
