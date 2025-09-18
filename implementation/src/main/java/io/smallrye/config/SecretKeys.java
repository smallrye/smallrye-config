package io.smallrye.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Provide a way to control the access to Secret Keys.
 * <p>
 * A Secret Key is a plain configuration name that modifies the behaviour of the config system by:
 * <ol>
 * <li>Omitting the name of the secret in {@link SmallRyeConfig#getPropertyNames()}</li>
 * <li>Omitting the name and value of the secret in the mapping {@code toString} method</li>
 * <li>Throwing a {@link SecurityException} when trying to retrieve the value via {@link SmallRyeConfig} programmatic API</li>
 * </ol>
 * A Secret Key is defined by either adding the configuration name with
 * {@link SmallRyeConfigBuilder#withSecretKeys(String...)} or by wrapping a member type of a
 * {@link ConfigMapping} with {@link Secret}.
 * <p>
 * Secret Keys are locked by default. Locking and unlocking keys is a local operation to the current executing thread.
 *
 * @see Secret
 * @see SmallRyeConfigBuilder#withSecretKeys(String...)
 */
@SuppressWarnings("squid:S5164")
public final class SecretKeys implements Serializable {
    @Serial
    private static final long serialVersionUID = -3226034787747746735L;

    private static final ThreadLocal<Boolean> LOCKED = new ThreadLocal<>();

    /**
     * Check if the Secret Keys are locked.
     *
     * @return {@code true} if the Secret Keys are locked or {@code false} otherwise.
     */
    public static boolean isLocked() {
        Boolean result = LOCKED.get();
        return result == null || result;
    }

    /**
     * Executes a {@code Runnable} with full access to the Secret Keys.
     *
     * @param runnable the code to execute
     */
    public static void doUnlocked(Runnable runnable) {
        doUnlocked(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Get the result of a {@code Supplier} with full access to the Secret Keys.
     *
     * @param supplier a {@code Supplier} to retrieve the results
     * @return the result
     * @param <T> the type of results
     */
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

    /**
     * Executes a {@code Runnable} with no access to the Secret Keys.
     *
     * @param runnable the code to execute
     */
    public static void doLocked(Runnable runnable) {
        doLocked(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Get the result of a {@code Supplier} with no access to the Secret Keys.
     *
     * @param supplier a {@code Supplier} to retrieve the results
     * @return the result
     * @param <T> the type of results
     */
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
