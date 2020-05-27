package io.smallrye.config;

import java.io.Serializable;

import io.smallrye.common.annotation.Experimental;

/**
 * This will allows up to plugin any validation framework. We can create our own or plugin BVal.
 */
@Experimental("Validation API to validate the resolved value of a configuration")
public interface ConfigValidator extends Serializable {
    /**
     * Mostly for CDI.
     */
    void validate(Class<?> klass, String propertyName, Object value) throws ConfigValidationException;

    /**
     * For single lookups.
     */
    void validate(ConfigValue configValue) throws ConfigValidationException;

    ConfigValidator EMPTY = new ConfigValidator() {
        private static final long serialVersionUID = -2161224207009567466L;

        @Override
        public void validate(final Class<?> klass, final String propertyName, final Object value)
                throws ConfigValidationException {

        }

        @Override
        public void validate(final ConfigValue configValue) throws ConfigValidationException {

        }
    };
}
