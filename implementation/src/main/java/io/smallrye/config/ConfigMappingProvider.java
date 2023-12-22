package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.ConfigMappings.getDefaults;
import static io.smallrye.config.ConfigMappings.getNames;
import static io.smallrye.config.ConfigMappings.getProperties;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;

/**
 *
 */
final class ConfigMappingProvider implements Serializable {
    private static final long serialVersionUID = 3977667610888849912L;

    private final Map<String, List<Class<?>>> roots;
    private final Set<String> keys;
    private final Map<String, Map<String, Set<String>>> names;
    private final List<String[]> ignoredPaths;
    private final boolean validateUnknown;

    ConfigMappingProvider(final Builder builder) {
        this.roots = new HashMap<>(builder.roots);
        this.keys = builder.keys;
        this.names = builder.names;
        this.ignoredPaths = builder.ignoredPaths;
        this.validateUnknown = builder.validateUnknown;
    }

    public static Builder builder() {
        return new Builder();
    }

    Map<Class<?>, Map<String, ConfigMappingObject>> mapConfiguration(final SmallRyeConfig config)
            throws ConfigValidationException {
        if (roots.isEmpty()) {
            return Collections.emptyMap();
        }

        // Register additional dissabiguation property names comparing mapped keys and env names
        matchPropertiesWithEnv(config, roots.keySet(), keys);

        // Perform the config mapping
        ConfigMappingContext context = SecretKeys.doUnlocked(new Supplier<ConfigMappingContext>() {
            @Override
            public ConfigMappingContext get() {
                return new ConfigMappingContext(config, roots, names);
            }
        });

        if (config.getOptionalValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, boolean.class).orElse(this.validateUnknown)) {
            context.reportUnknown(ignoredPaths);
        }

        List<ConfigValidationException.Problem> problems = context.getProblems();
        if (!problems.isEmpty()) {
            throw new ConfigValidationException(problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }

        return context.getRootsMap();
    }

