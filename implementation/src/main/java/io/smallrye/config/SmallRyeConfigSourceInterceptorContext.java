package io.smallrye.config;

import java.util.Iterator;

class SmallRyeConfigSourceInterceptorContext implements ConfigSourceInterceptorContext {
    private static final long serialVersionUID = 6654406739008729337L;

    private final ConfigSourceInterceptor interceptor;
    private final ConfigSourceInterceptorContext next;
    private final SmallRyeConfig config;

    SmallRyeConfigSourceInterceptorContext(
            final ConfigSourceInterceptor interceptor,
            final ConfigSourceInterceptorContext next,
            final SmallRyeConfig config) {
        this.interceptor = interceptor;
        this.next = next;
        this.config = config;
    }

    @Override
    public ConfigValue proceed(final String name) {
        return interceptor.getValue(next, name);
    }

    @Override
    public ConfigValue restart(final String name) {
        return config.interceptorChain().proceed(name);
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
