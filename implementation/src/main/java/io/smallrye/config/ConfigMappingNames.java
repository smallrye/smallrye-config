package io.smallrye.config;

import static io.smallrye.config.PropertyName.name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the full structure of mapping classes per class name, relative path and property name.
 */
class ConfigMappingNames {
    private final Map<String, Map<PropertyName, List<PropertyName>>> names;

    ConfigMappingNames(final Map<String, Map<String, Set<String>>> names) {
        this.names = new HashMap<>(names.size());

        for (Map.Entry<String, Map<String, Set<String>>> mappings : names.entrySet()) {
            // We chance this to be a List because different names may equal the PropertyName
            Map<PropertyName, List<PropertyName>> mappingPropertyNames = new HashMap<>();
            for (Map.Entry<String, Set<String>> mappingNames : mappings.getValue().entrySet()) {
                PropertyName key = name(mappingNames.getKey());
                mappingPropertyNames.putIfAbsent(key, new ArrayList<>());
                // Give priority to the star key
                if (key.getName().contains("*")) {
                    mappingPropertyNames.put(key, mappingPropertyNames.remove(key));
                }

                for (String value : mappingNames.getValue()) {
                    mappingPropertyNames.get(key).add(name(value));
                }
            }
            this.names.put(mappings.getKey(), mappingPropertyNames);
        }
    }

    public Map<String, Map<PropertyName, List<PropertyName>>> getNames() {
        return names;
    }

    /**
     * Matches that at least one runtime configuration name is in the root path and relative path of a mapping class.
     * This is required to trigger the construction of lazy mapping objects like <code>Optional</code> or
     * <code>Map</code>.
     *
     * @param mapping the class name of the mapping
     * @param rootPath the root path of the mapping
     * @param path the relative path to the mapping
     * @param names the runtime config names
     * @return <code>true</code> if a runtime config name exits in the mapping names or <code>false</code> otherwise
     */
    boolean hasAnyName(final String mapping, final String rootPath, final String path, final Iterable<String> names) {
        Map<PropertyName, List<PropertyName>> mappings = this.names.get(mapping);
        if (mappings == null) {
            return false;
        }

        // Simple case, no need to remove the rootPath from searched path and names
        if (rootPath == null || rootPath.isEmpty()) {
            return hasAnyName(mappings, path, names);
        }

        // The path does not match the rootPath or the next char is not a separator dot we can skip
        if (!path.startsWith(rootPath) || (path.length() > rootPath.length() && path.charAt(rootPath.length()) != '.'
                && path.charAt(rootPath.length()) != '[')) {
            return false;
        }

        // Same length replace with empty string since we know they already match, or start after the next separator
        PropertyName mappingName;
        if (path.length() == rootPath.length()) {
            mappingName = name("");
        } else if (path.charAt(rootPath.length()) == '.') {
            mappingName = name(path.substring(rootPath.length() + 1));
        } else {
            mappingName = name(path.substring(rootPath.length()));
        }
        List<PropertyName> mappingNames = mappings.get(mappingName);
        if (mappingNames == null) {
            return false;
        }

        for (String name : names) {
            // We can't check for next char separator dot because we may find collection names with square brackets
            if (name.startsWith(path)) {
                PropertyName propertyName = name.length() > rootPath.length() && name.charAt(rootPath.length()) == '.'
                        ? name(name.substring(rootPath.length() + 1))
                        : name(name.substring(rootPath.length()));
                if (mappingNames.contains(propertyName)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean hasAnyName(final Map<PropertyName, List<PropertyName>> mappings, final String path,
            final Iterable<String> names) {
        List<PropertyName> mappingNames = mappings.get(name(path));
        if (mappingNames == null || mappingNames.isEmpty()) {
            return false;
        }

        for (String name : names) {
            if (name.startsWith(path) && mappingNames.contains(name(name))) {
                return true;
            }
        }
        return false;
    }
}
