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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.smallrye.config.utils.ConfigSourceUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;
import static io.smallrye.config.utils.ConfigSourceUtil.CONFIG_ORDINAL_KEY;
import static io.smallrye.config.utils.ConfigSourceUtil.CONFIG_ORDINAL_100;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class PropertiesConfigSource implements ConfigSource, Serializable {

    private final Map<String, String> properties;
    private final String source;
    private final int ordinal;

    /**
     * Construct a new instance
     *
     * @param url a property file location
     * @throws IOException if an error occurred when reading from the input stream
     */
    public PropertiesConfigSource(URL url) throws IOException {
        this.source = url.toString();
        try (InputStream in = url.openStream()) {
            Properties p = new Properties();
            p.load(in);
            properties = ConfigSourceUtil.propertiesToMap(p);
        }
        this.ordinal = Integer.valueOf(properties.getOrDefault(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_100));
    }

    public PropertiesConfigSource(Properties properties, String source) {
        this.properties = ConfigSourceUtil.propertiesToMap(properties);
        this.source = source;
        this.ordinal = Integer.valueOf(properties.getProperty(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_100));
    }

    public PropertiesConfigSource(Map<String, String> properties, String source, int ordinal) {
        this.properties = new HashMap<>(properties);
        this.source = source;
        if (properties.containsKey(CONFIG_ORDINAL_KEY)) {
            this.ordinal = Integer.valueOf(properties.getOrDefault(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_100));
        } else {
            this.ordinal = ordinal;
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getValue(String s) {
        return properties.get(s);
    }

    @Override
    public String getName() {
        return "PropertiesConfigSource[source=" + source + "]";
    }

    @Override
    public String toString() {
        return getName();
    }
}
