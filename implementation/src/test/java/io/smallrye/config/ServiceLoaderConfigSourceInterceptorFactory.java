package io.smallrye.config;

public class ServiceLoaderConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        final ConfigValue profile = context.proceed("config.profile");
        return new ProfileConfigSourceInterceptor(profile != null ? profile.getValue() : null);
    }
}
