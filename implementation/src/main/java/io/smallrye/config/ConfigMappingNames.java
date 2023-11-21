package io.smallrye.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ConfigMappingNames {
    private final Map<String, Names> names;

    public ConfigMappingNames(final Map<String, Map<String, Set<String>>> names) {
        this.names = new HashMap<>(names.size());
        for (Map.Entry<String, Map<String, Set<String>>> entry : names.entrySet()) {
            this.names.put(entry.getKey(), new Names(entry.getValue()));
        }
    }

    Set<PropertyName> get(String mapping, String name) {
        return names.get(mapping).get(name);
    }

    private static class Names {
        private final Map<PropertyName, Set<PropertyName>> names;
        private final Map<PropertyName, Set<PropertyName>> anys;

        Names(Map<String, Set<String>> names) {
            this.names = new HashMap<>();
            this.anys = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : names.entrySet()) {
                if (entry.getKey().indexOf('*') == -1) {
                    this.names.put(new PropertyName(entry.getKey()), toMappingNameSet(entry.getValue()));
                } else {
                    this.anys.put(new PropertyName(entry.getKey()), toMappingNameSet(entry.getValue()));
                }
            }
        }

        Set<PropertyName> get(String name) {
            PropertyName mappingName = new PropertyName(name);
            return names.getOrDefault(mappingName, anys.get(mappingName));
        }

        private static Set<PropertyName> toMappingNameSet(Set<String> names) {
            Set<PropertyName> mappingNames = new HashSet<>(names.size());
            for (String name : names) {
                mappingNames.add(new PropertyName(name));
            }
            return mappingNames;
        }
    }
}
