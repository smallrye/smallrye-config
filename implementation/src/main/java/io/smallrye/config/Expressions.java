package io.smallrye.config;

import java.util.function.Supplier;

@SuppressWarnings("squid:S5164")
public final class Expressions {
    private static final ThreadLocal<Boolean> ENABLE = new ThreadLocal<>();

    private Expressions() {
        throw new UnsupportedOperationException();
    }

    public static boolean isEnabled() {
        Boolean result = ENABLE.get();
        return result == null ? true : result;
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
                ENABLE.remove();
            }
        } else {
            return supplier.get();
        }
    }
}
