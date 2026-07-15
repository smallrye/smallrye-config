package io.smallrye.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Omits a single map key from the configuration name when populating {@link java.util.Map} types. Configuration values
 * for the {@link java.util.Map} may be retrieved by the key defined in {@link WithUnnamedKey#value()}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE_USE })
public @interface WithUnnamedKey {
    /**
     * The key name to use to populate configuration values from a configuration path without a key.
     *
     * @return the Map key
     */
    String value() default "";

    /**
     * Whether the unnamed key should be included in the {@link java.util.Map} when its values come only from defaults.
     *
     * @return {@code true} to always include the unnamed key, {@code false} to exclude it when only default values
     *         are present
     */
    boolean eager() default true;
}
