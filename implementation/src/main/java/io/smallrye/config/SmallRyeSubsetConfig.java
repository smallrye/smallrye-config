package io.smallrye.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author George Gastaldi
 */
class SmallRyeSubsetConfig implements Config {

    private final String prefix;

    private final Config delegate;

    public SmallRyeSubsetConfig(String prefix, Config delegate) {
        this.prefix = prefix;
        this.delegate = delegate;
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return delegate.getValue(toSubsetPropertyName(propertyName), propertyType);
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        return delegate.getConfigValue(toSubsetPropertyName(propertyName));
    }

    @Override
    public <T> List<T> getValues(String propertyName, Class<T> propertyType) {
        return delegate.getValues(toSubsetPropertyName(propertyName), propertyType);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return delegate.getOptionalValue(toSubsetPropertyName(propertyName), propertyType);
    }

    @Override
    public <T> Optional<List<T>> getOptionalValues(String propertyName, Class<T> propertyType) {
        return delegate.getOptionalValues(toSubsetPropertyName(propertyName), propertyType);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return StreamSupport.stream(delegate.getPropertyNames().spliterator(), false)
                .map(this::chopSubsetPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return delegate.getConfigSources();
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return delegate.getConverter(forType);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Cannot unwrap a subset configuration");
    }

    private String toSubsetPropertyName(String propertyName) {
        if (propertyName.isBlank()) {
            return prefix;
        } else {
            return prefix + "." + propertyName;
        }
    }

    private String chopSubsetPropertyName(String propertyName) {
        if (propertyName.equalsIgnoreCase(prefix)) {
            return "";
        } else if (propertyName.startsWith(prefix + '.')) {
            return propertyName.substring(prefix.length() + 1);
        } else {
            return null;
        }
    }
}
