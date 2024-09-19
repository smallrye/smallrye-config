package io.smallrye.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * Provide a list of map keys when populating {@link java.util.Map} types.
 * <p>
 * When populating a {@link java.util.Map}, {@link SmallRyeConfig} requires the configuration names listed in
 * {@link SmallRyeConfig#getPropertyNames()} to be able to find the {@link java.util.Map} keys. The provided list will
 * effectively substitute the lookup in {@link SmallRyeConfig#getPropertyNames()}, thus enabling a
 * {@link org.eclipse.microprofile.config.spi.ConfigSource} that does not list its properties, to contribute
 * configuration to the {@link java.util.Map}.
 * <p>
 * Each key must exist in the final configuration (relative to the {@link java.util.Map} path segment), or the mapping
 * will fail with a {@link ConfigValidationException}.
 * <p>
 * In the case of {@link java.util.Map} value references a {@link java.util.Collection}, {@link SmallRyeConfig} would
 * still require the lookup in {@link SmallRyeConfig#getPropertyNames()}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE_USE })
public @interface WithKeys {
    /**
     * A {@link Class} implementing a {@link Supplier} of {@link Iterable} with the {@link java.util.Map} keys to look
     * in the configuration. Keys containing a <code>dot</code> are quoted.
     * <p>
     * The {@link Supplier} is instantiated when mapping the {@link java.util.Map}. It may be instanciated multiple
     * times if the {@link Class} is used across multiple {@link WithKeys}.
     *
     * @return A {@link Class} implementing a {@link Supplier} of {@link Iterable} of {@link String} keys
     */
    Class<? extends Supplier<Iterable<String>>> value();
}
