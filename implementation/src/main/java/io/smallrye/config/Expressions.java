package io.smallrye.config;

import java.util.function.Supplier;

@SuppressWarnings("squid:S5164")
public final class Expressions {
    private static final ThreadLocal<Boolean> ENABLE = ThreadLocal.withInitial(() -> Boolean.TRUE);

    private Expressions() {
        throw new UnsupportedOperationException();
    }

    public static boolean isEnabled() {
        return ENABLE.get();
    }

    public static void withoutExpansion(final Runnable action) {
        withoutExpansion(() -> {
            action.run();
            return null;
        });
    }

    public static <T> T withoutExpansion(Supplier<T> supplier) {
        if (isEnabled()) {
            ENABLE.set(false);
            try {
                return supplier.get();
            } finally {
                ENABLE.set(true);
            }
        } else {
            return supplier.get();
        }
    }
}
