package io.smallrye.configsource;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.typesafe.config.Config;

import io.smallrye.config.PropertiesConfigSource;

public class HoconConfigSource implements ConfigSource, Serializable {
    private final String source;
    private final PropertiesConfigSource delegate;

    public HoconConfigSource(Config config, String source, int ordinal) {
        this.source = source;
        this.delegate = new PropertiesConfigSource(Collections.unmodifiableMap(config.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> config.getString(entry.getKey())))), source, ordinal);
    }

    @Override
    public Map<String, String> getProperties() {
        return this.delegate.getProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return this.delegate.getPropertyNames();
    }

    @Override
    public int getOrdinal() {
        return delegate.getOrdinal();
    }

    @Override
    public String getValue(String s) {
        return delegate.getValue(s);
    }

    @Override
    public String getName() {
        return "HoconConfigSource[source=" + source + "]";
    }

    @Override
    public String toString() {
        return getName();
    }
}
