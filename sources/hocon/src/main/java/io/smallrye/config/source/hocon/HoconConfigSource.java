package io.smallrye.config.source.hocon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
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
        final Set<Map.Entry<String, ConfigValue>> entries = config.entrySet();
        for (Map.Entry<String, ConfigValue> entry : entries) {
            flatten(entry.getKey(), entry.getValue(), properties);
        }
        return properties;
    }

    private static void flatten(String path, ConfigValue value, Map<String, String> target) {
        if (value instanceof ConfigObject) {
            flattenObject(path, (ConfigObject) value, target);
        } else if (value instanceof ConfigList) {
            flattenList(path, (ConfigList) value, target);
        } else {
            target.put(path, value.unwrapped().toString());
        }
    }

    private static void flattenObject(String path, ConfigObject value, Map<String, String> target) {
        for (Map.Entry<String, ConfigValue> entry : value.entrySet()) {
            flatten(path + "." + entry.getKey(), entry.getValue(), target);
        }
    }

    private static void flattenList(String path, ConfigList value, Map<String, String> target) {
        for (int i = 0, valueSize = value.size(); i < valueSize; i++) {
            flatten(path + "[" + i + "]", value.get(i), target);
        }
    }
}
