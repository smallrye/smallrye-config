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
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.config.common.AbstractConfigSource;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class EnvConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -4525015934376795496L;
    private static final int DEFAULT_ORDINAL = 300;

    private final Map<String, String> cache = new ConcurrentHashMap<>(); //the regex match is expensive

    protected EnvConfigSource() {
        super("EnvConfigSource", getEnvOrdinal());
    }

    @Override
    public Map<String, String> getProperties() {
        return getEnvProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return unmodifiableSet(getProperties().keySet());
    }

    @Override
    public String getValue(String name) {
        if (name == null) {
            return null;
        }

        String cachedValue = cache.get(name);
        if (cachedValue != null) {
            return cachedValue;
        }

        final Map<String, String> properties = getProperties();

        // exact match
        String value = properties.get(name);
        if (value != null) {
            cache.put(name, value);
            return value;
        }

        // replace non-alphanumeric characters by underscores
        String sanitizedName = replaceNonAlphanumericByUnderscores(name);

        value = properties.get(sanitizedName);
        if (value != null) {
            cache.put(name, value);
            return value;
        }

        // replace non-alphanumeric characters by underscores and convert to uppercase
        value = properties.get(sanitizedName.toUpperCase());
        if (value != null) {
            cache.put(name, value);
            return value;
        }

        return null;
    }

    private static String replaceNonAlphanumericByUnderscores(String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if ('a' <= c && c <= 'z' ||
                    'A' <= c && c <= 'Z' ||
                    '0' <= c && c <= '9') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static Map<String, String> getEnvProperties() {
        return unmodifiableMap(doPrivileged((PrivilegedAction<Map<String, String>>) System::getenv));
    }

    /**
     * TODO
     * Ideally, this should use {@link EnvConfigSource#getValue(String)} directly, so we don't duplicate the property
     * names logic, but we need this method to be static.
     *
     * We do require a bigger change to rewrite {@link EnvConfigSource#getValue(String)} as static and still cache
     * values in each separate instance.
     *
     * @return the {@link EnvConfigSource} ordinal.
     */
    private static int getEnvOrdinal() {
        Map<String, String> envProperties = getEnvProperties();
        String ordStr = envProperties.get(CONFIG_ORDINAL_KEY);
        if (ordStr != null) {
            return Converters.INTEGER_CONVERTER.convert(ordStr);
        }

        String sanitazedOrdinalKey = replaceNonAlphanumericByUnderscores(CONFIG_ORDINAL_KEY);
        ordStr = envProperties.get(sanitazedOrdinalKey);
        if (ordStr != null) {
            return Converters.INTEGER_CONVERTER.convert(ordStr);
        }

        ordStr = envProperties.get(sanitazedOrdinalKey.toUpperCase());
        if (ordStr != null) {
            return Converters.INTEGER_CONVERTER.convert(ordStr);
        }

        return DEFAULT_ORDINAL;
    }
}
