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

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
class EnvConfigSourceTest {
    @Test
    void conversionOfEnvVariableNames() {
        String envProp = System.getenv("SMALLRYE_MP_CONFIG_PROP");
        assertNotNull(envProp);

        ConfigSource cs = new EnvConfigSource();
        assertEquals(envProp, cs.getValue("SMALLRYE_MP_CONFIG_PROP"));
        // the config source returns only the name of the actual env variable
        assertTrue(cs.getPropertyNames().contains("SMALLRYE_MP_CONFIG_PROP"));

        assertEquals(envProp, cs.getValue("smallrye_mp_config_prop"));
        assertFalse(cs.getPropertyNames().contains("smallrye_mp_config_prop"));

        assertEquals(envProp, cs.getValue("smallrye.mp.config.prop"));
        assertFalse(cs.getPropertyNames().contains("smallrye.mp.config.prop"));

        assertEquals(envProp, cs.getValue("SMALLRYE.MP.CONFIG.PROP"));
        assertFalse(cs.getPropertyNames().contains("SMALLRYE.MP.CONFIG.PROP"));

        assertEquals(envProp, cs.getValue("smallrye-mp-config-prop"));
        assertFalse(cs.getPropertyNames().contains("smallrye-mp-config-prop"));

        assertEquals(envProp, cs.getValue("SMALLRYE-MP-CONFIG-PROP"));
        assertFalse(cs.getPropertyNames().contains("SMALLRYE-MP-CONFIG-PROP"));

        assertEquals("1234", cs.getValue("smallrye_mp_config_prop_lower"));
        assertTrue(cs.getPropertyNames().contains("smallrye_mp_config_prop_lower"));
    }

    @Test
    void profileEnvVariables() {
        assertNotNull(System.getenv("SMALLRYE_MP_CONFIG_PROP"));
        assertNotNull(System.getenv("_ENV_SMALLRYE_MP_CONFIG_PROP"));

        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().withProfile("env").build();

        assertEquals("5678", config.getRawValue("smallrye.mp.config.prop"));
    }

    @Test
    void empty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().build();
        assertThrows(NoSuchElementException.class, () -> config.getValue("SMALLRYE_MP_CONFIG_EMPTY", String.class));
        assertTrue(
                stream(config.getPropertyNames().spliterator(), false).collect(toList()).contains("SMALLRYE_MP_CONFIG_EMPTY"));

        ConfigSource envConfigSource = StreamSupport.stream(config.getConfigSources().spliterator(), false)
                .filter(configSource -> configSource.getName().equals("EnvConfigSource"))
                .findFirst()
                .get();

        assertEquals("", envConfigSource.getValue("SMALLRYE_MP_CONFIG_EMPTY"));
    }

    @Test
    void ordinal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(new EnvConfigSource()).build();
        ConfigSource configSource = config.getConfigSources().iterator().next();

        assertTrue(configSource instanceof EnvConfigSource);
        assertEquals(configSource.getOrdinal(), 301);
    }

    @Test
    void indexed() {
        Map<String, String> env = new HashMap<String, String>() {
            {
                put("INDEXED_0_", "foo");
                put("INDEXED_0__PROP", "bar");
                put("INDEXED_0__PROPS_0_", "0");
                put("INDEXED_0__PROPS_1_", "1");
            }
        };

        EnvConfigSource envConfigSource = new EnvConfigSource(env, 300);
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(envConfigSource)
                .build();

        assertTrue(config.getValues("indexed", String.class, ArrayList::new).contains("foo"));
        assertTrue(config.getValues("indexed[0].props", String.class, ArrayList::new).contains("0"));
        assertTrue(config.getValues("indexed[0].props", String.class, ArrayList::new).contains("1"));
    }
}
