package io.smallrye.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public static class Secrets extends PropertyNamesMatcher<Boolean> {
        private final Set<String> secrets = new HashSet<>();

        public Set<String> getSecrets() {
            return Collections.unmodifiableSet(secrets);
        }

        /**
         * Match a name with the secret keys. A name registered with {@link #addNotSecret(String, List)} is more specific
         * than a secret key with wildcards, so it does not match.
         *
         * @param name the String to be matched
         * @return {@code true} if the name is a secret key or {@code false} otherwise.
         */
        @Override
        public boolean matches(final String name) {
            return Boolean.TRUE.equals(get(name));
        }

        @Override
        protected void add(String name, Boolean value) {
            secrets.add(name);
            super.add(name, Boolean.TRUE);
        }

        @Override
        protected void put(String name, Boolean value) {
            secrets.add(name);
            super.put(name, Boolean.TRUE);
        }

        /**
         * Registers a name that matches a secret key with wildcards, but that is not a secret. For instance, a
         * secret {@code datasource.*.password} in a named datasource, and a non-secret
         * {@code datasource.devservices.password} in a different mapping. An exact secret name is not overridden.
         *
         * @param name the name that is not a secret
         * @param profiles the active profiles
         */
        void addNotSecret(final String name, final List<String> profiles) {
            super.add(name, Boolean.FALSE);
            for (String profile : profiles) {
                super.add("%" + profile + "." + name, Boolean.FALSE);
            }
        }

        Secrets withProfiles(List<String> profiles) {
            for (String secret : secrets) {
                for (String profile : profiles) {
                    super.add("%" + profile + "." + secret, Boolean.TRUE);
                }
            }
            return this;
        }
    }
}
