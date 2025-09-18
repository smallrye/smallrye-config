package io.smallrye.config;

import java.io.Serial;
import java.util.Set;

import jakarta.annotation.Priority;

import io.smallrye.config._private.ConfigMessages;

/**
 * Intercept the resolution of a configuration name and throw {@link java.lang.SecurityException} if the name is a
 * Secret Key and the keys are locked.
 * <p>
 * To avoid having to recalculate the list of property names, the filter of secret keys is applied in
 * {@code SmallRyeConfig.ConfigSources.PropertyNames}, so this interceptor does not implement
 * {@link io.smallrye.config.ConfigSourceInterceptor#iterateNames(ConfigSourceInterceptorContext)}.
 *
 * @see SecretKeys
 */
@Priority(Priorities.LIBRARY + 100)
public class SecretKeysConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Serial
    private static final long serialVersionUID = 7291982039729980590L;

    private final Set<PropertyName> secrets;

    public SecretKeysConfigSourceInterceptor(final Set<PropertyName> secrets) {
        this.secrets = secrets;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (SecretKeys.isLocked() && secrets.contains(PropertyName.unprofiled(name))) {
            throw ConfigMessages.msg.notAllowed(name);
        }
        return context.proceed(name);
    }
}
