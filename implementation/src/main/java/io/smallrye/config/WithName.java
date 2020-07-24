package io.smallrye.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * The name of the configuration property or group.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Experimental("ConfigMapping API to group configuration properties")
public @interface WithName {
    /**
     * The name of the property or group. Must not be empty.
     *
     * @return the name
     */
    String value();
}
