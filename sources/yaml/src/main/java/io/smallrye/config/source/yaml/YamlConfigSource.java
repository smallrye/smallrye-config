package io.smallrye.config.source.yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.yaml.snakeyaml.Yaml;

import io.smallrye.config.source.file.AbstractUrlBasedSource;

/**
 * Yaml config source
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class YamlConfigSource extends AbstractUrlBasedSource {

    @Override
    protected String getFileExtension() {
        return "yaml";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, String> toMap(InputStream inputStream) {
        final Map<String, String> properties = new TreeMap<>();
        Yaml yaml = new Yaml();
        Map<String, Object> yamlInput = yaml.loadAs(inputStream, TreeMap.class);

        for (String key : yamlInput.keySet()) {
            populateMap(properties, key, yamlInput.get(key));
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private void populateMap(Map<String, String> properties, String key, Object o) {
        if (o instanceof Map) {
            Map map = (Map) o;
            for (Object mapKey : map.keySet()) {
                populateEntry(properties, key, String.valueOf(mapKey), map);
            }
        } else if (o instanceof List) {
            List<String> l = toStringList((List) o);
            properties.put(key, String.join(COMMA, l));
        } else {
            if (o != null) {
                properties.put(key, String.valueOf(o));
            } else {
                properties.put(key, null);
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void populateEntry(Map<String, String> properties, String key, String mapKey, Map<String, Object> map) {
        String format = "%s" + super.getKeySeparator() + "%s";
        if (map.get(mapKey) instanceof Map) {
            populateMap(properties, String.format(format, key, mapKey), (Map<String, Object>) map.get(mapKey));
        } else if (map.get(mapKey) instanceof List) {
            List<String> l = toStringList((List) map.get(mapKey));
            properties.put(String.format(format, key, mapKey), String.join(COMMA, l));
        } else {
            Object value = map.get(mapKey);
            if (value != null) {
                properties.put(String.format(format, key, mapKey), String.valueOf(value));
            } else {
                properties.put(String.format(format, key, mapKey), null);
            }
        }
    }

    private List<String> toStringList(List l) {
        List<String> nl = new ArrayList<>();
        for (Object o : l) {
            String s = String.valueOf(o);
            if (s.contains(COMMA))
                s = s.replaceAll(COMMA, "\\\\,"); // Escape comma
            nl.add(s);
        }
        return nl;
    }

    private static final String COMMA = ",";
}
