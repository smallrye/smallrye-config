package io.smallrye.config;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.annotation.Experimental;

/**
 * Extends the original {@link ConfigSource} to expose methods that return a {@link ConfigValue}. The
 * {@link ConfigValue} allows to retrieve additional metadata associated with the configuration resolution.
 * <p>
 *
 * This is to work around the limitation from the original {@link ConfigSource}. It exposes everything as plain Strings
 * and it is not possible to retrieve additional information associated with the Configuration. The
 * ConfigValueConfigSource tries to make this possible.
 * <p>
 *
 * Ideally, this should move the MicroProfile Config API, once the concept is well-proven.
 */
@Experimental("Extension to the original ConfigSource to allow retrieval of additional metadata on config lookup")
public interface ConfigValueConfigSource extends ConfigSource {
    /**
     * Return the {@link ConfigValue} for the specified property in this configuration source.
     *
     * @param propertyName the property name
     * @return the ConfigValue, or {@code null} if the property is not present
     */
    ConfigValue getConfigValue(String propertyName);

    /**
     * Return the properties in this configuration source as a Map of String and {@link ConfigValue}.
     *
     * @return a map containing properties of this configuration source
     */
    Map<String, ConfigValue> getConfigValueProperties();

    /**
     * Return the properties in this configuration source as a map.
     * <p>
     *
     * This wraps the original {@link ConfigValue} map returned by
     * {@link ConfigValueConfigSource#getConfigValueProperties()} and provides a view over the original map
     * via {@link ConfigValueMapView}.
     *
     * @return a map containing properties of this configuration source
     */
    @Override
    default Map<String, String> getProperties() {
        return Collections.unmodifiableMap(new ConfigValueMapView(getConfigValueProperties()));
    }

    /**
     * Return the value for the specified property in this configuration source.
     * <p>
     *
     * This wraps the original {@link ConfigValue} returned by {@link ConfigValueConfigSource#getConfigValue(String)}
     * and unwraps the property value contained {@link ConfigValue}. If the {@link ConfigValue} is null the unwrapped
     * value and return is also null.
     *
     * @param propertyName the property name
     * @return the property value, or {@code null} if the property is not present
     */
    @Override
    default String getValue(String propertyName) {
        final ConfigValue value = getConfigValue(propertyName);
        return value != null ? value.getValue() : null;
    }
}
