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

/**
 * This annotation may be placed in interfaces to group configuration properties with a common prefix.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * &#064;ConfigMapping(prefix = "server")
 * public interface Server {
 *     public String host(); // maps the property name server.host
 * 
 *     public int port(); // maps to the property name server.port
 * }
 * </pre>
 *
 * This annotation is also used in CDI aware environments to scan and register Config Mappings. Otherwise, Config
 * Mapping interfaces require registration via
 * {@link SmallRyeConfigBuilder#withMapping(java.lang.Class, java.lang.String)}.
 */
@Documented
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Experimental("ConfigMapping API to group configuration properties")
public @interface ConfigMapping {
    /**
     * The prefix of the configuration properties.
     *
     * @return the configuration property prefix
     */
    String prefix() default "";
}
