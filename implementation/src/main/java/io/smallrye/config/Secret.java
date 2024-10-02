package io.smallrye.config;

/**
 * A container type to mark a {@link io.smallrye.config.ConfigMapping} member as a {@link Secret} value.
 * <p>
 *
 * A {@link Secret} value modifies the behaviour of the config system by:
 * <ol>
 * <li>Omitting the name of the secret in {@link SmallRyeConfig#getPropertyNames()}</li>
 * <li>Omitting the name and value of the secret in the mapping {@code toString} method</li>
 * <li>Throwing a {@link SecurityException} when trying to retrieve the value via {@link SmallRyeConfig} programmatic API</li>
 * </ol>
 *
 * A {@link ConfigMapping} is still capable of performing the mapping without these restrictions, and the secret value
 * is available for retrieval in its declared member:
 *
 * <pre>
 * &#064;ConfigMapping(prefix = "credentials")
 * public interface Credentials {
 *     String username();
 *
 *     Secret&lt;String&gt; password();
 * }
 * </pre>
 *
 * A Secret can be of any type that can be converted by a registered
 * {@link org.eclipse.microprofile.config.spi.Converter} of the same type.
 *
 * @param <T> the secret type
 *
 * @see SecretKeys
 * @see SmallRyeConfigBuilder#withSecretKeys(String...)
 */
public interface Secret<T> {
    /**
     * Get the actual value of the {@link Secret}.
     *
     * @return the actual value of the {@link Secret}.
     */
    T get();
}
