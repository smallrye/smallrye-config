/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import static io.smallrye.config.common.utils.ConfigSourceUtil.CONFIG_ORDINAL_KEY;
import static io.smallrye.config.common.utils.StringUtil.isNumeric;
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static io.smallrye.config.common.utils.StringUtil.toLowerCaseAndDotted;
import static java.lang.Character.toLowerCase;
import static java.security.AccessController.doPrivileged;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigSource} to access Environment Variables following the mapping
 * rules defined by the MicroProfile Config specification.
 * <p>
 *
 * For a given property name <code>foo.bar.baz</code>, is matched to an environment variable with the following rules:
 *
 * <ol>
 * <li>Exact match (<code>foo.bar.baz</code>)</li>
 * <li>Replace each character that is neither alphanumeric nor <code>_</code> with <code>_</code>
 * (<code>foo_bar_baz</code>)</li>
 * <li>Replace each character that is neither alphanumeric nor <code>_</code> with <code>_</code>; then convert the name to
 * upper case (<code>FOO_BAR_BAZ</code>)</li>
 * </ol>
 * <p>
 *
 * Additionally, this implementation provides candidate matching dotted property name from the Environment
 * Variable name. These are required when a consumer relies on the list of properties to find additional
 * configurations. The MicroProfile Config specification defines a set of conversion rules to look up and find
 * values from environment variables even when using their dotted version, but it is unclear about property names.
 * <br>
 * Because an environment variable name may only be represented by a subset of characters, it is not possible
 * to represent exactly a dotted version name from an environment variable name, so consumers must be aware of such
 * limitations.
 */
