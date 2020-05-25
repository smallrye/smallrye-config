package io.smallrye.config;

import java.util.function.Supplier;

@SuppressWarnings("squid:S5164")
public final class SecretKeys {
    private static final ThreadLocal<Boolean> LOCKED = ThreadLocal.withInitial(() -> Boolean.TRUE);

    private SecretKeys() {
        throw new UnsupportedOperationException();
    }

    public static boolean isLocked() {
        return LOCKED.get();
    }

    public static void doUnlocked(Runnable runnable) {
        doUnlocked(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T doUnlocked(Supplier<T> supplier) {
        if (isLocked()) {
            LOCKED.set(false);
            try {
                return supplier.get();
            } finally {
                LOCKED.set(true);
            }
        } else {
            return supplier.get();
        }
    }

    public static void doLocked(Runnable runnable) {
        doLocked(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T doLocked(Supplier<T> supplier) {
        if (!isLocked()) {
            LOCKED.set(true);
            try {
                return supplier.get();
            } finally {
                LOCKED.set(false);
            }
        } else {
            return supplier.get();
        }
    }
}
