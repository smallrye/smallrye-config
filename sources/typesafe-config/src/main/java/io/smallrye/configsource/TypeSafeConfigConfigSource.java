package io.smallrye.configsource;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TypeSafeConfigConfigSource implements ConfigSource, Serializable {

    static final int DEFAULT_ORDINAL = 50;

    private final Map<String, String> properties;
    private final int ordinal;

    public TypeSafeConfigConfigSource() {
        final Config config = ConfigFactory.load();
        this.ordinal = config.hasPath(CONFIG_ORDINAL) ? config.getInt(CONFIG_ORDINAL) : DEFAULT_ORDINAL;
        this.properties = Collections.unmodifiableMap(config.entrySet().stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey, entry -> config.getString(entry.getKey()))));
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public int getOrdinal() {
        return this.ordinal;
    }

    @Override
    public String getValue(String s) {
        return this.properties.get(s);
    }

    @Override
    public String getName() {
        return TypeSafeConfigConfigSource.class.getSimpleName();
    }

    @Override
    public String toString() {
        return getName();
    }

}
