package io.smallrye.config;

/**
 * A {@code SecretKeysHandler} provides a way to decode or decrypt a secret configuration value.
 * <p>
 * A secret configuration value may be expressed as {@code ${handler::value}}, where the {@code handler} is
 * the name of the {@code SecretKeysHandler} to use for decode or decryption the {@code value} separated by a
 * double colon {@code ::}.
 * <p>
 * Instances of this interface will be discovered via the {@link java.util.ServiceLoader} mechanism and can be
 * registered by providing a {@code META-INF/services/io.smallrye.config.SecretKeysHandler} which contains the fully
 * qualified class name of the custom {@code SecretKeysHandler} implementation.
 */
public interface SecretKeysHandler {
    /**
     * Decodes the secret configuration value.
     *
     * @param secret the value to decode.
     * @return the secret decoded.
     */
    String decode(String secret);

    /**
     * The name of {@code SecretKeysHandler}.
     *
     * @return the name of the {@code SecretKeysHandler}.
     */
    String getName();
}
