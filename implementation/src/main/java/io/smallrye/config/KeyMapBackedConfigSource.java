package io.smallrye.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.AbstractConfigSource;

public class KeyMapBackedConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = 4378754290346888762L;

    private final KeyMap<String> properties;

    public KeyMapBackedConfigSource(final String name, final KeyMap<String> properties) {
        super(name, ConfigSource.DEFAULT_ORDINAL);
        this.properties = properties;
    }

    public KeyMapBackedConfigSource(final String name, final int ordinal, final KeyMap<String> properties) {
        super(name, ordinal);
        this.properties = properties;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    KeyMap<String> getKeyMapProperties() {
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return properties.findRootValue(propertyName);
    }
}
