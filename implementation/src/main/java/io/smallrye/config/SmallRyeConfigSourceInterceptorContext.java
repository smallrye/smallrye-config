package io.smallrye.config;

import java.util.Iterator;

class SmallRyeConfigSourceInterceptorContext implements ConfigSourceInterceptorContext {
    private static final long serialVersionUID = 6654406739008729337L;

    private final ConfigSourceInterceptor interceptor;
    private final ConfigSourceInterceptorContext next;
    private final SmallRyeConfig config;

    private static final ThreadLocal<RecursionCount> rcHolder = ThreadLocal.withInitial(RecursionCount::new);

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
        RecursionCount rc = rcHolder.get();
        rc.increment();
        try {
            return config.interceptorChain().proceed(name);
        } finally {
            rc.decrement();
        }
    }

    @Override
    public Iterator<String> iterateNames() {
        return interceptor.iterateNames(next);
    }

    @Override
    public Iterator<ConfigValue> iterateValues() {
        return interceptor.iterateValues(next);
    }

    static final class RecursionCount {
        int count;

        void increment() {
            int old = count;
            if (old == 20) {
                throw new IllegalStateException("Too many recursive interceptor actions");
            }
            count = old + 1;
        }

        void decrement() {
            count--;
        }
    }
}
