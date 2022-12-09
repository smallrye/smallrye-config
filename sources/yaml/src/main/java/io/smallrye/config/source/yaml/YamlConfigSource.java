package io.smallrye.config.source.yaml;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;

import io.smallrye.common.classloader.ClassPathUtils;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Yaml config source
 *
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class YamlConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = -418186029484956531L;

    private static final String NAME_PREFIX = "YamlConfigSource[source=";
    private static final int ORDINAL = ConfigSource.DEFAULT_ORDINAL + 10;
    private static final Yaml DUMPER;

    static {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.FOLDED);
        DUMPER = new Yaml(dumperOptions);
    }

    private final Set<String> propertyNames;

    public YamlConfigSource(String name, Map<String, String> source, int ordinal) {
        super(name, source, ordinal, false);
        this.propertyNames = filterPropertyNames(source);
    }

    @Deprecated
    public YamlConfigSource(String name, InputStream stream) throws IOException {
        this(name, stream, ORDINAL);
    }

    public YamlConfigSource(URL url) throws IOException {
        this(url, ORDINAL);
    }

    public YamlConfigSource(URL url, int ordinal) throws IOException {
        this(NAME_PREFIX + url.toString() + "]",
                ClassPathUtils.readStream(url, (Function<InputStream, Map<String, String>>) inputStream -> {
                    try {
                        return streamToMap(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }), ordinal);
    }

    @Deprecated
    public YamlConfigSource(String name, InputStream stream, int defaultOrdinal) throws IOException {
        this(name, streamToMap(stream), defaultOrdinal);
    }

    public YamlConfigSource(String name, String source) {
        this(name, source, ORDINAL);
    }

    public YamlConfigSource(String name, String source, int ordinal) {
        this(name, stringToMap(source), ordinal);
    }

    @Override
    public Set<String> getPropertyNames() {
        return propertyNames;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> streamToMap(InputStream inputStream) throws IOException {
        Assert.checkNotNullParam("inputStream", inputStream);
        final Map<String, String> yamlInput = new TreeMap<>();
        try {
            final Iterable<Object> objects = new Yaml(new StringConstructor(new LoaderOptions())).loadAll(inputStream);
            for (Object object : objects) {
                if (object instanceof Map) {
                    yamlInput.putAll(yamlInputToMap((Map<Object, Object>) object));
                }
            }
            inputStream.close();
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
        return yamlInput;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringToMap(String str) {
        final Map<String, String> yamlInput = new TreeMap<>();
        final Iterable<Object> objects = new Yaml(new StringConstructor(new LoaderOptions())).loadAll(str);
        for (Object object : objects) {
            if (object instanceof Map) {
                yamlInput.putAll(yamlInputToMap((Map<Object, Object>) object));
            }
        }
        return yamlInput;
    }

    private static Map<String, String> yamlInputToMap(final Map<Object, Object> yamlInput) {
        final Map<String, String> properties = new TreeMap<>();
        if (yamlInput != null) {
            flattenYaml("", yamlInput, properties, false);
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static void flattenYaml(String path, Map<Object, Object> source, Map<String, String> target, boolean indexed) {
        source.forEach((originalKey, value) -> {
            String key;
            if (originalKey == null) {
                key = "";
            } else {
                key = originalKey.toString();
            }

            if (key.contains(".")) {
                key = "\"" + key + "\"";
            }

            if (!key.isEmpty() && path != null && !path.isEmpty()) {
                key = indexed ? path + key : path + "." + key;
            } else if (path != null && !path.isEmpty()) {
                key = path;
            }

            if (value instanceof String) {
                target.put(key, (String) value);
            } else if (value instanceof Map) {
                flattenYaml(key, (Map<Object, Object>) value, target, false);
            } else if (value instanceof List) {
                final List<Object> list = (List<Object>) value;
                flattenList(key, list, target);
                for (int i = 0; i < list.size(); i++) {
                    flattenYaml(key, Collections.singletonMap("[" + i + "]", list.get(i)), target, true);
                }
            } else {
                if (value != null) {
                    target.put(key, value.toString());
                }
            }
        });
    }

    private static void flattenList(String key, List<Object> source, Map<String, String> target) {
        boolean mixed = false;
        List<String> flatten = new ArrayList<>();
        for (Object value : source) {
            if (value instanceof String || value instanceof Boolean) {
                flatten.add(value.toString());
            } else if (value != null) {
                mixed = true;
                break;
            }
        }

        if (!mixed) {
            target.put(key, flatten.stream().map(value -> {
                StringBuilder sb = new StringBuilder();
                escapeCommas(sb, value, 1);
                return sb.toString();
            }).collect(Collectors.joining(",")));
        } else {
            // Mark keys for later removal
            key = YamlConfigSource.class.getName() + ".filter." + key;
            // This dumps the entire YAML in a parent property. It was added to support complex mappings, but it is not
            // needed anymore with the indexed property support. We keep it for compatibility reasons.
            target.put(key, DUMPER.dump(singletonMap(key.substring(key.lastIndexOf(".") + 1), source)));
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

    private static Set<String> filterPropertyNames(Map<String, String> source) {
        final Set<String> filteredKeys = new HashSet<>();
        for (final String key : new HashSet<>(source.keySet())) {
            if (key.startsWith(YamlConfigSource.class.getName() + ".filter.")) {
                String originalKey = key.substring(55);
                source.put(originalKey, source.remove(key));
            } else {
                filteredKeys.add(key);
            }
        }
        return filteredKeys;
    }

    /**
     * Override some yaml constructors, so that the value written in the flatten result is more alike with the
     * source. For instance, timestamps may be written in a completely different format which prevents converters to
     * convert the correct value.
     */
    private static class StringConstructor extends SafeConstructor {
        public StringConstructor(final LoaderOptions loadingConfig) {
            super(loadingConfig);
            this.yamlConstructors.put(Tag.INT, new ConstructYamlStr());
            this.yamlConstructors.put(Tag.FLOAT, new ConstructYamlStr());
            this.yamlConstructors.put(Tag.TIMESTAMP, new ConstructYamlStr());
        }
    }
}
