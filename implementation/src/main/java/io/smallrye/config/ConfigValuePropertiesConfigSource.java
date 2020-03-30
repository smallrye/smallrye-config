package io.smallrye.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigValuePropertiesConfigSource extends MapBackedConfigValueConfigSource {
    private static final long serialVersionUID = 9070158352250209380L;

    private static final String NAME_PREFIX = "ConfigValuePropertiesConfigSource[source=";

    public ConfigValuePropertiesConfigSource(URL url) throws IOException {
        this(url, DEFAULT_ORDINAL);
    }

    public ConfigValuePropertiesConfigSource(URL url, int defaultOrdinal) throws IOException {
        this(url, NAME_PREFIX + url.toString() + "]", defaultOrdinal);
    }

    private ConfigValuePropertiesConfigSource(URL url, String name, int defaultOrdinal) throws IOException {
        super(name, urlToConfigValueMap(url, name, defaultOrdinal));
    }

    private static Map<String, ConfigValue> urlToConfigValueMap(URL locationOfProperties, String name, int ordinal)
            throws IOException {
        try (InputStreamReader reader = new InputStreamReader(locationOfProperties.openStream(), StandardCharsets.UTF_8)) {
            ConfigValueProperties p = new ConfigValueProperties(name, ordinal);
            p.load(reader);
            return p;
        }
    }
}
