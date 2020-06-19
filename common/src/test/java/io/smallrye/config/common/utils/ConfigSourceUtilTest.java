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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 */
public class ConfigSourceUtilTest {

    @Test
    public void propertiesToMap() {
        Properties properties = new Properties();
        properties.put("my.key1", "my.value1");
        properties.put("my.key2", "my.value2");
        properties.put("my.key3", 2);

        Map<String, String> map = ConfigSourceUtil.propertiesToMap(properties);
        assertEquals("my.value1", map.get("my.key1"));
        assertEquals("my.value2", map.get("my.key2"));
        assertEquals("2", map.get("my.key3"));
    }
}
