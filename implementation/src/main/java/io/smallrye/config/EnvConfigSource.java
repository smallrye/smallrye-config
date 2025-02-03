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
import static io.smallrye.config.common.utils.StringUtil.isAsciiLetterOrDigit;
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
import java.util.function.BiConsumer;

import io.smallrye.config.common.AbstractConfigSource;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigSource} to access Environment Variables.
 * <p>
 *
 * A property name matches to an environment variable with the following rules:
 *
 * <ol>
 * <li>Match alphanumeric characters (any case)</li>
 * <li>Match non-alphanumeric characters with <code>_</code></li>
 * <li>Closing quotes in the end of a property name require a double <code>_</code></li>
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

    public static final String NAME = "EnvConfigSource";
    public static final int ORDINAL = 300;

    private final EnvVars envVars;
    private EnvName reusableEnvName;

    protected EnvConfigSource() {
        this(ORDINAL);
    }

    protected EnvConfigSource(final int ordinal) {
        this(getEnvProperties(), ordinal);
    }

    public EnvConfigSource(final Map<String, String> properties, final int ordinal) {
        super(NAME, getEnvOrdinal(properties, ordinal));
        this.envVars = new EnvVars(properties);
        this.reusableEnvName = null;
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        envVars.getEnv().forEach(new BiConsumer<>() {
            @Override
            public void accept(EnvName key, EnvEntry entryValue) {
                if (entryValue.getEntries() != null) {
                    properties.putAll(entryValue.getEntries());
                } else {
                    properties.put(entryValue.getName(), entryValue.getValue());
                }
            }
        });
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
        EnvName envName = reusableEnvName;
        if (envName == null) {
            envName = new EnvName(propertyName);
            this.reusableEnvName = envName;
        } else {
            envName.setName(propertyName);
        }
        try {
            return envVars.getEnv().containsKey(envName);
        } finally {
            envName.reset();
        }
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
        private EnvName reusableEnvName;

        public EnvVars(final Map<String, String> properties) {
            this.env = new HashMap<>(properties.size());
            this.names = new HashSet<>(properties.size() * 2);
            properties.forEach(new BiConsumer<>() {
                @Override
                public void accept(String key, String value) {
                    EnvName envName = tmpEnvNameWith(key);
                    try {
                        EnvEntry envEntry = env.get(envName);
                        if (envEntry == null) {
                            env.put(envName, new EnvEntry(key, value));
                            // it means envName cannot be reused
                            envName = null;
                        } else {
                            envEntry.add(key, value);
                        }
                        EnvVars.this.names.add(key);
                        EnvVars.this.names.add(toLowerCaseAndDotted(key));
                    } finally {
                        if (envName != null) {
                            envName.reset();
                        } else {
                            reusableEnvName = null;
                        }
                    }
                }
            });
        }

        public String get(final String propertyName) {
            EnvName tmpEnvName = tmpEnvNameWith(propertyName);
            try {
                EnvEntry envEntry = env.get(tmpEnvName);
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
            } finally {
                tmpEnvName.reset();
            }
        }

        private EnvName tmpEnvNameWith(String propertyName) {
            EnvName envName = reusableEnvName;
            if (envName == null) {
                envName = new EnvName(propertyName);
                this.reusableEnvName = envName;
            } else {
                envName.setName(propertyName);
            }
            return envName;
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

        private String name;
        private int hash;
        private boolean hashIsZero;

        public EnvName(final String name) {
            assert name != null;
            this.name = name;
            this.hashIsZero = false;
            this.hash = 0;
        }

        public void reset() {
            this.name = null;
            this.hashIsZero = false;
            this.hash = 0;
        }

        public void setName(final String name) {
            this.name = name;
            this.hashIsZero = false;
            this.hash = 0;
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
            int h = hash;
            if (h == 0 && !hashIsZero) {
                int length = name.length();
                if (length >= 2) {
                    if (name.charAt(length - 1) == '_' && name.charAt(length - 2) == '_') {
                        length = length - 1;
                    }
                }

                for (int i = 0; i < length; i++) {
                    char c = name.charAt(i);
                    if (i == 0 && length > 1) {
                        // The first '%' or '_' is meaninful because it represents a profiled property name
                        if ((c == '%' || c == '_') && isAsciiLetterOrDigit(name.charAt(i + 1))) {
                            h = 31 * h + 31;
                            continue;
                        }
                    }

                    if (isAsciiLetterOrDigit(c)) {
                        h = 31 * h + toLowerCase(c);
                    }
                }
                if (h == 0) {
                    hashIsZero = true;
                } else {
                    hash = h;
                }
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

            char n;
            char o;

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
                    if (isAsciiLetterOrDigit(n)) {
                        return false;
                    } else if (n == '"' && other.length() - 1 == i) {
                        i = other.lastIndexOf("_", i - 1);
                        if (i == -1) {
                            return false;
                        }
                    }
                } else if (!isAsciiLetterOrDigit(o)) {
                    if (o != n && n != '_') {
                        return false;

                    }
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
