package io.smallrye.config;

import java.util.Objects;

import io.smallrye.common.annotation.Experimental;

/**
 * The ConfigValue is a metadata object that holds additional information after the lookup of a configuration.
 * <p>
 *
 * Right now, it is able to hold information like the configuration name, value, the Config Source from where
 * the configuration was loaded, the ordinal of the Config Source and a line number from where the configuration was
 * read if exists.
 * <p>
 *
 * This is used together with {@link ConfigValueConfigSource} and {@link ConfigSourceInterceptor} to expose the
 * Configuration lookup metadata.
 */
@Experimental("Extension to the original ConfigSource to allow retrieval of additional metadata on config lookup")
public class ConfigValue implements org.eclipse.microprofile.config.ConfigValue {
    private final String name;
    private final String value;
    private final String configSourceName;
    private final int configSourceOrdinal;

    private final int lineNumber;

    private ConfigValue(final ConfigValueBuilder builder) {
        this.name = builder.name;
        this.value = builder.value;
        this.configSourceName = builder.configSourceName;
        this.configSourceOrdinal = builder.configSourceOrdinal;
        this.lineNumber = builder.lineNumber;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getSourceName() {
        return getConfigSourceName();
    }

    @Override
    public int getSourceOrdinal() {
        return getConfigSourceOrdinal();
    }

    public String getConfigSourceName() {
        return configSourceName;
    }

    public int getConfigSourceOrdinal() {
        return configSourceOrdinal;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLocation() {
        return lineNumber != -1 ? configSourceName + ":" + lineNumber : configSourceName;
    }

    public ConfigValue withName(final String name) {
        return from().withName(name).build();
    }

    public ConfigValue withValue(final String value) {
        return from().withValue(value).build();
    }

    public ConfigValue withConfigSourceName(final String configSourceName) {
        return from().withConfigSourceName(configSourceName).build();
    }

    public ConfigValue withConfigSourceOrdinal(final int configSourceOrdinal) {
        return from().withConfigSourceOrdinal(configSourceOrdinal).build();
    }

    public ConfigValue withLineNumber(final int lineNumber) {
        return from().withLineNumber(lineNumber).build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigValue that = (ConfigValue) o;
        return name.equals(that.name) &&
                value.equals(that.value) &&
                configSourceName.equals(that.configSourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, configSourceName);
    }

    ConfigValueBuilder from() {
        return new ConfigValueBuilder()
                .withName(name)
                .withValue(value)
                .withConfigSourceName(configSourceName)
                .withConfigSourceOrdinal(configSourceOrdinal)
                .withLineNumber(lineNumber);
    }

    public static ConfigValueBuilder builder() {
        return new ConfigValueBuilder();
    }

    public static class ConfigValueBuilder {
        private String name;
        private String value;
        private String configSourceName;
        private int configSourceOrdinal;
        private int lineNumber = -1;

        public ConfigValueBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public ConfigValueBuilder withValue(final String value) {
            this.value = value;
            return this;
        }

        public ConfigValueBuilder withConfigSourceName(final String configSourceName) {
            this.configSourceName = configSourceName;
            return this;
        }

        public ConfigValueBuilder withConfigSourceOrdinal(final int configSourceOrdinal) {
            this.configSourceOrdinal = configSourceOrdinal;
            return this;
        }

        public ConfigValueBuilder withLineNumber(final int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public ConfigValue build() {
            return new ConfigValue(this);
        }
    }
}
