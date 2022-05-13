package io.smallrye.config;

public interface ConfigValidator {
    void validateMapping(Class<?> mappingClass, String prefix, Object mappingObject) throws ConfigValidationException;

    ConfigValidator EMPTY = new ConfigValidator() {
        @Override
        public void validateMapping(Class<?> mappingClass, String prefix, Object mappingObject)
                throws ConfigValidationException {

        }
    };
}
