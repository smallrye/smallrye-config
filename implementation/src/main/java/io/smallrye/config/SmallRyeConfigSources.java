package io.smallrye.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        for (final ConfigValueConfigSource configSource : configSources) {
            final ConfigValue configValue = configSource.getConfigValue(name);
            if (configValue != null) {
                return configValue;
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

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        for (final ConfigValueConfigSource configSource : configSources) {
            final Map<String, ConfigValue> configValueProperties = configSource.getConfigValueProperties();
            if (configValueProperties != null) {
                values.addAll(configValueProperties.values());
            }
        }
        return values.iterator();
    }
}
