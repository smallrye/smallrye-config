package io.smallrye.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import io.smallrye.config.common.utils.StringUtil;

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
 * {@link SmallRyeConfigBuilder#withMapping(java.lang.Class)}.
 *
 * @see SmallRyeConfigBuilder#withMapping(java.lang.Class)
 * @see SmallRyeConfigBuilder#withMapping(ConfigMappings.ConfigClass)
 */
@Documented
@Target({ FIELD, PARAMETER, TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigMapping {
    /**
     * The prefix of the configuration properties.
     *
     * @return the configuration property prefix
     */
    String prefix() default "";

    /**
     * The naming strategy to use for the config mapping. This only matters for method names that contain both
     * lower case and upper case characters.
     *
     * @return the config mapping naming strategy.
     */
    NamingStrategy namingStrategy() default NamingStrategy.KEBAB_CASE;

    /**
     * Match config mapping method names and configuration properties using bean-style getter names. This removes the
     * prefixes <code>get</code> or <code>is</code> and decapitalizes the next character. By default, bean-style getter
     * matching is <code>disabled</code>.
     * <p>
     * Config mapping declarations should prefer to use simple names that match one-to-one with their configuration
     * names. Bean-style getter matching allows multiple method names to match the same configuration name. For
     * instance, <code>getFoo</code> and <code>isFoo</code> both match <code>foo</code>, which may not be intended.
     *
     * @return a boolean <code>true</code> to enable bean style getter names matching, or <code>false</code> to disable
     *         it.
     */
    boolean beanStyleGetters() default false;

    enum NamingStrategy implements Function<String, String> {
        /**
         * The method name is used as is to map the configuration property.
         */
        VERBATIM(name -> name),
        /**
         * The method name is derived by replacing case changes with a dash to map the configuration property.
         */
        KEBAB_CASE(name -> {
            return StringUtil.skewer(name, '-');
        }),
        /**
         * The method name is derived by replacing case changes with an underscore to map the configuration property.
         */
        SNAKE_CASE(name -> {
            return StringUtil.skewer(name, '_');
        });

        private final Function<String, String> function;

        NamingStrategy(Function<String, String> function) {
            this.function = function;
        }

        public String apply(final String name) {
            return function.apply(name);
        }
    }
}
