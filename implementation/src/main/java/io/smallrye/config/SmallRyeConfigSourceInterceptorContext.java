package io.smallrye.config;

import java.util.Iterator;

class SmallRyeConfigSourceInterceptorContext implements ConfigSourceInterceptorContext {
    private static final long serialVersionUID = 6654406739008729337L;

    private final ConfigSourceInterceptor interceptor;
    private final SmallRyeConfigSourceInterceptorContext next;

    SmallRyeConfigSourceInterceptorContext(
            final ConfigSourceInterceptor interceptor,
            final SmallRyeConfigSourceInterceptorContext next) {
        this.interceptor = interceptor;
        this.next = next;
    }

    @Override
    public ConfigValue proceed(final String name) {
        return interceptor.getValue(next, name);
    }

    @Override
    public Iterator<String> iterateNames() {
        return interceptor.iterateNames(next);
    }

    @Override
    public Iterator<ConfigValue> iterateValues() {
        return interceptor.iterateValues(next);
    }
}
