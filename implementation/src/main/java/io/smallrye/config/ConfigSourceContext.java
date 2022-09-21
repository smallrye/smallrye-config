package io.smallrye.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.annotation.Experimental;

/**
 * Exposes contextual information on the ConfigSource initialization via {@link ConfigSourceFactory}.
 */
@Experimental("ConfigSource API Enhancements")
public interface ConfigSourceContext {
    ConfigValue getValue(String name);

    List<String> getProfiles();

    Iterator<String> iterateNames();

    class ConfigSourceContextConfigSource implements ConfigSource {
        private final ConfigSourceContext context;

        public ConfigSourceContextConfigSource(final ConfigSourceContext context) {
            this.context = context;
        }

        @Override
        public Set<String> getPropertyNames() {
            Set<String> names = new HashSet<>();
            Iterator<String> namesIterator = context.iterateNames();
            while (namesIterator.hasNext()) {
                names.add(namesIterator.next());
            }
            return names;
        }

        @Override
        public String getValue(final String propertyName) {
            ConfigValue value = context.getValue(propertyName);
            return value != null && value.getValue() != null ? value.getValue() : null;
        }

        @Override
        public String getName() {
            return ConfigSourceContextConfigSource.class.getName();
        }
    }
}
