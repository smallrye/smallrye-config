package io.smallrye.config.source.toml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import io.smallrye.common.classloader.ClassPathUtils;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.common.MapBackedConfigSource;

public class TomlConfigSource extends MapBackedConfigSource {
    @Serial
    private static final long serialVersionUID = -418186029484956531L;

    public static final String NAME = "TomlConfigSource[source=%s]";
    public static final int ORDINAL = DEFAULT_ORDINAL + 10;

    public TomlConfigSource(String name, Map<String, String> source, int ordinal) {
        super(name, source, ordinal, false);
    }

    public TomlConfigSource(URL url) throws IOException {
        this(url, ORDINAL);
    }

    public TomlConfigSource(URL url, int ordinal) throws IOException {
        this(String.format(NAME, url.toString()),
                ClassPathUtils.readStream(url, (Function<InputStream, Map<String, String>>) inputStream -> {
                    try {
                        return streamToMap(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }), ordinal);
    }

    public TomlConfigSource(String name, String source) {
        this(name, source, ORDINAL);
    }

    public TomlConfigSource(String name, String source, int ordinal) {
        this(name, stringToMap(source), ordinal);
    }

    private static Map<String, String> streamToMap(InputStream inputStream) throws IOException {
        Assert.checkNotNullParam("inputStream", inputStream);
        try (inputStream) {
            TomlParseResult result = Toml.parse(inputStream);
            return tomlToMap(result);
        }
    }

    private static Map<String, String> stringToMap(String str) {
        TomlParseResult result = Toml.parse(str);
        return tomlToMap(result);
    }

    private static Map<String, String> tomlToMap(TomlTable table) {
        final Map<String, String> properties = new TreeMap<>();
        if (table != null) {
            flattenToml("", table, properties);
        }
        return properties;
    }

    private static void flattenToml(String path, TomlTable source, Map<String, String> target) {
        for (String originalKey : source.keySet()) {
            String key = originalKey;

            if (key.contains(".")) {
                key = "\"" + key + "\"";
            }

            if (!key.isEmpty() && !path.isEmpty()) {
                key = path + "." + key;
            } else if (!path.isEmpty()) {
                key = path;
            }

            Object value = source.get(List.of(originalKey));

            if (value instanceof TomlTable) {
                flattenToml(key, (TomlTable) value, target);
            } else if (value instanceof final TomlArray array) {
                flattenArray(key, array, target);
            } else if (value != null) {
                target.put(key, value.toString());
            }
        }
    }

    private static void flattenArray(String key, TomlArray array, Map<String, String> target) {
        for (int i = 0; i < array.size(); i++) {
            Object element = array.get(i);
            String indexedKey = key + "[" + i + "]";
            if (element instanceof TomlTable) {
                flattenToml(indexedKey, (TomlTable) element, target);
            } else if (element instanceof TomlArray) {
                flattenArray(indexedKey, (TomlArray) element, target);
            } else if (element != null) {
                target.put(indexedKey, element.toString());
            }
        }
    }

}
