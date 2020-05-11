package io.smallrye.config.mapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Specify the comparator to use to compare the annotated type for range.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface CompareWith {
    /**
     * The comparator class to use.
     *
     * @return the comparator class
     */
    Class<? extends Comparator<?>> value();
}
