package io.smallrye.config;

import java.io.Serializable;

public interface ConfigValidator extends Serializable {
    void validate(Class<?> klass, String propertyName, Object value) throws ConfigValidationException;
}
