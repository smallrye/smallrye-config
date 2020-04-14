package io.smallrye.config;

public class ServiceLoaderConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if ("my.prop.loader".equals(name)) {
            return ConfigValue.builder().withName(name).withValue("loader").build();
        }
        return context.proceed(name);
    }
}
