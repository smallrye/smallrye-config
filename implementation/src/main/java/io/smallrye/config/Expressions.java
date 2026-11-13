package io.smallrye.config;

import java.util.function.Supplier;

/**
 * Provide a way to control the evaluation of configuration expressions.
 * <p>
 * Configuration expression expansion is enabled by default. Disabling expression expansion allows retrieving raw
 * configuration values containing expression syntax (such as <code>${expression}</code>) without evaluating or
 * expanding them.
 * <p>
 * Enabling and disabling expression expansion is a local operation to the current executing thread. To disable the
 * expansion for the entire configuration, set the {@code mp.config.property.expressions.enabled} configuration
 * property to {@code false}.
 *
 * @see ExpressionConfigSourceInterceptor
 */
@SuppressWarnings("squid:S5164")
public final class Expressions {
    private static final ThreadLocal<Boolean> ENABLE = new ThreadLocal<>();

    private Expressions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Check if configuration expression expansion is enabled.
     *
     * @return {@code true} if expression expansion is enabled or {@code false} otherwise.
     */
    public static boolean isEnabled() {
        Boolean result = ENABLE.get();
        return result == null ? true : result;
    }

    /**
     * Executes a {@code Runnable} with configuration expression expansion disabled.
     *
     * @param action the code to execute
     */
    public static void withoutExpansion(final Runnable action) {
        withoutExpansion(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Get the result of a {@code Supplier} with configuration expression expansion disabled.
     *
     * @param supplier a {@code Supplier} to retrieve the results
     * @return the result
     * @param <T> the type of results
     */
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
