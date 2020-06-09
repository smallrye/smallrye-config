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
        final ConfigValueNew configValueNew = new ConfigValueNew() {
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

        // Programatic API to validate
        assertThrows(ConfigValidationException.class, () -> configValueNew.max(10).min(0).getAs(Integer.class));
    }
}
