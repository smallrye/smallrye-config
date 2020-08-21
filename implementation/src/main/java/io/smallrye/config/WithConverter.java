package io.smallrye.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.annotation.Experimental;

/**
 * Specify the converter to use to convert the annotated type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE_USE })
@Experimental("ConfigMapping API to group configuration properties")
public @interface WithConverter {
    /**
     * The converter class to use.
     *
     * @return the converter class
     */
    Class<? extends Converter<?>> value();
}
