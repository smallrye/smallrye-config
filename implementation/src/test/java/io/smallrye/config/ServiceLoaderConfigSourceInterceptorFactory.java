package io.smallrye.config;

public class ServiceLoaderConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        return new ProfileConfigSourceInterceptor(context, "config.profile");
    }
}
