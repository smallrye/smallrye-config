package io.smallrye.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Supplier;

@SuppressWarnings("squid:S5164")
public final class SecretKeys implements Serializable {
    @Serial
    private static final long serialVersionUID = -3226034787747746735L;

    private static final ThreadLocal<Boolean> LOCKED = new ThreadLocal<>();

    public static boolean isLocked() {
        Boolean result = LOCKED.get();
        return result == null || result;
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
                LOCKED.remove();
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
