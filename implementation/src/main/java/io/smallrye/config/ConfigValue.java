package io.smallrye.config;

public class ConfigValue {
    private final String name;
    private final String value;
    private final String configSourceName;
    private final int configSourceOrdinal;

    private ConfigValue(final ConfigValueBuilder builder) {
        this.name = builder.name;
        this.value = builder.value;
        this.configSourceName = builder.configSourceName;
        this.configSourceOrdinal = builder.configSourceOrdinal;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getConfigSourceName() {
        return configSourceName;
    }

    public int getConfigSourceOrdinal() {
        return configSourceOrdinal;
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

    ConfigValueBuilder from() {
        return new ConfigValueBuilder()
                .withName(name)
                .withValue(value)
                .withConfigSourceName(configSourceName)
                .withConfigSourceOrdinal(configSourceOrdinal);
    }

    public static ConfigValueBuilder builder() {
        return new ConfigValueBuilder();
    }

    public static class ConfigValueBuilder {
        private String name;
        private String value;
        private String configSourceName;
        private int configSourceOrdinal;

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

        public ConfigValue build() {
            return new ConfigValue(this);
        }
    }
}
