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
                () -> configValue(config, configValue).validator().max(10).min(0).getAs(Integer.class));
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

    public static ConfigValueValidator configValue(final SmallRyeConfig config, final ConfigValue configValue) {
        return new ConfigValueValidator() {
            final ConfigValueValidatorBuilder validatorBuilder = new ConfigValueValidatorBuilder();

            @Override
            public ConfigValueValidator max(final long value) {
                validatorBuilder.max(value);
                return this;
            }

            @Override
            public ConfigValueValidator min(final long value) {
                validatorBuilder.min(value);
                return this;
            }

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
                if (!validatorBuilder.hasMax()) {
                    config.getOptionalValue(getName() + ".valid.max", Long.class).ifPresent(validatorBuilder::max);
                }

                if (!validatorBuilder.hasMin()) {
                    config.getOptionalValue(getName() + ".valid.min", Long.class).ifPresent(validatorBuilder::min);
                }

                ((SmallRyeConfigValidator) config.getConfigValidator()).validate(validatorBuilder, getValue());
                // lookup was already made in getValue, so we need a method to perform conversion only.
                return config.getValue(getName(), klass);
            }

            @Override
            public ConfigValueValidator validator() {
                return this;
            }
        };
    }
}
