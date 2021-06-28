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
import static java.util.Collections.unmodifiableMap;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class EnvConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = -4525015934376795496L;

    private static final int DEFAULT_ORDINAL = 300;

    protected EnvConfigSource() {
        this(DEFAULT_ORDINAL);
    }

    protected EnvConfigSource(final int ordinal) {
        this(getEnvProperties(), ordinal);
    }

    public EnvConfigSource(final Map<String, String> propertyMap, final int ordinal) {
        super("EnvConfigSource", propertyMap, getEnvOrdinal(propertyMap, ordinal));
    }

    @Override
    public String getValue(final String propertyName) {
        return getValue(propertyName, getProperties());
    }

    private static String getValue(final String name, final Map<String, String> properties) {
        if (name == null) {
            return null;
        }

        // exact match
        String value = properties.get(name);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores
        String sanitizedName = replaceNonAlphanumericByUnderscores(name);
        value = properties.get(sanitizedName);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores and convert to uppercase
        return properties.get(sanitizedName.toUpperCase());
    }

    /**
     * A new Map with the contents of System.getEnv. In the Windows implementation, the Map is an extension of
     * ProcessEnvironment. This causes issues with Graal and native mode, since ProcessEnvironment should not be
     * instantiated in the heap.
     */
    private static Map<String, String> getEnvProperties() {
        return unmodifiableMap(doPrivileged((PrivilegedAction<Map<String, String>>) () -> new HashMap<>(System.getenv())));
    }

    private static int getEnvOrdinal(final Map<String, String> properties, final int ordinal) {
        final String value = getValue(CONFIG_ORDINAL_KEY, properties);
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
}
