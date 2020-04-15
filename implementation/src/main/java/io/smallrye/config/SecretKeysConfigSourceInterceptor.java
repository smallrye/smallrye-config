package io.smallrye.config;

import java.util.Set;

public class SecretKeysConfigSourceInterceptor implements ConfigSourceInterceptor {
    private final Set<String> secrets;

    public SecretKeysConfigSourceInterceptor(final Set<String> secrets) {
        this.secrets = secrets;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (SecretKeys.isLocked() && isSecret(name)) {
            throw ConfigMessages.msg.notAllowed(name);
        }
        return context.proceed(name);
    }

    private boolean isSecret(final String name) {
        return secrets.contains(name);
    }
}
