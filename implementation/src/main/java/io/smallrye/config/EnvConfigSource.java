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

import static io.smallrye.config.EnvConfigSource.ResizableIntArray.MINUS_ONE;
import static io.smallrye.config.EnvConfigSource.ResizableIntArray.ZERO;
import static io.smallrye.config.ProfileConfigSourceInterceptor.activeName;
import static io.smallrye.config.common.utils.ConfigSourceUtil.CONFIG_ORDINAL_KEY;
import static io.smallrye.config.common.utils.ConfigSourceUtil.hasProfiledName;
import static io.smallrye.config.common.utils.StringUtil.isAsciiLetterOrDigit;
import static io.smallrye.config.common.utils.StringUtil.isNumericEquals;
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static io.smallrye.config.common.utils.StringUtil.toLowerCaseAndDotted;
import static java.lang.Character.toLowerCase;
import static java.security.AccessController.doPrivileged;

import java.io.Serial;
import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.common.utils.StringUtil;

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
    @Serial
    private static final long serialVersionUID = -4525015934376795496L;

    public static final String NAME = "EnvConfigSource";
    public static final int ORDINAL = 300;

    private final EnvVars envVars;
    private final boolean hasProfiledName;

    protected EnvConfigSource() {
        this(ORDINAL);
    }

    protected EnvConfigSource(final int ordinal) {
        this(getEnvProperties(), ordinal);
    }

    public EnvConfigSource(final Map<String, String> properties, final int ordinal) {
        super(NAME, getEnvOrdinal(properties, ordinal));
        this.envVars = new EnvVars(properties);
        this.hasProfiledName = hasProfiledName(getPropertyNames());
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
        if (!hasProfiledName && !propertyName.isEmpty() && propertyName.charAt(0) == '%') {
            return null;
        }
        return envVars.get(propertyName);
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

    void matchEnvWithProperties(final List<Map.Entry<String, Supplier<Iterator<String>>>> properties,
            final List<String> profiles) {
        for (String envName : new ArrayList<>(envVars.getNames())) {
            // Convert to the active key, since sources do not know which keys are active based on the profile
            String activeEnvName = activeName(envName, profiles);
            match: for (Map.Entry<String, Supplier<Iterator<String>>> property : properties) {
                String prefix = property.getKey();
                if (StringUtil.isInPath(prefix, activeEnvName)) {
                    Iterator<String> names = property.getValue().get();
                    while (names.hasNext()) {
                        if (matchEnvWithProperty(prefix, names.next(), envName, activeEnvName)) {
                            break match;
                        }
                    }
                }
            }
        }
    }

    private boolean matchEnvWithProperty(final String prefix, final String property, final String envName,
            final String activeEnvName) {
        int[] prefixDashes = indexOfDashes(prefix, 0, prefix.length(), activeEnvName, 0, prefix.length());
        if (prefixDashes[0] == -1) {
            return false;
        }

        int[] nameDashes = indexOfDashes(
                property, 0, property.length(),
                activeEnvName, prefix.isEmpty() ? 0 : prefix.length() + 1,
                prefix.isEmpty() ? activeEnvName.length() : activeEnvName.length() - prefix.length() - 1);
        if (nameDashes[0] == -1) {
            return false;
        }

        if (prefixDashes[0] == 0 && nameDashes[0] == 0) {
            // Properties are equals, but may contain Map key uppercases, so replace
            if (EnvName.equals(activeEnvName, property)) {
                envVars.getNames().remove(envName);
                envVars.getNames().add(property);
            }
            return false;
        }

        StringBuilder sb = new StringBuilder(activeEnvName);
        if (prefixDashes[0] != 0) {
            for (int dash : prefixDashes) {
                sb.setCharAt(dash, '-');
            }
        }
        if (nameDashes[0] != 0) {
            for (int dash : nameDashes) {
                sb.setCharAt(dash, '-');
            }
        }
        String dashedEnvName = sb.toString();

        // Properties are equals, but may contain Map key uppercases, so replace
        if (EnvName.equals(property, dashedEnvName)) {
            envVars.getNames().remove(envName);
            envVars.getNames().add(property);
            return true;
        }

        if (!activeEnvName.contentEquals(sb)) {
            envVars.getNames().remove(envName);
            envVars.getNames().add(dashedEnvName);
            return true;
        }
        return false;
    }

    /**
     * Find and returns all indexes of an Environment Variable name that match a <code>-</code> (dash), in its
     * equivalent property name representation name. For example:
     * <ul>
     * <li><code>foo-bar</code>> matches <code>FOO_BAR</code> with a dash at index 3</li>
     * <li><code>foo-bar</code>> matches <code>foo.bar</code> with a dash at index 3</li>
     * <li><code>foo.bar</code>> matches <code>FOO_BAR</code> with no dashes</li>
     * <li><code>foo.bar</code>> does not match <code>BAR_BAR</code></li>
     * <li><code>*.foo-bar</code>> does not match <code>BAZ_FOO_BAR</code> with a dash at index 7</li>
     * </ul>
     *
     * @param property the property name.
     * @param envProperty the Environment Variable name.
     * @return an array of int of indexes from the Environment Variable name that can match a <code>-</code> (dash);
     *         an array of int with the int element <code>0</code> if properties match but no dashes are found;
     *         an array of int with the int element <code>-1</code> if properties don't match.
     */
    static int[] indexOfDashes(final String property, final int offset, final int len,
            final String envProperty, final int eoffset, final int elen) {
        if (property.isEmpty()) {
            return ZERO.get();
        }

        ResizableIntArray dashesPosition = ZERO;
        int matchPosition = eoffset + elen - 1;
        for (int i = offset + len - 1; i >= offset; i--) {
            if (matchPosition == -1) {
                return MINUS_ONE.get();
            }

            char c = property.charAt(i);
            if (c == '.' || c == '-') {
                char p = envProperty.charAt(matchPosition);
                if (p != '.' && p != '-') { // a property coming from env can either be . or -
                    return MINUS_ONE.get();
                }
                if (c == '-') {
                    if (dashesPosition == ZERO) {
                        dashesPosition = new ResizableIntArray(1);
                    } else {
                        dashesPosition.ensureCapacity(dashesPosition.get().length + 1);
                    }
                    dashesPosition.get()[dashesPosition.get().length - 1] = matchPosition;
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
            } else if (toLowerCase(c) != toLowerCase(envProperty.charAt(matchPosition))) {
                return MINUS_ONE.get();
            } else {
                matchPosition--;
            }
        }
        if (matchPosition >= eoffset) {
            return MINUS_ONE.get();
        }

        return dashesPosition.get();
    }

    static class ResizableIntArray {
        static final ResizableIntArray ZERO = with(0);
        static final ResizableIntArray MINUS_ONE = with(-1);

        private int[] array;

        ResizableIntArray(int initialSize) {
            this.array = new int[initialSize];
        }

        int[] get() {
            return array;
        }

        void ensureCapacity(int capacity) {
            this.array = Arrays.copyOfRange(this.array, 0, capacity);
        }

        private static ResizableIntArray with(int i) {
            ResizableIntArray minusOne = new ResizableIntArray(1) {
                @Override
                void ensureCapacity(final int capacity) {
                    throw new UnsupportedOperationException();
                }
            };
            minusOne.array[0] = i;
            return minusOne;
        }
    }

    @Serial
    Object writeReplace() {
        return new Ser();
    }

    static final class Ser implements Serializable {
        @Serial
        private static final long serialVersionUID = 6812312718645271331L;

        @Serial
        Object readResolve() {
            return new EnvConfigSource();
        }
    }

    static final class EnvVars implements Serializable {
        @Serial
        private static final long serialVersionUID = -56318356411229247L;

        private final Map<EnvName, EnvEntry> env;
        private final Set<String> names;

        public EnvVars(final Map<String, String> properties) {
            this.env = new HashMap<>(properties.size());
            this.names = new HashSet<>(properties.size() * 2);
            properties.forEach(new BiConsumer<>() {
                @Override
                public void accept(String key, String value) {
                    EnvName envName = new EnvName(key);
                    EnvEntry envEntry = env.get(envName);
                    if (envEntry == null) {
                        env.put(envName, new EnvEntry(key, value));
                    } else {
                        envEntry.add(key, value);
                    }
                    EnvVars.this.names.add(key);
                    EnvVars.this.names.add(toLowerCaseAndDotted(key));
                }
            });
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
        @Serial
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

            // we cannot check for length equals because a " is represented by __

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
                    } else if (n == '_' && name.length() - 1 == matchPosition && name.length() > 1) {
                        if (name.charAt(matchPosition - 1) == '_') {
                            matchPosition--;
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
                            if (isNumericEquals(name, matchPosition - range, range, other, beginIndexed + range, range)) {
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
                    } else if (n == '"' && other.length() - 1 == i && other.length() > 1) {
                        if (other.charAt(i - 1) == '_') {
                            i--;
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
        @Serial
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
