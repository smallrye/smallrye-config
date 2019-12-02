package io.smallrye.config.util.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * The define the name of a config source
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Name {
    @Nonbinding
    String value();
}
