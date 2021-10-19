/*
 * Copyright 2019 Red Hat, Inc.
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

package io.smallrye.config.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.classloader.ClassPathUtils;

/**
 * utilities and constants for {@link ConfigSource} implementations
 *
 * @author helloween
 */
public class ConfigSourceUtil {
    public static final String CONFIG_ORDINAL_KEY = "config_ordinal";
    public static final String CONFIG_ORDINAL_100 = "100";

    private ConfigSourceUtil() {
    }

    /**
     * convert {@link Properties} to {@link Map}
     *
     * @param properties {@link Properties} object
     * @return {@link Map} object
     */
    @SuppressWarnings("squid:S2445")
    public static Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> map = new HashMap<>();
        synchronized (properties) {
            for (Map.Entry<Object, Object> e : properties.entrySet()) {
                map.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        return map;
    }

    public static Map<String, String> urlToMap(URL locationOfProperties) throws IOException {
        final Properties properties = new Properties();
        ClassPathUtils.consumeStream(locationOfProperties, inputStream -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
                properties.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return propertiesToMap(properties);
    }

    /**
     * Get the ordinal value configured within the given map.
     *
     * @param map the map to query
     * @param defaultOrdinal the ordinal to return if the ordinal key is not specified
     * @return the ordinal value to use
     */
    public static int getOrdinalFromMap(Map<String, String> map, int defaultOrdinal) {
        String ordStr = map.get(CONFIG_ORDINAL_KEY);
        return ordStr == null ? defaultOrdinal : Integer.parseInt(ordStr);
    }
}
