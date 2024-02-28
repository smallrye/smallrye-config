package io.smallrye.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfig.ConfigSourceWithPriority;

class SmallRyeConfigSources implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 7560201715403486552L;

    private final List<ConfigValueConfigSource> configSources;

    SmallRyeConfigSources(final List<ConfigSourceWithPriority> configSourcesWithPriorities) {
        List<ConfigValueConfigSource> configSources = new ArrayList<>();
        for (ConfigSourceWithPriority configSource : configSourcesWithPriorities) {
            configSources.add(ConfigValueConfigSourceWrapper.wrap(configSource.getSource()));
        }
        this.configSources = configSources;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        for (int i = 0, configSourcesSize = configSources.size(); i < configSourcesSize; i++) {
            final ConfigValueConfigSource configSource = configSources.get(i);
            final ConfigValue configValue = configSource.getConfigValue(name);
            if (configValue != null) {
                return configValue.from().withConfigSourcePosition(i).build();
            }
        }
        return null;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        for (final ConfigValueConfigSource configSource : configSources) {
            final Set<String> propertyNames = configSource.getPropertyNames();
            if (propertyNames != null) {
                names.addAll(propertyNames);
            }
        }
        return names.iterator();
    }

    static final class ConfigValueConfigSourceWrapper implements ConfigValueConfigSource, Serializable {
        private static final long serialVersionUID = -1109094614437147326L;

        private final ConfigSource configSource;

        private ConfigValueConfigSourceWrapper(final ConfigSource configSource) {
            this.configSource = configSource;
        }

        @Override
        public ConfigValue getConfigValue(final String propertyName) {
            String value = configSource.getValue(propertyName);
            if (value != null) {
                return ConfigValue.builder()
                        .withName(propertyName)
                        .withValue(value)
                        .withRawValue(value)
                        .withConfigSourceName(getName())
                        .withConfigSourceOrdinal(getOrdinal())
                        .build();
            }

            return null;
        }

        @Override
        public Map<String, ConfigValue> getConfigValueProperties() {
            return new ConfigValueMapStringView(configSource.getProperties(),
                    configSource.getName(),
                    configSource.getOrdinal());
        }

        @Override
        public Map<String, String> getProperties() {
            return configSource.getProperties();
        }

        @Override
        public String getValue(final String propertyName) {
            return configSource.getValue(propertyName);
        }

        @Override
        public Set<String> getPropertyNames() {
            return configSource.getPropertyNames();
        }

        @Override
        public String getName() {
            return configSource.getName();
        }

        @Override
        public int getOrdinal() {
            return configSource.getOrdinal();
        }

        static ConfigValueConfigSource wrap(final ConfigSource configSource) {
            if (configSource instanceof ConfigValueConfigSource) {
                return (ConfigValueConfigSource) configSource;
            } else {
                return new ConfigValueConfigSourceWrapper(configSource);
            }
        }
    }
}
