package io.smallrye.config;

import java.util.Map;

public class DefaultValuesConfigSource extends KeyMapBackedConfigSource {
    private static final long serialVersionUID = -6386021034957868328L;

    public DefaultValuesConfigSource(final KeyMap<String> properties) {
        super("DefaultValuesConfigSource", Integer.MIN_VALUE, properties);
    }

    void registerDefaults(final KeyMap<String> properties) {
        for (Map.Entry<String, KeyMap<String>> entry : properties.entrySet()) {
            getKeyMapProperties().put(entry.getKey(), entry.getValue());
        }
    }
}
