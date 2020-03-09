package io.smallrye.config.source.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.yaml.snakeyaml.Yaml;

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
        final StringBuilder keyBuilder = new StringBuilder();
        populateFromMapNode(properties, keyBuilder, yamlInput);
        return properties;
    }

    private static void populateFromMapNode(Map<String, String> properties, StringBuilder keyBuilder, Map<String, Object> o) {
        if (o == null)
            return;

        int len = keyBuilder.length();
        for (String nestedKey : o.keySet()) {
            if (nestedKey != null) {
                if (keyBuilder.length() > 0) {
                    keyBuilder.append('.');
                }
                if (nestedKey.indexOf('.') != -1) {
                    keyBuilder.append('"');
                    escapeQuotes(keyBuilder, nestedKey);
                    keyBuilder.append('"');
                } else {
                    keyBuilder.append(nestedKey);
                }
            }
            populateFromNode(properties, keyBuilder, o.get(nestedKey));
            keyBuilder.setLength(len);
        }
    }

    @SuppressWarnings("unchecked")
    private static void populateFromNode(Map<String, String> properties, StringBuilder keyBuilder, Object o) {
        if (o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            populateFromMapNode(properties, keyBuilder, map);
        } else if (o instanceof List) {
            StringBuilder b = new StringBuilder();
            populateFromEntryNode(b, o, 0);
            properties.put(keyBuilder.toString(), b.toString());
        } else {
            if (o != null) {
                properties.put(keyBuilder.toString(), o.toString());
            } else {
                properties.put(keyBuilder.toString(), "");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void populateFromEntryNode(StringBuilder valueBuilder, Object o, int escapeLevel) {
        if (o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                escapeMapKey(valueBuilder, entry.getKey(), escapeLevel + 1);
                appendEscaped(valueBuilder, '=', escapeLevel);
                populateFromEntryNode(valueBuilder, entry.getValue(), escapeLevel + 1);
            }
        } else if (o instanceof List) {
            final Iterator<?> iterator = ((List<?>) o).iterator();
            if (iterator.hasNext()) {
                populateFromEntryNode(valueBuilder, iterator.next(), escapeLevel + 1);
                while (iterator.hasNext()) {
                    appendEscaped(valueBuilder, ',', escapeLevel);
                    populateFromEntryNode(valueBuilder, iterator.next(), escapeLevel + 1);
                }
            }
        } else {
            if (o != null) {
                String src = o.toString();
                if (!src.isEmpty()) {
                    escapeCommas(valueBuilder, src, escapeLevel);
                }
            }
        }
    }

    private static void escape(StringBuilder b, int escapeLevel) {
        if (escapeLevel == 0) {
            return;
        }
        int count = 1 << (escapeLevel - 1);
        for (int i = 0; i < count; i++) {
            b.append('\\');
        }
    }

    private static void appendEscaped(StringBuilder b, char ch, int escapeLevel) {
        escape(b, escapeLevel);
        b.append(ch);
    }

    private static void escapeQuotes(StringBuilder b, String src) {
        int cp;
        for (int i = 0; i < src.length(); i += Character.charCount(cp)) {
            cp = src.codePointAt(i);
            if (cp == '\\' || cp == '"') {
                b.append('\\');
            }
            b.appendCodePoint(cp);
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

    private static void escapeMapKey(StringBuilder b, String src, int escapeLevel) {
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
