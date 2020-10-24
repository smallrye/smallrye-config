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
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class PropertiesConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = 1866835565147832432L;

    private static final String NAME_PREFIX = "PropertiesConfigSource[source=";

    /**
     * Construct a new instance
     *
     * @param url a property file location
     * @throws IOException if an error occurred when reading from the input stream
     */
    public PropertiesConfigSource(URL url) throws IOException {
        super(NAME_PREFIX + url.toString() + "]", ConfigSourceUtil.urlToMap(url));
    }

    public PropertiesConfigSource(URL url, int ordinal) throws IOException {
        super(NAME_PREFIX + url.toString() + "]", ConfigSourceUtil.urlToMap(url), ordinal);
    }

    public PropertiesConfigSource(Properties properties, String source) {
        super(NAME_PREFIX + source + "]", ConfigSourceUtil.propertiesToMap(properties));
    }

    public PropertiesConfigSource(Map<String, String> properties, String source, int ordinal) {
        super(NAME_PREFIX + source + "]", properties, ordinal);
    }
}
