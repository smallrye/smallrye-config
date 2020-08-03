package io.smallrye.config;

public class DefaultValuesConfigSource extends KeyMapBackedConfigSource {
    private static final long serialVersionUID = -6386021034957868328L;

    public DefaultValuesConfigSource(final KeyMap<String> properties) {
        super("DefaultValuesConfigSource", Integer.MIN_VALUE, properties);
    }

    void registerDefaults(final KeyMap<String> properties) {
        properties.forEach((key, value) -> getKeyMapProperties().put(key, value));
    }
}
