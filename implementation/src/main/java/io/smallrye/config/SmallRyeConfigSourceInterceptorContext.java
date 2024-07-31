package io.smallrye.config;

import java.io.Serializable;
import java.util.Iterator;
import java.util.function.Supplier;

class SmallRyeConfigSourceInterceptorContext implements ConfigSourceInterceptorContext {
    private static final long serialVersionUID = 6654406739008729337L;

    private final ConfigSourceInterceptor interceptor;
    private final ConfigSourceInterceptorContext next;
    private final InterceptorChain chain;

    private static final ThreadLocal<RecursionCount> rcHolder = ThreadLocal.withInitial(RecursionCount::new);

    SmallRyeConfigSourceInterceptorContext(
            final ConfigSourceInterceptor interceptor,
            final ConfigSourceInterceptorContext next,
            final InterceptorChain chain) {
        this.interceptor = interceptor;
        this.next = next;
        this.chain = chain.setChain(this);
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
            return chain.get().proceed(name);
        } finally {
            if (rc.decrement()) {
                // avoid leaking if the thread is cached
                rcHolder.remove();
            }
        }
    }

    @Override
    public Iterator<String> iterateNames() {
        return interceptor.iterateNames(next);
    }

    static class InterceptorChain implements Supplier<ConfigSourceInterceptorContext>, Serializable {
        private static final long serialVersionUID = 7387475787257736307L;

        private ConfigSourceInterceptorContext chain;

        @Override
        public ConfigSourceInterceptorContext get() {
            return chain;
        }

        public InterceptorChain setChain(final ConfigSourceInterceptorContext chain) {
            this.chain = chain;
            return this;
        }
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

        boolean decrement() {
            return --count == 0;
        }
    }
}
