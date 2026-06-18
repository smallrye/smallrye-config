package io.smallrye.config.source.hocon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import io.smallrye.common.classloader.ClassPathUtils;
import io.smallrye.config.common.MapBackedConfigSource;

public class HoconConfigSource extends MapBackedConfigSource {
    @Serial
    private static final long serialVersionUID = -458821383311704657L;

    public static final String NAME = "HoconConfigSource[source=%s]";
    public static final int ORDINAL = DEFAULT_ORDINAL + 5;

    public HoconConfigSource(String name, Map<String, String> source, int ordinal) {
        super(name, source, ordinal, false);
    }

    public HoconConfigSource(URL url) throws IOException {
        this(url, ORDINAL);
    }

    public HoconConfigSource(URL url, int ordinal) throws IOException {
        this(String.format(NAME, url.toString()), ClassPathUtils.readStream(url, inputStream -> {
            try {
                return streamToMap(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }), ordinal);
    }

    private static Map<String, String> streamToMap(InputStream inputStream) throws IOException {
        final Map<String, String> input = new TreeMap<>();
        try (Reader reader = new InputStreamReader(inputStream)) {
            Config config = ConfigFactory.parseReader(reader).resolve();
            input.putAll(configToMap(config));
        }
        return input;
    }

    private static Map<String, String> configToMap(Config config) {
        final Map<String, String> properties = new TreeMap<>();
        flattenObject(List.of(), config.root(), properties);
        return properties;
    }

    private static void flatten(List<String> path, ConfigValue value, Map<String, String> target) {
        if (value instanceof ConfigObject) {
            flattenObject(path, (ConfigObject) value, target);
        } else if (value instanceof ConfigList) {
            flattenList(path, (ConfigList) value, target);
        } else {
            target.put(toPropertyName(path), value.unwrapped().toString());
        }
    }

    private static void flattenObject(List<String> path, ConfigObject value, Map<String, String> target) {
        for (Map.Entry<String, ConfigValue> entry : value.entrySet()) {
            List<String> entryPath = new ArrayList<>(path);
            entryPath.add(entry.getKey());
            flatten(entryPath, entry.getValue(), target);
        }
    }

    private static void flattenList(List<String> path, ConfigList value, Map<String, String> target) {
        for (int i = 0, valueSize = value.size(); i < valueSize; i++) {
            List<String> indexedPath = new ArrayList<>(path);
            int last = indexedPath.size() - 1;
            indexedPath.set(last, indexedPath.get(last) + "[" + i + "]");
            flatten(indexedPath, value.get(i), target);
        }
    }

    private static String toPropertyName(List<String> path) {
        StringBuilder propertyName = new StringBuilder();
        for (String segment : path) {
            if (!propertyName.isEmpty()) {
                propertyName.append('.');
            }
            propertyName.append(renderSegment(segment));
        }
        return propertyName.toString();
    }

    private static String renderSegment(String segment) {
        if (!needsQuotes(segment)) {
            return segment;
        }
        return '"' + segment + '"';
    }

    private static boolean needsQuotes(String segment) {
        if (segment.isEmpty()) {
            return true;
        }
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '.' || c == '"' || Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }
}
