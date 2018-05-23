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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class EnvConfigSource implements ConfigSource, Serializable {

    EnvConfigSource() {
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(System.getenv());
    }

    @Override
    public int getOrdinal() {
        return 300;
    }

    @Override
    public String getValue(String name) {
        if (name == null) {
            return null;
        }

        // exact match
        String value = System.getenv(name);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores
        name = name.replaceAll("[^a-zA-Z0-9_]", "_");
        value = System.getenv(name);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores and convert to uppercase
        return System.getenv(name.toUpperCase());
    }

    @Override
    public String getName() {
        return "EnvConfigSource";
    }
}
