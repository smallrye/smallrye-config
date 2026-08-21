package io.smallrye.config;

import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;

public interface ConfigValidator {
    void validateMapping(GeneratedConfigClass configClass, Object configObject) throws ConfigValidationException;

    ConfigValidator EMPTY = new ConfigValidator() {
        @Override
        public void validateMapping(GeneratedConfigClass configClass, Object configObject) throws ConfigValidationException {

        }
    };
}
