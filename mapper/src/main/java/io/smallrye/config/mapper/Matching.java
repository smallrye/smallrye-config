package io.smallrye.config.mapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the regular expression pattern that must be matched for this property to be considered valid.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface Matching {
    /**
     * The pattern.
     *
     * @return the pattern
     */
    String value();
}