public class EnvConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -4525015934376795496L;

    private static final int DEFAULT_ORDINAL = 300;

    private final EnvVars envVars;

    protected EnvConfigSource() {
        this(DEFAULT_ORDINAL);
    }

    protected EnvConfigSource(final int ordinal) {
        this(getEnvProperties(), ordinal);
    }

    public EnvConfigSource(final Map<String, String> properties, final int ordinal) {
        super("EnvConfigSource", getEnvOrdinal(properties, ordinal));
        this.envVars = new EnvVars(properties);
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<EnvName, EnvEntry> entry : envVars.getEnv().entrySet()) {
            EnvEntry entryValue = entry.getValue();
            if (entryValue.getEntries() != null) {
                properties.putAll(entryValue.getEntries());
            } else {
                properties.put(entryValue.getName(), entryValue.getValue());
            }
        }
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return envVars.getNames();
    }

    @Override
    public String getValue(final String propertyName) {
        return envVars.get(propertyName);
    }

    boolean hasPropertyName(final String propertyName) {
        return envVars.getEnv().containsKey(new EnvName(propertyName));
    }

    /**
     * A new Map with the contents of System.getEnv. In the Windows implementation, the Map is an extension of
     * ProcessEnvironment. This causes issues with Graal and native mode, since ProcessEnvironment should not be
     * instantiated in the heap.
     */
    private static Map<String, String> getEnvProperties() {
        return doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            public Map<String, String> run() {
                return new HashMap<>(System.getenv());
            }
        });
    }

    private static int getEnvOrdinal(final Map<String, String> properties, final int ordinal) {
        String value = properties.get(CONFIG_ORDINAL_KEY);
        if (value == null) {
            value = properties.get(CONFIG_ORDINAL_KEY.toUpperCase());
        }
        if (value != null) {
            return Converters.INTEGER_CONVERTER.convert(value);
        }
        return ordinal;
    }

    Object writeReplace() {
        return new Ser();
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = 6812312718645271331L;

        Object readResolve() {
            return new EnvConfigSource();
        }
    }

    static final class EnvVars implements Serializable {
        private static final long serialVersionUID = -56318356411229247L;

        private final Map<EnvName, EnvEntry> env;
        private final Set<String> names;

        public EnvVars(final Map<String, String> properties) {
            this.env = new HashMap<>(properties.size());
            this.names = new HashSet<>(properties.size() * 2);
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                EnvName envName = new EnvName(entry.getKey());
                EnvEntry envEntry = env.get(envName);
                if (envEntry == null) {
                    env.put(envName, new EnvEntry(entry.getKey(), entry.getValue()));
                } else {
                    envEntry.add(entry.getKey(), entry.getValue());
                }
                this.names.add(entry.getKey());
                this.names.add(toLowerCaseAndDotted(entry.getKey()));
            }
        }

        public String get(final String propertyName) {
            EnvEntry envEntry = env.get(new EnvName(propertyName));
            if (envEntry != null) {
                String value = envEntry.get();
                if (value != null) {
                    return value;
                }

                value = envEntry.getEntries().get(propertyName);
                if (value != null) {
                    return value;
                }

                String envName = replaceNonAlphanumericByUnderscores(propertyName);
                value = envEntry.getEntries().get(envName);
                if (value != null) {
                    return value;
                }

                return envEntry.envEntries.get(envName.toUpperCase());
            }
            return null;
        }

        public Map<EnvName, EnvEntry> getEnv() {
            return env;
        }

        public Set<String> getNames() {
            return names;
        }
    }

    static final class EnvName implements Serializable {
        private static final long serialVersionUID = -2679716955093904512L;

        private final String name;

        public EnvName(final String name) {
            assert name != null;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final EnvName that = (EnvName) o;
            return equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            int h = 0;
            int length = name.length();
            if (length >= 2) {
                if (name.charAt(length - 1) == '_' && name.charAt(length - 2) == '_') {
                    length = length - 1;
                }
            }

            for (int i = 0; i < length; i++) {
                char c = name.charAt(i);
                if (i == 0) {
                    if (c == '%' || c == '_') {
                        h = 31 * h + 31;
                        continue;
                    }
                }

                switch (c) {
                    case '.':
                    case '_':
                    case '-':
                    case '"':
                    case '*':
                    case '[':
                    case ']':
                    case '/':
                        continue;
                }
                h = 31 * h + toLowerCase(c);
                // h = 31 * h + c;
            }
            return h;
        }

        @SuppressWarnings("squid:S4973")
        static boolean equals(final String name, final String other) {
            //noinspection StringEquality
            if (name == other) {
                return true;
            }

            if (name.isEmpty() && other.isEmpty()) {
                return true;
            }

            if (name.isEmpty() || other.isEmpty()) {
                return false;
            }

            char n = name.charAt(0);
            char o = other.charAt(0);

            if (o == '%' || o == '_') {
                if (n != '%' && n != '_') {
                    return false;
                }
            }

            int matchPosition = name.length() - 1;
            for (int i = other.length() - 1; i >= 0; i--) {
                if (matchPosition == -1) {
                    return false;
                }

                o = other.charAt(i);
                n = name.charAt(matchPosition);

                // profile
                if (i == 0 && (o == '%' || o == '_')) {
                    if (n == '%' || n == '_') {
                        return true;
                    }
                }

                if (o == '.') {
                    if (n != '.' && n != '-' && n != '_' && n != '/') {
                        return false;
                    }
                } else if (o == '-') {
                    if (n != '.' && n != '-' && n != '_' && n != '/') {
                        return false;
                    }
                } else if (o == '"') {
                    if (n != '"' && n != '_') {
                        return false;
                    } else if (n == '_' && name.length() - 1 == matchPosition) {
                        matchPosition = name.lastIndexOf("_", matchPosition - 1);
                        if (matchPosition == -1) {
                            return false;
                        }
                    }
                } else if (o == ']') {
                    if (n != ']' && n != '_') {
                        return false;
                    }
                    int beginIndexed = other.lastIndexOf('[', i);
                    if (beginIndexed != -1) {
                        int range = i - beginIndexed - 1;
                        if (name.lastIndexOf('_', matchPosition - 1) == matchPosition - range - 1
                                || name.lastIndexOf('[', matchPosition - 1) == matchPosition - range - 1) {
                            if (isNumeric(other, beginIndexed + range, i)
                                    && isNumeric(name, matchPosition - range, matchPosition)) {
                                matchPosition = matchPosition - range - 2;
                                i = i - range - 1;
                                continue;
                            }
                        }
                    }
                    return false;
                } else if (o == '_') {
                    if (n != '.' && n != '-' && n != '_' && n != '"' && n != ']' && n != '[' && n != '/') {
                        return false;
                    } else if (n == '"' && other.length() - 1 == i) {
                        i = other.lastIndexOf("_", i - 1);
                        if (i == -1) {
                            return false;
                        }
                    }
                    // } else if (o != n) {
                } else if (toLowerCase(o) != toLowerCase(n)) {
                    return false;
                }
                matchPosition--;
            }

            return matchPosition <= 0;
        }
    }

    static final class EnvEntry implements Serializable {
        private static final long serialVersionUID = -8786927401082731020L;

        private final String name;
        private final String value;
        private Map<String, String> envEntries;

        EnvEntry(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }

        String get() {
            return envEntries == null ? value : null;
        }

        Map<String, String> getEntries() {
            return envEntries;
        }

        void add(String name, String value) {
            if (envEntries == null) {
                envEntries = new HashMap<>();
                envEntries.put(this.name, this.value);
            }
            envEntries.put(name, value);
        }
    }
}
