package io.smallrye.config.validation;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigValueValidatorTest {
    @Test
    public void validate() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withValidator(new SmallRyeConfigValidator())
                .build();

        final ConfigValue configValue = config.getConfigValue("my.prop");
        // Programatic API to validate
        assertThrows(ConfigValidationException.class,
                () -> configValue(config, configValue).max(10).min(0).getAs(Integer.class));
    }

    @Test
    public void validateWithConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234",
                        "my.prop.valid.max", "10"))
                .withValidator(new SmallRyeConfigValidator())
                .build();

        final ConfigValue configValue = config.getConfigValue("my.prop");
        assertThrows(ConfigValidationException.class, () -> configValue(config, configValue).getAs(Integer.class));
    }

    public static ConfigValueNew configValue(final SmallRyeConfig config, final ConfigValue configValue) {
        return new ConfigValueNew() {
            final ConfigValueValidatorBuilder validatorBuilder = new ConfigValueValidatorBuilder();

            @Override
            public String getName() {
                return configValue.getName();
            }

            @Override
            public String getValue() {
                return configValue.getValue();
            }

            @Override
            public <T> T getAs(final Class<T> klass) {
                final T value = config.getValue(getName(), klass);

                // Probably we need to add a flag if we want to validate or not? So we don't have to do all these checks?
                if (!validatorBuilder.hasMax()) {
                    config.getOptionalValue(getName() + ".valid.max", Long.class).ifPresent(validatorBuilder::max);
                }
                if (!validatorBuilder.hasMin()) {
                    config.getOptionalValue(getName() + ".valid.min", Long.class).ifPresent(validatorBuilder::min);
                }

                ((SmallRyeConfigValidator) config.getConfigValidator()).validate(validatorBuilder, value);
                return value;
            }

            @Override
            public ConfigValueNew min(final long size) {
                validatorBuilder.min(size);
                return this;
            }

            @Override
            public ConfigValueNew max(final long size) {
                validatorBuilder.max(size);
                return this;
            }

            @Override
            public ConfigValueNew pattern(final String pattern) {
                return this;
            }
        };
    }
}
