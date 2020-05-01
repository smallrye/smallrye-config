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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class EnvConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -4525015934376795496L;

    EnvConfigSource() {
        super("EnvConfigSource", 300);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections
                .unmodifiableMap(AccessController.doPrivileged((PrivilegedAction<Map<String, String>>) System::getenv));
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(getProperties().keySet());
    }

    @Override
    public String getValue(String name) {
        if (name == null) {
            return null;
        }

        final Map<String, String> properties = getProperties();

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

    private String replaceNonAlphanumericByUnderscores(String name) {
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
}
