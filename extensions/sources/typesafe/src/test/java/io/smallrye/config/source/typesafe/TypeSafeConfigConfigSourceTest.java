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
package io.smallrye.config.source.typesafe;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TypeSafeConfigConfigSourceTest {

    private TypeSafeConfigConfigSource configSource;
    private Config testConfig;

    @BeforeEach
    public void init() {
        testConfig = ConfigFactory.parseResources(
                TypeSafeConfigConfigSourceTest.class.getClassLoader(),
                "TestConfig.conf");
        configSource = new TypeSafeConfigConfigSource(testConfig);
    }
    
    @Test
    void testGetProperties_empty() {
        TypeSafeConfigConfigSource emptyConfigSource = new TypeSafeConfigConfigSource(mock(Config.class));
        assertTrue(emptyConfigSource.getProperties().isEmpty());
    }

    @Test
    void testGetProperties_one() {
        // As properties are added to TestConfig.conf this will need to be incremented to match
        val NUM_EXPECTED_PROPERTIES = 2;
        assertEquals(NUM_EXPECTED_PROPERTIES, configSource.getProperties().size());
    }

    @Test
    void testGetValue_null() {
        assertNull(configSource.getValue("thisKeyDoestNotExist"));
    }

    @Test
    void testGetValue() {
        assertEquals("hello", configSource.getValue("test"));
    }


    @Test
    void testGetValue_exception() {
        // Retrieving a value that is not stringable from the configuration will result in an exception from TypeSafe
        // config which should return null through this interface.
        assertNull(configSource.getValue("config_object"));
    }

}
