package io.smallrye.config;

import java.util.Iterator;

class SmallRyeConfigSourceInterceptorContext implements ConfigSourceInterceptorContext {
    private static final long serialVersionUID = 6654406739008729337L;

    private final ConfigSourceInterceptor interceptor;
    private final ConfigSourceInterceptorContext next;

    SmallRyeConfigSourceInterceptorContext(
            final ConfigSourceInterceptor interceptor,
            final ConfigSourceInterceptorContext next) {
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
