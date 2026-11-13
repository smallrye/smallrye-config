package io.smallrye.config;

import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;

/**
 * A {@code ConfigValidator} validates the {@link ConfigMapping} instances after they are populated with the
 * configuration values.
 * <p>
 * Instances of this interface will be discovered by {@link SmallRyeConfigBuilder#addDiscoveredValidator()} via the
 * {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/io.smallrye.config.ConfigValidator} file, which contains the fully qualified class name of
 * the custom {@code ConfigValidator} implementation. A custom {@code ConfigValidator} can also be registered
 * programmatically via {@link SmallRyeConfigBuilder#withValidator(ConfigValidator)}.
 *
 * @see ConfigValidationException
 * @see SmallRyeConfigBuilder#withValidator(ConfigValidator)
 */
public interface ConfigValidator {
    /**
     * Validates a mapped configuration object against the constraints defined in the configuration class or
     * interface. The validation problems must be reported with a {@link ConfigValidationException}, which is
     * collected with the remaining mapping problems, instead of failing the mapping immediately.
     *
     * @param configClass the generated configuration class metadata for the mapping
     * @param configObject the mapped configuration object instance to validate
     * @throws ConfigValidationException if one or more validation constraints are violated
     */
    void validateMapping(GeneratedConfigClass configClass, Object configObject) throws ConfigValidationException;

    /**
     * An empty {@link ConfigValidator} implementation that performs no validation. This is the validator used when
     * no other validator is set or discovered, in which case the validation is skipped entirely.
     */
    ConfigValidator EMPTY = new ConfigValidator() {
        @Override
        public void validateMapping(GeneratedConfigClass configClass, Object configObject) throws ConfigValidationException {

        }
    };
}
