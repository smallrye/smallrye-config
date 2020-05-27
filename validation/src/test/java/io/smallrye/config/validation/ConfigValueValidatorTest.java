package io.smallrye.config.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

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
                // This call makes a new lookup. We need a method that just performs the conversion without the lookup.

                // This should be ConfigValue
                config.getConfigValidator().validate(null);

                return config.getValue(getName(), klass);
            }

            @Override
            public ConfigValueNew min(final long size) {
                return this;
            }

            @Override
            public ConfigValueNew max(final long size) {
                return this;
            }

            @Override
            public ConfigValueNew pattern(final String pattern) {
                return this;
            }
        };

        // Programatic API to validate
        final Integer integer = configValueNew.max(10).min(0).getAs(Integer.class);
        assertNotNull(integer);
        assertEquals(1234, integer.intValue());
    }
}
