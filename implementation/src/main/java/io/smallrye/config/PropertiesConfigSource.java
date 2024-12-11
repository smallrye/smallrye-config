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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import io.smallrye.common.classloader.ClassPathUtils;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class PropertiesConfigSource extends MapBackedConfigValueConfigSource {
    private static final long serialVersionUID = 1866835565147832432L;

    public static final String NAME = "PropertiesConfigSource[source=%s]";
    public static final int ORDINAL = DEFAULT_ORDINAL;

    public PropertiesConfigSource(URL url) throws IOException {
        this(url, DEFAULT_ORDINAL);
    }

    public PropertiesConfigSource(URL url, int defaultOrdinal) throws IOException {
        this(url, String.format(NAME, url.toString()), defaultOrdinal);
    }

    private PropertiesConfigSource(URL url, String name, int defaultOrdinal) throws IOException {
        super(name, urlToConfigValueMap(url, name, defaultOrdinal), defaultOrdinal);
    }

    public PropertiesConfigSource(Map<String, String> properties, String name) {
        this(properties, name, DEFAULT_ORDINAL);
    }

    public PropertiesConfigSource(Properties properties, String name) {
        this(ConfigSourceUtil.propertiesToMap(properties), name, DEFAULT_ORDINAL);
    }

    public PropertiesConfigSource(Map<String, String> properties, String name, int defaultOrdinal) {
        this(String.format(NAME, name), properties, defaultOrdinal);
    }

    private PropertiesConfigSource(String name, Map<String, String> properties, int defaultOrdinal) {
        super(name,
                new ConfigValueMapStringView(properties, name, ConfigSourceUtil.getOrdinalFromMap(properties, defaultOrdinal)),
                defaultOrdinal);
    }

    public PropertiesConfigSource(Properties properties, String name, int defaultOrdinal) {
        this(ConfigSourceUtil.propertiesToMap(properties), name, defaultOrdinal);
    }

    public static Map<String, ConfigValue> urlToConfigValueMap(URL locationOfProperties, String name, int ordinal)
            throws IOException {
        ConfigValueProperties properties = new ConfigValueProperties(name, ordinal);
        ClassPathUtils.consumeStream(locationOfProperties, inputStream -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
                properties.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return properties;
    }
}