    private static void matchPropertiesWithEnv(
            final SmallRyeConfig config,
            final Set<String> roots,
            final Set<String> mappedProperties) {
        // TODO - We shouldn't be mutating the EnvSource.
        // We should do the calculation when creating the EnvSource, but right now mappings and sources are not well integrated.

        // Check Env properties
        StringBuilder sb = new StringBuilder();
        for (ConfigSource configSource : config.getConfigSources(EnvConfigSource.class)) {
            EnvConfigSource envConfigSource = (EnvConfigSource) configSource;
            // Filter Envs with roots
            List<String> envProperties = new ArrayList<>();
            if (roots.contains("")) {
                envProperties.addAll(envConfigSource.getPropertyNames());
            } else {
                for (String envProperty : envConfigSource.getPropertyNames()) {
                    for (String root : roots) {
                        if (isEnvPropertyInRoot(envProperty, root)) {
                            envProperties.add(envProperty);
                            break;
                        }
                    }
                }
            }

            // Try to match Env with Root mapped property and generate the expected format
            for (String envProperty : envProperties) {
                // We improve matching here by filtering only mapped properties from the matched root
                for (String mappedProperty : mappedProperties) {
                    List<Integer> indexOfDashes = indexOfDashes(mappedProperty, envProperty);
                    if (indexOfDashes != null) {
                        sb.append(envProperty);
                        for (Integer dash : indexOfDashes) {
                            sb.setCharAt(dash, '-');
                        }
                        String expectedEnvProperty = sb.toString();
                        if (!envProperty.equals(expectedEnvProperty)) {
                            envConfigSource.getPropertyNames().add(sb.toString());
                            envConfigSource.getPropertyNames().remove(envProperty);
                        }
                        sb.setLength(0);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Matches if a dotted Environment property name is part of a registered root.
     *
     * @param envProperty a generated dotted property from the {@link EnvConfigSource}.
     * @param root the root name
     * @return <code>true</code> if the env property ir part of the root, or <code>false</code> otherwise.
     */
    private static boolean isEnvPropertyInRoot(final String envProperty, final String root) {
        if (envProperty.equals(root)) {
            return true;
        }

        // if property is less than the root no way to match
        if (envProperty.length() <= root.length()) {
            return false;
        }

        // foo.bar
        // foo.bar."baz"
        // foo.bar[0]
        char e = envProperty.charAt(root.length());
        if ((e == '.') || e == '[') {
            for (int i = 0; i < root.length(); i++) {
                char r = root.charAt(i);
                e = envProperty.charAt(i);
                if (r == '-') {
                    if (e != '.' && e != '-') {
                        return false;
                    }
                } else if (r != e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Finds and returns all indexes from a dotted Environment property name, related to its matched mapped
     * property name that must be replaced with a dash. This allows to set single environment variables as
     * <code>FOO_BAR_BAZ</code> and match them to mappeds properties like <code>foo.*.baz</code>,
     * <code>foo-bar.baz</code> or any other combinations find in mappings, without the need of additional metadata.
     *
     * @param mappedProperty the mapping property name.
     * @param envProperty a generated dotted property from the {@link EnvConfigSource}.
     * @return a List of indexes from the env property name to replace with a dash.
     */
    private static List<Integer> indexOfDashes(final String mappedProperty, final String envProperty) {
        if (mappedProperty.length() > envProperty.length()) {
            return null;
        }

        List<Integer> dashesPosition = null;
        int matchPosition = envProperty.length() - 1;
        for (int i = mappedProperty.length() - 1; i >= 0; i--) {
            if (matchPosition == -1) {
                return null;
            }

            char c = mappedProperty.charAt(i);
            if (c == '.' || c == '-') {
                char p = envProperty.charAt(matchPosition);
                if (p != '.' && p != '-') { // a property coming from env can either be . or -
                    return null;
                }
                if (c == '-') {
                    if (dashesPosition == null) {
                        dashesPosition = new ArrayList<>();
                    }
                    dashesPosition.add(matchPosition);
                }
                matchPosition--;
            } else if (c == '*') { // it's a map - skip to next separator
                char p = envProperty.charAt(matchPosition);
                if (p == '"') {
                    matchPosition = envProperty.lastIndexOf('"', matchPosition - 1);
                    if (matchPosition != -1) {
                        matchPosition = envProperty.lastIndexOf('.', matchPosition);
                    }
                }
                matchPosition = envProperty.lastIndexOf('.', matchPosition);
            } else if (c == ']') { // it's a collection - skip to next separator
                i = i - 2;
                matchPosition = envProperty.lastIndexOf('[', matchPosition);
                if (matchPosition != -1) {
                    matchPosition--;
                }
            } else if (c != envProperty.charAt(matchPosition)) {
                return null;
            } else {
                matchPosition--;
            }
        }
        return dashesPosition;
    }

    static final class Builder {
        Set<Class<?>> types = new HashSet<>();
        Map<String, List<Class<?>>> roots = new HashMap<>();
        Set<String> keys = new HashSet<>();
        Map<String, Map<String, Set<String>>> names = new HashMap<>();
        List<String[]> ignoredPaths = new ArrayList<>();
        boolean validateUnknown = true;
        SmallRyeConfigBuilder configBuilder = null;

        Builder() {
        }

        Builder addRoot(String prefix, Class<?> type) {
            Assert.checkNotNullParam("path", prefix);
            Assert.checkNotNullParam("type", type);
            types.add(type);
            roots.computeIfAbsent(prefix, k -> new ArrayList<>(4)).add(getConfigMappingClass(type));
            return this;
        }

        Builder keys(Set<String> keys) {
            Assert.checkNotNullParam("keys", keys);
            this.keys.addAll(keys);
            return this;
        }

        Builder names(Map<String, Map<String, Set<String>>> names) {
            Assert.checkNotNullParam("names", names);
            for (Map.Entry<String, Map<String, Set<String>>> entry : names.entrySet()) {
                Map<String, Set<String>> groupNames = this.names.computeIfAbsent(entry.getKey(),
                        new Function<String, Map<String, Set<String>>>() {
                            @Override
                            public Map<String, Set<String>> apply(
                                    final String s) {
                                return new HashMap<>();
                            }
                        });
                groupNames.putAll(entry.getValue());
            }
            return this;
        }

        Builder ignoredPath(String ignoredPath) {
            Assert.checkNotNullParam("ignoredPath", ignoredPath);
            ignoredPaths.add(ignoredPath.split("\\."));
            return this;
        }

        Builder validateUnknown(boolean validateUnknown) {
            this.validateUnknown = validateUnknown;
            return this;
        }

        Builder registerDefaults(SmallRyeConfigBuilder configBuilder) {
            this.configBuilder = configBuilder;
            return this;
        }

        ConfigMappingProvider build() {
            // We don't validate for MP ConfigProperties, so if all classes are MP ConfigProperties disable validation.
            boolean allConfigurationProperties = true;
            for (Class<?> type : types) {
                if (ConfigMappingClass.getConfigurationClass(type) == null) {
                    allConfigurationProperties = false;
                    break;
                }
            }

            if (allConfigurationProperties) {
                validateUnknown = false;
            }

            if (keys.isEmpty()) {
                for (Map.Entry<String, List<Class<?>>> entry : roots.entrySet()) {
                    for (Class<?> root : entry.getValue()) {
                        ConfigClassWithPrefix configClass = configClassWithPrefix(root, entry.getKey());
                        keys(getProperties(configClass).get(configClass.getKlass()).get(configClass.getPrefix()).keySet());
                    }
                }
            }

            if (names.isEmpty()) {
                for (Map.Entry<String, List<Class<?>>> entry : roots.entrySet()) {
                    for (Class<?> root : entry.getValue()) {
                        ConfigClassWithPrefix configClass = configClassWithPrefix(root, entry.getKey());
                        names(getNames(configClass));
                    }
                }
            }

            if (configBuilder != null) {
                Map<String, String> defaultValues = configBuilder.getDefaultValues();
                for (Map.Entry<String, List<Class<?>>> entry : roots.entrySet()) {
                    for (Class<?> root : entry.getValue()) {
                        for (Map.Entry<String, String> defaultEntry : getDefaults(configClassWithPrefix(root, entry.getKey()))
                                .entrySet()) {
                            defaultValues.putIfAbsent(defaultEntry.getKey(), defaultEntry.getValue());
                        }
                    }
                }
            }

            return new ConfigMappingProvider(this);
        }
    }
}
