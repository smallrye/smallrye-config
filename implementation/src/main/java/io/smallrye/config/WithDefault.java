package io.smallrye.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Specify the default value of a property.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Experimental("ConfigMapping API to group configuration properties")
public @interface WithDefault {
    /**
     * The default value of the property.
     *
     * @return the default value as a string
     */
    String value();
}
