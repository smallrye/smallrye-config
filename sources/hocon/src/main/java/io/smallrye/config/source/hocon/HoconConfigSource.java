package io.smallrye.config.source.hocon;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.typesafe.config.Config;

import io.smallrye.config.common.MapBackedConfigSource;

public class HoconConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = -458821383311704657L;

    public HoconConfigSource(Config config, String source, int ordinal) {
        super("HoconConfigSource[source=" + source + "]", Collections.unmodifiableMap(config.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> config.getString(entry.getKey())))), ordinal);
    }
}
