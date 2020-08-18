package io.smallrye.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

@Documented
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Experimental("TODO")
public @interface ConfigMapping {
    String prefix() default "";
}
