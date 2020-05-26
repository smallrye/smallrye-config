package io.smallrye.config.examples.interceptors;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class ExampleInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final ConfigValue configValue = context.proceed(name);
        if (configValue != null) {
            return configValue.withValue("intercepted " + configValue.getValue());
        }

        return configValue;
    }
}
