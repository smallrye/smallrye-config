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
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptySet;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.common.utils.StringUtil;

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
 * <p>
 *
 * Due to its high ordinality (<code>300</code>), the {@link EnvConfigSource} may be queried on every property name
 * lookup, just to not find a value and proceed to the next source. The conversion rules make such lookups inefficient
 * and unnecessary. In order to reduce lookups, this implementation provides the following mechanisms:
 * <p>
 * Keeps three forms of the environment variables names (original, lowercased and uppercased), to avoid having to
 * transform the property name on each lookup.
 * <br>
 * A dotted property name lookup is first matched to the existing names to avoid mapping and replacing rules in the
 * name. Property names with other special characters always require replacing rules:
 *
 * <ol>
 * <li>A lookup to <code>foo.bar</code> requires <code>FOO_BAR</code> converted to the dotted name <code>foo.bar</code></li>
 * <li>A lookup to <code>foo-bar</code> requires <code>FOO_BAR</code> no match, mapping applied</li>
 * <li>A lookup to <code>foo.baz</code> no match</li>
 * </ol>
 */
public class EnvConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -4525015934376795496L;

    private static final int DEFAULT_ORDINAL = 300;

    private final Map<String, String> properties;
    private final Set<String> names;

    protected EnvConfigSource() {
        this(DEFAULT_ORDINAL);
    }

    protected EnvConfigSource(final int ordinal) {
        this(getEnvProperties(), ordinal);
    }

    public EnvConfigSource(final Map<String, String> properties, final int ordinal) {
        super("EnvConfigSource", getEnvOrdinal(properties, ordinal));
        this.properties = new HashMap<>(properties.size() * 3);
        this.names = new HashSet<>(properties.size() * 2);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue());
            this.properties.put(entry.getKey().toLowerCase(), entry.getValue());
            this.properties.put(entry.getKey().toUpperCase(), entry.getValue());
            this.names.add(entry.getKey());
            this.names.add(StringUtil.toLowerCaseAndDotted(entry.getKey(), builder));
            builder.setLength(0);
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return this.names;
    }

    @Override
    public String getValue(final String propertyName) {
        return getValue(propertyName, getProperties(), names);
    }

    private static String getValue(final String propertyName, final Map<String, String> properties, final Set<String> names) {
        if (propertyName == null) {
            return null;
        }

        // exact match
        String value = properties.get(propertyName);
        if (value != null) {
            return value;
        }

        if (isDottedFormat(propertyName) && !names.contains(propertyName)) {
            return null;
        }

        return properties.get(replaceNonAlphanumericByUnderscores(propertyName));
    }

    /**
     * A new Map with the contents of System.getEnv. In the Windows implementation, the Map is an extension of
     * ProcessEnvironment. This causes issues with Graal and native mode, since ProcessEnvironment should not be
     * instantiated in the heap.
     */
    private static Map<String, String> getEnvProperties() {
        return doPrivileged((PrivilegedAction<Map<String, String>>) () -> new HashMap<>(System.getenv()));
    }

    private static int getEnvOrdinal(final Map<String, String> properties, final int ordinal) {
        String value = getValue(CONFIG_ORDINAL_KEY, properties, emptySet());
        if (value == null) {
            value = getValue(CONFIG_ORDINAL_KEY.toUpperCase(), properties, emptySet());
        }
        if (value != null) {
            return Converters.INTEGER_CONVERTER.convert(value);
        }
        return ordinal;
    }

    private static boolean isDottedFormat(final String propertyName) {
        for (int i = 0; i < propertyName.length(); i++) {
            char c = propertyName.charAt(i);
            if (!('a' <= c && c <= 'z') && !('0' <= c && c <= '9') && c != '.') {
                return false;
            }
        }
        return true;
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
}
