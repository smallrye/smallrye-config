package io.smallrye.config.source.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Yaml config source
 *
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class YamlConfigSource extends MapBackedConfigSource {

    static final int ORDINAL = ConfigSource.DEFAULT_ORDINAL + 10;

    private static final long serialVersionUID = -418186029484956531L;

    public YamlConfigSource(String name, InputStream stream) throws IOException {
        this(name, stream, ORDINAL);
    }

    public YamlConfigSource(String name, InputStream stream, int defaultOrdinal) throws IOException {
        super(name, streamToMap(stream), defaultOrdinal, false);
    }

    public YamlConfigSource(String name, String str) {
        this(name, str, ORDINAL);
    }

    public YamlConfigSource(String name, String str, int defaultOrdinal) {
        super(name, stringToMap(str), defaultOrdinal, false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> streamToMap(InputStream inputStream) throws IOException {
        Assert.checkNotNullParam("inputStream", inputStream);
        final Map<String, Object> yamlInput;
        try {
            yamlInput = new Yaml().loadAs(inputStream, HashMap.class);
            inputStream.close();
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
        return yamlInputToMap(yamlInput);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringToMap(String str) {
        final Map<String, Object> yamlInput = new Yaml().loadAs(str, HashMap.class);
        return yamlInputToMap(yamlInput);
    }

    private static Map<String, String> yamlInputToMap(final Map<String, Object> yamlInput) {
        final Map<String, String> properties = new TreeMap<>();
        if (yamlInput != null) {
            flattenYaml("", yamlInput, properties);
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static void flattenYaml(String path, Map<String, Object> source, Map<String, String> target) {
        source.forEach((key, value) -> {
            if (key != null && !key.isEmpty() && path != null && !path.isEmpty()) {
                key = path + "." + key;
            } else if (path != null && !path.isEmpty()) {
                key = path;
            } else if (key == null || key.isEmpty()) {
                key = "";
            }

            if (value instanceof String) {
                target.put(key, (String) value);
            } else if (value instanceof Map) {
                flattenYaml(key, (Map<String, Object>) value, target);
            } else if (value instanceof List) {
                final List<Object> list = (List<Object>) value;
                flattenList(key, list, target);
                for (int i = 0; i < list.size(); i++) {
                    flattenYaml(key, Collections.singletonMap("[" + i + "]", list.get(i)), target);
                }
            } else {
                target.put(key, (value != null ? value.toString() : ""));
            }
        });
    }

    private static void flattenList(String key, List<Object> source, Map<String, String> target) {
        if (source.stream().allMatch(o -> o instanceof String)) {
            target.put(key, source.stream().map(o -> {
                StringBuilder sb = new StringBuilder();
                escapeCommas(sb, o.toString(), 0);
                return sb.toString();
            }).collect(Collectors.joining(",")));
        } else {
            final DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
            dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.FOLDED);
            target.put(key,
                    new Yaml(dumperOptions).dump(Collections.singletonMap(key.substring(key.lastIndexOf(".") + 1), source)));
        }
    }

    private static void escapeCommas(StringBuilder b, String src, int escapeLevel) {
        int cp;
        for (int i = 0; i < src.length(); i += Character.charCount(cp)) {
            cp = src.codePointAt(i);
            if (cp == '\\' || cp == ',') {
                for (int j = 0; j < escapeLevel; j++) {
                    b.append('\\');
                }
            }
            b.appendCodePoint(cp);
        }
    }
}
