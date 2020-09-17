package io.smallrye.config;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * This Converter should not be used directly. This is only used as a marker to use to return a ConfigValue directly
 * after a configuration property lookup.
 */
class ConfigValueConverter implements Converter<ConfigValue> {
    static final Converter<ConfigValue> CONFIG_VALUE_CONVERTER = new ConfigValueConverter();

    @Override
    public ConfigValue convert(final String value) {
        throw new IllegalArgumentException();
    }
}
