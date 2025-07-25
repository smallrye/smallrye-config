package io.smallrye.config;

import static java.util.Collections.emptyIterator;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfig.ConfigSourceWithPriority;

class SmallRyeConfigSources implements ConfigSourceInterceptor {
    @Serial
    private static final long serialVersionUID = 7560201715403486552L;

    private final List<ConfigValueConfigSource> configSources;
    private final boolean negative;

    SmallRyeConfigSources(final List<ConfigSourceWithPriority> configSourcesWithPriorities, boolean negative) {
        this.negative = negative;
        List<ConfigValueConfigSource> configSources = new ArrayList<>();
        for (ConfigSourceWithPriority configSource : configSourcesWithPriorities) {
            if ((configSource.priority() < 0) == negative) {
                configSources.add(ConfigValueConfigSourceWrapper.wrap(configSource.getSource()));
            }
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
        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        return new Iterator<>() {
            final Iterator<ConfigValueConfigSource> configSourceIterator = configSources.iterator();
            Iterator<String> propertiesIterator = context.iterateNames();

            @Override
            public boolean hasNext() {
                if (propertiesIterator.hasNext()) {
                    return true;
                } else {
                    propertiesIterator = nextConfigSource();
                    if (propertiesIterator.hasNext()) {
                        return true;
                    } else if (configSourceIterator.hasNext()) {
                        return hasNext();
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public String next() {
                return propertiesIterator.next();
            }

            private Iterator<String> nextConfigSource() {
                if (configSourceIterator.hasNext()) {
                    Set<String> propertyNames = configSourceIterator.next().getPropertyNames();
                    if (propertyNames != null && !propertyNames.isEmpty()) {
                        return propertyNames.iterator();
                    }
                }
                return emptyIterator();
            }
        };
    }

    boolean negative() {
        return negative;
    }

    static final class ConfigValueConfigSourceWrapper implements ConfigValueConfigSource, Serializable {
        @Serial
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
